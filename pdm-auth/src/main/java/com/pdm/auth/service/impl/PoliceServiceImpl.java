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

/**
 * 警员服务实现类，提供警员的注册、查询、更新和状态管理功能。
 *
 * <ul>
 * <li><b>注册警员</b> —— 新增警员记录，未指定警号时自动生成唯一警号。</li>
 * <li><b>分页查询</b> —— 支持按警号、派出所、部门进行关键词模糊搜索，按创建时间倒序排列。</li>
 * <li><b>按警号查询</b> —— 根据警号精确查找警员信息。</li>
 * <li><b>更新警员信息</b> —— 支持部分字段更新（派出所、辖区、区域、部门、警衔）。</li>
 * <li><b>更新值班状态</b> —— 修改警员的在岗状态，仅允许"在岗""调岗""离职"三种值。</li>
 * </ul>
 *
 * @author freedom
 */
@Service
@RequiredArgsConstructor
public class PoliceServiceImpl implements PoliceService {

    private final PoliceMapper policeMapper;

    /**
     * 注册警员。
     *
     * <p>
     * 若传入的警员未指定警号，则自动以 {@code "P"} 为前缀生成 15 位唯一标识作为警号。 生成后会校验警号是否已存在，重复则抛出异常。
     *
     * @param police
     *            待注册的警员实体
     * @return 注册成功后的警员实体（含自动生成的警号）
     * @throws BusinessException
     *             警号已存在时抛出
     */
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

    /**
     * 分页查询警员列表。
     *
     * <p>
     * 支持按关键词模糊搜索警号、派出所和部门（多字段 OR 匹配），结果按创建时间倒序排列。 关键词为空时返回全部记录。
     *
     * @param page
     *            页码（从 1 开始）
     * @param size
     *            每页条数
     * @param keyword
     *            搜索关键词（可选，匹配警号、派出所、部门）
     * @return 分页结果，包含警员列表及分页信息
     */
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

    /**
     * 根据警号查询警员。
     *
     * @param policeNumber
     *            警号
     * @return 警员实体
     * @throws BusinessException
     *             警员不存在时抛出
     */
    @Override
    public Police getPoliceByNumber(String policeNumber) {
        Police police = policeMapper.selectByPoliceNumber(policeNumber);
        if (police == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND, "警员不存在");
        }
        return police;
    }

    /**
     * 更新警员信息。
     *
     * <p>
     * 根据警号定位警员，仅更新传入对象中非空的字段：
     * <ul>
     * <li>派出所（{@code policeStation}）</li>
     * <li>辖区（{@code jurisdiction}）</li>
     * <li>区域 ID（{@code areaId}）</li>
     * <li>部门（{@code department}）</li>
     * <li>警衔（{@code policeRank}）</li>
     * </ul>
     *
     * @param policeNumber
     *            警号
     * @param updates
     *            包含待更新字段的警员对象（仅非空字段生效）
     * @return 更新后的警员实体
     * @throws BusinessException
     *             警员不存在时抛出
     */
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

    /**
     * 更新警员值班状态。
     *
     * <p>
     * 仅允许以下三种状态值：
     * <ul>
     * <li>{@code "在岗"} —— 当前在岗</li>
     * <li>{@code "调岗"} —— 已调岗</li>
     * <li>{@code "离职"} —— 已离职</li>
     * </ul>
     *
     * @param policeNumber
     *            警号
     * @param dutyStatus
     *            值班状态（必须为"在岗""调岗""离职"之一）
     * @throws BusinessException
     *             警员不存在或状态值无效时抛出
     */
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
