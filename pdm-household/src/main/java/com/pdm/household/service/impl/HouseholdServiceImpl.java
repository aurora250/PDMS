package com.pdm.household.service.impl;

import cn.hutool.core.util.IdUtil;
import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.household.entity.*;
import com.pdm.household.mapper.*;
import com.pdm.household.service.HouseholdService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service @RequiredArgsConstructor
public class HouseholdServiceImpl implements HouseholdService {

    private final HouseholdRegisterMapper bookMapper;
    private final HouseholdBusinessRequestMapper businessMapper;
    private final HouseholdMigrationRequestMapper migrationMapper;
    private final ApprovalPermitMapper approvalPermitMapper;
    private final MigrationPermitMapper migrationPermitMapper;
    private final AreaMapper areaMapper;

    @Override @Transactional
    public HouseholdRegister applyBook(HouseholdRegister book) {
        if (book.getHouseholdBookNo() == null) {
            book.setHouseholdBookNo("HB" + IdUtil.fastSimpleUUID().substring(0, 20));
        }
        book.setEstablishDate(LocalDate.now());
        book.setStatus("审批中");
        bookMapper.insert(book);
        return book;
    }

    @Override @Transactional
    public HouseholdRegister reissueBook(String bookNo) {
        HouseholdRegister book = bookMapper.selectByBookNo(bookNo);
        if (book == null) throw new BusinessException(ErrorCode.HOUSEHOLD_BOOK_NOT_FOUND);
        // In production, generate new book number and copy data
        return book;
    }

    @Override @Transactional
    public HouseholdRegister renewBook(String bookNo) {
        return reissueBook(bookNo);
    }

    /** 四级审批流: 采集员录入→街道办初审→民警复核→市局审批 */
    @Override @Transactional
    public HouseholdBusinessRequest submitBusiness(HouseholdBusinessRequest request) {
        request.setHandleDate(LocalDate.now());
        request.setStatus("审批中");
        businessMapper.insert(request);
        return request;
    }

    @Override @Transactional
    public HouseholdBusinessRequest approveBusiness(Long rid, String status, String handlerUuid, String rejectReason) {
        HouseholdBusinessRequest req = businessMapper.selectById(rid);
        if (req == null) throw new BusinessException(ErrorCode.DATA_NOT_FOUND);
        req.setStatus(status);
        req.setHandlerUuid(handlerUuid);
        if (rejectReason != null) req.setRejectReason(rejectReason);
        businessMapper.updateById(req);
        return req;
    }

    @Override @Transactional
    public HouseholdMigrationRequest submitMigration(HouseholdMigrationRequest request) {
        request.setHandleDate(LocalDate.now());
        request.setStatus("准迁证审批中");
        migrationMapper.insert(request);
        return request;
    }

    @Override @Transactional
    public HouseholdMigrationRequest approveMigration(Long rid, String status, String handlerUuid, String rejectReason) {
        HouseholdMigrationRequest req = migrationMapper.selectById(rid);
        if (req == null) throw new BusinessException(ErrorCode.MIGRATION_NOT_FOUND);
        req.setStatus(status);
        req.setHandlerUuid(handlerUuid);
        if (rejectReason != null) req.setRejectReason(rejectReason);
        migrationMapper.updateById(req);
        return req;
    }

    @Override
    public List<HouseholdMigrationRequest> getMigrationTrace(String residentUuid) {
        return migrationMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HouseholdMigrationRequest>()
                        .eq(HouseholdMigrationRequest::getApplicantUuid, residentUuid)
                        .orderByDesc(HouseholdMigrationRequest::getCreateTime));
    }

    @Override @Transactional
    public ApprovalPermit issueApprovalPermit(ApprovalPermit permit) {
        if (permit.getPermitNo() == null) permit.setPermitNo("AP" + IdUtil.fastSimpleUUID().substring(0, 20));
        permit.setIssueDate(LocalDate.now());
        permit.setStatus("有效");
        approvalPermitMapper.insert(permit);
        return permit;
    }

    @Override @Transactional
    public MigrationPermit issueMigrationPermit(MigrationPermit permit) {
        if (permit.getPermitNo() == null) permit.setPermitNo("MP" + IdUtil.fastSimpleUUID().substring(0, 20));
        permit.setIssueDate(LocalDate.now());
        permit.setStatus("有效");
        migrationPermitMapper.insert(permit);
        return permit;
    }

    @Override
    public List<Area> getAreaTree() {
        return areaMapper.selectByLevel("省");
    }

    @Override
    public List<Area> getAreasByParent(Long parentId) {
        return areaMapper.selectByParentId(parentId);
    }
}
