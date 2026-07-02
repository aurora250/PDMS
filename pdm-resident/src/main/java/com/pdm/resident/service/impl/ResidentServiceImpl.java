package com.pdm.resident.service.impl;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.common.core.validator.IdCardValidator;
import com.pdm.common.dto.PageResult;
import com.pdm.resident.dto.ResidentImportResult;
import com.pdm.resident.dto.ResidentRelationVO;
import com.pdm.resident.dto.ResidentSearchRequest;
import com.pdm.resident.entity.Resident;
import com.pdm.resident.entity.ResidentChangeRequest;
import com.pdm.resident.entity.ResidentRelation;
import com.pdm.resident.es.ResidentEsRepository;
import com.pdm.resident.mapper.ResidentChangeRequestMapper;
import com.pdm.resident.mapper.ResidentMapper;
import com.pdm.resident.mapper.ResidentRelationMapper;
import com.pdm.resident.service.ResidentService;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResidentServiceImpl implements ResidentService {

    private final ResidentMapper residentMapper;
    private final ResidentRelationMapper relationMapper;
    private final ResidentChangeRequestMapper changeRequestMapper;
    private final ResidentEsRepository residentEsRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @CacheEvict(value = "residentSearch", allEntries = true)
    public Resident createResident(Resident resident) {
        // Validate ID card
        if (!IdCardValidator.isValid(resident.getIdCardNo())) {
            throw new BusinessException(ErrorCode.ID_CARD_INVALID);
        }
        if (residentMapper.selectByIdCardNo(resident.getIdCardNo()) != null) {
            throw new BusinessException(ErrorCode.ID_CARD_DUPLICATE);
        }

        // Auto-fill birth date and gender from ID card
        resident.setBirthDate(IdCardValidator.extractBirthDate(resident.getIdCardNo()));
        resident.setGender(IdCardValidator.extractGender(resident.getIdCardNo()));
        if (resident.getUuid() == null || resident.getUuid().isBlank()) {
            resident.setUuid(java.util.UUID.randomUUID().toString());
        }
        if (resident.getHouseholdStatus() == null) {
            resident.setHouseholdStatus("正常");
        }

        residentMapper.insert(resident);

        // 异步同步到 ES (简化版，生产环境用 Canal CDC)
        try {
            residentEsRepository.save(resident.getUuid(), resident);
        } catch (Exception e) {
            log.warn("Failed to sync resident to ES: {}", e.getMessage());
        }

        return resident;
    }

    @Override
    public Resident getResident(String uuid) {
        Resident resident = residentMapper.selectByUuid(uuid);
        if (resident == null) {
            throw new BusinessException(ErrorCode.RESIDENT_NOT_FOUND);
        }
        return resident;
    }

    @Override
    @Transactional
    @CacheEvict(value = "residentSearch", allEntries = true)
    public Resident updateResident(String uuid, Resident updates) {
        Resident resident = residentMapper.selectByUuid(uuid);
        if (resident == null) {
            throw new BusinessException(ErrorCode.RESIDENT_NOT_FOUND);
        }
        // 注销状态不允许修改
        if ("死亡注销".equals(resident.getHouseholdStatus()) || "迁出注销".equals(resident.getHouseholdStatus())) {
            throw new BusinessException(ErrorCode.RESIDENT_STATUS_INVALID);
        }

        if (StringUtils.hasText(updates.getName()))
            resident.setName(updates.getName());
        if (StringUtils.hasText(updates.getNation()))
            resident.setNation(updates.getNation());
        if (StringUtils.hasText(updates.getNationCode()))
            resident.setNationCode(updates.getNationCode());
        if (StringUtils.hasText(updates.getEducationLevel()))
            resident.setEducationLevel(updates.getEducationLevel());
        if (StringUtils.hasText(updates.getEducationCode()))
            resident.setEducationCode(updates.getEducationCode());
        if (StringUtils.hasText(updates.getBloodType()))
            resident.setBloodType(updates.getBloodType());
        if (StringUtils.hasText(updates.getMaritalStatus()))
            resident.setMaritalStatus(updates.getMaritalStatus());
        if (StringUtils.hasText(updates.getOccupation()))
            resident.setOccupation(updates.getOccupation());
        if (StringUtils.hasText(updates.getPhone()))
            resident.setPhone(updates.getPhone());
        if (StringUtils.hasText(updates.getResidence()))
            resident.setResidence(updates.getResidence());

        residentMapper.updateById(resident);

        try {
            residentEsRepository.save(resident.getUuid(), resident);
        } catch (Exception e) {
            log.warn("Failed to sync resident to ES: {}", e.getMessage());
        }

        return resident;
    }

    @Override
    @Transactional
    @CacheEvict(value = "residentSearch", allEntries = true)
    public void deleteResident(String uuid) {
        Resident resident = residentMapper.selectByUuid(uuid);
        if (resident == null) {
            throw new BusinessException(ErrorCode.RESIDENT_NOT_FOUND);
        }
        residentMapper.deleteById(resident.getId());
        try {
            residentEsRepository.delete(uuid);
        } catch (Exception e) {
            log.warn("Failed to delete resident from ES: {}", e.getMessage());
        }
    }

    @Override
    public PageResult<Resident> search(ResidentSearchRequest request) {
        try {
            List<Resident> residents = residentEsRepository.multiConditionSearch(request.getName(), request.getGender(),
                    request.getNation(), request.getNationCode(), request.getEducationLevel(),
                    request.getEducationCode(), request.getMaritalStatus(), request.getHouseholdStatus(),
                    request.getProvince(), request.getOffset(), request.getSize());
            long esTotal = residentEsRepository.multiConditionCount(request.getName(), request.getGender(),
                    request.getNation(), request.getNationCode(), request.getEducationLevel(),
                    request.getEducationCode(), request.getMaritalStatus(), request.getHouseholdStatus(),
                    request.getProvince());
            if (esTotal == 0) {
                return searchFromDb(request);
            }
            // 无筛选条件时，如果 ES 数据量明显少于 DB，说明全量同步未完成，自动触发重索引并降级到 DB
            if (!StringUtils.hasText(request.getName()) && !StringUtils.hasText(request.getProvince())) {
                long dbTotal = residentMapper.selectCount(new LambdaQueryWrapper<>());
                if (dbTotal > 0 && esTotal < dbTotal / 2) {
                    log.warn("ES data incomplete: ES={} vs DB={}, triggering reindex and falling back to DB",
                            esTotal, dbTotal);
                    triggerAsyncReindex();
                    return searchFromDb(request);
                }
            }
            return PageResult.of(residents, esTotal, request.getPage(), request.getSize());
        } catch (Exception e) {
            log.warn("ES search failed, fallback to DB: {}", e.getMessage());
            return searchFromDb(request);
        }
    }

    /** 异步触发全量重索引（不阻塞当前请求） */
    private void triggerAsyncReindex() {
        new Thread(() -> {
            try {
                residentEsRepository.createIndex();
                int count = reindexAllResidents();
                log.info("Async reindex complete: {} residents synced to ES", count);
            } catch (Exception e) {
                log.error("Async reindex failed", e);
            }
        }, "es-reindex").start();
    }

    private PageResult<Resident> searchFromDb(ResidentSearchRequest request) {
        LambdaQueryWrapper<Resident> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(request.getName())) {
            wrapper.like(Resident::getName, request.getName());
        }
        if (StringUtils.hasText(request.getGender())) {
            wrapper.eq(Resident::getGender, request.getGender());
        }
        if (StringUtils.hasText(request.getNation())) {
            wrapper.eq(Resident::getNation, request.getNation());
        }
        if (StringUtils.hasText(request.getNationCode())) {
            wrapper.eq(Resident::getNationCode, request.getNationCode());
        }
        if (StringUtils.hasText(request.getEducationLevel())) {
            wrapper.eq(Resident::getEducationLevel, request.getEducationLevel());
        }
        if (StringUtils.hasText(request.getEducationCode())) {
            wrapper.eq(Resident::getEducationCode, request.getEducationCode());
        }
        if (StringUtils.hasText(request.getMaritalStatus())) {
            wrapper.eq(Resident::getMaritalStatus, request.getMaritalStatus());
        }
        if (StringUtils.hasText(request.getHouseholdStatus())) {
            wrapper.eq(Resident::getHouseholdStatus, request.getHouseholdStatus());
        }
        if (StringUtils.hasText(request.getProvince())) {
            wrapper.like(Resident::getHouseholdAddress, request.getProvince());
        }
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<Resident> pageResult = residentMapper.selectPage(
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(request.getPage(), request.getSize()),
                wrapper);
        return PageResult.of(pageResult.getRecords(), pageResult.getTotal(), request.getPage(), request.getSize());
    }

    @Override
    public int reindexAllResidents() {
        // 确保索引存在
        try {
            residentEsRepository.createIndex();
        } catch (Exception e) {
            log.warn("Failed to create ES index during reindex: {}", e.getMessage());
        }

        // 分页读取 PostgreSQL 全部居民，批量写入 ES
        int pageSize = 500;
        int page = 1;
        int total = 0;
        while (true) {
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<Resident> pg = residentMapper.selectPage(
                    com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(page, pageSize),
                    new LambdaQueryWrapper<>());
            if (pg.getRecords().isEmpty()) {
                break;
            }
            try {
                residentEsRepository.bulkSave(pg.getRecords(), Resident::getUuid);
                total += pg.getRecords().size();
                log.info("Reindexed {} residents to ES (page {})", total, page);
            } catch (Exception e) {
                log.error("Failed to bulk-save resident batch to ES at page {}: {}", page, e.getMessage());
            }
            page++;
        }
        log.info("Reindex complete: {} residents synced to ES", total);
        return total;
    }

    @Override
    public ResidentRelation getRelations(String uuid) {
        ResidentRelation relation = relationMapper.selectByPersonUuid(uuid);
        if (relation == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "未找到人员关系");
        }
        return relation;
    }

    @Override
    @Transactional
    public ResidentRelation setRelations(ResidentRelation relation) {
        String uuid = relation.getRelationPersonUuid();

        // Check circular relation
        if (relation.getFatherUuid() != null) {
            ResidentRelation fatherRel = relationMapper.selectByPersonUuid(relation.getFatherUuid());
            if (fatherRel != null && uuid.equals(fatherRel.getFatherUuid())) {
                throw new BusinessException(ErrorCode.RELATION_CIRCULAR);
            }
        }

        // Update or insert
        ResidentRelation existing = relationMapper.selectByPersonUuid(uuid);
        if (existing != null) {
            if (relation.getFatherUuid() != null)
                existing.setFatherUuid(relation.getFatherUuid());
            if (relation.getMotherUuid() != null)
                existing.setMotherUuid(relation.getMotherUuid());
            if (relation.getSpouseUuid() != null)
                existing.setSpouseUuid(relation.getSpouseUuid());
            relationMapper.updateById(existing);
        } else {
            relationMapper.insert(relation);
        }

        // Auto-bidirectional spouse relation
        if (relation.getSpouseUuid() != null) {
            ResidentRelation spouseRel = relationMapper.selectByPersonUuid(relation.getSpouseUuid());
            if (spouseRel != null) {
                spouseRel.setSpouseUuid(uuid);
                relationMapper.updateById(spouseRel);
            } else {
                ResidentRelation newSpouseRel = new ResidentRelation();
                newSpouseRel.setRelationPersonUuid(relation.getSpouseUuid());
                newSpouseRel.setSpouseUuid(uuid);
                relationMapper.insert(newSpouseRel);
            }
        }

        return relationMapper.selectByPersonUuid(uuid);
    }

    @Override
    public ResidentRelationVO getRelationsWithNames(String uuid) {
        ResidentRelation relation = relationMapper.selectByPersonUuid(uuid);

        ResidentRelationVO vo = new ResidentRelationVO();
        vo.setRelationPersonUuid(uuid);

        if (relation == null) {
            // 本人未录入关系，但仍可反向查询子女、配偶
            vo.setChildren(relationMapper.selectChildren(uuid));
            return vo;
        }

        // 批量查询相关人员的姓名
        List<String> relatedUuids = new ArrayList<>();
        if (relation.getFatherUuid() != null)
            relatedUuids.add(relation.getFatherUuid());
        if (relation.getMotherUuid() != null)
            relatedUuids.add(relation.getMotherUuid());
        if (relation.getSpouseUuid() != null)
            relatedUuids.add(relation.getSpouseUuid());

        Map<String, String> nameMap = new java.util.HashMap<>();
        if (!relatedUuids.isEmpty()) {
            List<Map<String, Object>> names = residentMapper.batchGetNames(relatedUuids);
            for (Map<String, Object> row : names) {
                nameMap.put((String) row.get("uuid"), (String) row.get("name"));
            }
        }

        vo.setFatherUuid(relation.getFatherUuid());
        vo.setFatherName(nameMap.get(relation.getFatherUuid()));
        vo.setMotherUuid(relation.getMotherUuid());
        vo.setMotherName(nameMap.get(relation.getMotherUuid()));
        vo.setSpouseUuid(relation.getSpouseUuid());
        vo.setSpouseName(nameMap.get(relation.getSpouseUuid()));

        // 查询子女
        vo.setChildren(relationMapper.selectChildren(uuid));

        return vo;
    }

    @Override
    public List<Map<String, Object>> getChildren(String uuid) {
        return relationMapper.selectChildren(uuid);
    }

    @Override
    public PageResult<ResidentChangeRequest> listChangeRequests(String status, int page, int size) {
        LambdaQueryWrapper<ResidentChangeRequest> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            wrapper.eq(ResidentChangeRequest::getStatus, status);
        }
        wrapper.orderByDesc(ResidentChangeRequest::getRequestTime);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ResidentChangeRequest> result = changeRequestMapper
                .selectPage(com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(page, size), wrapper);
        return PageResult.of(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public ResidentChangeRequest submitChangeRequest(ResidentChangeRequest request) {
        if (request.getRequestTime() == null) {
            request.setRequestTime(LocalDate.now());
        }
        if (request.getStatus() == null) {
            request.setStatus("请求");
        }
        changeRequestMapper.insert(request);
        return request;
    }

    /**
     * 变更申请审批状态机（与户籍业务一致）: 一般事项: 请求 → [民警通过] → 通过（自动应用变更到居民数据） 特殊事项: 请求 → [民警提交市局] →
     * 市局审批中 → [市局通过] → 通过 驳回: 请求/市局审批中 → [驳回] → 驳回
     */
    @Override
    @Transactional
    public ResidentChangeRequest approveChangeRequest(Long rid, String action, String handlerUuid) {
        ResidentChangeRequest request = changeRequestMapper.selectById(rid);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "变更请求不存在");
        }

        String current = request.getStatus();
        String next;

        switch (action) {
            case "通过" :
                if ("请求".equals(current)) {
                    next = "通过"; // 民警直接通过（一般事项）
                } else if ("市局审批中".equals(current)) {
                    next = "通过"; // 市局最终通过
                } else {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许审批通过: " + current);
                }
                break;
            case "提交市局" :
                if (!"请求".equals(current)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "仅请求状态可提交市局: " + current);
                }
                next = "市局审批中";
                break;
            case "驳回" :
                if ("通过".equals(current) || "驳回".equals(current)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "当前状态不允许驳回: " + current);
                }
                next = "驳回";
                break;
            default :
                throw new BusinessException(ErrorCode.PARAM_ERROR, "未知审批操作: " + action);
        }

        request.setStatus(next);
        String currentHandlers = request.getHandlerIdList();
        if (currentHandlers == null) {
            request.setHandlerIdList(handlerUuid);
        } else {
            request.setHandlerIdList(currentHandlers + ";" + handlerUuid);
        }
        changeRequestMapper.updateById(request);

        // If approved, apply the change to resident
        if ("通过".equals(next)) {
            try {
                Map<String, Object> modifiedData = objectMapper.readValue(request.getModifiedData(), Map.class);
                Resident resident = residentMapper.selectByUuid(request.getApplicantUuid());
                if (resident != null) {
                    // 修改已有居民：逐字段更新
                    if (modifiedData.containsKey("name"))
                        resident.setName((String) modifiedData.get("name"));
                    if (modifiedData.containsKey("formerName"))
                        resident.setFormerName((String) modifiedData.get("formerName"));
                    if (modifiedData.containsKey("gender"))
                        resident.setGender((String) modifiedData.get("gender"));
                    if (modifiedData.containsKey("nation"))
                        resident.setNation((String) modifiedData.get("nation"));
                    if (modifiedData.containsKey("nationCode"))
                        resident.setNationCode((String) modifiedData.get("nationCode"));
                    if (modifiedData.containsKey("educationLevel"))
                        resident.setEducationLevel((String) modifiedData.get("educationLevel"));
                    if (modifiedData.containsKey("educationCode"))
                        resident.setEducationCode((String) modifiedData.get("educationCode"));
                    if (modifiedData.containsKey("bloodType"))
                        resident.setBloodType((String) modifiedData.get("bloodType"));
                    if (modifiedData.containsKey("maritalStatus"))
                        resident.setMaritalStatus((String) modifiedData.get("maritalStatus"));
                    if (modifiedData.containsKey("occupation"))
                        resident.setOccupation((String) modifiedData.get("occupation"));
                    if (modifiedData.containsKey("phone"))
                        resident.setPhone((String) modifiedData.get("phone"));
                    if (modifiedData.containsKey("residence"))
                        resident.setResidence((String) modifiedData.get("residence"));
                    if (modifiedData.containsKey("areaId"))
                        resident.setAreaId(toLong(modifiedData.get("areaId")));
                    if (modifiedData.containsKey("householdType"))
                        resident.setHouseholdType((String) modifiedData.get("householdType"));
                    if (modifiedData.containsKey("householdAddress"))
                        resident.setHouseholdAddress((String) modifiedData.get("householdAddress"));
                    if (modifiedData.containsKey("householdAreaId"))
                        resident.setHouseholdAreaId(toLong(modifiedData.get("householdAreaId")));
                    residentMapper.updateById(resident);
                    try { residentEsRepository.save(resident.getUuid(), resident); } catch (Exception e) { log.warn("Failed to sync updated resident to ES: {}", e.getMessage()); }
                } else {
                    // 新增居民：从 modifiedData 构建新 Resident 并插入
                    Resident newResident = new Resident();
                    newResident.setUuid(request.getApplicantUuid());
                    newResident.setName((String) modifiedData.get("name"));
                    newResident.setFormerName((String) modifiedData.get("formerName"));
                    newResident.setGender((String) modifiedData.get("gender"));
                    newResident.setIdCardNo((String) modifiedData.get("idCardNo"));
                    newResident.setNation((String) modifiedData.get("nation"));
                    newResident.setNationCode((String) modifiedData.get("nationCode"));
                    newResident.setEducationLevel((String) modifiedData.get("educationLevel"));
                    newResident.setEducationCode((String) modifiedData.get("educationCode"));
                    newResident.setBloodType((String) modifiedData.get("bloodType"));
                    newResident.setMaritalStatus((String) modifiedData.get("maritalStatus"));
                    newResident.setOccupation((String) modifiedData.get("occupation"));
                    newResident.setPhone((String) modifiedData.get("phone"));
                    newResident.setResidence((String) modifiedData.get("residence"));
                    newResident.setAreaId(toLong(modifiedData.get("areaId")));
                    newResident.setHouseholdType((String) modifiedData.get("householdType"));
                    newResident.setHouseholdStatus(
                            modifiedData.containsKey("householdStatus") ? (String) modifiedData.get("householdStatus")
                                    : "正常");
                    newResident.setHouseholdAddress((String) modifiedData.get("householdAddress"));
                    newResident.setHouseholdAreaId(toLong(modifiedData.get("householdAreaId")));

                    // Auto-fill birth date and gender from ID card
                    if (newResident.getIdCardNo() != null && IdCardValidator.isValid(newResident.getIdCardNo())) {
                        newResident.setBirthDate(IdCardValidator.extractBirthDate(newResident.getIdCardNo()));
                        newResident.setGender(IdCardValidator.extractGender(newResident.getIdCardNo()));
                    }
                    // Validate ID card
                    if (!IdCardValidator.isValid(newResident.getIdCardNo())) {
                        throw new BusinessException(ErrorCode.ID_CARD_INVALID);
                    }
                    if (residentMapper.selectByIdCardNo(newResident.getIdCardNo()) != null) {
                        throw new BusinessException(ErrorCode.ID_CARD_DUPLICATE);
                    }

                    residentMapper.insert(newResident);
                    try {
                        residentEsRepository.save(newResident.getUuid(), newResident);
                    } catch (Exception e) {
                        log.warn("Failed to sync new resident to ES: {}", e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to apply change request", e);
                throw e instanceof BusinessException ? (BusinessException) e
                        : new BusinessException(ErrorCode.SYSTEM_ERROR, "变更应用失败: " + e.getMessage());
            }
        }

        return request;
    }

    @Override
    public ResidentImportResult importExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        int[] counters = { 0, 0 }; // [success, fail]

        try {
            EasyExcel.read(file.getInputStream(), Resident.class, new ReadListener<Resident>() {
                @Override
                public void invoke(Resident resident, AnalysisContext context) {
                    try {
                        counters[0]++;
                        residentMapper.insert(resident);
                    } catch (Exception e) {
                        counters[1]++;
                        counters[0]--;
                        errors.add("行" + context.readRowHolder().getRowIndex() + ": " + e.getMessage());
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }

                @Override
                public void onException(Exception exception, AnalysisContext context) {
                    counters[1]++;
                    errors.add("解析错误: " + exception.getMessage());
                }
            }).sheet().doRead();
        } catch (Exception e) {
            errors.add("文件读取失败: " + e.getMessage());
            counters[1]++;
        }

        return ResidentImportResult.builder().totalCount(counters[0] + counters[1]).successCount(counters[0])
                .failCount(counters[1]).errorMessages(errors).build();
    }

    @Override
    public void exportExcel(Map<String, Object> conditions, OutputStream outputStream) {
        LambdaQueryWrapper<Resident> wrapper = new LambdaQueryWrapper<>();
        if (conditions != null) {
            if (StringUtils.hasText((CharSequence) conditions.get("name")))
                wrapper.like(Resident::getName, (String) conditions.get("name"));
            if (StringUtils.hasText((CharSequence) conditions.get("gender")))
                wrapper.eq(Resident::getGender, (String) conditions.get("gender"));
            if (StringUtils.hasText((CharSequence) conditions.get("nation")))
                wrapper.eq(Resident::getNation, (String) conditions.get("nation"));
            if (StringUtils.hasText((CharSequence) conditions.get("idCardNo")))
                wrapper.eq(Resident::getIdCardNo, (String) conditions.get("idCardNo"));
        }
        wrapper.eq(Resident::getIsDeleted, 0);
        List<Resident> residents = residentMapper.selectList(wrapper);
        EasyExcel.write(outputStream, Resident.class).sheet("常住人口").doWrite(residents);
    }

    /** 安全地将 Object 转为 Long */
    private static Long toLong(Object val) {
        if (val == null) return null;
        if (val instanceof Number n) return n.longValue();
        try { return Long.parseLong(val.toString()); } catch (NumberFormatException e) { return null; }
    }
}
