package com.pdm.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.pdm.log.entity.LoginLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {
}
