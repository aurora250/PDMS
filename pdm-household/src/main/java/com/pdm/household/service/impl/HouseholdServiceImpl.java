package com.pdm.household.service.impl;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.common.core.util.PermitNumberGenerator;
import com.pdm.household.entity.*;
import com.pdm.household.mapper.*;
import com.pdm.household.service.HouseholdService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HouseholdServiceImpl implements HouseholdService {

    private final HouseholdRegisterMapper bookMapper;
    private final HouseholdBusinessRequestMapper businessMapper;
    private final HouseholdMigrationRequestMapper migrationMapper;
    private final ApprovalPermitMapper approvalPermitMapper;
    private final MigrationPermitMapper migrationPermitMapper;
    private final AreaMapper areaMapper;
    private final ResidentMapper residentMapper;

    @Override
    @Transactional
    public HouseholdRegister applyBook(HouseholdRegister book) {
        if (book.getHouseholdBookNo() == null) {
            book.setHouseholdBookNo("HB" + IdUtil.fastSimpleUUID().substring(0, 20));
        }
        book.setEstablishDate(LocalDate.now());
        book.setStatus("审批中");
        bookMapper.insert(book);
        return book;
    }

    @Override
    @Transactional
    public HouseholdRegister reissueBook(String bookNo) {
        HouseholdRegister book = bookMapper.selectByBookNo(bookNo);
        if (book == null)
            throw new BusinessException(ErrorCode.HOUSEHOLD_BOOK_NOT_FOUND);
        // In production, generate new book number and copy data
        return book;
    }

    @Override
    @Transactional
    public HouseholdRegister renewBook(String bookNo) {
        return reissueBook(bookNo);
    }

    @Override
    public Map<String, Object> getBookByResident(String residentUuid) {
        HouseholdRegister book = bookMapper.selectByResidentUuid(residentUuid);
        if (book == null) {
            return null;
        }

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("householdBookNo", book.getHouseholdBookNo());
        result.put("householderUuid", book.getHouseholderUuid());
        result.put("establishDate", book.getEstablishDate());
        result.put("hukouAddress", book.getHukouAddress());
        result.put("hukouAreaId", book.getHukouAreaId());
        result.put("status", book.getStatus());
        result.put("memberUuidList", book.getMemberUuidList());

        // 批量填充户主姓名
        if (book.getHouseholderUuid() != null) {
            List<Map<String, Object>> names = residentMapper
                    .batchGetNames(Collections.singletonList(book.getHouseholderUuid()));
            if (!names.isEmpty()) {
                result.put("householderName", names.get(0).get("name"));
            }
        }

        // 解析成员UUID列表并批量填充姓名
        if (book.getMemberUuidList() != null && !book.getMemberUuidList().isEmpty()) {
            List<String> memberUuids = Arrays.asList(book.getMemberUuidList().split(","));
            List<String> cleanUuids = memberUuids.stream().map(String::trim).filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            if (!cleanUuids.isEmpty()) {
                List<Map<String, Object>> memberNames = residentMapper.batchGetNames(cleanUuids);
                result.put("members", memberNames);
            } else {
                result.put("members", Collections.emptyList());
            }
        } else {
            result.put("members", Collections.emptyList());
        }

        return result;
    }

    /**
     * 审批流: 采集员上报 → 民警审核 → 市局（仅特殊事项）
     *         街道办只附加材料，不参与审批
     * 状态机:
     *   一般事项: 审批中 → [民警通过] → 已批准
     *   特殊事项: 审批中 → [民警提交市局] → 市局审批中 → [市局通过] → 已批准
     *   驳回:     审批中/市局审批中 → [驳回] → 已驳回
     */
    @Override
    @Transactional
    public HouseholdBusinessRequest submitBusiness(HouseholdBusinessRequest request) {
        if (request.getHandleDate() == null) {
            request.setHandleDate(LocalDate.now());
        }
        request.setStatus("审批中");
        businessMapper.insert(request);
        return request;
    }

    @Override
    @Transactional
    public HouseholdBusinessRequest approveBusiness(Long rid, String action, String handlerUuid, String rejectReason) {
        HouseholdBusinessRequest req = businessMapper.selectById(rid);
        if (req == null)
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);

        String current = req.getStatus();
        String next;

        switch (action) {
            case "通过":
                if ("审批中".equals(current)) {
                    next = "已批准";           // 民警直接通过（一般事项）
                } else if ("市局审批中".equals(current)) {
                    next = "已批准";           // 市局最终通过
                } else {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "当前状态不允许审批通过: " + current);
                }
                break;
            case "提交市局":
                if (!"审批中".equals(current)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "仅审批中状态可提交市局: " + current);
                }
                next = "市局审批中";
                break;
            case "驳回":
                if ("已批准".equals(current) || "已驳回".equals(current)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "当前状态不允许驳回: " + current);
                }
                next = "已驳回";
                break;
            default:
                throw new BusinessException(ErrorCode.PARAM_ERROR, "未知审批操作: " + action);
        }

        req.setStatus(next);
        req.setHandlerUuid(handlerUuid);
        if (rejectReason != null)
            req.setRejectReason(rejectReason);
        businessMapper.updateById(req);
        return req;
    }

    @Override
    @Transactional
    public HouseholdBusinessRequest attachBusinessMaterial(Long rid, String attachmentPath, String remark) {
        HouseholdBusinessRequest req = businessMapper.selectById(rid);
        if (req == null)
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "业务申请不存在");

        // 将新材料路径追加到现有 attachment 字段
        String existing = req.getAttachment();
        if (existing == null || existing.isEmpty()) {
            req.setAttachment(attachmentPath);
        } else {
            req.setAttachment(existing + ";" + attachmentPath);
        }
        // 备注记录附加材料说明
        if (remark != null && !remark.isEmpty()) {
            String existingRemark = req.getRemark();
            if (existingRemark == null || existingRemark.isEmpty()) {
                req.setRemark("[附加材料] " + remark);
            } else {
                req.setRemark(existingRemark + " | [附加材料] " + remark);
            }
        }
        businessMapper.updateById(req);
        return req;
    }

    @Override
    @Transactional
    public HouseholdMigrationRequest attachMigrationMaterial(Long rid, String attachmentPath, String remark) {
        HouseholdMigrationRequest req = migrationMapper.selectById(rid);
        if (req == null)
            throw new BusinessException(ErrorCode.MIGRATION_NOT_FOUND);

        String existing = req.getAttachment();
        if (existing == null || existing.isEmpty()) {
            req.setAttachment(attachmentPath);
        } else {
            req.setAttachment(existing + ";" + attachmentPath);
        }
        if (remark != null && !remark.isEmpty()) {
            String existingRemark = req.getRemark();
            if (existingRemark == null || existingRemark.isEmpty()) {
                req.setRemark("[附加材料] " + remark);
            } else {
                req.setRemark(existingRemark + " | [附加材料] " + remark);
            }
        }
        migrationMapper.updateById(req);
        return req;
    }

    /**
     * 迁移审批状态机:
     *   准迁证审批中 → [民警通过] → 准迁证已批准（自动签发准迁证）
     *   准迁证已批准 → [民警通过] → 迁移证已批准（自动签发迁移证）
     *   迁移证已批准 → [民警通过] → 迁移审批通过
     *   任意非终态 → [驳回] → 对应阶段驳回
     */
    private static final java.util.Map<String, String> MIGRATION_APPROVAL_NEXT = java.util.Map.of(
            "准迁证审批中", "准迁证已批准",
            "准迁证已批准", "迁移证已批准",
            "迁移证已批准", "迁移审批通过"
    );

    @Override
    @Transactional
    public HouseholdMigrationRequest submitMigration(HouseholdMigrationRequest request) {
        if (request.getHandleDate() == null) {
            request.setHandleDate(LocalDate.now());
        }
        request.setStatus("准迁证审批中");
        migrationMapper.insert(request);
        return request;
    }

    @Override
    @Transactional
    public HouseholdMigrationRequest approveMigration(Long rid, String action, String handlerUuid,
            String rejectReason) {
        HouseholdMigrationRequest req = migrationMapper.selectById(rid);
        if (req == null)
            throw new BusinessException(ErrorCode.MIGRATION_NOT_FOUND);

        String current = req.getStatus();

        if ("驳回".equals(action)) {
            if (current.contains("驳回") || "迁移审批通过".equals(current)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许驳回: " + current);
            }
            // 准迁证审批中→准迁证审批驳回, 准迁证已批准/迁移证已批准→迁移审批驳回
            if (current.startsWith("准迁证")) {
                req.setStatus("准迁证审批驳回");
            } else {
                req.setStatus("迁移审批驳回");
            }
        } else {
            // 通过：按状态机流转
            String nextStatus = MIGRATION_APPROVAL_NEXT.get(current);
            if (nextStatus == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "当前状态不允许审批通过: " + current);
            }
            req.setStatus(nextStatus);

            // 自动签发证件并回填证号
            if ("准迁证已批准".equals(nextStatus) && req.getApprovalPermitNo() == null) {
                ApprovalPermit approvalPermit = new ApprovalPermit();
                approvalPermit.setUuid(req.getApplicantUuid());
                approvalPermit.setPermitNo(PermitNumberGenerator.approvalPermit(null,
                        LocalDate.now(), System.currentTimeMillis() % 1_000_000));
                approvalPermit.setIssueDate(LocalDate.now());
                approvalPermit.setStatus("有效");
                approvalPermitMapper.insert(approvalPermit);
                req.setApprovalPermitNo(approvalPermit.getPermitNo());
            }
            if ("迁移证已批准".equals(nextStatus) && req.getMigrationPermitNo() == null) {
                MigrationPermit migrationPermit = new MigrationPermit();
                migrationPermit.setUuid(req.getApplicantUuid());
                migrationPermit.setPermitNo(PermitNumberGenerator.migrationPermit(null,
                        LocalDate.now(), System.currentTimeMillis() % 1_000_000));
                migrationPermit.setIssueDate(LocalDate.now());
                migrationPermit.setStatus("有效");
                migrationPermitMapper.insert(migrationPermit);
                req.setMigrationPermitNo(migrationPermit.getPermitNo());
            }
        }

        req.setHandlerUuid(handlerUuid);
        if (rejectReason != null)
            req.setRejectReason(rejectReason);
        migrationMapper.updateById(req);
        return req;
    }

    @Override
    public List<HouseholdMigrationRequest> getMigrationTrace(String residentUuid) {
        return migrationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HouseholdMigrationRequest>()
                        .eq(HouseholdMigrationRequest::getApplicantUuid, residentUuid)
                        .orderByDesc(HouseholdMigrationRequest::getCreateTime));
    }

    @Override
    @Transactional
    public ApprovalPermit issueApprovalPermit(ApprovalPermit permit) {
        if (permit.getPermitNo() == null)
            permit.setPermitNo(PermitNumberGenerator.approvalPermit(null, LocalDate.now(),
                    System.currentTimeMillis() % 1_000_000));
        permit.setIssueDate(LocalDate.now());
        permit.setStatus("有效");
        approvalPermitMapper.insert(permit);
        return permit;
    }

    @Override
    @Transactional
    public MigrationPermit issueMigrationPermit(MigrationPermit permit) {
        if (permit.getPermitNo() == null)
            permit.setPermitNo(PermitNumberGenerator.migrationPermit(null, LocalDate.now(),
                    System.currentTimeMillis() % 1_000_000));
        permit.setIssueDate(LocalDate.now());
        permit.setStatus("有效");
        migrationPermitMapper.insert(permit);
        return permit;
    }

    @Override
    @Transactional
    public ApprovalPermit voidApprovalPermit(Long id) {
        ApprovalPermit permit = approvalPermitMapper.selectById(id);
        if (permit == null)
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "证件不存在");
        permit.setStatus("作废");
        approvalPermitMapper.updateById(permit);
        return permit;
    }

    @Override
    @Transactional
    public MigrationPermit voidMigrationPermit(Long id) {
        MigrationPermit permit = migrationPermitMapper.selectById(id);
        if (permit == null)
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "证件不存在");
        permit.setStatus("作废");
        migrationPermitMapper.updateById(permit);
        return permit;
    }

    @Override
    public List<Area> getAreaTree() {
        return areaMapper.selectByLevel("省");
    }

    @Override
    public List<Area> getAreasByParent(Long parentId) {
        return areaMapper.selectByParentId(parentId);
    }

    @Override
    public List<Area> getAreaAncestors(Long areaId) {
        return areaMapper.selectAncestors(areaId);
    }

    @Override
    public String getAreaPath(Long areaId) {
        return areaMapper.selectAreaPath(areaId);
    }

    @Override
    public List<Area> getAllAreas() {
        return areaMapper.selectAll();
    }
}
