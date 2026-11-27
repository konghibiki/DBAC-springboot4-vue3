package com.example.springboot.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.system.dto.PermissionDTO;
import com.example.springboot.system.vo.PermissionVO;

import java.util.List;

/**
 * 权限服务接口
 */
public interface PermissionService {

    /**
     * 获取所有权限列表
     */
    Page<PermissionVO> getAllPermissionList(Integer pageNum, Integer pageSize);

    /**
     * Permission directory used by role assignment.
     */
    List<PermissionVO> getPermissionDirectory();

    /**
     * 获取当前用户权限列表
     */
    List<String> getPermissionListByUserId();

    /**
     * 获取当前用户权限对象列表（含权限名称与编码，供职权分配展示）
     */
    List<PermissionVO> getPermissionVOListBySelf();

    /**
     * 获取角色权限列表
     */
    List<String> getPermissionListByRoleId(Long roleId);

    /**
     * 获取所有list权限
     */
    List<String> getAllPermissionListOfList();

    /**
     * 搜索权限
     */
    Page<PermissionVO> searchPermission(String findInfo, Integer pageNum, Integer pageSize);

    /**
     * 权限增加
     */
    void addPermission(PermissionDTO permissionDTO);

    /**
     * 权限删除
     */
    void deletePermission(Long permissionId, String password);

    /**
     * 权限更新
     */
    void updatePermission(PermissionDTO permissionDTO);
}
