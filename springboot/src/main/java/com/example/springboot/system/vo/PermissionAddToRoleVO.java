package com.example.springboot.system.vo;

import lombok.Data;

/**
 * 自己的权限VO
 */

@Data
public class PermissionAddToRoleVO {
    /**
     * 权限ID
     */
    private Long permissionId;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限类型
     */
    private String permissionType;
}
