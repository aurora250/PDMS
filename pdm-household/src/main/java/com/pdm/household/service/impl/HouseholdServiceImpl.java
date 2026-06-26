package com.pdm.household.service.impl;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.household.entity.*;
import com.pdm.household.mapper.*;
import com.pdm.household.service.HouseholdService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;

/**
 * 户政业务服务实现类，提供户口本管理、业务审批、迁移管理和证件签发等功能。
 *
 * <ul>
 *   <li><b>户口本管理</b> —— 申请、补办、换新户口本。</li>
 *   <li><b>业务审批</b> —— 四级审批流（采集员录入 → 街道办初审 → 民警复核 → 市局审批），
 *       覆盖登记、注销、户主变更等业务。</li>
 *   <li><b>迁移管理</b> —— 提交迁移申请、审批迁移、查询迁移轨迹。</li>
 *   <li><b>证件签发</b> —— 签发准迁证和迁移证。</li>
 *   <li><b>区域查询</b> —— 获取省级区域树和按父级查询子区域。</li>
 * </ul>
 *
 * @author freedom
 */
@Service
@RequiredArgsConstructor
public class HouseholdServiceImpl implements HouseholdService {

    private final HouseholdRegisterMapper bookMapper;
    private final HouseholdBusinessRequestMapper businessMapper;
    private final HouseholdMigrationRequestMapper migrationMapper;
    private final ApprovalPermitMapper approvalPermitMapper;
    private final MigrationPermitMapper migrationPermitMapper;
    private final AreaMapper areaMapper;

    /**
     * 申请户口本。
     *
     * <p>若未指定户口本编号，则自动以 {@code "HB"} 为前缀生成 20 位唯一编号。
     * 新户口本建立日期为当天，初始状态为"审批中"。
     *
     * @param book 户口本实体
     * @return 创建后的户口本实体（含自动生成的编号）
     */
    @Override
    @Transactional
    public HouseholdRegister applyBook(HouseholdRegister book) {
        if (book.getHouseholdBookNo() == null) {
            book.setHouseholdBookNo("HB" + IdUtil.fastSimpleUUID().substring(0, 20));
        }
        book.setEstablishDate(LocalDate.now());
        book.setStatus("审批中");
        bookMapper.insert(book);
        return book;
    }

    /**
     * 补办户口本。
     *
     * <p>根据户口本编号查找原记录，校验存在后返回。生产环境应生成新编号并复制原数据。
     *
     * @param bookNo 户口本编号
     * @return 原户口本实体
     * @throws BusinessException 户口本不存在时抛出
     */
    @Override
    @Transactional
    public HouseholdRegister reissueBook(String bookNo) {
        HouseholdRegister book = bookMapper.selectByBookNo(bookNo);
        if (book == null)
            throw new BusinessException(ErrorCode.HOUSEHOLD_BOOK_NOT_FOUND);
        return book;
    }

    /**
     * 换新户口本。
     *
     * <p>逻辑与补办相同，直接复用 {@link #reissueBook(String)}。
     *
     * @param bookNo 户口本编号
     * @return 原户口本实体
     * @throws BusinessException 户口本不存在时抛出
     */
    @Override
    @Transactional
    public HouseholdRegister renewBook(String bookNo) {
        return reissueBook(bookNo);
    }

    /**
     * 提交户政业务申请。
     *
     * <p>适用于登记、注销、户主变更等业务。提交后进入四级审批流程：
     * <b>采集员录入 → 街道办初审 → 民警复核 → 市局审批</b>。
     * 初始状态为"审批中"，处理日期为当天。
     *
     * @param request 业务申请实体
     * @return 创建后的业务申请实体
     */
    @Override
    @Transactional
    public HouseholdBusinessRequest submitBusiness(HouseholdBusinessRequest request) {
        request.setHandleDate(LocalDate.now());
        request.setStatus("审批中");
        businessMapper.insert(request);
        return request;
    }

    /**
     * 审批户政业务。
     *
     * <p>根据申请 ID 查找记录，更新审批状态、处理人和驳回原因（可选）。
     *
     * @param rid          业务申请 ID
     * @param status       审批结果状态
     * @param handlerUuid  处理人 UUID
     * @param rejectReason 驳回原因（通过时可为 {@code null}）
     * @return 更新后的业务申请实体
     * @throws BusinessException 业务申请不存在时抛出
     */
    @Override
    @Transactional
    public HouseholdBusinessRequest approveBusiness(Long rid, String status, String handlerUuid, String rejectReason) {
        HouseholdBusinessRequest req = businessMapper.selectById(rid);
        if (req == null)
            throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        req.setStatus(status);
        req.setHandlerUuid(handlerUuid);
        if (rejectReason != null)
            req.setRejectReason(rejectReason);
        businessMapper.updateById(req);
        return req;
    }

