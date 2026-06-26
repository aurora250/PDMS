package com.pdm.floatingpopulation.controller;

import com.pdm.common.core.result.Result;
import com.pdm.floatingpopulation.entity.FpRegisterRecord;
import com.pdm.floatingpopulation.entity.ResidentPermit;
import com.pdm.floatingpopulation.entity.ResidentPermitRenewal;
import com.pdm.floatingpopulation.entity.ResidentRegistration;
import com.pdm.floatingpopulation.service.FloatingPopulationService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * 流动人口管理控制器。
 *
 * <p>提供流动人口管理相关的 REST API 接口，包括流动人口登记、居住证管理（申请、审批、签发、续期）、 常住人口居住登记管理以及数据统计（热力图、趋势分析）。 所有接口挂载在
 * {@code /api/fp} 路径下，需携带有效 JWT 令牌方可访问。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fp")
public class FloatingPopulationController {

    private final FloatingPopulationService floatingPopulationService;

    /**
     * 流动人口登记。
     *
     * <p>录入流动人口的基本信息，登记日期自动设为当天。
     *
     * @param record 流动人口登记记录
     * @return 创建后的登记记录
     */
    @PostMapping("/register")
    public Result<FpRegisterRecord> registerFp(@RequestBody FpRegisterRecord record) {
        return Result.success(floatingPopulationService.registerFp(record));
    }

    /**
     * 更新流动人口登记信息。
     *
     * @param rid 记录 ID
     * @param record 更新后的登记信息
     * @return 更新后的登记记录
     */
    @PutMapping("/register/{rid}")
    public Result<FpRegisterRecord> updateFp(
            @PathVariable Long rid, @RequestBody FpRegisterRecord record) {
        return Result.success(floatingPopulationService.updateFp(rid, record));
    }

    /**
     * 注销流动人口登记。
     *
     * @param rid 记录 ID
     * @return 操作结果
     */
    @DeleteMapping("/register/{rid}")
    public Result<Void> cancelFp(@PathVariable Long rid) {
        floatingPopulationService.cancelFp(rid);
        return Result.success();
    }

    /**
     * 申请居住证。
     *
     * <p>流动人口提交居住证申请，系统自动生成证件编号并初始化状态。
     *
     * @param permit 居住证实体
     * @return 创建后的居住证实體
     */
    @PostMapping("/permit/apply")
    public Result<ResidentPermit> applyPermit(@RequestBody ResidentPermit permit) {
        return Result.success(floatingPopulationService.applyPermit(permit));
    }

    /**
     * 审批居住证。
     *
     * @param id 居住证 ID
     * @param reviewerUuid 审核人 UUID，从请求头 {@code X-User-Uuid} 中获取
     * @return 审批后的居住证实體
     */
    @PutMapping("/permit/{id}/approve")
    public Result<ResidentPermit> approvePermit(
            @PathVariable Long id, @RequestHeader("X-User-Uuid") String reviewerUuid) {
        return Result.success(floatingPopulationService.approvePermit(id, reviewerUuid));
    }

    /**
     * 签发居住证。
     *
     * <p>审批通过后正式签发居住证，设置签发日期和有效期并关联到登记记录。
     *
     * @param id 居住证 ID
     * @return 签发后的居住证实體
     */
    @PutMapping("/permit/{id}/issue")
    public Result<ResidentPermit> issuePermit(@PathVariable Long id) {
        return Result.success(floatingPopulationService.issuePermit(id));
    }

    /**
     * 续期居住证。
     *
     * <p>为有效期即将届满的居住证办理续期，延长有效期一年。
     *
     * @param id 居住证 ID
     * @param renewal 续期信息
     * @return 续期记录
     */
    @PostMapping("/permit/{id}/renew")
    public Result<ResidentPermitRenewal> renewPermit(
            @PathVariable Long id, @RequestBody ResidentPermitRenewal renewal) {
        return Result.success(floatingPopulationService.renewPermit(id, renewal));
    }

    /**
     * 常住人口居住登记。
     *
     * @param registration 常住人口登记信息
     * @return 创建后的登记记录
     */
    @PostMapping("/residence/register")
    public Result<ResidentRegistration> registerResidence(
            @RequestBody ResidentRegistration registration) {
        return Result.success(floatingPopulationService.registerResidence(registration));
    }

    /**
     * 变更常住人口居住信息。
     *
     * <p>仅更新请求中提供的非空字段，未提供的字段保持不变。
     *
     * @param rid 记录 ID
     * @param registration 更新后的登记信息
     * @return 更新后的登记记录
     */
    @PutMapping("/residence/{rid}")
    public Result<ResidentRegistration> changeResidence(
            @PathVariable Long rid, @RequestBody ResidentRegistration registration) {
        return Result.success(floatingPopulationService.changeResidence(rid, registration));
    }

    /**
     * 注销常住人口居住登记。
     *
     * @param rid 记录 ID
     * @return 操作结果
     */
    @DeleteMapping("/residence/{rid}")
    public Result<Void> cancelResidence(@PathVariable Long rid) {
        floatingPopulationService.cancelResidence(rid);
        return Result.success();
    }

    /**
     * 获取人口热力图数据。
     *
     * <p>基于常住人口分布数据生成区域人口密度热力图。
     *
     * @return 热力图数据列表，包含区域 ID、地址类型和居民 UUID
     */
    @GetMapping("/statistics/heatmap")
    public Result<List<Map<String, Object>>> getHeatmapData() {
        return Result.success(floatingPopulationService.getHeatmapData());
    }

    /**
     * 获取人口趋势数据。
     *
     * <p>基于流动人口登记记录生成时间维度的趋势分析数据。
     *
     * @return 趋势数据列表，包含登记日期和记录 ID
     */
    @GetMapping("/statistics/trend")
    public Result<List<Map<String, Object>>> getTrendData() {
        return Result.success(floatingPopulationService.getTrendData());
    }
}
