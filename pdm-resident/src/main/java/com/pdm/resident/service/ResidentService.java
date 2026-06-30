package com.pdm.resident.service;

import com.pdm.common.dto.PageResult;
import com.pdm.resident.dto.ResidentImportResult;
import com.pdm.resident.dto.ResidentSearchRequest;
import com.pdm.resident.dto.ResidentRelationVO;
import com.pdm.resident.entity.Resident;
import com.pdm.resident.entity.ResidentChangeRequest;
import com.pdm.resident.entity.ResidentRelation;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ResidentService {

    Resident createResident(Resident resident);

    Resident getResident(String uuid);

    Resident updateResident(String uuid, Resident updates);

    void deleteResident(String uuid);

    PageResult<Resident> search(ResidentSearchRequest request);

    ResidentRelation getRelations(String uuid);

    /** 获取关系（含姓名） */
    ResidentRelationVO getRelationsWithNames(String uuid);

    /** 获取子女列表 */
    List<Map<String, Object>> getChildren(String uuid);

    ResidentRelation setRelations(ResidentRelation relation);

    PageResult<ResidentChangeRequest> listChangeRequests(String status, int page, int size);

    ResidentChangeRequest submitChangeRequest(ResidentChangeRequest request);

    ResidentChangeRequest approveChangeRequest(Long rid, String status, String handlerUuid);

    ResidentImportResult importExcel(MultipartFile file);

    void exportExcel(Map<String, Object> conditions, java.io.OutputStream outputStream);
}
