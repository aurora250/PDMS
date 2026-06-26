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

/**
 * 重点人员管理控制器
 * 处理重点人员相关HTTP接口请求，包括新增、修改、撤销、查询、走访计划、信访记录等功能
 *
 * @author 开发者
 * @since 1.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/keyperson")
public class KeypersonController {

    /**
     * 重点人员业务服务接口
     */
    private final KeypersonService keypersonService;

    /**
     * 新增指定重点人员
     *
     * @param keyPerson 重点人员实体对象（包含uuid、管控等级等信息）
     * @return 新增后的重点人员信息
     */
    @PostMapping("/")
    public Result<KeyPerson> designate(@RequestBody KeyPerson keyPerson) {
        return Result.success(keypersonService.designateKeyPerson(keyPerson));
    }

    /**
     * 修改重点人员管控等级
     *
     * @param uuid 重点人员唯一标识
     * @param body 请求体（包含controlLevel字段，代表新的管控等级）
     * @return 修改后的重点人员信息
     */
    @PutMapping("/{uuid}")
    public Result<KeyPerson> updateControlLevel(@PathVariable String uuid, @RequestBody Map<String, String> body) {
        return Result.success(keypersonService.updateControlLevel(uuid, body.get("controlLevel")));
    }

    /**
     * 撤销重点人员身份
     *
     * @param uuid 重点人员唯一标识
     * @return 空返回结果（成功状态）
     */
    @DeleteMapping("/{uuid}")
    public Result<Void> revoke(@PathVariable String uuid) {
        keypersonService.revokeKeyPerson(uuid);
        return Result.success();
    }

    /**
     * 多条件查询重点人员列表
     *
     * @param conditions 查询条件（支持uuid、controlLevel、controlType、responsiblePoliceNo等字段）
     * @return 符合条件的重点人员列表
     */
    @GetMapping("/search")
    public Result<List<KeyPerson>> search(@RequestParam Map<String, Object> conditions) {
        return Result.success(keypersonService.searchKeyPersons(conditions));
    }

    /**
     * 生成重点人员走访计划
     *
     * @param visitPlan 走访计划实体（包含重点人员uuid、走访类型等信息）
     * @return 生成后的走访计划信息
     */
    @PostMapping("/visit-plan")
    public Result<VisitPlan> generateVisitPlan(@RequestBody VisitPlan visitPlan) {
        return Result.success(keypersonService.generateVisitPlan(visitPlan));
    }

    /**
     * 完成走访计划（更新计划状态并可选记录信访信息）
     *
     * @param id 走访计划ID
     * @param body 请求体（包含actualDate：实际走访日期；petitionRecord：可选信访记录信息）
     * @return 完成后的走访计划信息
     */
    @PutMapping("/visit-plan/{id}")
    public Result<VisitPlan> completeVisit(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        LocalDate actualDate = LocalDate.parse((String) body.get("actualDate"));
        PetitionRecord petitionRecord = null;
        if (body.get("petitionRecord") != null) {
            petitionRecord = new PetitionRecord();
            // Map fields from body if provided
        }
        return Result.success(keypersonService.completeVisit(id, actualDate, petitionRecord));
    }

    /**
     * 记录重点人员信访信息
     *
     * @param petitionRecord 信访记录实体（包含重点人员uuid、地址、备注等信息）
     * @return 保存后的信访记录信息
     */
    @PostMapping("/petition")
    public Result<PetitionRecord> recordPetition(@RequestBody PetitionRecord petitionRecord) {
        return Result.success(keypersonService.recordPetition(petitionRecord));
    }

    /**
     * 获取重点人员GIS可视化数据
     *
     * @return GIS数据列表（包含uuid、管控等级、管控类型等字段）
     */
    @GetMapping("/gis")
    public Result<List<Map<String, Object>>> getGisData() {
        return Result.success(keypersonService.getGisData());
    }
}