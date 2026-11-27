
package com.example.springboot.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.system.common.Result;
import com.example.springboot.system.dto.UserProfileUpdateDTO;
import com.example.springboot.system.dto.UserRegisterDTO;
import com.example.springboot.system.dto.UserUpdateDTO;
import com.example.springboot.system.service.UserService;
import com.example.springboot.system.vo.UserInfoVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController extends BaseController {

    private final UserService userService;

    /**
     * 更新个人资料及可选的裁剪头像。
     */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserInfoVO> updateProfile(
            @Valid @ModelAttribute UserProfileUpdateDTO dto) {

        userService.updateProfile(dto);
        return success(userService.getSelfInfo(), "个人资料更新成功");
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/self-info")
    public Result<UserInfoVO> getSelfInfo() {
        return success(userService.getSelfInfo(), "获取成功");
    }

    /**
     * 获取全部用户信息
     */
    @GetMapping("/all/{pageNum}")
    @SaCheckPermission("user.list")
    public Result<Page<UserInfoVO>> getAllInfo(
            @PathVariable @Min(1) Integer pageNum,
            @RequestParam @Min(1) @Max(100) Integer pageSize) {
        return success(
                userService.getAllInfo(pageNum, pageSize), "获取成功");
    }

    /**
     * 获取指定用户信息
     */
    @GetMapping("/info/{pageNum}")
    @SaCheckPermission("user.list")
    public Result<Page<UserInfoVO>> getUserInfo(
            @PathVariable @Min(1) Integer pageNum,
            @RequestParam @Min(1) @Max(100) Integer pageSize,
            @RequestParam String findInfo, @RequestParam String roleName) {
        return success(userService.getUserInfo(findInfo, roleName, pageNum, pageSize), "获取成功");
    }

    /**
     * 添加用户
     */
    @SaCheckPermission("user.add")
    @PostMapping("/add")
    public Result<Void> addUser(@Valid @RequestBody UserRegisterDTO dto) {
        userService.addUser(dto);
        return success(null,"添加成功");
    }

    /**
     * 修改用户
     */
    @SaCheckPermission("user.update")
    @PutMapping("/update")
    public Result<Void> updateUser(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(dto);
        return success(null,"修改成功");
    }

    /**
     * 删除用户
     */
    @SaCheckPermission("user.delete")
    @DeleteMapping("/delete")
    public Result<Void> deleteUser(@RequestParam Long userId, @RequestParam String password) {
        userService.deleteUser(userId, password);
        return success(null,"删除成功");
    }

}