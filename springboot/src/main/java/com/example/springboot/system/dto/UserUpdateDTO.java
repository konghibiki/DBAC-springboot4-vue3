package com.example.springboot.system.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

/**
 * 用户更新DTO
 */
@Data
public class UserUpdateDTO {

    // 用户ID
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    // 用户名
    @Size(min = 1, max = 18, message = "昵称长度必须在1到18之间")
    private String userName;

    // 性别
    private Integer gender;

    // 状态
    private String status;

    // 邮箱
    @Email(message = "邮箱格式错误")
    @Size(max = 254, message = "邮箱地址不能超过254位")
    private String email;

    // 密码
    @Size(min = 6, max = 18, message = "密码长度必须在6到18之间")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&#._-]+$",
            message = "密码必须包含至少一个字母和一个数字，且只能使用允许的字符"
    )
    private String password;

    // 角色
    @NotEmpty(message = "请至少选择一个用户职能")
    private List<String> role;
}
