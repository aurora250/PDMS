package com.pdm.resident.service;

import com.pdm.common.dto.PageResult;
import com.pdm.resident.dto.ResidentImportResult;
import com.pdm.resident.dto.ResidentSearchRequest;
import com.pdm.resident.entity.Resident;
import com.pdm.resident.entity.ResidentChangeRequest;
import com.pdm.resident.entity.ResidentRelation;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

/**
 * 常住人口业务逻辑接口
 * 定义常住人口的增删改查、关系管理、变更申请等核心业务操作
 */
public interface ResidentService {

    /**
     * 创建常住人口信息
     * @param resident 常住人口实体对象
     * @return 创建后的常住人口信息
     */
    Resident createResident(Resident resident);

    /**
     * 根据UUID查询常住人口信息
     * @param uuid 人员唯一标识
     * @return 常住人口实体对象
     */
    Resident getResident(String uuid);

    /**
     * 更新常住人口信息
     * @param uuid 人员唯一标识
     * @param updates 待更新的字段信息
     * @return 更新后的常住人口信息
     */
    Resident updateResident(String uuid, Resident updates);

    /**
     * 删除常住人口信息（逻辑删除）
     * @param uuid 人员唯一标识
     */
    void deleteResident(String uuid);

    /**
     * 多条件分页查询常住人口信息
     * @param request 分页及查询条件
     * @return 分页查询结果
     */
    PageResult<Resident> search(ResidentSearchRequest request);

    /**
     * 根据UUID查询人员亲属关系
     * @param uuid 人员唯一标识
     * @return 亲属关系实体对象
     */
    ResidentRelation getRelations(String uuid);

    /**
     * 设置/更新人员亲属关系
     * @param relation 亲属关系实体对象
     * @return 更新后的亲属关系信息
     */
    ResidentRelation setRelations(ResidentRelation relation);

    /**
     * 提交常住人口信息变更申请
     * @param request 变更申请实体对象
     * @return 提交后的变更申请信息
     */
    ResidentChangeRequest submitChangeRequest(ResidentChangeRequest request);

    /**
     * 审批常住人口信息变更申请
     * @param rid 申请单主键ID
     * @param status 审批状态（通过/驳回）
     * @param handlerUuid 处理人UUID
     * @return 审批后的变更申请信息
     */
    ResidentChangeRequest approveChangeRequest(Long rid, String status, String handlerUuid);

    /**
     * 导入Excel文件批量新增常住人口
     * @param file Excel文件
     * @return 导入结果统计
     */
    ResidentImportResult importExcel(MultipartFile file);

    /**
     * 导出常住人口信息到Excel
     * @param conditions 查询条件
     * @param outputStream 输出流
     */
    void exportExcel(Map<String, Object> conditions, java.io.OutputStream outputStream);
}