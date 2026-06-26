package com.pdm.auth.controller;

import com.pdm.auth.entity.Police;
import com.pdm.auth.service.PoliceService;
import com.pdm.common.core.result.Result;
import com.pdm.common.dto.PageResult;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

import lombok.RequiredArgsConstructor;

/**
 * 警员管理控制器。
 *
 * <p>提供警员的注册、查询、更新和状态管理 REST API 接口，挂载在 {@code /api/auth/police} 路径下。 需要系统管理员或用户管理员角色方可访问。
 */
@RestController
@RequestMapping("/api/auth/police")
@RequiredArgsConstructor
public class PoliceController {

    private final PoliceService policeService;

    /**
     * 注册警员。
     *
     * <p>新增警员记录，未指定警号时系统自动生成。
     *
     * @param police 警员实体
     * @return 注册成功后的警员信息（含警号）
     */
    @PostMapping
    public Result<Police> registerPolice(@RequestBody Police police) {
        return Result.success(policeService.registerPolice(police));
    }

    /**
     * 分页查询警员列表。
     *
     * <p>支持按关键词模糊搜索警号、派出所和部门。
     *
     * @param page 页码，默认 1
     * @param size 每页条数，默认 20
     * @param keyword 搜索关键词（可选）
     * @return 警员分页结果
     */
    @GetMapping
    public Result<PageResult<Police>> listPolice(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Page<Police> policePage = policeService.listPolice(page, size, keyword);
        return Result.success(
                PageResult.of(policePage.getRecords(), policePage.getTotal(), page, size));
    }

    /**
     * 根据警号查询警员详情。
     *
     * @param policeNumber 警号
     * @return 警员信息
     */
    @GetMapping("/{policeNumber}")
    public Result<Police> getPolice(@PathVariable String policeNumber) {
        return Result.success(policeService.getPoliceByNumber(policeNumber));
    }

    /**
     * 更新警员信息。
     *
     * <p>仅更新传入对象中非空的字段（派出所、辖区、区域、部门、警衔）。
     *
     * @param policeNumber 警号
     * @param updates 包含待更新字段的警员对象
     * @return 更新后的警员信息
     */
    @PutMapping("/{policeNumber}")
    public Result<Police> updatePolice(
            @PathVariable String policeNumber, @RequestBody Police updates) {
        return Result.success(policeService.updatePolice(policeNumber, updates));
    }

    /**
     * 更新警员值班状态。
     *
     * <p>仅允许设置为"在岗""调岗""离职"三种状态。
     *
     * @param policeNumber 警号
     * @param body 请求体，包含 {@code dutyStatus} 字段
     * @return 操作结果
     */
    @PutMapping("/{policeNumber}/status")
    public Result<Void> updateStatus(
            @PathVariable String policeNumber, @RequestBody Map<String, String> body) {
        policeService.updatePoliceStatus(policeNumber, body.get("dutyStatus"));
        return Result.success();
    }
}
