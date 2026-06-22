package com.pdm.missingperson.service;

import com.pdm.common.dto.PageResult;
import com.pdm.common.dto.PageRequest;
import com.pdm.missingperson.entity.MissingPerson;
import com.pdm.missingperson.entity.MissingPersonRecovery;

import java.util.Map;

public interface MissingpersonService {

    MissingPerson register(MissingPerson missingPerson);

    void cancel(Long rid);

    MissingPersonRecovery recordRecovery(MissingPersonRecovery recovery);

    PageResult<MissingPerson> search(String residentUuid, String status, String name, PageRequest pageRequest);

    Map<String, Object> getStatistics();
}
