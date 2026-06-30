package com.pdm.resident.controller;

import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;
import com.pdm.resident.dto.ResidentImportResult;
import com.pdm.resident.dto.ResidentRelationVO;
import com.pdm.resident.dto.ResidentSearchRequest;
import com.pdm.resident.entity.Resident;
import com.pdm.resident.entity.ResidentChangeRequest;
import com.pdm.resident.entity.ResidentRelation;
import com.pdm.resident.service.ResidentService;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

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

    /** 获取居民关系（含姓名 + 子女） */
    @GetMapping("/{uuid}/relations-detail")
    public Result<ResidentRelationVO> getRelationsDetail(@PathVariable String uuid) {
        return Result.success(residentService.getRelationsWithNames(uuid));
    }

    /** 获取子女列表 */
    @GetMapping("/{uuid}/children")
    public Result<List<Map<String, Object>>> getChildren(@PathVariable String uuid) {
        return Result.success(residentService.getChildren(uuid));
    }

    @GetMapping("/change-request")
    public Result<PageResult<ResidentChangeRequest>> listChangeRequests(@RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        return Result.success(residentService.listChangeRequests(status, page, size));
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

    @GetMapping("/export")
    public void exportExcel(@RequestParam(required = false) Map<String, Object> conditions,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=residents.xlsx");
        residentService.exportExcel(conditions, response.getOutputStream());
    }
}
