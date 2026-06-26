package com.pdm.resident.mapper;

import com.pdm.resident.entity.Resident;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 常住人口数据访问层
 * 基于MyBatis-Plus实现常住人口数据库操作，扩展自定义查询方法
 */
@Mapper
public interface ResidentMapper extends BaseMapper<Resident> {

    /**
     * 根据UUID查询未删除的常住人口信息
     * @param uuid 人员唯一标识
     * @return 常住人口实体对象
     */
    @Select("SELECT * FROM resident WHERE uuid = #{uuid} AND is_deleted = 0")
    Resident selectByUuid(@Param("uuid") String uuid);

    /**
     * 根据身份证号查询未删除的常住人口信息
     * @param idCardNo 身份证号
     * @return 常住人口实体对象
     */
    @Select("SELECT * FROM resident WHERE id_card_no = #{idCardNo} AND is_deleted = 0")
    Resident selectByIdCardNo(@Param("idCardNo") String idCardNo);
}