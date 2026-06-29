package com.pdm.household.mapper;

import com.pdm.household.entity.Area;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AreaMapper extends BaseMapper<Area> {
    /**
     * 按父区域 area_id 查找子区域。 注: area 表的 parent_id 列存储的是 area_code（6位行政区划代码）而非自增
     * area_id， 因此需先通过 area_id 查出 area_code，再以此为 parent_id 查找子节点。
     */
    @Select("SELECT * FROM area WHERE parent_id = (SELECT area_code FROM area WHERE area_id = #{parentId}) AND is_deleted = 0 ORDER BY area_code")
    List<Area> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询某个 area_id 的完整祖先链（从省到区县）。 使用递归 CTE，通过 area_code 匹配 parent_id 向上遍历。
     */
    @Select("WITH RECURSIVE area_path AS ("
            + "  SELECT area_id, area_code, area_name, parent_id, area_level, 1 AS depth "
            + "  FROM area WHERE area_id = #{areaId} AND is_deleted = 0 " + "  UNION ALL "
            + "  SELECT a.area_id, a.area_code, a.area_name, a.parent_id, a.area_level, p.depth + 1 "
            + "  FROM area a, area_path p WHERE a.area_code::BIGINT = p.parent_id AND a.is_deleted = 0 "
            + ") SELECT area_id, area_code, area_name, parent_id, area_level FROM area_path ORDER BY depth DESC")
    List<Area> selectAncestors(@Param("areaId") Long areaId);

    /** 根据 area_id 拼接完整地址路径（省-市-区） */
    @Select("WITH RECURSIVE area_path AS ("
            + "  SELECT area_id, area_code, area_name, parent_id, area_level, 1 AS depth "
            + "  FROM area WHERE area_id = #{areaId} AND is_deleted = 0 " + "  UNION ALL "
            + "  SELECT a.area_id, a.area_code, a.area_name, a.parent_id, a.area_level, p.depth + 1 "
            + "  FROM area a, area_path p WHERE a.area_code::BIGINT = p.parent_id AND a.is_deleted = 0 "
            + ") SELECT STRING_AGG(area_name, '' ORDER BY depth DESC) FROM area_path")
    String selectAreaPath(@Param("areaId") Long areaId);

    @Select("SELECT * FROM area WHERE area_level = #{level} AND is_deleted = 0 ORDER BY area_code")
    List<Area> selectByLevel(@Param("level") String level);

    /** 获取全部区域数据（用于构建完整级联树） */
    @Select("SELECT * FROM area WHERE is_deleted = 0 ORDER BY area_code")
    List<Area> selectAll();
}
