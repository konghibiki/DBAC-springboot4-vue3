package com.example.springboot.system.mapper;

import com.example.springboot.system.entity.User;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户映射接口 user表
 */
@Mapper
public interface UserMapper extends MPJBaseMapper<User> {
}

