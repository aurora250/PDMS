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

@Service
@RequiredArgsConstructor
public class KeypersonServiceImpl implements KeypersonService {

    private final KeyPersonMapper keyPersonMapper;
    private final VisitPlanMapper visitPlanMapper;
    private final PetitionRecordMapper petitionRecordMapper;

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

    @Override
    @Transactional
    public VisitPlan completeVisit(
            Long planId, LocalDate actualDate, PetitionRecord petitionRecord) {
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

    @Override
    @Transactional
    public PetitionRecord recordPetition(PetitionRecord petitionRecord) {
        petitionRecord.setPetitionTime(LocalDateTime.now());
        petitionRecordMapper.insert(petitionRecord);
        return petitionRecord;
    }

    @Override
    public List<Map<String, Object>> getGisData() {
        // Return simplified GIS data from key_person table
        List<KeyPerson> list = keyPersonMapper.selectList(null);
        return list.stream()
                .map(
                        kp ->
                                Map.<String, Object>of(
                                        "uuid",
                                        kp.getUuid(),
                                        "controlLevel",
                                        kp.getControlLevel(),
                                        "controlType",
                                        kp.getControlType()))
                .toList();
    }
}
