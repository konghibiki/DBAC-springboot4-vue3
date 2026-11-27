package com.example.springboot.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.springboot.system.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {
}
