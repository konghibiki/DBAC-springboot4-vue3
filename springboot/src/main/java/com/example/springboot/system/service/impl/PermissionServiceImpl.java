package com.example.springboot.system.service.impl;


import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.example.springboot.system.dto.PermissionDTO;
import com.example.springboot.system.entity.Permission;
import com.example.springboot.system.entity.RolePermission;
import com.example.springboot.system.mapper.PermissionMapper;
import com.example.springboot.system.mapper.RoleMapper;
import com.example.springboot.system.mapper.RolePermissionMapper;
import com.example.springboot.system.mapper.UserMapper;
import com.example.springboot.system.service.IBaseService;
import com.example.springboot.system.service.PermissionService;
import com.example.springboot.system.util.PermissionUtils;
import com.example.springboot.system.vo.PermissionVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 权限服务实现类
 */
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl
        extends CrudRepository<PermissionMapper, Permission>
        implements IBaseService<Permission>, PermissionService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final PermissionMapper permissionMapper;
    private final RolePermissionMapper rolePermissionMapper;

    private final PasswordEncoder passwordEncoder;

    /**
     * 获取所有权限列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Page<PermissionVO> getAllPermissionList(Integer pageNum, Integer pageSize) {
        return permissionMapper.selectJoinPage(
                    new Page<>(pageNum, pageSize),
                    PermissionVO.class,
                    new MPJLambdaWrapper<Permission>()
                            .select(
                                    Permission::getPermissionId,
                                    Permission::getPermissionName,
                                    Permission::getPermissionCode,
                                    Permission::getPermissionType,
                                    Permission::getDescription
                            )
                            .orderByAsc(Permission::getPermissionId)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PermissionVO> getPermissionDirectory() {
        return permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .select(
                                Permission::getPermissionId,
                                Permission::getPermissionName,
                                Permission::getPermissionCode,
                                Permission::getPermissionType,
                                Permission::getDescription
                        )
                        .orderByAsc(Permission::getPermissionId)
        ).stream()
                .map(PermissionVO::new)
                .toList();
    }

    /**
     * 获取当前用户权限列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> getPermissionListByUserId() {

        // 1. 获取当前激活角色 ID
        Object activeRoleId = StpUtil.getTokenSession().get("activeRoleId");

        if (activeRoleId == null) {
            return List.of();
        }

        Long roleId = Long.valueOf(activeRoleId.toString());

        // 2. 查询当前角色拥有的原始权限码
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>()
                                .select(RolePermission::getPermissionId)
                                .eq(RolePermission::getRoleId, roleId)
                ).stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();

        if (permissionIds.isEmpty()) {
            return List.of();
        }

        Set<String> rolePermissionCodes = permissionMapper.selectByIds(permissionIds)
                .stream()
                .map(Permission::getPermissionCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (rolePermissionCodes.isEmpty()) {
            return List.of();
        }

        // 3. 查询 Permission 表中所有实际存在的权限码
        Set<String> allPermissionCodes = permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                                .select(Permission::getPermissionCode)
                ).stream()
                .map(Permission::getPermissionCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 5. 返回实际权限码
        return allPermissionCodes.stream()
                .filter(permissionCode -> rolePermissionCodes.stream()
                        .anyMatch(rolePermission ->
                                PermissionUtils.matches(
                                        rolePermission,
                                        permissionCode
                                )
                        )).distinct().collect(Collectors.toList());
    }

    /**
     * 获取当前用户权限对象列表（含权限名称与编码，供职权分配展示）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<PermissionVO> getPermissionVOListBySelf() {
        Set<String> permissionCodes = new HashSet<>(getPermissionListByUserId());
        if (permissionCodes.isEmpty()) {
            return List.of();
        }
        return permissionMapper.selectList(
                new LambdaQueryWrapper<Permission>()
                        .select(Permission::getPermissionId, Permission::getPermissionName,
                                Permission::getPermissionCode, Permission::getPermissionType,
                                Permission::getDescription)
        ).stream()
                .filter(permission -> permissionCodes.contains(permission.getPermissionCode()))
                .map(PermissionVO::new)
                .collect(Collectors.toList());
    }

    /**
     * 获取角色权限列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> getPermissionListByRoleId(Long roleId) {

        // 1. 判断角色是否存在
        if (roleMapper.selectById(roleId) == null) {
            throw new IllegalArgumentException("角色不存在");
        }

        // 2. 获取当前授权者所拥有的权限码（支持通配符）
        Set<String> userPermissionCodes = new HashSet<>(
                getPermissionListByUserId()
        );

        if (userPermissionCodes.isEmpty()) {
            return List.of();
        }

        // 3. 获取目标角色拥有的 Permission ID
        List<Long> permissionIds = rolePermissionMapper.selectList(
                        new LambdaQueryWrapper<RolePermission>()
                                .eq(RolePermission::getRoleId, roleId)
                ).stream()
                .map(RolePermission::getPermissionId)
                .distinct()
                .toList();

        if (permissionIds.isEmpty()) {
            return List.of();
        }

        // 4. 批量查询目标角色对应的 Permission Code
        Set<String> dtoPermissionCodes = permissionMapper.selectByIds(permissionIds)
                .stream()
                .map(Permission::getPermissionCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 5. 使用 PermissionUtils 过滤
        Set<String> filteredPermissionCodes = PermissionUtils.filterPermissions(
                userPermissionCodes,
                dtoPermissionCodes
        );

        // 6. 返回过滤后的真实权限码
        return new ArrayList<>(filteredPermissionCodes);
    }

    /**
     * 获取所有list权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> getAllPermissionListOfList() {
        return permissionMapper.selectList(
                        new LambdaQueryWrapper<Permission>()
                                .select(Permission::getPermissionCode)
                                .eq(Permission::getPermissionType, "API")
                ).stream()
                .map(Permission::getPermissionCode)
                .toList();
    }

    /**
     * 搜索权限
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Page<PermissionVO> searchPermission(
            String findInfo, Integer pageNum, Integer pageSize) {
        return permissionMapper.selectJoinPage(
                        new Page<>(pageNum, pageSize),
                        PermissionVO.class,
                        new MPJLambdaWrapper<Permission>()
                                .eq(Permission::getPermissionId, findInfo)
                                .or()
                                .like(Permission::getPermissionCode, findInfo)
                                .or()
                                .like(Permission::getPermissionName, findInfo)
                );
    }

    /**
     * 权限增加
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPermission(PermissionDTO permissionDTO) {

        Permission permission = new Permission();

        permissionMapper.insert(permissionInfoSet(permission, permissionDTO));
    }

    /**
     * 权限删除
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePermission(Long permissionId, String password) {

        Permission permission = permissionMapper.selectById(permissionId);
        if(permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }
        if(!passwordEncoder.matches(password, userMapper.selectById(
                StpUtil.getLoginIdAsLong()).getPassword())) {
            throw new IllegalArgumentException("权限密码错误");
        }

        permissionMapper.deleteById(permissionId);
    }

    /**
     * 权限更新
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePermission(PermissionDTO permissionDTO) {

        Permission permission = permissionMapper.selectById(permissionDTO.getPermissionId());
        if(permission == null) {
            throw new IllegalArgumentException("权限不存在");
        }

        permissionMapper.updateById(permissionInfoSet(permission, permissionDTO));
    }

    private Permission permissionInfoSet(Permission permission, PermissionDTO permissionDTO) {

        if(permissionMapper.selectOne(
                new MPJLambdaWrapper<Permission>()
                        .eq(Permission::getPermissionCode, permissionDTO.getPermissionCode())
        ) != null) {
            throw new IllegalArgumentException("权限标识已存在");
        }

        if(permissionMapper.selectOne(
                new MPJLambdaWrapper<Permission>()
                        .eq(Permission::getPermissionName, permissionDTO.getPermissionName())
        ) != null) {
            throw new IllegalArgumentException("权限名称已存在");
        }

        permission.setPermissionName(permissionDTO.getPermissionName());
        permission.setPermissionCode(permissionDTO.getPermissionCode());
        permission.setPermissionType(permissionDTO.getPermissionType());
        permission.setDescription(permissionDTO.getDescription());
        return permission;
    }
}
