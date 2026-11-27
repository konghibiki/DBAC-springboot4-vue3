package com.example.springboot.system.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UserProfileUpdateDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 1, max = 18, message = "用户名长度必须在1到18之间")
    private String userName;

    @NotNull(message = "请选择性别")
    @Min(value = 0, message = "性别选项无效")
    @Max(value = 2, message = "性别选项无效")
    private Integer gender;

    private MultipartFile avatar;
}
