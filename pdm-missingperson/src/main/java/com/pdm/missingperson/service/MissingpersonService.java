package com.pdm.missingperson.service;

import com.pdm.common.dto.PageRequest;
import com.pdm.common.dto.PageResult;
import com.pdm.missingperson.entity.MissingPerson;
import com.pdm.missingperson.entity.MissingPersonRecovery;
import java.util.Map;

/**
 * 失踪人员业务服务接口
 * 定义失踪人员注册、注销、寻回记录、搜索、统计等核心业务逻辑
 */
public interface MissingpersonService {

    /**
     * 注册失踪人员信息
     *
     * @param missingPerson 失踪人员实体信息
     * @return 注册后的失踪人员实体（包含自增主键等信息）
     */
    MissingPerson register(MissingPerson missingPerson);

    /**
     * 注销失踪人员记录
     *
     * @param rid 失踪人员记录主键ID
     */
    void cancel(Long rid);

    /**
     * 记录失踪人员寻回信息
     *
     * @param recovery 寻回记录实体信息
     * @return 保存后的寻回记录实体（包含自增主键等信息）
     */
    MissingPersonRecovery recordRecovery(MissingPersonRecovery recovery);

    /**
     * 分页搜索失踪人员信息
     *
     * @param residentUuid 居民唯一标识（可选）
     * @param status 状态（可选）
     * @param name 姓名（可选）
     * @param pageRequest 分页请求参数（页码、页大小、排序等）
     * @return 分页结果集，包含失踪人员列表及分页信息
     */
    PageResult<MissingPerson> search(String residentUuid, String status, String name, PageRequest pageRequest);

    /**
     * 获取失踪人员统计数据
     *
     * @return 统计结果Map，包含总数量、失踪中数量、已寻回数量、按性别/年龄段统计等信息
     */
    Map<String, Object> getStatistics();
}