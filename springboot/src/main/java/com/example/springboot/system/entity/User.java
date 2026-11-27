package com.example.springboot.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户对象 user表
 */
@Data
@TableName("user")
public class User {

    // 用户ID
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    // 用户邮箱
    private String email;
    // 用户密码
    private String password;
    // 账号状态
    private Integer status;
}
