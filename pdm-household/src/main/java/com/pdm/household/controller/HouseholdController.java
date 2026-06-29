package com.pdm.household.controller;

import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;
import com.pdm.household.entity.*;
import com.pdm.household.mapper.*;
import com.pdm.household.service.HouseholdService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class HouseholdController {
    private final HouseholdService householdService;
    private final HouseholdRegisterMapper bookMapper;
    private final HouseholdBusinessRequestMapper businessMapper;
    private final HouseholdMigrationRequestMapper migrationMapper;
    private final ApprovalPermitMapper approvalPermitMapper;
    private final MigrationPermitMapper migrationPermitMapper;

    // ──────────── 户口簿 ────────────
    @GetMapping("/api/household/book/search")
    public Result<PageResult<HouseholdRegister>> listBooks(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<HouseholdRegister> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty())
            w.like(HouseholdRegister::getHouseholdBookNo, keyword);
        w.orderByDesc(HouseholdRegister::getCreateTime);
        Page<HouseholdRegister> r = bookMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
    }

    @PostMapping("/api/household/book/apply")
    public Result<HouseholdRegister> applyBook(@RequestBody HouseholdRegister book) {
        return Result.success(householdService.applyBook(book));
    }

    @PostMapping("/api/household/book/reissue")
    public Result<HouseholdRegister> reissueBook(@RequestBody Map<String, String> body) {
        return Result.success(householdService.reissueBook(body.get("bookNo")));
    }

    @PostMapping("/api/household/book/renew")
    public Result<HouseholdRegister> renewBook(@RequestBody Map<String, String> body) {
        return Result.success(householdService.renewBook(body.get("bookNo")));
    }

    // ──────────── 户籍业务 ────────────
    @GetMapping("/api/household/business")
    public Result<PageResult<HouseholdBusinessRequest>> listBusiness(@RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<HouseholdBusinessRequest> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty())
            w.eq(HouseholdBusinessRequest::getStatus, status);
        if (businessType != null && !businessType.isEmpty())
            w.eq(HouseholdBusinessRequest::getBusinessType, businessType);
        w.orderByDesc(HouseholdBusinessRequest::getCreateTime);
        Page<HouseholdBusinessRequest> r = businessMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
    }

    @PostMapping("/api/household/business")
    public Result<HouseholdBusinessRequest> submitBusiness(@RequestBody HouseholdBusinessRequest req) {
        return Result.success(householdService.submitBusiness(req));
    }

    @PutMapping("/api/household/business/{rid}/approve")
    public Result<HouseholdBusinessRequest> approveBusiness(@PathVariable Long rid,
            @RequestBody Map<String, String> body, @RequestHeader("X-User-Uuid") String handlerUuid) {
        return Result.success(
                householdService.approveBusiness(rid, body.get("status"), handlerUuid, body.get("rejectReason")));
    }

    // ──────────── 户籍迁移 ────────────
    @GetMapping("/api/household/migration")
    public Result<PageResult<HouseholdMigrationRequest>> listMigrations(@RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<HouseholdMigrationRequest> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty())
            w.eq(HouseholdMigrationRequest::getStatus, status);
        if (businessType != null && !businessType.isEmpty())
            w.eq(HouseholdMigrationRequest::getBusinessType, businessType);
        w.orderByDesc(HouseholdMigrationRequest::getCreateTime);
        Page<HouseholdMigrationRequest> r = migrationMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
    }

    @PostMapping("/api/household/migration")
    public Result<HouseholdMigrationRequest> submitMigration(@RequestBody HouseholdMigrationRequest req) {
        return Result.success(householdService.submitMigration(req));
    }

    @PutMapping("/api/household/migration/{rid}/approve")
    public Result<HouseholdMigrationRequest> approveMigration(@PathVariable Long rid,
            @RequestBody Map<String, String> body, @RequestHeader("X-User-Uuid") String handlerUuid) {
        return Result.success(
                householdService.approveMigration(rid, body.get("status"), handlerUuid, body.get("rejectReason")));
    }

    @GetMapping("/api/household/migration/trace/{uuid}")
    public Result<List<HouseholdMigrationRequest>> getMigrationTrace(@PathVariable String uuid) {
        return Result.success(householdService.getMigrationTrace(uuid));
    }

    // ──────────── 证件 ────────────
    @GetMapping("/api/household/approval-permit")
    public Result<PageResult<ApprovalPermit>> listApprovalPermits(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<ApprovalPermit> w = new LambdaQueryWrapper<>();
        w.orderByDesc(ApprovalPermit::getCreateTime);
        Page<ApprovalPermit> r = approvalPermitMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
    }

    @PostMapping("/api/household/approval-permit")
    public Result<ApprovalPermit> issueApprovalPermit(@RequestBody ApprovalPermit permit) {
        return Result.success(householdService.issueApprovalPermit(permit));
    }

    @GetMapping("/api/household/migration-permit")
    public Result<PageResult<MigrationPermit>> listMigrationPermits(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<MigrationPermit> w = new LambdaQueryWrapper<>();
        w.orderByDesc(MigrationPermit::getCreateTime);
        Page<MigrationPermit> r = migrationPermitMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
    }

    @PostMapping("/api/household/migration-permit")
    public Result<MigrationPermit> issueMigrationPermit(@RequestBody MigrationPermit permit) {
        return Result.success(householdService.issueMigrationPermit(permit));
    }

    // ──────────── 行政区划 ────────────
    @GetMapping("/api/area")
    public Result<List<Area>> getAreaTree(@RequestParam(required = false) Long parentId) {
        if (parentId != null)
            return Result.success(householdService.getAreasByParent(parentId));
        return Result.success(householdService.getAreaTree());
    }

    /** 获取区域祖先链（省→市→区） */
    @GetMapping("/api/area/{areaId}/ancestors")
    public Result<List<Area>> getAreaAncestors(@PathVariable Long areaId) {
        return Result.success(householdService.getAreaAncestors(areaId));
    }

    /** 获取区域完整路径字符串（用于地址拼接） */
    @GetMapping("/api/area/{areaId}/path")
    public Result<String> getAreaPath(@PathVariable Long areaId) {
        return Result.success(householdService.getAreaPath(areaId));
    }

    /** 获取全部区域数据（前端级联选择器一次性加载完整树） */
    @GetMapping("/api/area/tree")
    public Result<List<Area>> getAreaTreeFull() {
        return Result.success(householdService.getAllAreas());
    }
}
