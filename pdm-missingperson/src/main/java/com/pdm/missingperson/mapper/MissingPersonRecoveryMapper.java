package com.pdm.missingperson.mapper;

import com.pdm.missingperson.entity.MissingPersonRecovery;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 失踪人员寻回记录数据访问层接口
 * 继承MyBatis-Plus的BaseMapper，实现寻回记录的基础数据库操作
 */
@Mapper
public interface MissingPersonRecoveryMapper extends BaseMapper<MissingPersonRecovery> {
}