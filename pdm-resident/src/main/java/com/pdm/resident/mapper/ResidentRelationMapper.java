package com.pdm.resident.mapper;

import com.pdm.resident.entity.ResidentRelation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 常住人口亲属关系数据访问层
 * 基于MyBatis-Plus实现亲属关系数据库操作，扩展自定义查询方法
 */
@Mapper
public interface ResidentRelationMapper extends BaseMapper<ResidentRelation> {

    /**
     * 根据人员UUID查询未删除的亲属关系
     * @param uuid 人员唯一标识
     * @return 亲属关系实体对象
     */
    @Select("SELECT * FROM resident_relation WHERE relation_person_uuid = #{uuid} AND is_deleted = 0")
    ResidentRelation selectByPersonUuid(@Param("uuid") String uuid);
}