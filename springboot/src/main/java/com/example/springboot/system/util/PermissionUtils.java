package com.example.springboot.system.util;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 权限对比判断类
 */
public class PermissionUtils {

    /**
     * 判断 grantedPermission 是否有权覆盖 requestedPermission
     *
     * *.add  -> user.add    true
     * user.* -> user.add     true
     * user.add -> user.add   true
     * user.add -> user.*     false
     */
    public static boolean matches(String grantedPermission, String requestedPermission) {

        if (grantedPermission == null || requestedPermission == null) {
            return false;
        }

        String[] granted = grantedPermission.split("\\.");
        String[] requested = requestedPermission.split("\\.");

        // 权限结构长度必须一致
        if (granted.length != requested.length) {
            return false;
        }

        for (int i = 0; i < granted.length; i++) {

            // 当前授权段为通配符，可以覆盖目标段
            if ("*".equals(granted[i])) {
                continue;
            }

            // 目标段为通配符，但授权段不是通配符，无法覆盖
            if ("*".equals(requested[i])) {
                return false;
            }

            // 普通字符串必须完全匹配
            if (!granted[i].equals(requested[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * 筛选出当前用户有权授予的权限
     */
    public static Set<String> filterPermissions(
            Set<String> userPermissions,
            Set<String> dtoPermissions) {

        return dtoPermissions.stream()
                .filter(dtoPermission ->
                        userPermissions.stream().anyMatch(
                                userPermission -> matches(
                                        userPermission,
                                        dtoPermission
                                )
                        )
                )
                .collect(Collectors.toSet());
    }
}