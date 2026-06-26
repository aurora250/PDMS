package com.pdm.keyperson.service.impl;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.keyperson.entity.KeyPerson;
import com.pdm.keyperson.entity.PetitionRecord;
import com.pdm.keyperson.entity.VisitPlan;
import com.pdm.keyperson.mapper.KeyPersonMapper;
import com.pdm.keyperson.mapper.PetitionRecordMapper;
import com.pdm.keyperson.mapper.VisitPlanMapper;
import com.pdm.keyperson.service.KeypersonService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * 重点人员业务服务实现类
 * 实现KeypersonService接口，处理重点人员核心业务逻辑
 */
@Service
@RequiredArgsConstructor
public class KeypersonServiceImpl implements KeypersonService {

    /**
     * 重点人员Mapper接口
     */
    private final KeyPersonMapper keyPersonMapper;

    /**
     * 走访计划Mapper接口
     */
    private final VisitPlanMapper visitPlanMapper;

    /**
     * 信访记录Mapper接口
     */
    private final PetitionRecordMapper petitionRecordMapper;

    /**
     * 新增指定重点人员
     *
     * @param keyPerson 重点人员实体（包含uuid、管控等级等必填信息）
     * @return 新增后的重点人员实体
     * @throws BusinessException 若重点人员已存在则抛出该异常
     */
    @Override
    @Transactional
    public KeyPerson designateKeyPerson(KeyPerson keyPerson) {
        KeyPerson existing = keyPersonMapper.selectByUuid(keyPerson.getUuid());
        if (existing != null) {
            throw new BusinessException(ErrorCode.KEY_PERSON_ALREADY_EXISTS);
        }
        keyPerson.setDesignatedAt(LocalDateTime.now());
        keyPersonMapper.insert(keyPerson);
        return keyPerson;
    }

    /**
     * 撤销重点人员身份
     *
     * @param uuid 重点人员唯一标识
     * @throws BusinessException 若重点人员不存在则抛出该异常
     */
    @Override
    @Transactional
    public void revokeKeyPerson(String uuid) {
        KeyPerson keyPerson = keyPersonMapper.selectByUuid(uuid);
        if (keyPerson == null) {
            throw new BusinessException(ErrorCode.KEY_PERSON_NOT_FOUND);
        }
        keyPerson.setRevokedAt(LocalDateTime.now());
        keyPersonMapper.updateById(keyPerson);
    }

    /**
     * 修改重点人员管控等级
     *
     * @param uuid 重点人员唯一标识
     * @param controlLevel 新的管控等级（一级/二级/其他）
     * @return 修改后的重点人员实体
     * @throws BusinessException 若重点人员不存在则抛出该异常
     */
    @Override
    @Transactional
    public KeyPerson updateControlLevel(String uuid, String controlLevel) {
        KeyPerson keyPerson = keyPersonMapper.selectByUuid(uuid);
        if (keyPerson == null) {
            throw new BusinessException(ErrorCode.KEY_PERSON_NOT_FOUND);
        }
        keyPerson.setControlLevel(controlLevel);
        keyPersonMapper.updateById(keyPerson);
        return keyPerson;
    }

    /**
     * 多条件查询重点人员列表
     *
     * @param conditions 查询条件（支持uuid、controlLevel、controlType、responsiblePoliceNo）
     * @return 符合条件的重点人员列表
     */
    @Override
    public List<KeyPerson> searchKeyPersons(Map<String, Object> conditions) {
        LambdaQueryWrapper<KeyPerson> wrapper = new LambdaQueryWrapper<>();
        if (conditions.get("uuid") != null) {
            wrapper.eq(KeyPerson::getUuid, conditions.get("uuid"));
        }
        if (conditions.get("controlLevel") != null) {
            wrapper.eq(KeyPerson::getControlLevel, conditions.get("controlLevel"));
        }
        if (conditions.get("controlType") != null) {
            wrapper.eq(KeyPerson::getControlType, conditions.get("controlType"));
        }
        if (conditions.get("responsiblePoliceNo") != null) {
            wrapper.eq(KeyPerson::getResponsiblePoliceNo, conditions.get("responsiblePoliceNo"));
        }
        wrapper.orderByDesc(KeyPerson::getCreateTime);
        return keyPersonMapper.selectList(wrapper);
    }

