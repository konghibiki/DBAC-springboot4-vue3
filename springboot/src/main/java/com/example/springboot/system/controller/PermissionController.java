package com.example.springboot.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.system.common.Result;
import com.example.springboot.system.dto.PermissionDTO;
import com.example.springboot.system.service.PermissionService;
import com.example.springboot.system.vo.PermissionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
@Validated
public class PermissionController extends BaseController{

    private final PermissionService permissionService;

    /**
     * 获取所有权限列表
     */
    @GetMapping("/all/{pageNum}")
    @SaCheckPermission("permission.list")
    public Result<Page<PermissionVO>> getAllPermissionList(
            @PathVariable @Min(1) Integer pageNum,
            @RequestParam @Min(1) @Max(100) Integer pageSize) {
        return success(permissionService
                .getAllPermissionList(pageNum, pageSize));
    }

    @GetMapping("/all")
    @SaCheckPermission("permission.list")
    public Result<List<PermissionVO>> getPermissionDirectory() {
        return success(permissionService.getPermissionDirectory());
    }

    /**
     * 获取当前用户权限
     */
    @GetMapping("/self")
    public Result<List<PermissionVO>> getPermission() {
        return success(permissionService
                .getPermissionVOListBySelf());
    }

    /**
     * 获得编辑的角色权限
     */
    @SaCheckPermission("role.update")
    @GetMapping("/role")
    public Result<List<String>> getRolePermission(@RequestParam Long roleId) {
        return success(permissionService
                .getPermissionListByRoleId(roleId));
    }

    /**
     * 获取所有list权限
     */
    @SaCheckPermission("permission.list")
    @GetMapping("/listOfList")
    public Result<List<String>> getAllPermissionListOfList() {
        return success(permissionService
                .getAllPermissionListOfList());
    }

    /**
     * 搜索权限
     */
    @GetMapping("/search/{pageNum}")
    @SaCheckPermission("permission.list")
    public Result<Page<PermissionVO>> searchPermission(
            @PathVariable @Min(1) Integer pageNum,
            @RequestParam @Min(1) @Max(100) Integer pageSize,
            @RequestParam String findInfo) {
        return success(permissionService
                .searchPermission(findInfo, pageNum, pageSize));
    }

    /**
     * 权限增加
     */
    @PostMapping("/add")
    @SaCheckPermission("permission.add")
    public Result<Void> addPermission(@Valid @RequestBody PermissionDTO permissionDTO) {
        permissionService.addPermission(permissionDTO);
        return success(null, "权限增加成功");
    }


    /**
     * 权限删除
     */
    @PostMapping("/delete")
    @SaCheckPermission("permission.delete")
    public Result<Void> deletePermission(@RequestParam Long permissionId, @RequestParam String password) {
        permissionService.deletePermission(permissionId, password);
        return success(null, "权限删除成功");
    }

    /**
     * 权限更新
     */
    @PostMapping("/update")
    @SaCheckPermission("permission.update")
    public Result<Void> updatePermission(@Valid @RequestBody PermissionDTO permissionDTO) {
        permissionService.updatePermission(permissionDTO);
        return success(null, "权限更新成功");
    }
}
