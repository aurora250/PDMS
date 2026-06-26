package com.pdm.keyperson.service;

import com.pdm.keyperson.entity.KeyPerson;
import com.pdm.keyperson.entity.PetitionRecord;
import com.pdm.keyperson.entity.VisitPlan;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 重点人员业务服务接口
 * 定义重点人员全生命周期管理、走访计划、信访记录等核心业务方法
 */
public interface KeypersonService {

    /**
     * 新增指定重点人员
     *
     * @param keyPerson 重点人员实体（包含uuid、管控等级等必填信息）
     * @return 新增后的重点人员实体
     */
    KeyPerson designateKeyPerson(KeyPerson keyPerson);

    /**
     * 撤销重点人员身份
     *
     * @param uuid 重点人员唯一标识
     */
    void revokeKeyPerson(String uuid);

    /**
     * 修改重点人员管控等级
     *
     * @param uuid 重点人员唯一标识
     * @param controlLevel 新的管控等级
     * @return 修改后的重点人员实体
     */
    KeyPerson updateControlLevel(String uuid, String controlLevel);

    /**
     * 多条件查询重点人员列表
     *
     * @param conditions 查询条件（支持uuid、controlLevel、controlType、responsiblePoliceNo）
     * @return 符合条件的重点人员列表
     */
    List<KeyPerson> searchKeyPersons(Map<String, Object> conditions);

    /**
     * 生成重点人员走访计划
     *
     * @param visitPlan 走访计划基础信息（包含keyPersonUuid）
     * @return 生成后的走访计划实体
     */
    VisitPlan generateVisitPlan(VisitPlan visitPlan);

    /**
     * 完成走访计划
     *
     * @param planId 走访计划ID
     * @param actualDate 实际走访日期
     * @param petitionRecord 信访记录（可选）
     * @return 完成后的走访计划实体
     */
    VisitPlan completeVisit(Long planId, LocalDate actualDate, PetitionRecord petitionRecord);

    /**
     * 记录重点人员信访信息
     *
     * @param petitionRecord 信访记录实体
     * @return 保存后的信访记录实体
     */
    PetitionRecord recordPetition(PetitionRecord petitionRecord);

    /**
     * 获取重点人员GIS可视化数据
     *
     * @return GIS数据列表（包含uuid、管控等级、管控类型）
     */
    List<Map<String, Object>> getGisData();
}