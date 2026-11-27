package com.example.springboot.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RoleDTO {

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 角色名称
     */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 18, message = "角色名称最多18个字符")
    private String roleName;

    /**
     * 角色所包含的权限列表
     */
    @NotEmpty(message = "角色所包含的权限列表不能为空")
    private List<String> rolePermissions;

    /**
     * 角色等级
     */
    private Integer roleLevel;
}
