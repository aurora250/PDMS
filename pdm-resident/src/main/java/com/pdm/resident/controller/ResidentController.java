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

/**
 * 常住人口接口控制器
 * 提供常住人口的RESTful API接口，处理HTTP请求并调用业务层方法
 */
@RestController
@RequestMapping("/api/resident")
@RequiredArgsConstructor
public class ResidentController {

    /**
     * 常住人口业务服务
     */
    private final ResidentService residentService;

    /**
     * 创建常住人口信息
     * @param resident 常住人口实体对象
     * @return 响应结果（包含创建后的常住人口信息）
     */
    @PostMapping
    public Result<Resident> create(@Valid @RequestBody Resident resident) {
        return Result.success(residentService.createResident(resident));
    }

    /**
     * 根据UUID查询常住人口信息
     * @param uuid 人员唯一标识
     * @return 响应结果（包含常住人口信息）
     */
    @GetMapping("/{uuid}")
    public Result<Resident> get(@PathVariable String uuid) {
        return Result.success(residentService.getResident(uuid));
    }

    /**
     * 更新常住人口信息
     * @param uuid 人员唯一标识
     * @param updates 待更新字段信息
     * @return 响应结果（包含更新后的常住人口信息）
     */
    @PutMapping("/{uuid}")
    public Result<Resident> update(@PathVariable String uuid, @RequestBody Resident updates) {
        return Result.success(residentService.updateResident(uuid, updates));
    }

    /**
     * 删除常住人口信息
     * @param uuid 人员唯一标识
     * @return 响应结果（操作成功标识）
     */
    @DeleteMapping("/{uuid}")
    public Result<Void> delete(@PathVariable String uuid) {
        residentService.deleteResident(uuid);
        return Result.success();
    }

    /**
     * 多条件分页查询常住人口
     * @param request 查询条件及分页参数
     * @return 响应结果（包含分页查询结果）
     */
    @PostMapping("/search")
    public Result<PageResult<Resident>> search(@RequestBody ResidentSearchRequest request) {
        return Result.success(residentService.search(request));
    }

    /**
     * 查询人员亲属关系
     * @param uuid 人员唯一标识
     * @return 响应结果（包含亲属关系信息）
     */
    @GetMapping("/{uuid}/relations")
    public Result<ResidentRelation> getRelations(@PathVariable String uuid) {
        return Result.success(residentService.getRelations(uuid));
    }

    /**
     * 设置人员亲属关系
     * @param uuid 人员唯一标识
     * @param relation 亲属关系信息
     * @return 响应结果（包含更新后的亲属关系信息）
     */
    @PostMapping("/{uuid}/relations")
    public Result<ResidentRelation> setRelations(@PathVariable String uuid, @RequestBody ResidentRelation relation) {
        relation.setRelationPersonUuid(uuid);
        return Result.success(residentService.setRelations(relation));
    }

    /**
     * 提交信息变更申请
     * @param request 变更申请信息
     * @return 响应结果（包含提交后的申请信息）
     */
    @PostMapping("/change-request")
    public Result<ResidentChangeRequest> submitChangeRequest(@RequestBody ResidentChangeRequest request) {
        return Result.success(residentService.submitChangeRequest(request));
    }

    /**
     * 审批信息变更申请
     * @param rid 申请单ID
     * @param status 审批状态
     * @param handlerUuid 处理人UUID
     * @return 响应结果（包含审批后的申请信息）
     */
    @PutMapping("/change-request/{rid}/approve")
    public Result<ResidentChangeRequest> approveChangeRequest(@PathVariable Long rid, @RequestParam String status,
                                                              @RequestHeader("X-User-Uuid") String handlerUuid) {
        return Result.success(residentService.approveChangeRequest(rid, status, handlerUuid));
    }

    /**
     * 导入Excel批量新增常住人口
     * @param file Excel文件
     * @return 响应结果（包含导入统计结果）
     */
    @PostMapping("/import")
    public Result<ResidentImportResult> importExcel(@RequestParam("file") MultipartFile file) {
        return Result.success(residentService.importExcel(file));
    }
}