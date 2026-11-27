package com.example.springboot.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 用户详情对象 user_info表
 */
@Data
@TableName("user_info")
public class UserInfo {

    // 用户详情ID
    @TableId(value = "info_id", type = IdType.AUTO)
    private Long infoId;
    // 用户ID
    @TableField("user_id")
    private Long userId;
    // 用户名
    // 数据库列名是 username（没有下划线），MP默认的驼峰转下划线规则会映射成 user_name，
    // 列名不符合规则时用 @TableField 显式指定真实列名
    @TableField("user_name")
    private String userName;
    // 用户性别
    private Integer gender;
    // 用户头像
    private String avatar;
    // 创建时间
    // 控制Json格式转换
    // pattern = "yyyy-MM-dd HH:mm:ss" 表示格式转换为： xxxx-xx-xx xx:xx:xx
    @TableField("create_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    // 更新时间
    @TableField("update_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
