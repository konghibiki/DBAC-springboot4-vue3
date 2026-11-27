package com.example.springboot.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.system.dto.UserProfileUpdateDTO;
import com.example.springboot.system.dto.UserRegisterDTO;
import com.example.springboot.system.dto.UserUpdateDTO;
import com.example.springboot.system.vo.UserInfoVO;

public interface UserService {

    /**
     * 更新当前用户个人资料。
     */
    void updateProfile(UserProfileUpdateDTO dto);


    /**
     * 获取当前登录用户信息
     */
    UserInfoVO getSelfInfo();


    /**
     * 获取全部用户信息
     */
    Page<UserInfoVO> getAllInfo(Integer pageNum, Integer pageSize);


    /**
     * 获取指定用户信息
     */
    Page<UserInfoVO> getUserInfo(String findInfo, String roleName, Integer pageNum, Integer pageSize);

    /**
     * 添加用户
     */
    void addUser(UserRegisterDTO dto);

    /**
     * 修改用户
     */
    void updateUser(UserUpdateDTO dto);

    /**
     * 删除用户
     */
    void deleteUser(Long userId, String password);
}