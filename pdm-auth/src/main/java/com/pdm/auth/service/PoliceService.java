package com.pdm.auth.service;

import com.pdm.auth.entity.Police;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

public interface PoliceService {

    Police registerPolice(Police police);

    Page<Police> listPolice(int page, int size, String keyword, String residentUuid);

    Police getPoliceByNumber(String policeNumber);

    Police updatePolice(String policeNumber, Police updates);

    void updatePoliceStatus(String policeNumber, String dutyStatus);

    /** 查询未关联系统用户的民警列表（供创建用户时选择） */
    List<Map<String, Object>> listUnassociated();

    /** 获取当前登录用户的民警信息 */
    Police getCurrentPolice();

    /** 获取所有在岗民警的 resident_uuid 列表 */
    List<String> getAllPoliceResidentUuids();
}
