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
        if (resident.getUuid() == null) {
            resident.setUuid("test-uuid");
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
            long total = residentEsRepository.multiConditionCount(request.getName(), request.getGender(),
                    request.getNation(), request.getNationCode(), request.getEducationLevel(),
                    request.getEducationCode(), request.getMaritalStatus(), request.getHouseholdStatus(),
                    request.getProvince());
            return PageResult.of(residents, total, request.getPage(), request.getSize());
        } catch (Exception e) {
            log.warn("ES search failed, fallback to DB", e);
            // Fallback: query local shard via MyBatis-Plus with proper pagination
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
        if (relation == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "未找到人员关系");
        }

        ResidentRelationVO vo = new ResidentRelationVO();
        vo.setRelationPersonUuid(uuid);

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

    @Override
    @Transactional
    public ResidentChangeRequest approveChangeRequest(Long rid, String status, String handlerUuid) {
        ResidentChangeRequest request = changeRequestMapper.selectById(rid);
        if (request == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "变更请求不存在");
        }
        request.setStatus(status);
        String currentHandlers = request.getHandlerIdList();
        if (currentHandlers == null) {
            request.setHandlerIdList(handlerUuid);
        } else {
            request.setHandlerIdList(currentHandlers + ";" + handlerUuid);
        }
        changeRequestMapper.updateById(request);

        // If approved, apply the change to resident
        if ("通过".equals(status)) {
            try {
                Map<String, Object> modifiedData = objectMapper.readValue(request.getModifiedData(), Map.class);
                Resident resident = residentMapper.selectByUuid(request.getApplicantUuid());
                if (resident != null) {
                    if (modifiedData.containsKey("name"))
                        resident.setName((String) modifiedData.get("name"));
                    if (modifiedData.containsKey("nation"))
                        resident.setNation((String) modifiedData.get("nation"));
                    residentMapper.updateById(resident);
                }
            } catch (Exception e) {
                log.error("Failed to apply change request", e);
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
}