    /**
     * 提交户口迁移申请。
     *
     * <p>初始状态为"准迁证审批中"，处理日期为当天。
     *
     * @param request 迁移申请实体
     * @return 创建后的迁移申请实体
     */
    @Override
    @Transactional
    public HouseholdMigrationRequest submitMigration(HouseholdMigrationRequest request) {
        request.setHandleDate(LocalDate.now());
        request.setStatus("准迁证审批中");
        migrationMapper.insert(request);
        return request;
    }

    /**
     * 审批户口迁移申请。
     *
     * <p>根据申请 ID 查找记录，更新审批状态、处理人和驳回原因（可选）。
     *
     * @param rid          迁移申请 ID
     * @param status       审批结果状态
     * @param handlerUuid  处理人 UUID
     * @param rejectReason 驳回原因（通过时可为 {@code null}）
     * @return 更新后的迁移申请实体
     * @throws BusinessException 迁移申请不存在时抛出
     */
    @Override
    @Transactional
    public HouseholdMigrationRequest approveMigration(Long rid, String status, String handlerUuid,
            String rejectReason) {
        HouseholdMigrationRequest req = migrationMapper.selectById(rid);
        if (req == null)
            throw new BusinessException(ErrorCode.MIGRATION_NOT_FOUND);
        req.setStatus(status);
        req.setHandlerUuid(handlerUuid);
        if (rejectReason != null)
            req.setRejectReason(rejectReason);
        migrationMapper.updateById(req);
        return req;
    }

    /**
     * 查询居民的迁移轨迹。
     *
     * <p>根据申请人 UUID 查询其所有迁移记录，按创建时间倒序排列。
     *
     * @param residentUuid 居民 UUID
     * @return 迁移申请列表，无记录时返回空列表
     */
    @Override
    public List<HouseholdMigrationRequest> getMigrationTrace(String residentUuid) {
        return migrationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HouseholdMigrationRequest>()
                        .eq(HouseholdMigrationRequest::getApplicantUuid, residentUuid)
                        .orderByDesc(HouseholdMigrationRequest::getCreateTime));
    }

    /**
     * 签发准迁证。
     *
     * <p>若未指定证件编号，则自动以 {@code "AP"} 为前缀生成 20 位唯一编号。
     * 签发日期为当天，初始状态为"有效"。
     *
     * @param permit 准迁证实體
     * @return 创建后的准迁证实体（含自动生成的编号）
     */
    @Override
    @Transactional
    public ApprovalPermit issueApprovalPermit(ApprovalPermit permit) {
        if (permit.getPermitNo() == null)
            permit.setPermitNo("AP" + IdUtil.fastSimpleUUID().substring(0, 20));
        permit.setIssueDate(LocalDate.now());
        permit.setStatus("有效");
        approvalPermitMapper.insert(permit);
        return permit;
    }

    /**
     * 签发迁移证。
     *
     * <p>若未指定证件编号，则自动以 {@code "MP"} 为前缀生成 20 位唯一编号。
     * 签发日期为当天，初始状态为"有效"。
     *
     * @param permit 迁移证实体
     * @return 创建后的迁移证实体（含自动生成的编号）
     */
    @Override
    @Transactional
    public MigrationPermit issueMigrationPermit(MigrationPermit permit) {
        if (permit.getPermitNo() == null)
            permit.setPermitNo("MP" + IdUtil.fastSimpleUUID().substring(0, 20));
        permit.setIssueDate(LocalDate.now());
        permit.setStatus("有效");
        migrationPermitMapper.insert(permit);
        return permit;
    }

    /**
     * 获取省级区域树。
     *
     * @return 省级区域列表
     */
    @Override
    public List<Area> getAreaTree() {
        return areaMapper.selectByLevel("省");
    }

    /**
     * 根据父级 ID 查询子区域。
     *
     * @param parentId 父级区域 ID
     * @return 子区域列表
     */
    @Override
    public List<Area> getAreasByParent(Long parentId) {
        return areaMapper.selectByParentId(parentId);
    }
}
