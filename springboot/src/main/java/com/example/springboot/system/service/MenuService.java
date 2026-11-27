package com.example.springboot.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.system.dto.MenuDTO;
import com.example.springboot.system.vo.MenuVO;

import java.util.List;

public interface MenuService {

    /**
     * 获取用户自己的菜单列表
     */
    List<String> listSelf();

    /**
     * 获取菜单列表
     */
    Page<MenuVO> list(Integer pageNum, Integer pageSize);

    /**
     * 添加菜单
     */
    void add(MenuDTO menuDTO);


    /**
     * 修改菜单
     */
    void update(MenuDTO menuDTO);

    /**
     * 删除菜单
     */
    void delete(Long menuId, String password);
}
