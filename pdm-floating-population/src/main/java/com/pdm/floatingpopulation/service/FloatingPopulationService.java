package com.pdm.floatingpopulation.service;

import com.pdm.floatingpopulation.entity.FpRegisterRecord;
import com.pdm.floatingpopulation.entity.ResidentPermit;
import com.pdm.floatingpopulation.entity.ResidentPermitRenewal;
import com.pdm.floatingpopulation.entity.ResidentRegistration;

import java.util.List;
import java.util.Map;

public interface FloatingPopulationService {

    FpRegisterRecord registerFp(FpRegisterRecord record);

    FpRegisterRecord updateFp(Long rid, FpRegisterRecord record);

    void cancelFp(Long rid);

    ResidentPermit applyPermit(ResidentPermit permit);

    ResidentPermit approvePermit(Long id, String reviewerUuid);

    ResidentPermit issuePermit(Long id);

    ResidentPermitRenewal renewPermit(Long id, ResidentPermitRenewal renewal, String operatorUuid);

    ResidentRegistration registerResidence(ResidentRegistration registration);

    ResidentRegistration changeResidence(Long rid, ResidentRegistration registration);

    void cancelResidence(Long rid);

    List<Map<String, Object>> getHeatmapData();

    List<Map<String, Object>> getTrendData();
}
