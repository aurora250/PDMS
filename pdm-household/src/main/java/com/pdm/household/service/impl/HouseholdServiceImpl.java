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
            List<Map<String, Object>> names = residentMapper.batchGetNames(
                Collections.singletonList(book.getHouseholderUuid()));
            if (!names.isEmpty()) {
                result.put("householderName", names.get(0).get("name"));
            }
        }

        // 解析成员UUID列表并批量填充姓名
        if (book.getMemberUuidList() != null && !book.getMemberUuidList().isEmpty()) {
            List<String> memberUuids = Arrays.asList(book.getMemberUuidList().split(","));
            List<String> cleanUuids = memberUuids.stream()
                .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
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

    /** 四级审批流: 采集员录入→街道办初审→民警复核→市局审批 */
    @Override
    @Transactional
    public HouseholdBusinessRequest submitBusiness(HouseholdBusinessRequest request) {
        request.setHandleDate(LocalDate.now());
        request.setStatus("审批中");
        businessMapper.insert(request);
        return request;
    }

    @Override
    @Transactional
    public HouseholdBusinessRequest approveBusiness(Long rid, String status, String handlerUuid, String rejectReason) {
        HouseholdBusinessRequest req = businessMapper.selectById(rid);
        if (req == null)
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        req.setStatus(status);
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

    @Override
    @Transactional
    public HouseholdMigrationRequest submitMigration(HouseholdMigrationRequest request) {
        request.setHandleDate(LocalDate.now());
        request.setStatus("准迁证审批中");
        migrationMapper.insert(request);
        return request;
    }

    @Override
    @Transactional
    public HouseholdMigrationRequest approveMigration(Long rid, String status, String handlerUuid,
            String rejectReason) {
        HouseholdMigrationRequest req = migrationMapper.selectById(rid);
        if (req == null)
            throw new BusinessException(ErrorCode.MIGRATION_NOT_FOUND);
        req.setStatus(status);
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
