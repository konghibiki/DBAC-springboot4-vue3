package com.example.springboot.system.vo;

import lombok.Data;

import java.util.List;

@Data
public class UserInfoVO {

    // 用户基础信息
    private Long userId;

    private String email;

    /**
     * 用户角色
     */
    private List<String> role;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 性别
     * 0：保密
     * 1：男
     * 2：女
     */
    private String gender;

    /**
     * 账号状态
     */
    private String status;

    private String createTime;
}