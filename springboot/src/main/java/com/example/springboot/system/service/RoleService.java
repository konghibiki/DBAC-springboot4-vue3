package com.example.springboot.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.system.dto.RoleDTO;
import com.example.springboot.system.entity.Role;
import com.example.springboot.system.vo.RoleVO;

import java.util.List;

public interface RoleService {

    /**
     * 获取角色列表 （添加用户时使用）
     */
    List<RoleVO> getRoleList();

    /**
     * Validate that the active role may assign every requested role.
     */
    List<Role> validateAssignableRoles(List<String> roleNames);

    /**
     * 获取所有角色列表（分页）
     */
    Page<RoleVO> getRoleAllList(Integer pageNum, Integer pageSize);

    /**
     * 获取用户角色列表
     */
    List<RoleVO> getRoleListByUserId(Long userId);

    /**
     * 查找角色（分页）
     */
    Page<RoleVO> findRoleList(String findInfo, Integer pageNum, Integer pageSize);

    /**
     * 添加角色
     */
    void addRole(RoleDTO role);

    /**
     * 修改角色
     */
    void updateRole(RoleDTO role);

    /**
     * 删除角色
     */
    void deleteRole(Long roleId, String password);
}
