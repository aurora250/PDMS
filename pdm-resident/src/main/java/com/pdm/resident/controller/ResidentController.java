package com.pdm.resident.controller;

import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;
import com.pdm.resident.dto.ResidentImportResult;
import com.pdm.resident.dto.ResidentSearchRequest;
import com.pdm.resident.entity.Resident;
import com.pdm.resident.entity.ResidentChangeRequest;
import com.pdm.resident.entity.ResidentRelation;
import com.pdm.resident.service.ResidentService;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/resident")
@RequiredArgsConstructor
public class ResidentController {

    private final ResidentService residentService;

    @PostMapping
    public Result<Resident> create(@Valid @RequestBody Resident resident) {
        return Result.success(residentService.createResident(resident));
    }

    @GetMapping("/{uuid}")
    public Result<Resident> get(@PathVariable String uuid) {
        return Result.success(residentService.getResident(uuid));
    }

    @PutMapping("/{uuid}")
    public Result<Resident> update(@PathVariable String uuid, @RequestBody Resident updates) {
        return Result.success(residentService.updateResident(uuid, updates));
    }

    @DeleteMapping("/{uuid}")
    public Result<Void> delete(@PathVariable String uuid) {
        residentService.deleteResident(uuid);
        return Result.success();
    }

    @PostMapping("/search")
    public Result<PageResult<Resident>> search(@RequestBody ResidentSearchRequest request) {
        return Result.success(residentService.search(request));
    }

    @GetMapping("/{uuid}/relations")
    public Result<ResidentRelation> getRelations(@PathVariable String uuid) {
        return Result.success(residentService.getRelations(uuid));
    }

    @PostMapping("/{uuid}/relations")
    public Result<ResidentRelation> setRelations(@PathVariable String uuid, @RequestBody ResidentRelation relation) {
        relation.setRelationPersonUuid(uuid);
        return Result.success(residentService.setRelations(relation));
    }

    @PostMapping("/change-request")
    public Result<ResidentChangeRequest> submitChangeRequest(@RequestBody ResidentChangeRequest request) {
        return Result.success(residentService.submitChangeRequest(request));
    }

    @PutMapping("/change-request/{rid}/approve")
    public Result<ResidentChangeRequest> approveChangeRequest(@PathVariable Long rid, @RequestParam String status,
            @RequestHeader("X-User-Uuid") String handlerUuid) {
        return Result.success(residentService.approveChangeRequest(rid, status, handlerUuid));
    }

    @PostMapping("/import")
    public Result<ResidentImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.success(residentService.importExcel(file));
    }
}
