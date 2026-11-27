package com.example.springboot.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.system.common.Result;
import com.example.springboot.system.dto.RoleDTO;
import com.example.springboot.system.service.RoleService;
import com.example.springboot.system.vo.RoleVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色控制器
 */
@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@Validated
public class RoleController extends BaseController {

    private final RoleService roleService;

    /**
     * 获取当前用户职能表
     */
    @GetMapping("/self")
    public Result<List<RoleVO>> getSelfRoleList() {
        return success(
                roleService.getRoleListByUserId(
                        StpUtil.getLoginIdAsLong()), "获取成功");
    }

    /**
     * 获取角色 （添加用户时使用）
     */
    @SaCheckPermission("role.list")
    @GetMapping("/list")
    public Result<List<RoleVO>> getRoleList() {
        return success(
                roleService.getRoleList(), "获取成功");
    }

    /**
     *  获取所有角色
     */
    @SaCheckPermission("role.list")
    @GetMapping("/all/{pageNum}")
    public Result<Page<RoleVO>> getAllRoleList(
            @PathVariable @Min(1) Integer pageNum,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize) {
        return success(
                roleService.getRoleAllList(pageNum, pageSize), "获取成功");
    }

    /**
     * 查找角色（分页）
     */
    @SaCheckPermission("role.list")
    @GetMapping("/find/{pageNum}")
    public Result<Page<RoleVO>> findRoleList(
            @PathVariable @Min(1) Integer pageNum,
            @RequestParam String findInfo,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) Integer pageSize
) {
        return success(
                roleService.findRoleList(findInfo, pageNum, pageSize), "获取成功");
    }


    /**
     * 添加角色
     */
    @SaCheckPermission("role.add")
    @PostMapping("/add")
    public Result<Void> addRole(@Valid @RequestBody RoleDTO role) {
        roleService.addRole(role);
        return success(null, "添加成功");
    }

    /**
     * 修改角色
     */
    @SaCheckPermission("role.update")
    @PostMapping("/update")
    public Result<Void> updateRole(@Valid @RequestBody RoleDTO role) {
        roleService.updateRole(role);
        return success(null, "修改成功");
    }

    /**
     * 删除角色
     */
    @SaCheckPermission("role.delete")
    @DeleteMapping("/delete")
    public Result<Void> deleteRole(@RequestParam Long roleId, @RequestParam String password) {
        roleService.deleteRole(roleId, password);
        return success(null, "删除成功");
    }
}
