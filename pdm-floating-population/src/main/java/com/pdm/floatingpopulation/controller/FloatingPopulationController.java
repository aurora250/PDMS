package com.pdm.floatingpopulation.controller;

import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;
import com.pdm.floatingpopulation.entity.FpRegisterRecord;
import com.pdm.floatingpopulation.entity.ResidentPermit;
import com.pdm.floatingpopulation.entity.ResidentPermitRenewal;
import com.pdm.floatingpopulation.entity.ResidentRegistration;
import com.pdm.floatingpopulation.mapper.FpRegisterRecordMapper;
import com.pdm.floatingpopulation.mapper.ResidentPermitMapper;
import com.pdm.floatingpopulation.mapper.ResidentRegistrationMapper;
import com.pdm.floatingpopulation.service.FloatingPopulationService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fp")
public class FloatingPopulationController {

    private final FloatingPopulationService floatingPopulationService;
    private final FpRegisterRecordMapper fpRegisterMapper;
    private final ResidentPermitMapper permitMapper;
    private final ResidentRegistrationMapper residenceMapper;

    // ──────────── 流动人口登记 ────────────
    @GetMapping("/register")
    public Result<PageResult<FpRegisterRecord>> listFpRegisters(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<FpRegisterRecord> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty())
            w.and(wr -> wr.like(FpRegisterRecord::getUuid, keyword).or().like(FpRegisterRecord::getResidencePermitNo,
                    keyword));
        w.orderByDesc(FpRegisterRecord::getCreateTime);
        Page<FpRegisterRecord> r = fpRegisterMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
    }

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

    // ──────────── 居住证 ────────────
    @GetMapping("/permit")
    public Result<PageResult<ResidentPermit>> listPermits(@RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<ResidentPermit> w = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty())
            w.eq(ResidentPermit::getStatus, status);
        if (keyword != null && !keyword.isEmpty())
            w.and(wr -> wr.like(ResidentPermit::getPermitNo, keyword).or().like(ResidentPermit::getUuid, keyword));
        w.orderByDesc(ResidentPermit::getCreateTime);
        Page<ResidentPermit> r = permitMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
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
    public Result<ResidentPermitRenewal> renewPermit(@PathVariable Long id, @RequestBody ResidentPermitRenewal renewal,
            @RequestHeader(value = "X-User-Uuid", required = false) String operatorUuid) {
        return Result.success(floatingPopulationService.renewPermit(id, renewal, operatorUuid));
    }

    // ──────────── 居住地登记 ────────────
    @GetMapping("/residence")
    public Result<PageResult<ResidentRegistration>> listResidences(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<ResidentRegistration> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty())
            w.and(wr -> wr.like(ResidentRegistration::getUuid, keyword).or()
                    .like(ResidentRegistration::getCurrentAddress, keyword).or()
                    .like(ResidentRegistration::getOriginalAddress, keyword));
        w.orderByDesc(ResidentRegistration::getCreateTime);
        Page<ResidentRegistration> r = residenceMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
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
