package com.pdm.floatingpopulation.service.impl;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.common.core.util.PermitNumberGenerator;
import com.pdm.floatingpopulation.entity.FpRegisterRecord;
import com.pdm.floatingpopulation.entity.ResidentPermit;
import com.pdm.floatingpopulation.entity.ResidentPermitRenewal;
import com.pdm.floatingpopulation.entity.ResidentRegistration;
import com.pdm.floatingpopulation.mapper.FpRegisterRecordMapper;
import com.pdm.floatingpopulation.mapper.ResidentPermitMapper;
import com.pdm.floatingpopulation.mapper.ResidentPermitRenewalMapper;
import com.pdm.floatingpopulation.mapper.ResidentRegistrationMapper;
import com.pdm.floatingpopulation.service.FloatingPopulationService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FloatingPopulationServiceImpl implements FloatingPopulationService {

    private final FpRegisterRecordMapper fpRegisterRecordMapper;
    private final ResidentPermitMapper residentPermitMapper;
    private final ResidentPermitRenewalMapper residentPermitRenewalMapper;
    private final ResidentRegistrationMapper residentRegistrationMapper;

    @Override
    @Transactional
    public FpRegisterRecord registerFp(FpRegisterRecord record) {
        if (record.getRegisterDate() == null) {
            record.setRegisterDate(LocalDate.now());
        }
        fpRegisterRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public FpRegisterRecord updateFp(Long rid, FpRegisterRecord record) {
        FpRegisterRecord existing = fpRegisterRecordMapper.selectById(rid);
        if (existing == null) {
            throw new BusinessException(ErrorCode.FP_RECORD_NOT_FOUND);
        }
        // 部分更新：只合并非空字段，避免覆盖已有数据
        if (record.getUuid() != null)
            existing.setUuid(record.getUuid());
        if (record.getRegisterDate() != null)
            existing.setRegisterDate(record.getRegisterDate());
        if (record.getResidencePermitNo() != null)
            existing.setResidencePermitNo(record.getResidencePermitNo());
        if (record.getAgentUuid() != null)
            existing.setAgentUuid(record.getAgentUuid());
        if (record.getAttachment() != null)
            existing.setAttachment(record.getAttachment());
        if (record.getReviewerUuid() != null)
            existing.setReviewerUuid(record.getReviewerUuid());
        if (record.getRejectReason() != null)
            existing.setRejectReason(record.getRejectReason());
        fpRegisterRecordMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void cancelFp(Long rid) {
        FpRegisterRecord existing = fpRegisterRecordMapper.selectById(rid);
        if (existing == null) {
            throw new BusinessException(ErrorCode.FP_RECORD_NOT_FOUND);
        }
        fpRegisterRecordMapper.deleteById(rid);
    }

    @Override
    @Transactional
    public ResidentPermit applyPermit(ResidentPermit permit) {
        permit.setPermitNo(
                PermitNumberGenerator.residentPermit(null, LocalDate.now(), System.currentTimeMillis() % 1_000_000));
        permit.setStatus("申领");
        if (permit.getIssueDate() == null) {
            permit.setIssueDate(LocalDate.now());
        }
        if (permit.getExpiryDate() == null) {
            permit.setExpiryDate(LocalDate.now().plusYears(1));
        }
        residentPermitMapper.insert(permit);
        return permit;
    }

    @Override
    @Transactional
    public ResidentPermit approvePermit(Long id, String reviewerUuid) {
        ResidentPermit permit = residentPermitMapper.selectById(id);
        if (permit == null) {
            throw new BusinessException(ErrorCode.RESIDENT_PERMIT_NOT_FOUND);
        }
        if (!"申领".equals(permit.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅可审批状态为\"申领\"的居住证，当前状态：" + permit.getStatus());
        }
        permit.setStatus("已批准");
        permit.setReviewerUuid(reviewerUuid);
        residentPermitMapper.updateById(permit);
        return permit;
    }

    @Override
    @Transactional
    public ResidentPermit issuePermit(Long id) {
        ResidentPermit permit = residentPermitMapper.selectById(id);
        if (permit == null) {
            throw new BusinessException(ErrorCode.RESIDENT_PERMIT_NOT_FOUND);
        }
        if (!"已批准".equals(permit.getStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅可制发状态为\"已批准\"的居住证，当前状态：" + permit.getStatus());
        }
        permit.setStatus("有效");
        permit.setIssueDate(LocalDate.now());
        permit.setExpiryDate(LocalDate.now().plusYears(1));
        residentPermitMapper.updateById(permit);

        // Update register record with permit no
        LambdaQueryWrapper<FpRegisterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FpRegisterRecord::getUuid, permit.getUuid());
        FpRegisterRecord record = fpRegisterRecordMapper.selectOne(wrapper);
        if (record != null) {
            record.setResidencePermitNo(permit.getPermitNo());
            fpRegisterRecordMapper.updateById(record);
        }

        return permit;
    }

    @Override
    @Transactional
    public ResidentPermitRenewal renewPermit(Long id, ResidentPermitRenewal renewal, String operatorUuid) {
        ResidentPermit permit = residentPermitMapper.selectById(id);
        if (permit == null) {
            throw new BusinessException(ErrorCode.RESIDENT_PERMIT_NOT_FOUND);
        }
        if (!"有效".equals(permit.getStatus())) {
            throw new BusinessException(ErrorCode.RESIDENT_PERMIT_EXPIRED);
        }
        renewal.setPermitNo(permit.getPermitNo());
        renewal.setOldExpiryDate(permit.getExpiryDate());
        // Respect user-provided dates, fallback to defaults
        if (renewal.getNewExpiryDate() == null) {
            renewal.setNewExpiryDate(permit.getExpiryDate().plusYears(1));
        }
        if (renewal.getRenewalDate() == null) {
            renewal.setRenewalDate(LocalDate.now());
        }
        // Set operator UUID from authenticated user context
        if (operatorUuid != null && !operatorUuid.isEmpty()) {
            renewal.setOperatorUuid(operatorUuid);
        }
        residentPermitRenewalMapper.insert(renewal);

        permit.setExpiryDate(renewal.getNewExpiryDate());
        residentPermitMapper.updateById(permit);

        // 同步更新流动人口登记记录中的居住证信息
        LambdaQueryWrapper<FpRegisterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FpRegisterRecord::getUuid, permit.getUuid());
        FpRegisterRecord record = fpRegisterRecordMapper.selectOne(wrapper);
        if (record != null) {
            record.setResidencePermitNo(permit.getPermitNo());
            fpRegisterRecordMapper.updateById(record);
        }

        return renewal;
    }

    @Override
    @Transactional
    public ResidentRegistration registerResidence(ResidentRegistration registration) {
        if (registration.getRegisterDate() == null) {
            registration.setRegisterDate(LocalDate.now());
        }
        residentRegistrationMapper.insert(registration);
        return registration;
    }

    @Override
    @Transactional
    public ResidentRegistration changeResidence(Long rid, ResidentRegistration registration) {
        ResidentRegistration existing = residentRegistrationMapper.selectById(rid);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        // 只更新非空字段，避免覆盖已有数据
        if (registration.getCurrentAddress() != null)
            existing.setCurrentAddress(registration.getCurrentAddress());
        if (registration.getOriginalAddress() != null)
            existing.setOriginalAddress(registration.getOriginalAddress());
        if (registration.getAreaId() != null)
            existing.setAreaId(registration.getAreaId());
        if (registration.getAddressType() != null)
            existing.setAddressType(registration.getAddressType());
        if (registration.getHouseOwnership() != null)
            existing.setHouseOwnership(registration.getHouseOwnership());
        if (registration.getPurpose() != null)
            existing.setPurpose(registration.getPurpose());
        if (registration.getExpectedDuration() != null)
            existing.setExpectedDuration(registration.getExpectedDuration());
        if (registration.getWorkUnit() != null)
            existing.setWorkUnit(registration.getWorkUnit());
        if (registration.getRegisterDate() != null)
            existing.setRegisterDate(registration.getRegisterDate());
        residentRegistrationMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional
    public void cancelResidence(Long rid) {
        ResidentRegistration existing = residentRegistrationMapper.selectById(rid);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        residentRegistrationMapper.deleteById(rid);
    }

    @Override
    @Cacheable(value = "fpHeatmap", unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> getHeatmapData() {
        // Limit full table scan to 10,000 rows max
        LambdaQueryWrapper<ResidentRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.last("LIMIT 10000");
        List<ResidentRegistration> list = residentRegistrationMapper.selectList(wrapper);
        return list.stream().map(r -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("areaId", r.getAreaId());
            item.put("addressType", r.getAddressType());
            item.put("uuid", r.getUuid());
            return item;
        }).toList();
    }

    @Override
    @Cacheable(value = "fpTrend", unless = "#result == null || #result.isEmpty()")
    public List<Map<String, Object>> getTrendData() {
        // Limit full table scan to 10,000 rows max
        LambdaQueryWrapper<FpRegisterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.last("LIMIT 10000");
        List<FpRegisterRecord> list = fpRegisterRecordMapper.selectList(wrapper);
        return list.stream().map(r -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("registerDate", r.getRegisterDate() != null ? r.getRegisterDate().toString() : null);
            item.put("rid", r.getRid());
            return item;
        }).toList();
    }
}
