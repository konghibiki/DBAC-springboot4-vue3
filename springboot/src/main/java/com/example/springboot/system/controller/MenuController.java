package com.example.springboot.system.controller;


import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.system.common.Result;
import com.example.springboot.system.dto.MenuDTO;
import com.example.springboot.system.service.MenuService;
import com.example.springboot.system.vo.MenuVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单控制器
 */

@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
@Validated
public class MenuController extends BaseController {

    private final MenuService menuService;

    /**
     * 获取用户自己的菜单列表
     */
    @GetMapping("/self")
    public Result<List<String>> getMenuSelf() {
        return success(menuService.listSelf(), "获取用户自己的菜单列表成功");
    }

    /**
     * 获取菜单列表
     */
    @SaCheckPermission("menu.list")
    @GetMapping("/list/{pageNum}")
    public Result<Page<MenuVO>> getMenuList(
            @PathVariable @Min(1) Integer pageNum,
            @RequestParam @Min(1) @Max(100) Integer pageSize) {
        return success(menuService.list(pageNum, pageSize), "获取菜单列表成功");
    }

    /**
     * 添加菜单
     */
    @SaCheckPermission("menu.add")
    @PostMapping("/add")
    public Result<Void> addMenu(@Valid @RequestBody MenuDTO menuDTO) {
        menuService.add(menuDTO);
        return success(null, "添加菜单成功");
    }

    /**
     * 修改菜单
     */
    @SaCheckPermission("menu.update")
    @PostMapping("/update")
    public Result<Void> updateMenu(@Valid @RequestBody MenuDTO menuDTO) {
        menuService.update(menuDTO);
        return success(null, "修改菜单成功");
    }

    /**
     * 删除菜单
     */
    @SaCheckPermission("menu.delete")
    @PostMapping("/delete")
    public Result<Void> deleteMenu(@RequestParam Long menuId, @RequestParam String password) {
        menuService.delete(menuId, password);
        return success(null, "删除菜单成功");
    }
}
