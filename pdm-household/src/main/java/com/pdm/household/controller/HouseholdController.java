package com.pdm.household.controller;

import com.pdm.common.core.result.Result;
import com.pdm.household.entity.*;
import com.pdm.household.service.HouseholdService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * 户政管理控制器。
 *
 * <p>
 * 提供户政业务相关的 REST API 接口，包括户口本管理（申请、补办、换新）、
 * 户政业务审批（登记、注销、户主变更）、户口迁移管理（申请、审批、轨迹查询）、 证件签发（准迁证、迁移证）以及行政区划查询。 所有接口均需携带有效的 JWT
 * 令牌方可访问。
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class HouseholdController {

    private final HouseholdService householdService;

    /**
     * 申请户口本。
     *
     * <p>
     * 提交户口本申请信息，系统自动生成户口本编号并初始化状态。
     * </p>
     *
     * @param book
     *            户口本申请实体
     * @return 创建后的户口本实体
     */
    @PostMapping("/api/household/book/apply")
    public Result<HouseholdRegister> applyBook(@RequestBody HouseholdRegister book) {
        return Result.success(householdService.applyBook(book));
    }

    /**
     * 补办户口本。
     *
     * <p>
     * 根据户口本编号补办丢失或损坏的户口本。
     * </p>
     *
     * @param body
     *            请求体，包含 {@code bookNo} 字段
     * @return 补办后的户口本实体
     */
    @PostMapping("/api/household/book/reissue")
    public Result<HouseholdRegister> reissueBook(@RequestBody Map<String, String> body) {
        return Result.success(householdService.reissueBook(body.get("bookNo")));
    }

    /**
     * 换新户口本。
     *
     * <p>
     * 根据户口本编号换新已满页或破损的户口本。
     * </p>
     *
     * @param body
     *            请求体，包含 {@code bookNo} 字段
     * @return 换新后的户口本实体
     */
    @PostMapping("/api/household/book/renew")
    public Result<HouseholdRegister> renewBook(@RequestBody Map<String, String> body) {
        return Result.success(householdService.renewBook(body.get("bookNo")));
    }

    /**
     * 提交户政业务申请。
     *
     * <p>
     * 适用于户籍登记、注销、户主变更等业务场景。 提交后进入四级审批流程。
     * </p>
     *
     * @param req
     *            业务申请实体
     * @return 创建后的业务申请实体
     */
    @PostMapping("/api/household/business")
    public Result<HouseholdBusinessRequest> submitBusiness(@RequestBody HouseholdBusinessRequest req) {
        return Result.success(householdService.submitBusiness(req));
    }

    /**
     * 审批户政业务。
     *
     * <p>
     * 处理人对待审批的户政业务进行审核，可审批通过或驳回。
     * </p>
     *
     * @param rid
     *            业务申请 ID
     * @param body
     *            请求体，包含 {@code status} 审批状态和 {@code rejectReason} 驳回原因
     * @param handlerUuid
     *            处理人 UUID，从请求头 {@code X-User-Uuid} 中获取
     * @return 更新后的业务申请实体
     */
    @PutMapping("/api/household/business/{rid}/approve")
    public Result<HouseholdBusinessRequest> approveBusiness(@PathVariable Long rid,
            @RequestBody Map<String, String> body, @RequestHeader("X-User-Uuid") String handlerUuid) {
        return Result.success(
                householdService.approveBusiness(rid, body.get("status"), handlerUuid, body.get("rejectReason")));
    }

    /**
     * 提交户口迁移申请。
     *
     * <p>
     * 居民提交户口迁移申请，包括迁入地和迁出地信息。
     * </p>
     *
     * @param req
     *            迁移申请实体
     * @return 创建后的迁移申请实体
     */
    @PostMapping("/api/household/migration")
    public Result<HouseholdMigrationRequest> submitMigration(@RequestBody HouseholdMigrationRequest req) {
        return Result.success(householdService.submitMigration(req));
    }

    /**
     * 审批户口迁移申请。
     *
     * <p>
     * 处理人对户口迁移申请进行审核，可审批通过或驳回。
     * </p>
     *
     * @param rid
     *            迁移申请 ID
     * @param body
     *            请求体，包含 {@code status} 审批状态和 {@code rejectReason} 驳回原因
     * @param handlerUuid
     *            处理人 UUID，从请求头 {@code X-User-Uuid} 中获取
     * @return 更新后的迁移申请实体
     */
    @PutMapping("/api/household/migration/{rid}/approve")
    public Result<HouseholdMigrationRequest> approveMigration(@PathVariable Long rid,
            @RequestBody Map<String, String> body, @RequestHeader("X-User-Uuid") String handlerUuid) {
        return Result.success(
                householdService.approveMigration(rid, body.get("status"), handlerUuid, body.get("rejectReason")));
    }

    /**
     * 查询居民的迁移轨迹。
     *
     * <p>
     * 根据居民 UUID 查询其所有历史迁移记录。
     * </p>
     *
     * @param uuid
     *            居民 UUID
     * @return 迁移申请列表
     */
    @GetMapping("/api/household/migration/trace/{uuid}")
    public Result<List<HouseholdMigrationRequest>> getMigrationTrace(@PathVariable String uuid) {
        return Result.success(householdService.getMigrationTrace(uuid));
    }

    /**
     * 签发准迁证。
     *
     * <p>
     * 为通过迁移审批的居民签发准迁证，是户口迁移流程中的重要环节。
     * </p>
     *
     * @param permit
     *            准迁证实體
     * @return 创建后的准迁证实体
     */
    @PostMapping("/api/household/approval-permit")
    public Result<ApprovalPermit> issueApprovalPermit(@RequestBody ApprovalPermit permit) {
        return Result.success(householdService.issueApprovalPermit(permit));
    }

    /**
     * 签发迁移证。
     *
     * <p>
     * 为已完成准迁的居民签发迁移证。
     * </p>
     *
     * @param permit
     *            迁移证实体
     * @return 创建后的迁移证实体
     */
    @PostMapping("/api/household/migration-permit")
    public Result<MigrationPermit> issueMigrationPermit(@RequestBody MigrationPermit permit) {
        return Result.success(householdService.issueMigrationPermit(permit));
    }

    /**
     * 获取行政区划区域树。
     *
     * <p>
     * 若提供 {@code parentId} 查询参数，则返回该父级下的子区域列表； 否则返回省级（顶层）区域列表。
     * </p>
     *
     * @param parentId
     *            父级区域 ID（可选）
     * @return 区域列表
     */
    @GetMapping("/api/area")
    public Result<List<Area>> getAreaTree(@RequestParam(required = false) Long parentId) {
        if (parentId != null)
            return Result.success(householdService.getAreasByParent(parentId));
        return Result.success(householdService.getAreaTree());
    }
}
