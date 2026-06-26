package com.pdm.keyperson.controller;

import com.pdm.common.core.result.Result;
import com.pdm.keyperson.entity.KeyPerson;
import com.pdm.keyperson.entity.PetitionRecord;
import com.pdm.keyperson.entity.VisitPlan;
import com.pdm.keyperson.service.KeypersonService;

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

    @PostMapping("/")
    public Result<KeyPerson> designate(@RequestBody KeyPerson keyPerson) {
        return Result.success(keypersonService.designateKeyPerson(keyPerson));
    }

    @PutMapping("/{uuid}")
    public Result<KeyPerson> updateControlLevel(
            @PathVariable String uuid, @RequestBody Map<String, String> body) {
        return Result.success(keypersonService.updateControlLevel(uuid, body.get("controlLevel")));
    }

    @DeleteMapping("/{uuid}")
    public Result<Void> revoke(@PathVariable String uuid) {
        keypersonService.revokeKeyPerson(uuid);
        return Result.success();
    }

    @GetMapping("/search")
    public Result<List<KeyPerson>> search(@RequestParam Map<String, Object> conditions) {
        return Result.success(keypersonService.searchKeyPersons(conditions));
    }

    @PostMapping("/visit-plan")
    public Result<VisitPlan> generateVisitPlan(@RequestBody VisitPlan visitPlan) {
        return Result.success(keypersonService.generateVisitPlan(visitPlan));
    }

    @PutMapping("/visit-plan/{id}")
    public Result<VisitPlan> completeVisit(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        LocalDate actualDate = LocalDate.parse((String) body.get("actualDate"));
        PetitionRecord petitionRecord = null;
        if (body.get("petitionRecord") != null) {
            petitionRecord = new PetitionRecord();
            // Map fields from body if provided
        }
        return Result.success(keypersonService.completeVisit(id, actualDate, petitionRecord));
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
