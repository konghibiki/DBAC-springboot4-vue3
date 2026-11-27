package com.example.springboot.system.controller;


import cn.dev33.satoken.stp.StpUtil;
import com.example.springboot.system.common.Result;
import com.example.springboot.system.dto.UserEmailUpdateDTO;
import com.example.springboot.system.dto.UserLoginDTO;
import com.example.springboot.system.dto.UserPasswordUpdateDTO;
import com.example.springboot.system.dto.UserRegisterDTO;
import com.example.springboot.system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthService authService;

    /**
     * 创建账号
     */
    @PostMapping("/register")
    public Result<Long> register(
            @Valid @RequestBody UserRegisterDTO dto) {

        // Public registration must never accept caller-selected roles.
        dto.setRole(null);
        authService.register(dto);

        return success(null, "账号创建成功");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Boolean> login(
            @Valid @RequestBody UserLoginDTO dto) {

        return authService.login(dto) ? success(true, "登录成功")
                : success(false, "请选择权限登录");
    }

    /**
     * 选择权限登录
     */
    @PostMapping("/loginWithRole")
    public Result<Void> loginWithRole(
            @Valid @RequestBody Long roleId) {

        authService.loginWithRole(roleId);
        return success(null, "登录成功");
    }

    /**
     * 退出当前登录账号
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        StpUtil.logout();
        return success(null, "退出登录成功");
    }

    /**
     * 更改当前账号绑定邮箱。
     */
    @PutMapping("/email")
    public Result<Void> updateEmail(
            @Valid @RequestBody UserEmailUpdateDTO dto) {

        authService.updateEmail(dto);
        return success(null, "绑定邮箱更新成功");
    }

    /**
     * 校验当前密码后设置新密码。
     */
    @PutMapping("/password")
    public Result<Void> updatePassword(
            @Valid @RequestBody UserPasswordUpdateDTO dto) {

        authService.updatePassword(dto);
        return success(null, "密码更新成功");
    }

    /**
     * 检查当前用户是否已经登录
     */
    @GetMapping("/isLogin")
    public Result<Boolean> isLogin() {
        if (!StpUtil.isLogin()) {
            return success(false);
        }

        Object activeRoleId = StpUtil.getTokenSession().get("activeRoleId");
        if (activeRoleId == null || "".equals(activeRoleId.toString())) {
            return success(false);
        }

        if (activeRoleId instanceof Number number) {
            return success(number.longValue() > 0);
        }

        try {
            return success(Long.parseLong(activeRoleId.toString()) > 0);
        } catch (NumberFormatException exception) {
            return success(false);
        }
    }

}
