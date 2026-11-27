package com.example.springboot.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuDTO {

    // 菜单ID
    private Long menuId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(min = 1, max = 20, message = "菜单名称长度必须在1到20之间")
    private String menuName;

    @NotBlank(message = "路由路径不能为空")
    @Size(min = 1, max = 200, message = "路由路径长度必须在1到200之间")
    private String routePath;

    @NotBlank(message = "路由名称不能为空")
    @Size(min = 1, max = 50, message = "路由名称长度必须在1到50之间")
    private String routeName;

    @NotBlank(message = "组件路径不能为空")
    @Size(min = 1, max = 200, message = "组件路径长度必须在1到200之间")
    private String componentPath;

    @NotBlank(message = "权限不能为空")
    private String permissionCode;

    @NotNull(message = "是否缓存不能为空")
    private Boolean keepAlive;

    // 图标
    private String icon;
}
