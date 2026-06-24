package com.pdm.household.controller;

import com.pdm.common.core.result.Result;
import com.pdm.household.entity.*;
import com.pdm.household.service.HouseholdService;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class HouseholdController {
    private final HouseholdService householdService;

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

    @PostMapping("/api/household/approval-permit")
    public Result<ApprovalPermit> issueApprovalPermit(@RequestBody ApprovalPermit permit) {
        return Result.success(householdService.issueApprovalPermit(permit));
    }

    @PostMapping("/api/household/migration-permit")
    public Result<MigrationPermit> issueMigrationPermit(@RequestBody MigrationPermit permit) {
        return Result.success(householdService.issueMigrationPermit(permit));
    }

    @GetMapping("/api/area")
    public Result<List<Area>> getAreaTree(@RequestParam(required = false) Long parentId) {
        if (parentId != null)
            return Result.success(householdService.getAreasByParent(parentId));
        return Result.success(householdService.getAreaTree());
    }
}
