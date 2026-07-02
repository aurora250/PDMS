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
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
    private final ResidentRelationMapper relationMapper;
    private final PoliceMapper policeMapper;

    @Override
    @Transactional
    public HouseholdRegister applyBook(HouseholdRegister book) {
        if (book.getHouseholdBookNo() == null || book.getHouseholdBookNo().isBlank()) {
            book.setEstablishDate(LocalDate.now());
            // 生成户口簿号: 地区码(6位) + 年份(4位) + 序号(8位零填充)
            String areaCode = "000000";
            if (book.getHukouAreaId() != null) {
                Area area = areaMapper.selectById(book.getHukouAreaId());
                if (area != null && area.getAreaCode() != null) {
                    areaCode = area.getAreaCode();
                }
            }
            String year = String.valueOf(book.getEstablishDate().getYear());
            String prefix = areaCode + year;
            String maxNo = bookMapper.selectMaxBookNoByPrefix(prefix + "%");
            long seq = 1L;
            if (maxNo != null && maxNo.length() >= 18) {
                try { seq = Long.parseLong(maxNo.substring(10)) + 1; } catch (NumberFormatException e) { /* use 1 */ }
            }
            book.setHouseholdBookNo(prefix + String.format("%08d", seq));
        }
        if (book.getEstablishDate() == null) {
            book.setEstablishDate(LocalDate.now());
        }
        book.setStatus("审批中");
        bookMapper.insert(book);
        return book;
    }

    @Override
    @Transactional
    public HouseholdRegister reissueBook(String bookNo) {
        HouseholdRegister oldBook = bookMapper.selectByBookNo(bookNo);
        if (oldBook == null)
            throw new BusinessException(ErrorCode.HOUSEHOLD_BOOK_NOT_FOUND);

        // 生成新户口簿号
        String areaCode = "000000";
        if (oldBook.getHukouAreaId() != null) {
            Area area = areaMapper.selectById(oldBook.getHukouAreaId());
            if (area != null && area.getAreaCode() != null) areaCode = area.getAreaCode();
        }
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = areaCode + year;
        String maxNo = bookMapper.selectMaxBookNoByPrefix(prefix + "%");
        long seq = 1L;
        if (maxNo != null && maxNo.length() >= 18) {
            try { seq = Long.parseLong(maxNo.substring(10)) + 1; } catch (NumberFormatException e) { /* use 1 */ }
        }
        String newBookNo = prefix + String.format("%08d", seq);

        // 创建新户口簿，复制原数据
        HouseholdRegister newBook = new HouseholdRegister();
        newBook.setHouseholdBookNo(newBookNo);
        newBook.setHouseholderUuid(oldBook.getHouseholderUuid());
        newBook.setEstablishDate(LocalDate.now());
        newBook.setHukouAddress(oldBook.getHukouAddress());
        newBook.setHukouAreaId(oldBook.getHukouAreaId());
        newBook.setMemberUuidList(oldBook.getMemberUuidList());
        newBook.setStatus("有效");
        bookMapper.insert(newBook);

        // 原户口簿标记作废，清空关联防止查询时误返回旧记录
        oldBook.setStatus("无效");
        oldBook.setHouseholderUuid(null);
        oldBook.setMemberUuidList(null);
        bookMapper.updateById(oldBook);
        log.info("补办户口簿: 旧号={} 已作废, 新号={}", bookNo, newBookNo);

        return newBook;
    }

    @Override
    @Transactional
    public HouseholdRegister renewBook(String bookNo) {
        return reissueBook(bookNo);
    }

    @Override
    @Transactional
    public HouseholdRegister approveBook(Long id, String action, String handlerUuid) {
        HouseholdRegister book = bookMapper.selectById(id);
        if (book == null)
            throw new BusinessException(ErrorCode.HOUSEHOLD_BOOK_NOT_FOUND);

        String current = book.getStatus();
        if ("通过".equals(action)) {
            if (!"审批中".equals(current))
                throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许审批通过: " + current);
            book.setStatus("有效");
        } else if ("驳回".equals(action)) {
            if (!"审批中".equals(current))
                throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许驳回: " + current);
            book.setStatus("已驳回");
        } else {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "未知审批操作: " + action);
        }
        bookMapper.updateById(book);
        return book;
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
     * 审批流: 采集员上报 → 民警审核 → 市局（仅特殊事项） 街道办只附加材料，不参与审批 状态机: 一般事项: 审批中 → [民警通过] → 已批准
     * 特殊事项: 审批中 → [民警提交市局] → 市局审批中 → [市局通过] → 已批准 驳回: 审批中/市局审批中 → [驳回] → 已驳回
     */
    @Override
    @Transactional
    public HouseholdBusinessRequest submitBusiness(HouseholdBusinessRequest request) {
        if (request.getHandleDate() == null) {
            request.setHandleDate(LocalDate.now());
        }
        request.setStatus("审批中");
        businessMapper.insertBusiness(request);
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
            case "通过" :
                if ("审批中".equals(current)) {
                    next = "已批准"; // 民警直接通过（一般事项）
                } else if ("市局审批中".equals(current)) {
                    next = "已批准"; // 市局最终通过
                } else {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许审批通过: " + current);
                }
                break;
            case "提交市局" :
                if (!"审批中".equals(current)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "仅审批中状态可提交市局: " + current);
                }
                next = "市局审批中";
                break;
            case "驳回" :
                if ("已批准".equals(current) || "已驳回".equals(current)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许驳回: " + current);
                }
                next = "已驳回";
                break;
            default :
                throw new BusinessException(ErrorCode.PARAM_ERROR, "未知审批操作: " + action);
        }

        req.setStatus(next);
        req.setHandlerUuid(handlerUuid);
        if (rejectReason != null)
            req.setRejectReason(rejectReason);
        businessMapper.updateBusiness(req);

        // 审批通过后执行业务落地操作
        if ("已批准".equals(next)) {
            executeBusinessEffect(req);
        }
        return req;
    }

    /**
     * 根据业务类型执行对应的数据变更。当前支持：出生登记、死亡注销、分户立户。
     */
    private void executeBusinessEffect(HouseholdBusinessRequest req) {
        String biz = req.getBusinessType();
        String json = req.getDetailJson();
        if (biz == null || json == null || json.isEmpty()) {
            log.warn("业务类型或 detailJson 为空，跳过落地: rid={}, biz={}", req.getRid(), biz);
            return;
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> d = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
            switch (biz) {
                case "出生登记" -> executeBirthRegistration(req, d);
                case "死亡注销" -> executeDeathCancellation(req, d);
                case "分户立户" -> executeHouseholdSplit(req, d);
                default -> log.info("业务类型 {} 暂无落地逻辑，跳过", biz);
            }
        } catch (Exception e) {
            log.error("业务落地失败: rid={}, biz={}", req.getRid(), biz, e);
            throw new BusinessException(ErrorCode.PARAM_ERROR, "业务数据落地失败: " + e.getMessage());
        }
    }

    /** 出生登记：创建新生儿居民记录，并加入户口簿成员列表 */
    private void executeBirthRegistration(HouseholdBusinessRequest req, Map<String, Object> d) {
        String name = (String) d.get("name");
        String gender = (String) d.get("gender");
        String birthDateStr = (String) d.get("birthDate");
        String fatherUuid = (String) d.get("fatherUuid");
        String motherUuid = (String) d.get("motherUuid");
        String nation = (String) d.get("nation");
        String birthCertNo = (String) d.get("birthCertNo");

        if (name == null || gender == null || birthDateStr == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新生儿姓名、性别、出生日期不能为空");
        }
        LocalDate birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ISO_LOCAL_DATE);

        // 以父亲（优先）或母亲的户籍信息作为新生儿的户籍地址
        String parentUuid = fatherUuid != null ? fatherUuid : motherUuid;
        Map<String, Object> parentInfo = null;
        if (parentUuid != null) {
            parentInfo = residentMapper.selectByUuid(parentUuid);
        }
        String residence = parentInfo != null ? (String) parentInfo.get("residence") : "";
        Object parentAreaId = parentInfo != null ? parentInfo.get("area_id") : null;
        String householdAddress = parentInfo != null ? (String) parentInfo.get("household_address") : "";
        Object householdAreaId = parentInfo != null ? parentInfo.get("household_area_id") : null;

        // 生成临时身份证号：区域码(6) + 日期(8) + 序号(4)
        String areaCode = "000000";
        if (parentAreaId != null) {
            Area a = areaMapper.selectById((Long) parentAreaId);
            if (a != null && a.getAreaCode() != null) areaCode = a.getAreaCode();
        }
        String birthPart = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String seqPart = String.format("%04d", System.currentTimeMillis() % 10000);
        String idCardNo = areaCode + birthPart + seqPart;

        String newUuid = UUID.randomUUID().toString();
        Map<String, Object> params = new HashMap<>();
        params.put("uuid", newUuid);
        params.put("name", name);
        params.put("gender", gender);
        params.put("idCardNo", idCardNo);
        params.put("nation", nation != null ? nation : "汉族");
        params.put("birthDate", birthDate);
        params.put("phone", "");
        params.put("residence", residence);
        params.put("areaId", parentAreaId);
        params.put("householdType", "居民户口");
        params.put("householdStatus", "正常");
        params.put("householdAddress", householdAddress);
        params.put("householdAreaId", householdAreaId);
        residentMapper.insertResident(params);
        log.info("出生登记: 新生儿 {} (uuid={}) 已创建", name, newUuid);

        // 同步家庭关系：为新生儿建立父子/母子关系
        if (fatherUuid != null || motherUuid != null) {
            relationMapper.upsertRelation(newUuid, fatherUuid, motherUuid);
            log.info("出生登记: 新生儿 {} 的家庭关系已建立, father={}, mother={}", newUuid, fatherUuid, motherUuid);
        }

        // 将新生儿加入父/母的户口簿成员列表
        if (parentUuid != null) {
            HouseholdRegister book = bookMapper.selectByResidentUuid(parentUuid);
            if (book != null) {
                String existing = book.getMemberUuidList();
                if (existing == null || existing.isEmpty()) {
                    book.setMemberUuidList(newUuid);
                } else {
                    book.setMemberUuidList(existing + "," + newUuid);
                }
                bookMapper.updateById(book);
            }
        }
    }

    /** 死亡注销：将申请人的户籍状态标记为死亡注销，并从户口簿成员列表中移除 */
    private void executeDeathCancellation(HouseholdBusinessRequest req, Map<String, Object> d) {
        String applicantUuid = req.getApplicantUuid();
        residentMapper.updateHouseholdStatus(applicantUuid, "死亡注销");
        log.info("死亡注销: 居民 {} 户籍状态已更新为死亡注销", applicantUuid);

        // 从户口簿成员列表中移除
        HouseholdRegister book = bookMapper.selectByResidentUuid(applicantUuid);
        if (book != null && book.getMemberUuidList() != null && !book.getMemberUuidList().isEmpty()) {
            List<String> members = new ArrayList<>(Arrays.asList(book.getMemberUuidList().split(",")));
            members.removeIf(m -> m.trim().equals(applicantUuid));
            book.setMemberUuidList(members.isEmpty() ? null : String.join(",", members));
            // 如果死亡者是户主，将户主设为第一个成员（或置空）
            if (applicantUuid.equals(book.getHouseholderUuid()) && !members.isEmpty()) {
                book.setHouseholderUuid(members.get(0).trim());
            }
            bookMapper.updateById(book);
        }
    }

    /** 分户立户：创建新的户口簿 */
    private void executeHouseholdSplit(HouseholdBusinessRequest req, Map<String, Object> d) {
        String newHouseholderUuid = (String) d.get("newHouseholderUuid");
        Object hukouAreaIdObj = d.get("hukouAreaId");
        String hukouAddressDetail = (String) d.get("hukouAddressDetail");
        @SuppressWarnings("unchecked")
        List<String> memberUuids = (List<String>) d.get("memberUuids");

        if (newHouseholderUuid == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新户主UUID不能为空");
        }
        Long hukouAreaId = hukouAreaIdObj != null ? ((Number) hukouAreaIdObj).longValue() : null;

        // 生成户口簿号
        String areaCode = "000000";
        if (hukouAreaId != null) {
            Area a = areaMapper.selectById(hukouAreaId);
            if (a != null && a.getAreaCode() != null) areaCode = a.getAreaCode();
        }
        String year = String.valueOf(LocalDate.now().getYear());
        String prefix = areaCode + year;
        String maxNo = bookMapper.selectMaxBookNoByPrefix(prefix + "%");
        long seq = 1L;
        if (maxNo != null && maxNo.length() >= 18) {
            try { seq = Long.parseLong(maxNo.substring(10)) + 1; } catch (NumberFormatException e) { /* use 1 */ }
        }

        // 组装完整户籍地址
        String fullAddress = hukouAddressDetail;
        if (hukouAreaId != null) {
            String path = areaMapper.selectAreaPath(hukouAreaId);
            if (path != null && !path.isEmpty()) fullAddress = path + hukouAddressDetail;
        }

        HouseholdRegister newBook = new HouseholdRegister();
        newBook.setHouseholdBookNo(prefix + String.format("%08d", seq));
        newBook.setHouseholderUuid(newHouseholderUuid);
        newBook.setEstablishDate(LocalDate.now());
        newBook.setHukouAddress(fullAddress != null ? fullAddress : "");
        newBook.setHukouAreaId(hukouAreaId);
        newBook.setStatus("有效"); // 审批通过直接有效
        if (memberUuids != null && !memberUuids.isEmpty()) {
            newBook.setMemberUuidList(String.join(",", memberUuids));
        }
        bookMapper.insert(newBook);
        log.info("分户立户: 新户口簿 {} 已创建，户主={}", newBook.getHouseholdBookNo(), newHouseholderUuid);

        // 如果指定了成员UUID，从原户口簿中移除这些成员
        if (memberUuids != null && !memberUuids.isEmpty()) {
            HouseholdRegister oldBook = bookMapper.selectByResidentUuid(req.getApplicantUuid());
            if (oldBook != null && oldBook.getMemberUuidList() != null && !oldBook.getMemberUuidList().isEmpty()) {
                Set<String> toRemove = new HashSet<>(memberUuids);
                List<String> remaining = Arrays.stream(oldBook.getMemberUuidList().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty() && !toRemove.contains(s))
                        .collect(Collectors.toList());
                oldBook.setMemberUuidList(remaining.isEmpty() ? null : String.join(",", remaining));
                bookMapper.updateById(oldBook);
            }
        }
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
        businessMapper.updateBusiness(req);
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
     * 迁移审批状态机: 准迁证审批中 → [民警通过] → 准迁证已批准（自动签发准迁证） 准迁证已批准 → [民警通过] → 迁移证已批准（自动签发迁移证）
     * 迁移证已批准 → [民警通过] → 迁移审批通过 任意非终态 → [驳回] → 对应阶段驳回
     */
    private static final java.util.Map<String, String> MIGRATION_APPROVAL_NEXT = java.util.Map.of("准迁证审批中", "准迁证已批准",
            "准迁证已批准", "迁移证已批准", "迁移证已批准", "迁移审批通过");

    @Override
    @Transactional
    public HouseholdMigrationRequest submitMigration(HouseholdMigrationRequest request) {
        if (request.getHandleDate() == null) {
            request.setHandleDate(LocalDate.now());
        }
        if (request.getAttachment() == null) {
            request.setAttachment("");
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

        // 查询处理民警信息（含辖区和派出所名称）
        java.util.Map<String, Object> handlerPolice = policeMapper.selectByUserUuid(handlerUuid);
        if (handlerPolice == null)
            throw new BusinessException(ErrorCode.PARAM_ERROR, "当前用户不是民警，无权审批迁移");

        Long handlerAreaId = (Long) handlerPolice.get("area_id");
        String handlerStation = (String) handlerPolice.get("police_station");

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
                throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许审批通过: " + current);
            }

            // === 辖区校验 ===
            if ("准迁证审批中".equals(current)) {
                // 准迁证：民警辖区必须与迁入地匹配
                checkJurisdiction(handlerAreaId, req.getIncomingAreaId(), "迁入地");
            } else if ("准迁证已批准".equals(current)) {
                // 迁移证：民警辖区必须与迁出地匹配
                checkJurisdiction(handlerAreaId, req.getOutgoingAreaId(), "迁出地");
            }

            req.setStatus(nextStatus);

            // 自动签发准迁证（准迁证审批中 → 准迁证已批准）
            if ("准迁证已批准".equals(nextStatus) && req.getApprovalPermitNo() == null) {
                String incomingAreaCode = getAreaCode(req.getIncomingAreaId());
                ApprovalPermit approvalPermit = new ApprovalPermit();
                approvalPermit.setUuid(req.getApplicantUuid());
                approvalPermit.setPermitNo(PermitNumberGenerator.approvalPermit(incomingAreaCode, LocalDate.now(),
                        System.currentTimeMillis() % 1_000_000));
                approvalPermit.setIssueDate(LocalDate.now());
                approvalPermit.setExpiryDate(LocalDate.now().plusDays(30));
                approvalPermit.setIssuingAuthority(handlerStation != null ? handlerStation : "公安机关");
                approvalPermit.setStatus("有效");
                approvalPermitMapper.insert(approvalPermit);
                req.setApprovalPermitNo(approvalPermit.getPermitNo());
            }

            // 自动签发迁移证（准迁证已批准 → 迁移证已批准）
            if ("迁移证已批准".equals(nextStatus) && req.getMigrationPermitNo() == null) {
                String outgoingAreaCode = getAreaCode(req.getOutgoingAreaId());
                MigrationPermit migrationPermit = new MigrationPermit();
                migrationPermit.setUuid(req.getApplicantUuid());
                migrationPermit.setPermitNo(PermitNumberGenerator.migrationPermit(outgoingAreaCode, LocalDate.now(),
                        System.currentTimeMillis() % 1_000_000));
                migrationPermit.setIssueDate(LocalDate.now());
                migrationPermit.setExpiryDate(LocalDate.now().plusDays(30));
                migrationPermit.setOutgoingPoliceStation(handlerStation != null ? handlerStation : "公安机关");
                migrationPermit.setStatus("有效");
                migrationPermitMapper.insert(migrationPermit);
                req.setMigrationPermitNo(migrationPermit.getPermitNo());
            }

            // 迁移审批通过：同步更新居民户籍地址
            if ("迁移审批通过".equals(nextStatus)) {
                if (req.getIncomingAddress() != null) {
                    residentMapper.updateHouseholdAddress(req.getApplicantUuid(),
                            req.getIncomingAddress(), req.getIncomingAreaId());
                    log.info("迁移审批通过: 居民 {} 户籍地址已更新为迁入地", req.getApplicantUuid());
                }
            }
        }

        req.setHandlerUuid(handlerUuid);
        if (rejectReason != null)
            req.setRejectReason(rejectReason);
        migrationMapper.updateById(req);
        return req;
    }

    /** 校验民警辖区与迁移目标地区是否匹配（至少同市，area_code前4位一致） */
    private void checkJurisdiction(Long handlerAreaId, Long targetAreaId, String label) {
        if (handlerAreaId == null || targetAreaId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "缺少辖区或" + label + "地区信息，无法校验辖区权限");
        }
        String handlerCode = getAreaCode(handlerAreaId);
        String targetCode = getAreaCode(targetAreaId);
        if (handlerCode == null || targetCode == null || handlerCode.length() < 4 || targetCode.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无法获取辖区或" + label + "的地区编码");
        }
        if (!handlerCode.substring(0, 4).equals(targetCode.substring(0, 4))) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    String.format("当前民警辖区与%s不匹配，无权审批此迁移", label));
        }
    }

    /** 安全获取 area_code */
    private String getAreaCode(Long areaId) {
        if (areaId == null) return null;
        Area area = areaMapper.selectById(areaId);
        return area != null ? area.getAreaCode() : null;
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
        if (permit.getExpiryDate() == null)
            permit.setExpiryDate(LocalDate.now().plusDays(30));
        if (permit.getIssuingAuthority() == null)
            permit.setIssuingAuthority("公安机关");
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
        if (permit.getExpiryDate() == null)
            permit.setExpiryDate(LocalDate.now().plusDays(30));
        if (permit.getOutgoingPoliceStation() == null)
            permit.setOutgoingPoliceStation("公安机关");
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
