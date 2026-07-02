package com.pdm.household.service;

import com.pdm.household.entity.*;

import java.util.List;
import java.util.Map;

public interface HouseholdService {
    // Household book
    HouseholdRegister applyBook(HouseholdRegister book);

    HouseholdRegister reissueBook(String bookNo);

    HouseholdRegister renewBook(String bookNo);

    HouseholdRegister approveBook(Long id, String action, String handlerUuid);

    /** 按居民UUID查询户口簿信息（含成员列表） */
    Map<String, Object> getBookByResident(String residentUuid);

    // Business request (registration/cancellation/householder change)
    HouseholdBusinessRequest submitBusiness(HouseholdBusinessRequest request);

    HouseholdBusinessRequest approveBusiness(Long rid, String status, String handlerUuid, String rejectReason);

    /** 附加审核材料（街道办权限） */
    HouseholdBusinessRequest attachBusinessMaterial(Long rid, String attachmentPath, String remark);

    // Migration
    HouseholdMigrationRequest submitMigration(HouseholdMigrationRequest request);

    HouseholdMigrationRequest approveMigration(Long rid, String status, String handlerUuid, String rejectReason);

    /** 附加审核材料（街道办权限） */
    HouseholdMigrationRequest attachMigrationMaterial(Long rid, String attachmentPath, String remark);

    List<HouseholdMigrationRequest> getMigrationTrace(String residentUuid);

    // Permits
    ApprovalPermit issueApprovalPermit(ApprovalPermit permit);

    MigrationPermit issueMigrationPermit(MigrationPermit permit);

    ApprovalPermit voidApprovalPermit(Long id);

    MigrationPermit voidMigrationPermit(Long id);

    // Area
    List<Area> getAreaTree();

    List<Area> getAreasByParent(Long parentId);

    /** 获取区域的完整祖先链（省→市→区） */
    List<Area> getAreaAncestors(Long areaId);

    /** 获取区域的完整路径字符串（省+市+区） */
    String getAreaPath(Long areaId);

    /** 获取全部区域数据（用于前端级联树） */
    List<Area> getAllAreas();
}
