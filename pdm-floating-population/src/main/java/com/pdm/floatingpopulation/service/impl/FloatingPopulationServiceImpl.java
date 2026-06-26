package com.pdm.floatingpopulation.service.impl;

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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;

/**
 * 流动人口管理服务实现类，提供流动人口登记、居住证管理和数据统计等功能。
 *
 * <ul>
 * <li><b>流动人口登记</b> —— 登记、更新、注销流动人口信息。</li>
 * <li><b>居住证管理</b> —— 申请、审批、签发、续期居住证，签发时自动关联登记记录。</li>
 * <li><b>常住人口管理</b> —— 居住登记、信息变更（仅更新非空字段）、注销。</li>
 * <li><b>数据统计</b> —— 人口热力图和趋势分析。</li>
 * </ul>
 *
 * @author freedom
 */
@Service
@RequiredArgsConstructor
public class FloatingPopulationServiceImpl implements FloatingPopulationService {

    /** 流动人口登记记录 Mapper */
    private final FpRegisterRecordMapper fpRegisterRecordMapper;
    /** 居住证 Mapper */
    private final ResidentPermitMapper residentPermitMapper;
    /** 居住证续期记录 Mapper */
    private final ResidentPermitRenewalMapper residentPermitRenewalMapper;
    /** 常住人口登记 Mapper */
    private final ResidentRegistrationMapper residentRegistrationMapper;

    /**
     * 流动人口登记。
     *
     * <p>
     * 登记日期自动设为当天，居住证编号初始为 {@code null}，待签发时回填。
     * </p>
     *
     * @param record
     *            流动人口登记记录
     * @return 创建后的登记记录
     */
    @Override
    @Transactional
    public FpRegisterRecord registerFp(FpRegisterRecord record) {
        record.setRegisterDate(LocalDate.now());
        record.setResidencePermitNo(null);
        fpRegisterRecordMapper.insert(record);
        return record;
    }

    /**
     * 更新流动人口登记信息。
     *
     * @param rid
     *            记录 ID
     * @param record
     *            更新后的登记信息
     * @return 更新后的登记记录
     * @throws BusinessException
     *             登记记录不存在时抛出
     */
    @Override
    @Transactional
    public FpRegisterRecord updateFp(Long rid, FpRegisterRecord record) {
        FpRegisterRecord existing = fpRegisterRecordMapper.selectById(rid);
        if (existing == null) {
            throw new BusinessException(ErrorCode.FP_RECORD_NOT_FOUND);
        }
        record.setRid(rid);
        fpRegisterRecordMapper.updateById(record);
        return record;
    }

    /**
     * 注销流动人口登记。
     *
     * @param rid
     *            记录 ID
     * @throws BusinessException
     *             登记记录不存在时抛出
     */
    @Override
    @Transactional
    public void cancelFp(Long rid) {
        FpRegisterRecord existing = fpRegisterRecordMapper.selectById(rid);
        if (existing == null) {
            throw new BusinessException(ErrorCode.FP_RECORD_NOT_FOUND);
        }
        fpRegisterRecordMapper.deleteById(rid);
    }

    /**
     * 申请居住证。
     *
     * <p>
     * 自动以 {@code "RP"} 为前缀生成 20 位唯一证件编号，签发日期默认为当天， 有效期默认为签发日期起一年，初始状态为"有效"。
     * </p>
     *
     * @param permit
     *            居住证实体
     * @return 创建后的居住证实体
     */
    @Override
    @Transactional
    public ResidentPermit applyPermit(ResidentPermit permit) {
        permit.setPermitNo("RP" + IdUtil.fastSimpleUUID().substring(0, 20));
        permit.setStatus("有效");
        if (permit.getIssueDate() == null) {
            permit.setIssueDate(LocalDate.now());
        }
        if (permit.getExpiryDate() == null) {
            permit.setExpiryDate(LocalDate.now().plusYears(1));
        }
        residentPermitMapper.insert(permit);
        return permit;
    }

