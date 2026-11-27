package com.example.springboot.system.service;

import com.example.springboot.system.dto.UserEmailUpdateDTO;
import com.example.springboot.system.dto.UserLoginDTO;
import com.example.springboot.system.dto.UserPasswordUpdateDTO;
import com.example.springboot.system.dto.UserRegisterDTO;


public interface AuthService {

    /**
     * 创建用户账号及用户详情
     */
    void register(UserRegisterDTO dto);

    /**
     * 用户登录
     */
    Boolean login(UserLoginDTO dto);

    /**
     * 选择权限登录
     */
    void loginWithRole(Long roleId);

    /**
     * 用户权限检查
     */

    Boolean checkRole();

    /**
     * 更新当前用户绑定邮箱。
     */
    void updateEmail(UserEmailUpdateDTO dto);

    /**
     * 校验当前密码并更新为新密码。
     */
    void updatePassword(UserPasswordUpdateDTO dto);
}
