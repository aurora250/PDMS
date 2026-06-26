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

/**
 * 失踪人员接口控制器
 * 提供失踪人员注册、注销、寻回记录、搜索、统计等RESTful API
 */
@RestController
@RequestMapping("/api/missing")
@RequiredArgsConstructor
public class MissingpersonController {

    /**
     * 失踪人员业务服务实例
     */
    private final MissingpersonService missingpersonService;

    /**
     * 注册失踪人员信息
     *
     * @param missingPerson 失踪人员实体信息（JSON请求体）
     * @return 响应结果，包含注册后的失踪人员信息
     */
    @PostMapping
    public Result<MissingPerson> register(@RequestBody MissingPerson missingPerson) {
        return Result.success(missingpersonService.register(missingPerson));
    }

    /**
     * 注销失踪人员记录
     *
     * @param rid 失踪人员记录主键ID（路径参数）
     * @return 响应结果，无返回数据
     */
    @DeleteMapping("/{rid}")
    public Result<Void> cancel(@PathVariable Long rid) {
        missingpersonService.cancel(rid);
        return Result.success();
    }

    /**
     * 记录失踪人员寻回信息
     *
     * @param recovery 寻回记录实体信息（JSON请求体）
     * @return 响应结果，包含保存后的寻回记录信息
     */
    @PostMapping("/recovery")
    public Result<MissingPersonRecovery> recordRecovery(@RequestBody MissingPersonRecovery recovery) {
        return Result.success(missingpersonService.recordRecovery(recovery));
    }

    /**
     * 分页搜索失踪人员信息
     *
     * @param residentUuid 居民唯一标识（可选请求参数）
     * @param status 状态（可选请求参数）
     * @param name 姓名（可选请求参数）
     * @param page 页码，默认1（请求参数）
     * @param size 页大小，默认20（请求参数）
     * @return 响应结果，包含分页后的失踪人员列表及分页信息
     */
    @GetMapping("/search")
    public Result<PageResult<MissingPerson>> search(@RequestParam(required = false) String residentUuid,
                                                    @RequestParam(required = false) String status, @RequestParam(required = false) String name,
                                                    @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = new PageRequest(page, size, null, "DESC");
        return Result.success(missingpersonService.search(residentUuid, status, name, pageRequest));
    }

    /**
     * 获取失踪人员统计数据
     *
     * @return 响应结果，包含统计数据Map
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        return Result.success(missingpersonService.getStatistics());
    }
}