    /**
     * 审批居住证。
     *
     * @param id
     *            居住证 ID
     * @param reviewerUuid
     *            审核人 UUID
     * @return 审批后的居住证实体
     * @throws BusinessException
     *             居住证不存在时抛出
     */
    @Override
    @Transactional
    public ResidentPermit approvePermit(Long id, String reviewerUuid) {
        ResidentPermit permit = residentPermitMapper.selectById(id);
        if (permit == null) {
            throw new BusinessException(ErrorCode.RESIDENT_PERMIT_NOT_FOUND);
        }
        permit.setStatus("有效");
        residentPermitMapper.updateById(permit);
        return permit;
    }

    /**
     * 签发居住证。
     *
     * <p>
     * 设置签发日期和有效期（签发日期起一年），并将证件编号回填到关联的流动人口登记记录中。
     * </p>
     *
     * @param id
     *            居住证 ID
     * @return 签发后的居住证实体
     * @throws BusinessException
     *             居住证不存在时抛出
     */
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

    /**
     * 续期居住证。
     *
     * <p>
     * 仅状态为"有效"的居住证方可续期，续期后有效期延长一年。 续期记录保存原有效期和新有效期信息。
     * </p>
     *
     * @param id
     *            居住证 ID
     * @param renewal
     *            续期信息
     * @return 续期记录
     * @throws BusinessException
     *             居住证不存在或已过期时抛出
     */
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
        renewal.setPermitNo(permit.getPermitNo());
        renewal.setOldExpiryDate(permit.getExpiryDate());
        renewal.setNewExpiryDate(permit.getExpiryDate().plusYears(1));
        renewal.setRenewalDate(LocalDate.now());
        residentPermitRenewalMapper.insert(renewal);

        permit.setExpiryDate(renewal.getNewExpiryDate());
        residentPermitMapper.updateById(permit);

        return renewal;
    }

    /**
     * 常住人口居住登记。
     *
     * <p>
     * 登记日期自动设为当天。
     * </p>
     *
     * @param registration
     *            常住人口登记信息
     * @return 创建后的登记记录
     */
    @Override
    @Transactional
    public ResidentRegistration registerResidence(ResidentRegistration registration) {
        registration.setRegisterDate(LocalDate.now());
        residentRegistrationMapper.insert(registration);
        return registration;
    }

    /**
     * 变更常住人口居住信息。
     *
     * <p>
     * 仅更新请求中提供的非空字段，未提供的字段保持不变， 避免覆盖已有有效数据。
     * </p>
     *
     * @param rid
     *            记录 ID
     * @param registration
     *            更新后的登记信息
     * @return 更新后的登记记录
     * @throws BusinessException
     *             登记记录不存在时抛出
     */
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

    /**
     * 注销常住人口居住登记。
     *
     * @param rid
     *            记录 ID
     * @throws BusinessException
     *             登记记录不存在时抛出
     */
    @Override
    @Transactional
    public void cancelResidence(Long rid) {
        ResidentRegistration existing = residentRegistrationMapper.selectById(rid);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        }
        residentRegistrationMapper.deleteById(rid);
    }

    /**
     * 获取人口热力图数据。
     *
     * <p>
     * 基于常住人口分布数据生成区域维度的热力图数据， 包含区域 ID、地址类型和居民 UUID。
     * </p>
     *
     * @return 热力图数据列表
     */
    @Override
    public List<Map<String, Object>> getHeatmapData() {
        List<ResidentRegistration> list = residentRegistrationMapper.selectList(null);
        return list.stream().map(r -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("areaId", r.getAreaId());
            item.put("addressType", r.getAddressType());
            item.put("uuid", r.getUuid());
            return item;
        }).toList();
    }

    /**
     * 获取人口趋势数据。
     *
     * <p>
     * 基于流动人口登记记录生成时间维度的趋势分析数据， 包含登记日期和记录 ID。
     * </p>
     *
     * @return 趋势数据列表
     */
    @Override
    public List<Map<String, Object>> getTrendData() {
        List<FpRegisterRecord> list = fpRegisterRecordMapper.selectList(null);
        return list.stream().map(r -> {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("registerDate", r.getRegisterDate() != null ? r.getRegisterDate().toString() : null);
            item.put("rid", r.getRid());
            return item;
        }).toList();
    }
}
