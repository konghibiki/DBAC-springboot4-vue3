package com.example.springboot.system.vo;

import com.example.springboot.system.entity.Permission;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 权限VO
 */
@Data
@NoArgsConstructor
public class PermissionVO {

    /**
     * 权限ID
     */
    private Long permissionId;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 权限描述
     */
    private String permissionDesc;

    /**
     * 权限code
     */
    private String permissionCode;

    /**
     * 权限类型
     */
    private String permissionType;

    public PermissionVO(Permission permission) {
        this.permissionId = permission.getPermissionId();
        this.permissionCode = permission.getPermissionCode();
        this.permissionName = permission.getPermissionName();
        this.permissionType = permission.getPermissionType();
        this.permissionDesc = permission.getDescription();
    }
}
