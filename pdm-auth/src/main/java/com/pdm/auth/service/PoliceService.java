package com.pdm.auth.service;

import com.pdm.auth.entity.Police;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 警员服务接口。
 *
 * <p>定义警员管理相关的业务操作，包括注册、分页查询、按警号查询、信息更新和值班状态管理。
 */
public interface PoliceService {

    /**
     * 注册警员。
     *
     * @param police 待注册的警员实体
     * @return 注册成功后的警员实体
     */
    Police registerPolice(Police police);

    /**
     * 分页查询警员列表，支持关键词模糊搜索。
     *
     * @param page 页码
     * @param size 每页条数
     * @param keyword 搜索关键词（可选）
     * @return 警员分页结果
     */
    Page<Police> listPolice(int page, int size, String keyword);

    /**
     * 根据警号查询警员。
     *
     * @param policeNumber 警号
     * @return 警员实体
     */
    Police getPoliceByNumber(String policeNumber);

    /**
     * 更新警员信息。
     *
     * @param policeNumber 警号
     * @param updates 包含待更新字段的警员对象
     * @return 更新后的警员实体
     */
    Police updatePolice(String policeNumber, Police updates);

    /**
     * 更新警员值班状态。
     *
     * @param policeNumber 警号
     * @param dutyStatus 值班状态
     */
    void updatePoliceStatus(String policeNumber, String dutyStatus);
}
