package com.pdm.auth.service.impl;

import com.pdm.auth.entity.Police;
import com.pdm.auth.mapper.PoliceMapper;
import com.pdm.auth.service.PoliceService;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PoliceServiceImpl implements PoliceService {

    private final PoliceMapper policeMapper;

    @Override
    @Transactional
    public Police registerPolice(Police police) {
        if (police.getPoliceNumber() == null || police.getPoliceNumber().isBlank()) {
            // Auto-generate: P + 8 random digits, collision check below
            String generated;
            do {
                generated = "P" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
            } while (policeMapper.selectByPoliceNumber(generated) != null);
            police.setPoliceNumber(generated);
        }
        // Validate format: P + 8 digits
        if (!police.getPoliceNumber().matches("^P\\d{8}$")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "警号格式错误，应为P+8位数字，如 P11010001");
        }
        if (policeMapper.selectByPoliceNumber(police.getPoliceNumber()) != null) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATE, "警号已存在");
        }
        policeMapper.insert(police);
        return police;
    }

    @Override
    public Page<Police> listPolice(int page, int size, String keyword, String residentUuid) {
        return policeMapper.selectPageWithResidentName(Page.of(page, size),
                StringUtils.hasText(keyword) ? keyword : null,
                StringUtils.hasText(residentUuid) ? residentUuid : null);
    }

    @Override
    public Police getPoliceByNumber(String policeNumber) {
        Police police = policeMapper.selectByPoliceNumber(policeNumber);
        if (police == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "警员不存在");
        }
        return police;
    }

    @Override
    @Transactional
    public Police updatePolice(String policeNumber, Police updates) {
        Police police = policeMapper.selectByPoliceNumber(policeNumber);
        if (police == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "警员不存在");
        }
        if (StringUtils.hasText(updates.getPoliceStation())) {
            police.setPoliceStation(updates.getPoliceStation());
        }
        if (StringUtils.hasText(updates.getJurisdiction())) {
            police.setJurisdiction(updates.getJurisdiction());
        }
        if (updates.getAreaId() != null) {
            police.setAreaId(updates.getAreaId());
        }
        if (StringUtils.hasText(updates.getDepartment())) {
            police.setDepartment(updates.getDepartment());
        }
        if (StringUtils.hasText(updates.getPoliceRank())) {
            police.setPoliceRank(updates.getPoliceRank());
        }
        policeMapper.updateById(police);
        return police;
    }

    @Override
    @Transactional
    public void updatePoliceStatus(String policeNumber, String dutyStatus) {
        Police police = policeMapper.selectByPoliceNumber(policeNumber);
        if (police == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "警员不存在");
        }
        if (!List.of("在岗", "调岗", "离职").contains(dutyStatus)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "无效的值班状态");
        }
        police.setDutyStatus(dutyStatus);
        policeMapper.updateById(police);
    }
}
