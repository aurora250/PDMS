package com.pdm.auth.mapper;

import com.pdm.auth.entity.Police;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 警员 Mapper 接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper} 获得通用 CRUD 能力， 并定义按警号和用户 UUID 查询的专用方法，查询时自动过滤已逻辑删除的记录。
 */
@Mapper
public interface PoliceMapper extends BaseMapper<Police> {

    /**
     * 根据警号查询警员（排除已删除记录）。
     *
     * @param policeNumber 警号
     * @return 警员实体，未找到则返回 {@code null}
     */
    @Select("SELECT * FROM police WHERE police_number = #{policeNumber} AND is_deleted = 0")
    Police selectByPoliceNumber(@Param("policeNumber") String policeNumber);

    /**
     * 根据用户 UUID 查询警员（排除已删除记录）。
     *
     * @param userUuid 用户 UUID
     * @return 警员实体，未找到则返回 {@code null}
     */
    @Select("SELECT * FROM police WHERE user_uuid = #{userUuid} AND is_deleted = 0")
    Police selectByUserUuid(@Param("userUuid") String userUuid);
}
