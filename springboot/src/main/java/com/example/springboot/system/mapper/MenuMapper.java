package com.example.springboot.system.mapper;

import com.example.springboot.system.entity.Menu;
import com.github.yulichang.base.MPJBaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统菜单表Mapper
 */
@Mapper
public interface MenuMapper extends MPJBaseMapper<Menu> {
}
