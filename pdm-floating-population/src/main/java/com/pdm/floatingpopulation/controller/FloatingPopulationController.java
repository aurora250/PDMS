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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fp")
public class FloatingPopulationController {

    private final FloatingPopulationService floatingPopulationService;

    @PostMapping("/register")
    public Result<FpRegisterRecord> registerFp(@RequestBody FpRegisterRecord record) {
        return Result.success(floatingPopulationService.registerFp(record));
    }

    @PutMapping("/register/{rid}")
    public Result<FpRegisterRecord> updateFp(@PathVariable Long rid, @RequestBody FpRegisterRecord record) {
        return Result.success(floatingPopulationService.updateFp(rid, record));
    }

    @DeleteMapping("/register/{rid}")
    public Result<Void> cancelFp(@PathVariable Long rid) {
        floatingPopulationService.cancelFp(rid);
        return Result.success();
    }

    @PostMapping("/permit/apply")
    public Result<ResidentPermit> applyPermit(@RequestBody ResidentPermit permit) {
        return Result.success(floatingPopulationService.applyPermit(permit));
    }

    @PutMapping("/permit/{id}/approve")
    public Result<ResidentPermit> approvePermit(@PathVariable Long id,
            @RequestHeader("X-User-Uuid") String reviewerUuid) {
        return Result.success(floatingPopulationService.approvePermit(id, reviewerUuid));
    }

    @PutMapping("/permit/{id}/issue")
    public Result<ResidentPermit> issuePermit(@PathVariable Long id) {
        return Result.success(floatingPopulationService.issuePermit(id));
    }

    @PostMapping("/permit/{id}/renew")
    public Result<ResidentPermitRenewal> renewPermit(@PathVariable Long id,
            @RequestBody ResidentPermitRenewal renewal) {
        return Result.success(floatingPopulationService.renewPermit(id, renewal));
    }

    @PostMapping("/residence/register")
    public Result<ResidentRegistration> registerResidence(@RequestBody ResidentRegistration registration) {
        return Result.success(floatingPopulationService.registerResidence(registration));
    }

    @PutMapping("/residence/{rid}")
    public Result<ResidentRegistration> changeResidence(@PathVariable Long rid,
            @RequestBody ResidentRegistration registration) {
        return Result.success(floatingPopulationService.changeResidence(rid, registration));
    }

    @DeleteMapping("/residence/{rid}")
    public Result<Void> cancelResidence(@PathVariable Long rid) {
        floatingPopulationService.cancelResidence(rid);
        return Result.success();
    }

    @GetMapping("/statistics/heatmap")
    public Result<List<Map<String, Object>>> getHeatmapData() {
        return Result.success(floatingPopulationService.getHeatmapData());
    }

    @GetMapping("/statistics/trend")
    public Result<List<Map<String, Object>>> getTrendData() {
        return Result.success(floatingPopulationService.getTrendData());
    }
}
