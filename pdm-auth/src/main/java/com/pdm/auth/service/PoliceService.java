package com.pdm.auth.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pdm.auth.entity.Police;

public interface PoliceService {

    Police registerPolice(Police police);

    Page<Police> listPolice(int page, int size, String keyword);

    Police getPoliceByNumber(String policeNumber);

    Police updatePolice(String policeNumber, Police updates);

    void updatePoliceStatus(String policeNumber, String dutyStatus);
}
