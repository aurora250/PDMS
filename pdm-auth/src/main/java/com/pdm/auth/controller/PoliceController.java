package com.pdm.auth.controller;

import com.pdm.auth.entity.Police;
import com.pdm.auth.service.PoliceService;
import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/police")
@RequiredArgsConstructor
public class PoliceController {

    private final PoliceService policeService;

    @PostMapping
    public Result<Police> registerPolice(@RequestBody Police police) {
        return Result.success(policeService.registerPolice(police));
    }

    @GetMapping
    public Result<PageResult<Police>> listPolice(@RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String residentUuid) {
        Page<Police> policePage = policeService.listPolice(page, size, keyword, residentUuid);
        return Result.success(PageResult.of(policePage.getRecords(), policePage.getTotal(), page, size));
    }

    @GetMapping("/{policeNumber}")
    public Result<Police> getPolice(@PathVariable String policeNumber) {
        return Result.success(policeService.getPoliceByNumber(policeNumber));
    }

    @PutMapping("/{policeNumber}")
    public Result<Police> updatePolice(@PathVariable String policeNumber, @RequestBody Police updates) {
        return Result.success(policeService.updatePolice(policeNumber, updates));
    }

    @PutMapping("/{policeNumber}/status")
    public Result<Void> updateStatus(@PathVariable String policeNumber, @RequestBody Map<String, String> body) {
        policeService.updatePoliceStatus(policeNumber, body.get("dutyStatus"));
        return Result.success();
    }

    /** 查询未关联系统用户的民警列表（供创建民警用户时选择实名认证） */
    @GetMapping("/unassociated")
    public Result<List<Map<String, Object>>> listUnassociated() {
        return Result.success(policeService.listUnassociated());
    }
}
