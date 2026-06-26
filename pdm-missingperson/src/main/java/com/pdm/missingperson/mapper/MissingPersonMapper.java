package com.pdm.missingperson.mapper;

import com.pdm.missingperson.entity.MissingPerson;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 失踪人员数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，实现失踪人员基础数据库操作，扩展按居民UUID查询未删除失踪人员的方法
 */
@Mapper
public interface MissingPersonMapper extends BaseMapper<MissingPerson> {

    /**
     * 根据居民UUID查询未被逻辑删除的失踪人员信息
     *
     * @param residentUuid 居民唯一标识
     * @return 匹配的失踪人员实体，无匹配则返回null
     */
    @Select("SELECT * FROM missing_person WHERE resident_uuid = #{residentUuid} AND is_deleted = 0")
    MissingPerson selectByResidentUuid(@Param("residentUuid") String residentUuid);
}