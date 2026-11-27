package com.example.springboot.system.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 用户登录DTO
 */
@Data
public class UserLoginDTO {

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

    @NotNull(message = "请选择是否记住我")
    private Boolean rememberMe;
}
