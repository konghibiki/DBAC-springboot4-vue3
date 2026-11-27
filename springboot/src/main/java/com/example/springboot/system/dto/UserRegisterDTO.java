package com.example.springboot.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 用户注册DTO
 */
@Data
public class UserRegisterDTO {

    /**
     * 登录账号
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式错误")
    private String email;

    /**
     * 登录密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 18, message = "密码长度必须在6到18之间")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&#._-]+$",
            message = "密码必须包含至少一个字母和一个数字，且只能使用允许的字符"
    )
    private String password;

    /**
     * 用户昵称
     */
    @NotBlank(message = "昵称不能为空")
    @Size(min = 1, max = 18, message = "昵称长度必须在1到18之间")
    private String userName;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 角色
     */
    private List<String> role;
}
