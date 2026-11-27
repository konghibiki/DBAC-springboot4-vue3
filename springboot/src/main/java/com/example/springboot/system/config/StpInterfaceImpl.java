package com.example.springboot.system.config;

import cn.dev33.satoken.stp.StpInterface;
import com.example.springboot.system.service.PermissionService;
import com.example.springboot.system.service.RoleService;
import com.example.springboot.system.vo.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义权限加载接口实现类
 */
@Component // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {


    private final RoleService roleService;
    private final PermissionService permissionService;

    /**
     * 获取用户权限码
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return permissionService.getPermissionListByUserId();
    }

    /**
     * 获取用户角色
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        Long userId = Long.valueOf(loginId.toString());

        return roleService.getRoleListByUserId(userId).stream()
                .map(RoleVO::getRoleName)
                .collect(Collectors.toList());
    }
}