    /**
     * 生成重点人员走访计划
     * 根据重点人员管控等级自动计算走访间隔（一级7天/二级30天/其他90天）
     *
     * @param visitPlan 走访计划基础信息（包含keyPersonUuid）
     * @return 生成后的走访计划（包含计划日期、状态等）
     * @throws BusinessException 若重点人员不存在则抛出该异常
     */
    @Override
    @Transactional
    public VisitPlan generateVisitPlan(VisitPlan visitPlan) {
        String uuid = visitPlan.getKeyPersonUuid();
        KeyPerson keyPerson = keyPersonMapper.selectByUuid(uuid);
        if (keyPerson == null) {
            throw new BusinessException(ErrorCode.KEY_PERSON_NOT_FOUND);
        }

        // Determine interval based on control level
        int intervalDays;
        if ("一级".equals(keyPerson.getControlLevel())) {
            intervalDays = 7;
        } else if ("二级".equals(keyPerson.getControlLevel())) {
            intervalDays = 30;
        } else {
            intervalDays = 90;
        }

        visitPlan.setPlannedDate(LocalDate.now().plusDays(intervalDays));
        visitPlan.setStatus("待走访");
        visitPlan.setIsAlerted(0);
        visitPlanMapper.insert(visitPlan);
        return visitPlan;
    }

    /**
     * 完成走访计划
     * 更新计划状态为已完成，若有信访记录则同步保存
     *
     * @param planId 走访计划ID
     * @param actualDate 实际走访日期
     * @param petitionRecord 信访记录（可选，可为null）
     * @return 完成后的走访计划
     * @throws BusinessException 若走访计划不存在则抛出该异常
     */
    @Override
    @Transactional
    public VisitPlan completeVisit(Long planId, LocalDate actualDate, PetitionRecord petitionRecord) {
        VisitPlan plan = visitPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(ErrorCode.VISIT_PLAN_NOT_FOUND);
        }
        plan.setActualDate(actualDate);
        plan.setStatus("已完成");
        visitPlanMapper.updateById(plan);

        if (petitionRecord != null) {
            petitionRecord.setKeyPersonUuid(plan.getKeyPersonUuid());
            petitionRecord.setHandlerPoliceNo(plan.getAssignedPoliceNo());
            petitionRecord.setPetitionTime(LocalDateTime.now());
            if (petitionRecord.getAddress() == null) {
                petitionRecord.setAddress("");
            }
            if (petitionRecord.getEvaluation() == null) {
                petitionRecord.setEvaluation("已完成走访");
            }
            petitionRecordMapper.insert(petitionRecord);
        }

        return plan;
    }

    /**
     * 记录重点人员信访信息
     * 自动填充信访时间为当前时间
     *
     * @param petitionRecord 信访记录实体（包含keyPersonUuid、地址、备注等）
     * @return 保存后的信访记录实体
     */
    @Override
    @Transactional
    public PetitionRecord recordPetition(PetitionRecord petitionRecord) {
        petitionRecord.setPetitionTime(LocalDateTime.now());
        petitionRecordMapper.insert(petitionRecord);
        return petitionRecord;
    }

    /**
     * 获取重点人员GIS可视化数据
     * 提取重点人员uuid、管控等级、管控类型封装为GIS数据格式
     *
     * @return GIS数据列表（Map结构：uuid/controlLevel/controlType）
     */
    @Override
    public List<Map<String, Object>> getGisData() {
        // Return simplified GIS data from key_person table
        List<KeyPerson> list = keyPersonMapper.selectList(null);
        return list.stream().map(kp -> Map.<String, Object>of("uuid", kp.getUuid(), "controlLevel",
                kp.getControlLevel(), "controlType", kp.getControlType())).toList();
    }
}