package com.pdm.keyperson.mapper;

import com.pdm.keyperson.entity.VisitPlan;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 走访计划Mapper接口
 * 基于MyBatis-Plus实现走访计划表（visit_plan）的CRUD操作
 *
 * @author 开发者
 * @since 1.0.0
 */
@Mapper
public interface VisitPlanMapper extends BaseMapper<VisitPlan> {
}