package com.pdm.household.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pdm.household.entity.Area;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface AreaMapper extends BaseMapper<Area> {
    @Select("SELECT * FROM area WHERE parent_id = #{parentId} AND is_deleted = 0 ORDER BY area_code")
    List<Area> selectByParentId(@Param("parentId") Long parentId);

    @Select("SELECT * FROM area WHERE area_level = #{level} AND is_deleted = 0 ORDER BY area_code")
    List<Area> selectByLevel(@Param("level") String level);
}
