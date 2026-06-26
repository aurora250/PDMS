package com.pdm.household.service;

import com.pdm.household.entity.*;

import java.util.List;

/**
 * 户政业务服务接口。
 *
 * <p>
 * 提供户政管理相关的核心业务方法，包括：
 * </p>
 * <ul>
 * <li><b>户口本管理</b> —— 申请、补办、换新户口本。</li>
 * <li><b>业务审批</b> —— 提交和审批户政业务（登记、注销、户主变更等）。</li>
 * <li><b>迁移管理</b> —— 提交和审批户口迁移、查询迁移轨迹。</li>
 * <li><b>证件签发</b> —— 签发准迁证和迁移证。</li>
 * <li><b>区域查询</b> —— 获取行政区划区域树和子区域。</li>
 * </ul>
 */
public interface HouseholdService {

    /**
     * 申请户口本。
     *
     * @param book
     *            户口本实体
     * @return 创建后的户口本实体
     */
    HouseholdRegister applyBook(HouseholdRegister book);

    /**
     * 补办户口本。
     *
     * @param bookNo
     *            户口本编号
     * @return 补办后的户口本实体
     */
    HouseholdRegister reissueBook(String bookNo);

    /**
     * 换新户口本。
     *
     * @param bookNo
     *            户口本编号
     * @return 换新后的户口本实体
     */
    HouseholdRegister renewBook(String bookNo);

    /**
     * 提交户政业务申请（登记、注销、户主变更等）。
     *
     * @param request
     *            业务申请实体
     * @return 创建后的业务申请实体
     */
    HouseholdBusinessRequest submitBusiness(HouseholdBusinessRequest request);

    /**
     * 审批户政业务。
     *
     * @param rid
     *            业务申请 ID
     * @param status
     *            审批结果状态
     * @param handlerUuid
     *            处理人 UUID
     * @param rejectReason
     *            驳回原因（通过时可为 {@code null}）
     * @return 更新后的业务申请实体
     */
    HouseholdBusinessRequest approveBusiness(Long rid, String status, String handlerUuid, String rejectReason);

    /**
     * 提交户口迁移申请。
     *
     * @param request
     *            迁移申请实体
     * @return 创建后的迁移申请实体
     */
    HouseholdMigrationRequest submitMigration(HouseholdMigrationRequest request);

    /**
     * 审批户口迁移申请。
     *
     * @param rid
     *            迁移申请 ID
     * @param status
     *            审批结果状态
     * @param handlerUuid
     *            处理人 UUID
     * @param rejectReason
     *            驳回原因（通过时可为 {@code null}）
     * @return 更新后的迁移申请实体
     */
    HouseholdMigrationRequest approveMigration(Long rid, String status, String handlerUuid, String rejectReason);

    /**
     * 查询居民的迁移轨迹。
     *
     * @param residentUuid
     *            居民 UUID
     * @return 迁移申请列表
     */
    List<HouseholdMigrationRequest> getMigrationTrace(String residentUuid);

    /**
     * 签发准迁证。
     *
     * @param permit
     *            准迁证实體
     * @return 创建后的准迁证实体
     */
    ApprovalPermit issueApprovalPermit(ApprovalPermit permit);

    /**
     * 签发迁移证。
     *
     * @param permit
     *            迁移证实体
     * @return 创建后的迁移证实体
     */
    MigrationPermit issueMigrationPermit(MigrationPermit permit);

    /**
     * 获取省级区域树。
     *
     * @return 省级区域列表
     */
    List<Area> getAreaTree();

    /**
     * 根据父级 ID 查询子区域。
     *
     * @param parentId
     *            父级区域 ID
     * @return 子区域列表
     */
    List<Area> getAreasByParent(Long parentId);
}
