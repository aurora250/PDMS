package com.pdm.household.service;

import com.pdm.household.entity.*;

import java.util.List;

public interface HouseholdService {
    // Household book
    HouseholdRegister applyBook(HouseholdRegister book);

    HouseholdRegister reissueBook(String bookNo);

    HouseholdRegister renewBook(String bookNo);

    // Business request (registration/cancellation/householder change)
    HouseholdBusinessRequest submitBusiness(HouseholdBusinessRequest request);

    HouseholdBusinessRequest approveBusiness(Long rid, String status, String handlerUuid, String rejectReason);

    // Migration
    HouseholdMigrationRequest submitMigration(HouseholdMigrationRequest request);

    HouseholdMigrationRequest approveMigration(Long rid, String status, String handlerUuid, String rejectReason);

    List<HouseholdMigrationRequest> getMigrationTrace(String residentUuid);

    // Permits
    ApprovalPermit issueApprovalPermit(ApprovalPermit permit);

    MigrationPermit issueMigrationPermit(MigrationPermit permit);

    // Area
    List<Area> getAreaTree();

    List<Area> getAreasByParent(Long parentId);
}
