package com.example.springboot.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.example.springboot.system.dto.RoleDTO;
import com.example.springboot.system.entity.Permission;
import com.example.springboot.system.entity.Role;
import com.example.springboot.system.entity.RolePermission;
import com.example.springboot.system.entity.UserRole;
import com.example.springboot.system.mapper.*;
import com.example.springboot.system.service.IBaseService;
import com.example.springboot.system.service.PermissionService;
import com.example.springboot.system.service.RoleService;
import com.example.springboot.system.util.PermissionUtils;
import com.example.springboot.system.vo.RoleVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 角色服务实现类
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl
        extends CrudRepository<RoleMapper, Role>
        implements IBaseService<Role>, RoleService {


    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;

    private final RolePermissionMapper rolePermissionMapper;
    private final UserRoleMapper userRoleMapper;

    private final PermissionService permissionService;

    private final PasswordEncoder passwordEncoder;

    /**
     * 获取角色列表 （添加用户时使用）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RoleVO> getRoleList() {
        Integer activeRoleLevel = getActiveRole().getRoleLevel();
        List<Role> roles = roleMapper.selectList(null);

        return roles.stream()
                .filter(role -> role.getRoleLevel() > activeRoleLevel)
                .map(role -> new RoleVO(role.getRoleId(), role.getRoleName(), role.getRoleLevel()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Role> validateAssignableRoles(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new IllegalArgumentException("请至少选择一个用户职能");
        }

        Set<String> requestedNames = new HashSet<>(roleNames);
        List<Role> roles = roleMapper.selectList(
                new LambdaQueryWrapper<Role>()
                        .in(Role::getRoleName, requestedNames)
        );
        if (roles.size() != requestedNames.size()) {
            throw new IllegalArgumentException("所选角色不存在");
        }

        Integer activeRoleLevel = getActiveRole().getRoleLevel();
        if (roles.stream().anyMatch(role -> role.getRoleLevel() <= activeRoleLevel)) {
            throw new IllegalArgumentException("不能分配不低于当前角色等级的职能");
        }
        return roles;
    }

    private Role getActiveRole() {
        Object activeRoleId = StpUtil.getTokenSession().get("activeRoleId");
        if (activeRoleId == null) {
            throw new IllegalArgumentException("当前登录职能无效");
        }
        Role activeRole = roleMapper.selectById(Long.valueOf(activeRoleId.toString()));
        if (activeRole == null) {
            throw new IllegalArgumentException("当前登录职能不存在");
        }
        return activeRole;
    }

    /**
     * 获取所有角色列表（分页）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Page<RoleVO> getRoleAllList(Integer pageNum, Integer pageSize) {

        return roleMapper.selectJoinPage(
                new Page<>(pageNum, pageSize),
                RoleVO.class,
                new MPJLambdaWrapper<Role>()
                        .orderByAsc(Role::getRoleLevel)
        );
    }

    /**
     * 获取用户角色列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<RoleVO> getRoleListByUserId(Long userId) {

        // 1. 查询用户角色ID
        List<Long> roleIdList = userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>()
                                .eq(UserRole::getUserId, userId)
                ).stream()
                .map(UserRole::getRoleId)
                .toList();

        if (roleIdList.isEmpty()) {
            return List.of();
        }

        // 2. 根据角色ID查询角色名称
        List<Role> roleList = roleMapper.selectByIds(roleIdList);

        return roleList.stream()
                .map(role -> new RoleVO(role.getRoleId(), role.getRoleName(), role.getRoleLevel()))
                .toList();
    }

    /**
     * 查找角色（分页）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Page<RoleVO> findRoleList(String findInfo, Integer pageNum, Integer pageSize) {

        return roleMapper.selectJoinPage(
                new Page<>(pageNum, pageSize),
                RoleVO.class,
                new MPJLambdaWrapper<Role>()
                        .like(Role::getRoleName, findInfo)
                        .or()
                        .like(Role::getRoleId, findInfo)
                        .orderByAsc(Role::getRoleLevel)
        );
    }

    /**
     * 添加角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRole(RoleDTO role) {

        String roleName = role.getRoleName();

        // 1. 校验角色名称是否已存在
        if (roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleName, roleName)
        ) != null) {
            throw new IllegalArgumentException("角色名称已存在");
        }
        Role newRole = new Role();
        newRole.setRoleName(roleName);
        newRole.setRoleLevel(2);
        roleMapper.insert(newRole);

        // 2. 取得新角色的ID
        Long roleId = newRole.getRoleId();

        // 3. 检查用户权限
        List<Long> permissionIds = getRolePermissionCodes(role);

        RolePermission rolePermission = new RolePermission();
        // 4. 保存角色权限
        permissionIds.forEach(permissionId -> {
            rolePermission.setRoleId(roleId);
            rolePermission.setPermissionId(permissionId);
            rolePermissionMapper.insert(rolePermission);
        });
    }

    /**
     * 修改角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(RoleDTO role) {
        // 0. 校验角色等级是否为普通角色
        if (role.getRoleLevel() != 2) {
            throw new IllegalArgumentException("不能修改超级管理员或普通用户的权限");
        }

        // 1. 校验角色是否存在
        Role existingRole = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleId, role.getRoleId())
                        .eq(Role::getRoleName, role.getRoleName())
        );

        if (existingRole == null) {
            throw new IllegalArgumentException("角色不存在");
        }

        // 2. 更新角色名称
        if (roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                        .ne(Role::getRoleId, role.getRoleId())
                        .eq(Role::getRoleName, role.getRoleName())
        ) != null) {
            throw new IllegalArgumentException("角色名称已存在");
        }

        existingRole.setRoleName(role.getRoleName());
        roleMapper.updateById(existingRole);

        // 3. 检查用户权限
        List<Long> permissionIds = getRolePermissionCodes(role);

        // 4. 更新角色权限
        RolePermission rolePermission = new RolePermission();

        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, existingRole.getRoleId())
        );

        // 3. 保存角色权限
        permissionIds.forEach(permissionId -> {
            rolePermission.setRoleId(existingRole.getRoleId());
            rolePermission.setPermissionId(permissionId);
            rolePermissionMapper.insert(rolePermission);
        });
    }

    /**
     * 获取角色权限（用于修改角色和创建角色）
     */
    @Transactional(rollbackFor = Exception.class)
    protected List<Long> getRolePermissionCodes(RoleDTO role) {

        // 1. 获取当前操作角色的权限码（授权范围）
        Set<String> userPermissionCodes = new HashSet<>(
                permissionService.getPermissionListByUserId()
        );

        if (userPermissionCodes.isEmpty()
                || role.getRolePermissions() == null
                || role.getRolePermissions().isEmpty()) {
            return List.of();
        }

        // 2. 获取 DTO 传入的权限名称
        Set<String> dtoPermissionNames = new HashSet<>(
                role.getRolePermissions()
        );

        // 3. 批量查询 DTO 对应的 Permission
        List<Permission> dtoPermissions = permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .in(Permission::getPermissionName, dtoPermissionNames)
        );

        // 4. 提取 DTO 传入的具体权限码
        Set<String> dtoPermissionCodes = dtoPermissions.stream()
                .map(Permission::getPermissionCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 5. 获取数据库中所有真实权限码（用于通配符展开）
        Set<String> allPermissionCodes = permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                                .select(Permission::getPermissionCode)
                ).stream()
                .map(Permission::getPermissionCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 6. 遍历真实权限码，使用 PermissionUtils 匹配当前角色权限
        Set<String> matchedPermissionCodes = allPermissionCodes.stream()
                .filter(permissionCode -> userPermissionCodes.stream()
                        .anyMatch(userPermission ->
                                PermissionUtils.matches(
                                        userPermission,
                                        permissionCode
                                )
                        ))
                .collect(Collectors.toSet());

        // 7. 将当前角色允许分配的权限，与 DTO 申请的权限取交集
        Set<String> filteredPermissionCodes = PermissionUtils.filterPermissions(
                matchedPermissionCodes,
                dtoPermissionCodes
        );

        if (filteredPermissionCodes.isEmpty()) {
            return List.of();
        }

        // 8. 权限码批量转换为 Permission ID
        return permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                                .in(Permission::getPermissionCode, filteredPermissionCodes)
                                .select(Permission::getPermissionId)
                ).stream()
                .map(Permission::getPermissionId)
                .toList();
    }

    /**
     * 删除角色
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRole(Long roleId, String password) {

        Long adminId = StpUtil.getLoginIdAsLong();

        // 1. 校验密码
        if (!passwordEncoder.matches(password, userMapper.selectById(adminId).getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        // 2. 校验角色是否存在
        Role existingRole = roleMapper.selectById(
                roleId
        );

        if (existingRole == null) {
            throw new IllegalArgumentException("角色不存在");
        }

        // 3. 校验角色是否有用户关联
        if (userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getRoleId, roleId)
        ) > 0) {
            throw new IllegalArgumentException("角色下有用户关联，不能删除");
        }

        if (existingRole.getRoleLevel() != 2) {
            throw new IllegalArgumentException("不能删除超级管理员或普通用户");
        }

        // 4. 删除角色
        roleMapper.deleteById(roleId);

        // 5. 删除角色权限
        rolePermissionMapper.delete(
                new LambdaQueryWrapper<RolePermission>()
                        .eq(RolePermission::getRoleId, roleId)
        );
    }
}
