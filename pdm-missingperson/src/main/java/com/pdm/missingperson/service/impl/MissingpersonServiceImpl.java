package com.pdm.missingperson.service.impl;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.common.dto.PageRequest;
import com.pdm.common.dto.PageResult;
import com.pdm.missingperson.entity.MissingPerson;
import com.pdm.missingperson.entity.MissingPersonRecovery;
import com.pdm.missingperson.mapper.MissingPersonMapper;
import com.pdm.missingperson.mapper.MissingPersonRecoveryMapper;
import com.pdm.missingperson.service.MissingpersonService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MissingpersonServiceImpl implements MissingpersonService {

    private final MissingPersonMapper missingPersonMapper;
    private final MissingPersonRecoveryMapper recoveryMapper;

    @Override
    @Transactional
    public MissingPerson register(MissingPerson missingPerson) {
        if (missingPerson.getStatus() == null) {
            missingPerson.setStatus("失踪中");
        }
        missingPersonMapper.insert(missingPerson);
        return missingPerson;
    }

    @Override
    @Transactional
    public void cancel(Long rid) {
        MissingPerson missingPerson = missingPersonMapper.selectById(rid);
        if (missingPerson == null) {
            throw new BusinessException(ErrorCode.MISSING_PERSON_NOT_FOUND);
        }
        missingPersonMapper.deleteById(rid);
    }

    @Override
    @Transactional
    public MissingPersonRecovery recordRecovery(MissingPersonRecovery recovery) {
        Long missingRecordRid = recovery.getMissingRecordRid();
        MissingPerson missingPerson = missingPersonMapper.selectById(missingRecordRid);
        if (missingPerson == null) {
            throw new BusinessException(ErrorCode.MISSING_PERSON_NOT_FOUND);
        }
        if ("已经寻回".equals(missingPerson.getStatus())) {
            throw new BusinessException(ErrorCode.MISSING_PERSON_ALREADY_RECOVERED);
        }

        recoveryMapper.insert(recovery);

        missingPerson.setStatus("已经寻回");
        missingPersonMapper.updateById(missingPerson);

        return recovery;
    }

    @Override
    public PageResult<MissingPerson> search(String residentUuid, String status, String name, PageRequest pageRequest) {
        LambdaQueryWrapper<MissingPerson> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(residentUuid)) {
            wrapper.eq(MissingPerson::getResidentUuid, residentUuid);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(MissingPerson::getStatus, status);
        }
        if (StringUtils.hasText(name)) {
            wrapper.like(MissingPerson::getName, name);
        }
        wrapper.orderByDesc(MissingPerson::getCreateTime);

        IPage<MissingPerson> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        IPage<MissingPerson> result = missingPersonMapper.selectPage(page, wrapper);

        return PageResult.of(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalCount = missingPersonMapper.selectCount(null);
        stats.put("totalCount", totalCount);

        long missingCount = missingPersonMapper
                .selectCount(new LambdaQueryWrapper<MissingPerson>().eq(MissingPerson::getStatus, "失踪中"));
        stats.put("missingCount", missingCount);

        long recoveredCount = missingPersonMapper
                .selectCount(new LambdaQueryWrapper<MissingPerson>().eq(MissingPerson::getStatus, "已经寻回"));
        stats.put("recoveredCount", recoveredCount);

        Map<String, Long> byGender = new HashMap<>();
        byGender.put("男", 0L);
        byGender.put("女", 0L);
        stats.put("byGender", byGender);

        Map<String, Long> byAgeGroup = new HashMap<>();
        byAgeGroup.put("0-12", 0L);
        byAgeGroup.put("13-18", 0L);
        byAgeGroup.put("19-35", 0L);
        byAgeGroup.put("36-60", 0L);
        byAgeGroup.put("60+", 0L);
        stats.put("byAgeGroup", byAgeGroup);

        return stats;
    }
}
