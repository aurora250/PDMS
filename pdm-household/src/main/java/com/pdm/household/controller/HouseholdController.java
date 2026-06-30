package com.pdm.household.controller;

import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;
import com.pdm.household.entity.*;
import com.pdm.household.mapper.*;
import com.pdm.household.service.HouseholdService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

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
    private final ResidentMapper residentMapper;

    // ──────────── 户口簿 ────────────
    @GetMapping("/api/household/book/search")
    public Result<PageResult<HouseholdRegister>> listBooks(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<HouseholdRegister> w = new LambdaQueryWrapper<>();
        // keyword: 先按户口簿号 LIKE 搜索；若匹配不到再通过 resident 表查户主姓名对应的 UUID
        if (keyword != null && !keyword.isEmpty()) {
            // 先查 resident 表中 name LIKE keyword 的 uuid
            List<String> matchedUuids = residentMapper.selectUuidsByName(keyword);
            if (!matchedUuids.isEmpty()) {
                w.and(wr -> wr.like(HouseholdRegister::getHouseholdBookNo, keyword).or()
                        .in(HouseholdRegister::getHouseholderUuid, matchedUuids).or()
                        .like(HouseholdRegister::getHukouAddress, keyword));
            } else {
                w.and(wr -> wr.like(HouseholdRegister::getHouseholdBookNo, keyword).or()
                        .like(HouseholdRegister::getHukouAddress, keyword));
            }
        }
        if (status != null && !status.isEmpty())
            w.eq(HouseholdRegister::getStatus, status);
        w.orderByDesc(HouseholdRegister::getCreateTime);
        Page<HouseholdRegister> r = bookMapper.selectPage(Page.of(page, size), w);

        // 批量填充户主姓名
        List<String> uuids = r.getRecords().stream().map(HouseholdRegister::getHouseholderUuid).filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (!uuids.isEmpty()) {
            Map<String, String> nameMap = residentMapper.batchGetNames(uuids).stream()
                    .collect(Collectors.toMap(m -> (String) m.get("uuid"), m -> (String) m.get("name")));
            r.getRecords().forEach(b -> b.setHouseholderName(nameMap.get(b.getHouseholderUuid())));
        }
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

    /** 按居民UUID查询其户口簿 */
    @GetMapping("/api/household/book/by-resident/{residentUuid}")
    public Result<Map<String, Object>> getBookByResident(@PathVariable String residentUuid) {
        Map<String, Object> book = householdService.getBookByResident(residentUuid);
        if (book == null) {
            return Result.success(null);
        }
        return Result.success(book);
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

    /** 附加审核材料（街道办权限） */
    @PostMapping("/api/household/business/{rid}/material")
    public Result<HouseholdBusinessRequest> attachBusinessMaterial(@PathVariable Long rid,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "attachmentPath", required = false) String attachmentPath,
            @RequestParam(value = "remark", required = false) String remark) {
        // 如果上传了文件，文件路径由前端先上传到 /api/file/upload 获得
        // 如果直接传路径，使用传入的路径
        String path = attachmentPath;
        if (path == null && file != null) {
            path = file.getOriginalFilename(); // fallback
        }
        return Result.success(householdService.attachBusinessMaterial(rid, path, remark));
    }

    // ──────────── 户籍迁移 ────────────
    @GetMapping("/api/household/migration")
    public Result<PageResult<HouseholdMigrationRequest>> listMigrations(@RequestParam(required = false) String status,
            @RequestParam(required = false) String businessType, @RequestParam(required = false) String fromAddress,
            @RequestParam(required = false) String toAddress, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<HouseholdMigrationRequest> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty())
            w.eq(HouseholdMigrationRequest::getStatus, status);
        if (businessType != null && !businessType.isEmpty())
            w.eq(HouseholdMigrationRequest::getBusinessType, businessType);
        if (fromAddress != null && !fromAddress.isEmpty())
            w.like(HouseholdMigrationRequest::getOutgoingAddress, fromAddress);
        if (toAddress != null && !toAddress.isEmpty())
            w.like(HouseholdMigrationRequest::getIncomingAddress, toAddress);
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

    /** 附加审核材料（街道办权限） */
    @PostMapping("/api/household/migration/{rid}/material")
    public Result<HouseholdMigrationRequest> attachMigrationMaterial(@PathVariable Long rid,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "attachmentPath", required = false) String attachmentPath,
            @RequestParam(value = "remark", required = false) String remark) {
        String path = attachmentPath;
        if (path == null && file != null) {
            path = file.getOriginalFilename();
        }
        return Result.success(householdService.attachMigrationMaterial(rid, path, remark));
    }

    @GetMapping("/api/household/migration/trace/{uuid}")
    public Result<List<HouseholdMigrationRequest>> getMigrationTrace(@PathVariable String uuid) {
        return Result.success(householdService.getMigrationTrace(uuid));
    }

    // ──────────── 证件 ────────────
    @GetMapping("/api/household/approval-permit")
    public Result<PageResult<ApprovalPermit>> listApprovalPermits(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<ApprovalPermit> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty())
            w.like(ApprovalPermit::getPermitNo, keyword);
        if (status != null && !status.isEmpty())
            w.eq(ApprovalPermit::getStatus, status);
        w.orderByDesc(ApprovalPermit::getCreateTime);
        Page<ApprovalPermit> r = approvalPermitMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
    }

    @PostMapping("/api/household/approval-permit")
    public Result<ApprovalPermit> issueApprovalPermit(@RequestBody ApprovalPermit permit) {
        return Result.success(householdService.issueApprovalPermit(permit));
    }

    @PutMapping("/api/household/approval-permit/{id}/void")
    public Result<ApprovalPermit> voidApprovalPermit(@PathVariable Long id) {
        return Result.success(householdService.voidApprovalPermit(id));
    }

    @GetMapping("/api/household/migration-permit")
    public Result<PageResult<MigrationPermit>> listMigrationPermits(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<MigrationPermit> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty())
            w.like(MigrationPermit::getPermitNo, keyword);
        if (status != null && !status.isEmpty())
            w.eq(MigrationPermit::getStatus, status);
        w.orderByDesc(MigrationPermit::getCreateTime);
        Page<MigrationPermit> r = migrationPermitMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
    }

    @PostMapping("/api/household/migration-permit")
    public Result<MigrationPermit> issueMigrationPermit(@RequestBody MigrationPermit permit) {
        return Result.success(householdService.issueMigrationPermit(permit));
    }

    @PutMapping("/api/household/migration-permit/{id}/void")
    public Result<MigrationPermit> voidMigrationPermit(@PathVariable Long id) {
        return Result.success(householdService.voidMigrationPermit(id));
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
