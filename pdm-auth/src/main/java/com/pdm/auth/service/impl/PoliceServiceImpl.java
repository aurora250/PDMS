package com.pdm.auth.service.impl;

import com.pdm.auth.entity.Police;
import com.pdm.auth.mapper.PoliceMapper;
import com.pdm.auth.service.PoliceService;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PoliceServiceImpl implements PoliceService {

    private final PoliceMapper policeMapper;

    @Override
    @Transactional
    public Police registerPolice(Police police) {
        if (police.getPoliceNumber() == null) {
            police.setPoliceNumber("P" + IdUtil.fastSimpleUUID().substring(0, 15));
        }
        if (policeMapper.selectByPoliceNumber(police.getPoliceNumber()) != null) {
            throw new BusinessException(ErrorCode.DATA_DUPLICATE, "警号已存在");
        }
        policeMapper.insert(police);
        return police;
    }

    @Override
    public Page<Police> listPolice(int page, int size, String keyword) {
        LambdaQueryWrapper<Police> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Police::getPoliceNumber, keyword).or().like(Police::getPoliceStation, keyword).or()
                    .like(Police::getDepartment, keyword));
        }
        wrapper.orderByDesc(Police::getCreateTime);
        return policeMapper.selectPage(Page.of(page, size), wrapper);
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
