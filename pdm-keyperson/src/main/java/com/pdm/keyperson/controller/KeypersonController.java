package com.pdm.keyperson.controller;

import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;
import com.pdm.keyperson.entity.KeyPerson;
import com.pdm.keyperson.entity.PetitionRecord;
import com.pdm.keyperson.entity.VisitPlan;
import com.pdm.keyperson.mapper.PetitionRecordMapper;
import com.pdm.keyperson.mapper.VisitPlanMapper;
import com.pdm.keyperson.service.KeypersonService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/keyperson")
public class KeypersonController {

    private final KeypersonService keypersonService;
    private final VisitPlanMapper visitPlanMapper;
    private final PetitionRecordMapper petitionMapper;

    @PostMapping("/")
    public Result<KeyPerson> designate(@RequestBody KeyPerson keyPerson) {
        return Result.success(keypersonService.designateKeyPerson(keyPerson));
    }

    @PutMapping("/{uuid}")
    public Result<KeyPerson> updateControlLevel(@PathVariable String uuid, @RequestBody Map<String, String> body) {
        return Result.success(keypersonService.updateControlLevel(uuid, body.get("controlLevel")));
    }

    @DeleteMapping("/{uuid}")
    public Result<Void> revoke(@PathVariable String uuid) {
        keypersonService.revokeKeyPerson(uuid);
        return Result.success();
    }

    @GetMapping("/search")
    public Result<PageResult<KeyPerson>> search(@RequestParam(required = false) String controlLevel,
            @RequestParam(required = false) String controlType, @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(keypersonService.searchKeyPersons(controlLevel, controlType, keyword, page, size));
    }

    // ──────────── 走访计划 ────────────
    @GetMapping("/visit-plan")
    public Result<PageResult<VisitPlan>> listVisitPlans(@RequestParam(required = false) String keyPersonUuid,
            @RequestParam(required = false) String status, @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<VisitPlan> w = new LambdaQueryWrapper<>();
        if (keyPersonUuid != null && !keyPersonUuid.isEmpty())
            w.eq(VisitPlan::getKeyPersonUuid, keyPersonUuid);
        if (status != null && !status.isEmpty())
            w.eq(VisitPlan::getStatus, status);
        if (startDate != null && !startDate.isEmpty())
            w.ge(VisitPlan::getPlannedDate, LocalDate.parse(startDate));
        if (endDate != null && !endDate.isEmpty())
            w.le(VisitPlan::getPlannedDate, LocalDate.parse(endDate));
        w.orderByAsc(VisitPlan::getPlannedDate);
        Page<VisitPlan> r = visitPlanMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
    }

    @PostMapping("/visit-plan")
    public Result<VisitPlan> generateVisitPlan(@RequestBody VisitPlan visitPlan) {
        return Result.success(keypersonService.generateVisitPlan(visitPlan));
    }

    @PutMapping("/visit-plan/{id}")
    public Result<VisitPlan> completeVisit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LocalDate actualDate = LocalDate.parse((String) body.get("actualDate"));
        PetitionRecord petitionRecord = null;
        if (body.get("petitionRecord") != null) {
            petitionRecord = new PetitionRecord();
        }
        return Result.success(keypersonService.completeVisit(id, actualDate, petitionRecord));
    }

    // ──────────── 信访记录 ────────────
    @GetMapping("/petition")
    public Result<PageResult<PetitionRecord>> listPetitions(@RequestParam(required = false) String keyPersonUuid,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        LambdaQueryWrapper<PetitionRecord> w = new LambdaQueryWrapper<>();
        if (keyPersonUuid != null && !keyPersonUuid.isEmpty())
            w.eq(PetitionRecord::getKeyPersonUuid, keyPersonUuid);
        w.orderByDesc(PetitionRecord::getPetitionTime);
        Page<PetitionRecord> r = petitionMapper.selectPage(Page.of(page, size), w);
        return Result.success(PageResult.of(r.getRecords(), r.getTotal(), page, size));
    }

    @PostMapping("/petition")
    public Result<PetitionRecord> recordPetition(@RequestBody PetitionRecord petitionRecord) {
        return Result.success(keypersonService.recordPetition(petitionRecord));
    }

    @GetMapping("/gis")
    public Result<List<Map<String, Object>>> getGisData() {
        return Result.success(keypersonService.getGisData());
    }
}
