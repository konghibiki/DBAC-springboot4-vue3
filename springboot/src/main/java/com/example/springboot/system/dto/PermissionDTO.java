package com.example.springboot.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PermissionDTO {

    private Long permissionId;

    @NotBlank(message = "权限码不能为空")
    @Size(max = 100, message = "权限码长度不能超过100个字符")
    private String permissionCode;

    @NotBlank(message = "权限名称不能为空")
    @Size(max = 100, message = "权限名称长度不能超过100个字符")
    private String permissionName;

    @NotBlank(message = "权限类型不能为空")
    private String permissionType;

    @Size(max = 255, message = "描述长度不能超过255个字符")
    private String description;
}
