package com.pdm.keyperson.service;

import com.pdm.common.dto.PageResult;
import com.pdm.keyperson.entity.KeyPerson;
import com.pdm.keyperson.entity.PetitionRecord;
import com.pdm.keyperson.entity.VisitPlan;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface KeypersonService {

    KeyPerson designateKeyPerson(KeyPerson keyPerson);

    void revokeKeyPerson(String uuid);

    KeyPerson updateControlLevel(String uuid, String controlLevel);

    PageResult<KeyPerson> searchKeyPersons(String controlLevel, String controlType, String keyword, int page, int size);

    VisitPlan generateVisitPlan(VisitPlan visitPlan);

    VisitPlan completeVisit(Long planId, LocalDate actualDate, PetitionRecord petitionRecord);

    PetitionRecord recordPetition(PetitionRecord petitionRecord);

    List<Map<String, Object>> getGisData();
}
