package com.example.springboot.system.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
@TableName("permission")
public class Permission {

    // 权限ID
    @TableId(value = "permission_id", type = IdType.AUTO)
    private Long permissionId;

    // 权限名称
    @TableField("permission_name")
    private String permissionName;

    // 权限标识
    @TableField("permission_code")
    private String permissionCode;

    // 权限类型
    @TableField("permission_type")
    private String permissionType;

    // 权限描述
    private String description;

    // 权限创建时间
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
