package com.example.springboot.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统菜单表
 */
@Data
@TableName("menu")
public class Menu {
    /** 菜单ID */
    @TableId(value = "menu_id", type = IdType.AUTO)
    private Long menuId;

    /** 菜单名称 */
    @TableField("menu_name")
    private String menuName;

    /** 路由路径 */
    @TableField("route_path")
    private String routePath;

    /** 路由名字 */
    @TableField("route_name")
    private String routeName;

    /** 组件路径 */
    @TableField("component_path")
    private String componentPath;

    /** 菜单权限 */
    @TableField("permission_code")
    private String permissionCode;

    /** 菜单图标 */
    @TableField("icon")
    private String icon;

    /** 菜单是否缓存 */
    @TableField("keep_alive")
    private Boolean keepAlive;

    /** 菜单创建时间 */
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 菜单更新时间 */
    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
