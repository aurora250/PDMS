package com.pdm.floatingpopulation.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.floatingpopulation.entity.FpRegisterRecord;
import com.pdm.floatingpopulation.entity.ResidentPermit;
import com.pdm.floatingpopulation.entity.ResidentPermitRenewal;
import com.pdm.floatingpopulation.entity.ResidentRegistration;
import com.pdm.floatingpopulation.mapper.FpRegisterRecordMapper;
import com.pdm.floatingpopulation.mapper.ResidentPermitMapper;
import com.pdm.floatingpopulation.mapper.ResidentPermitRenewalMapper;
import com.pdm.floatingpopulation.mapper.ResidentRegistrationMapper;
import com.pdm.floatingpopulation.service.FloatingPopulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
        record.setRid("FP" + IdUtil.fastSimpleUUID().substring(0, 20));
        record.setRegisterDate(LocalDate.now());
        record.setResidencePermitNo(null);
        fpRegisterRecordMapper.insert(record);
        return record;
    }

    @Override
    @Transactional
    public FpRegisterRecord updateFp(String rid, FpRegisterRecord record) {
        LambdaQueryWrapper<FpRegisterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FpRegisterRecord::getRid, rid);
        FpRegisterRecord existing = fpRegisterRecordMapper.selectOne(wrapper);
        if (existing == null) {
            throw new BusinessException(ErrorCode.FP_RECORD_NOT_FOUND);
        }
        record.setId(existing.getId());
        record.setRid(rid);
        fpRegisterRecordMapper.updateById(record);
        return record;
    }

    @Override
    @Transactional
    public void cancelFp(String rid) {
        LambdaQueryWrapper<FpRegisterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FpRegisterRecord::getRid, rid);
        FpRegisterRecord existing = fpRegisterRecordMapper.selectOne(wrapper);
        if (existing == null) {
            throw new BusinessException(ErrorCode.FP_RECORD_NOT_FOUND);
        }
        fpRegisterRecordMapper.deleteById(existing.getId());
    }

    @Override
    @Transactional
    public ResidentPermit applyPermit(ResidentPermit permit) {
        permit.setPermitNo("RP" + IdUtil.fastSimpleUUID().substring(0, 20));
        permit.setStatus("待审批");
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
        permit.setStatus("待签发");
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
    public ResidentPermitRenewal renewPermit(Long id, ResidentPermitRenewal renewal) {
        ResidentPermit permit = residentPermitMapper.selectById(id);
        if (permit == null) {
            throw new BusinessException(ErrorCode.RESIDENT_PERMIT_NOT_FOUND);
        }
        if (!"有效".equals(permit.getStatus())) {
            throw new BusinessException(ErrorCode.RESIDENT_PERMIT_EXPIRED);
        }
        renewal.setRenewalId("RN" + IdUtil.fastSimpleUUID().substring(0, 20));
        renewal.setPermitNo(permit.getPermitNo());
        renewal.setOldExpiryDate(permit.getExpiryDate());
        renewal.setNewExpiryDate(permit.getExpiryDate().plusYears(1));
        renewal.setRenewalDate(LocalDate.now());
        residentPermitRenewalMapper.insert(renewal);

        permit.setExpiryDate(renewal.getNewExpiryDate());
        residentPermitMapper.updateById(permit);

        return renewal;
    }

    @Override
    @Transactional
    public ResidentRegistration registerResidence(ResidentRegistration registration) {
        registration.setRid("RS" + IdUtil.fastSimpleUUID().substring(0, 20));
        registration.setRegisterDate(LocalDate.now());
        residentRegistrationMapper.insert(registration);
        return registration;
    }

    @Override
    @Transactional
    public ResidentRegistration changeResidence(String rid, ResidentRegistration registration) {
        LambdaQueryWrapper<ResidentRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResidentRegistration::getRid, rid);
        ResidentRegistration existing = residentRegistrationMapper.selectOne(wrapper);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        registration.setId(existing.getId());
        registration.setRid(rid);
        residentRegistrationMapper.updateById(registration);
        return registration;
    }

    @Override
    @Transactional
    public void cancelResidence(String rid) {
        LambdaQueryWrapper<ResidentRegistration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ResidentRegistration::getRid, rid);
        ResidentRegistration existing = residentRegistrationMapper.selectOne(wrapper);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        residentRegistrationMapper.deleteById(existing.getId());
    }

    @Override
    public List<Map<String, Object>> getHeatmapData() {
        // Return simplified heatmap data grouped by area
        List<ResidentRegistration> list = residentRegistrationMapper.selectList(null);
        return list.stream().map(r -> Map.<String, Object>of(
                "areaId", r.getAreaId(),
                "addressType", r.getAddressType(),
                "uuid", r.getUuid()
        )).toList();
    }

    @Override
    public List<Map<String, Object>> getTrendData() {
        // Return simplified trend data grouped by register date
        List<FpRegisterRecord> list = fpRegisterRecordMapper.selectList(null);
        return list.stream().map(r -> Map.<String, Object>of(
                "registerDate", r.getRegisterDate() != null ? r.getRegisterDate().toString() : null,
                "rid", r.getRid()
        )).toList();
    }
}
