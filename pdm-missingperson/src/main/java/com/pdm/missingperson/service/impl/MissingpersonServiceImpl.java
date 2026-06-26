package com.pdm.missingperson.service.impl;

import com.pdm.common.core.exception.BusinessException;
import com.pdm.common.core.result.ErrorCode;
import com.pdm.common.dto.PageRequest;
import com.pdm.common.dto.PageResult;
import com.pdm.missingperson.entity.MissingPerson;
import com.pdm.missingperson.entity.MissingPersonRecovery;
import com.pdm.missingperson.mapper.MissingPersonMapper;
import com.pdm.missingperson.mapper.MissingPersonRecoveryMapper;
import com.pdm.missingperson.service.MissingpersonService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 失踪人员业务服务实现类
 * 实现MissingpersonService接口，完成失踪人员相关业务逻辑的具体实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissingpersonServiceImpl implements MissingpersonService {

    /**
     * 失踪人员数据访问层实例
     */
    private final MissingPersonMapper missingPersonMapper;

    /**
     * 失踪人员寻回记录数据访问层实例
     */
    private final MissingPersonRecoveryMapper recoveryMapper;

    /**
     * 注册失踪人员信息
     * 若状态未指定，默认设置为"失踪中"，并将信息插入数据库
     *
     * @param missingPerson 失踪人员实体信息
     * @return 注册后的失踪人员实体（包含自增主键等信息）
     */
    @Override
    @Transactional
    public MissingPerson register(MissingPerson missingPerson) {
        if (missingPerson.getStatus() == null) {
            missingPerson.setStatus("失踪中");
        }
        missingPersonMapper.insert(missingPerson);
        return missingPerson;
    }

    /**
     * 注销失踪人员记录
     * 根据主键ID查询记录，若不存在则抛出业务异常，存在则删除该记录
     *
     * @param rid 失踪人员记录主键ID
     * @throws BusinessException 当失踪人员记录不存在时抛出
     */
    @Override
    @Transactional
    public void cancel(Long rid) {
        MissingPerson missingPerson = missingPersonMapper.selectById(rid);
        if (missingPerson == null) {
            throw new BusinessException(ErrorCode.MISSING_PERSON_NOT_FOUND);
        }
        missingPersonMapper.deleteById(rid);
    }

    /**
     * 记录失踪人员寻回信息
     * 1. 校验失踪人员记录是否存在，不存在则抛异常
     * 2. 校验该人员是否已寻回，已寻回则抛异常
     * 3. 保存寻回记录，更新失踪人员状态为"已经寻回"
     *
     * @param recovery 寻回记录实体信息
     * @return 保存后的寻回记录实体（包含自增主键等信息）
     * @throws BusinessException 失踪人员记录不存在或已寻回时抛出
     */
    @Override
    @Transactional
    public MissingPersonRecovery recordRecovery(MissingPersonRecovery recovery) {
        Long missingRecordRid = recovery.getMissingRecordRid();
        MissingPerson missingPerson = missingPersonMapper.selectById(missingRecordRid);
        if (missingPerson == null) {
            throw new BusinessException(ErrorCode.MISSING_PERSON_NOT_FOUND);
        }
        if ("已经寻回".equals(missingPerson.getStatus())) {
            throw new BusinessException(ErrorCode.MISSING_PERSON_ALREADY_RECOVERED);
        }

        recoveryMapper.insert(recovery);

        missingPerson.setStatus("已经寻回");
        missingPersonMapper.updateById(missingPerson);

        return recovery;
    }

    /**
     * 分页搜索失踪人员信息
     * 根据居民UUID、状态筛选，按创建时间降序排序，返回分页结果
     * 注：姓名参数暂未参与筛选逻辑
     *
     * @param residentUuid 居民唯一标识（可选）
     * @param status 状态（可选）
     * @param name 姓名（可选，暂未使用）
     * @param pageRequest 分页请求参数（页码、页大小、排序等）
     * @return 分页结果集，包含失踪人员列表及分页信息
     */
    @Override
    public PageResult<MissingPerson> search(String residentUuid, String status, String name, PageRequest pageRequest) {
        LambdaQueryWrapper<MissingPerson> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(residentUuid)) {
            wrapper.eq(MissingPerson::getResidentUuid, residentUuid);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(MissingPerson::getStatus, status);
        }
        wrapper.orderByDesc(MissingPerson::getCreateTime);

        IPage<MissingPerson> page = new Page<>(pageRequest.getPage(), pageRequest.getSize());
        IPage<MissingPerson> result = missingPersonMapper.selectPage(page, wrapper);

        return PageResult.of(result.getRecords(), result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    /**
     * 获取失踪人员统计数据
     * 统计总数量、失踪中数量、已寻回数量，性别和年龄段统计暂返回默认空值
     *
     * @return 统计结果Map，包含以下key：
     *         totalCount - 总记录数
     *         missingCount - 失踪中数量
     *         recoveredCount - 已寻回数量
     *         byGender - 按性别统计（男/女，暂为0）
     *         byAgeGroup - 按年龄段统计（0-12/13-18/19-35/36-60/60+，暂为0）
     */
    @Override
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalCount = missingPersonMapper.selectCount(null);
        stats.put("totalCount", totalCount);

        long missingCount = missingPersonMapper
                .selectCount(new LambdaQueryWrapper<MissingPerson>().eq(MissingPerson::getStatus, "失踪中"));
        stats.put("missingCount", missingCount);

        long recoveredCount = missingPersonMapper
                .selectCount(new LambdaQueryWrapper<MissingPerson>().eq(MissingPerson::getStatus, "已经寻回"));
        stats.put("recoveredCount", recoveredCount);

        Map<String, Long> byGender = new HashMap<>();
        byGender.put("男", 0L);
        byGender.put("女", 0L);
        stats.put("byGender", byGender);

        Map<String, Long> byAgeGroup = new HashMap<>();
        byAgeGroup.put("0-12", 0L);
        byAgeGroup.put("13-18", 0L);
        byAgeGroup.put("19-35", 0L);
        byAgeGroup.put("36-60", 0L);
        byAgeGroup.put("60+", 0L);
        stats.put("byAgeGroup", byAgeGroup);

        return stats;
    }
}