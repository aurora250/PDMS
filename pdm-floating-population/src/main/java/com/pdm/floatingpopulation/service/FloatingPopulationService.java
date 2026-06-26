package com.pdm.floatingpopulation.service;

import com.pdm.floatingpopulation.entity.FpRegisterRecord;
import com.pdm.floatingpopulation.entity.ResidentPermit;
import com.pdm.floatingpopulation.entity.ResidentPermitRenewal;
import com.pdm.floatingpopulation.entity.ResidentRegistration;

import java.util.List;
import java.util.Map;

/**
 * 流动人口管理服务接口。
 *
 * <p>提供流动人口管理相关的核心业务方法，包括：
 *
 * <ul>
 *   <li><b>流动人口登记</b> —— 登记、更新、注销流动人口信息。
 *   <li><b>居住证管理</b> —— 申请、审批、签发、续期居住证。
 *   <li><b>常住人口管理</b> —— 常住人口居住登记、信息变更、注销。
 *   <li><b>数据统计</b> —— 人口热力图和趋势分析。
 * </ul>
 */
public interface FloatingPopulationService {

    /**
     * 流动人口登记。
     *
     * @param record 流动人口登记记录
     * @return 创建后的登记记录
     */
    FpRegisterRecord registerFp(FpRegisterRecord record);

    /**
     * 更新流动人口登记信息。
     *
     * @param rid 记录 ID
     * @param record 更新后的登记信息
     * @return 更新后的登记记录
     */
    FpRegisterRecord updateFp(Long rid, FpRegisterRecord record);

    /**
     * 注销流动人口登记。
     *
     * @param rid 记录 ID
     */
    void cancelFp(Long rid);

    /**
     * 申请居住证。
     *
     * @param permit 居住证实體
     * @return 创建后的居住证实體
     */
    ResidentPermit applyPermit(ResidentPermit permit);

    /**
     * 审批居住证。
     *
     * @param id 居住证 ID
     * @param reviewerUuid 审核人 UUID
     * @return 审批后的居住证实體
     */
    ResidentPermit approvePermit(Long id, String reviewerUuid);

    /**
     * 签发居住证。
     *
     * @param id 居住证 ID
     * @return 签发后的居住证实体
     */
    ResidentPermit issuePermit(Long id);

    /**
     * 续期居住证。
     *
     * @param id 居住证 ID
     * @param renewal 续期信息
     * @return 续期记录
     */
    ResidentPermitRenewal renewPermit(Long id, ResidentPermitRenewal renewal);

    /**
     * 常住人口居住登记。
     *
     * @param registration 常住人口登记信息
     * @return 创建后的登记记录
     */
    ResidentRegistration registerResidence(ResidentRegistration registration);

    /**
     * 变更常住人口居住信息。
     *
     * @param rid 记录 ID
     * @param registration 更新后的登记信息
     * @return 更新后的登记记录
     */
    ResidentRegistration changeResidence(Long rid, ResidentRegistration registration);

    /**
     * 注销常住人口居住登记。
     *
     * @param rid 记录 ID
     */
    void cancelResidence(Long rid);

    /**
     * 获取人口热力图数据。
     *
     * @return 热力图数据列表
     */
    List<Map<String, Object>> getHeatmapData();

    /**
     * 获取人口趋势数据。
     *
     * @return 趋势数据列表
     */
    List<Map<String, Object>> getTrendData();
}
