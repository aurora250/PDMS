package com.pdm.household.mapper;

import com.pdm.household.entity.Area;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 行政区划区域 Mapper 接口。
 *
 * <p>继承 MyBatis-Plus {@link BaseMapper}，提供区域表的基础 CRUD 操作， 并扩展了按父级 ID 和层级查询的方法，用于构建区域树。
 */
@Mapper
public interface AreaMapper extends BaseMapper<Area> {

    /**
     * 根据父级 ID 查询子区域列表。
     *
     * @param parentId 父级区域 ID
     * @return 子区域列表，按区域编码排序
     */
    @Select(
            "SELECT * FROM area WHERE parent_id = #{parentId} AND is_deleted = 0 ORDER BY area_code")
    List<Area> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 根据区域层级查询区域列表。
     *
     * @param level 区域层级（省/市/区县等）
     * @return 该层级的区域列表，按区域编码排序
     */
    @Select("SELECT * FROM area WHERE area_level = #{level} AND is_deleted = 0 ORDER BY area_code")
    List<Area> selectByLevel(@Param("level") String level);
}
