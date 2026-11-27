package com.example.springboot.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色对象 role表
 */
@Data
@TableName("role")
public class Role {

    // 角色ID
    @TableId(value = "role_id", type = IdType.AUTO)
    private Long roleId;

    // 角色名称
    @TableField("role_name")
    private String roleName;

    // 角色等级
    // 1: 最高 仅限超级管理员
    // 2: 管理员等
    // 3: 普通用户
    @TableField("role_level")
    private Integer roleLevel;
}
