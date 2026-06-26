package com.pdm.missingperson.controller;

import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageRequest;
import com.pdm.common.dto.PageResult;
import com.pdm.missingperson.entity.MissingPerson;
import com.pdm.missingperson.entity.MissingPersonRecovery;
import com.pdm.missingperson.service.MissingpersonService;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/missing")
@RequiredArgsConstructor
public class MissingpersonController {

    private final MissingpersonService missingpersonService;

    @PostMapping
    public Result<MissingPerson> register(@RequestBody MissingPerson missingPerson) {
        return Result.success(missingpersonService.register(missingPerson));
    }

    @DeleteMapping("/{rid}")
    public Result<Void> cancel(@PathVariable Long rid) {
        missingpersonService.cancel(rid);
        return Result.success();
    }

    @PostMapping("/recovery")
    public Result<MissingPersonRecovery> recordRecovery(
            @RequestBody MissingPersonRecovery recovery) {
        return Result.success(missingpersonService.recordRecovery(recovery));
    }

    @GetMapping("/search")
    public Result<PageResult<MissingPerson>> search(
            @RequestParam(required = false) String residentUuid,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = new PageRequest(page, size, null, "DESC");
        return Result.success(missingpersonService.search(residentUuid, status, name, pageRequest));
    }

    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(missingpersonService.getStatistics());
    }
}
