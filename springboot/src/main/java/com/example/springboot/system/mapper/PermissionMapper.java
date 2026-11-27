package com.example.springboot.system.mapper;

import com.example.springboot.system.entity.Permission;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PermissionMapper extends MPJBaseMapper<Permission> {
}
