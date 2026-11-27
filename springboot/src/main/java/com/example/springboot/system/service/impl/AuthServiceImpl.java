package com.example.springboot.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.example.springboot.system.common.enums.UserStatusEnum;
import com.example.springboot.system.dto.UserEmailUpdateDTO;
import com.example.springboot.system.dto.UserLoginDTO;
import com.example.springboot.system.dto.UserPasswordUpdateDTO;
import com.example.springboot.system.dto.UserRegisterDTO;
import com.example.springboot.system.entity.Role;
import com.example.springboot.system.entity.User;
import com.example.springboot.system.entity.UserInfo;
import com.example.springboot.system.entity.UserRole;
import com.example.springboot.system.mapper.RoleMapper;
import com.example.springboot.system.mapper.UserInfoMapper;
import com.example.springboot.system.mapper.UserMapper;
import com.example.springboot.system.mapper.UserRoleMapper;
import com.example.springboot.system.service.AuthService;
import com.example.springboot.system.service.IBaseService;
import com.example.springboot.system.service.RoleService;
import com.example.springboot.system.vo.RoleVO;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl
        extends CrudRepository<UserMapper, User>
        implements IBaseService<User>, AuthService {

    private final UserMapper userMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    /**
     * 创建账号，同时创建用户详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO dto) {

        // 1. 检查用户邮箱是否已经存在
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, dto.getEmail())
        );

        if (count > 0) {
            throw new IllegalArgumentException("邮箱已注册");
        }

        // 2. 创建 user 主表对象
        User user = new User();

        user.setEmail(dto.getEmail());

        // 密码必须加密，不能明文保存
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // 账号默认状态为正常
        user.setStatus(UserStatusEnum.NORMAL.getCode());

        // 3. 插入 user 表
        userMapper.insert(user);

        // 4. 获取数据库自动生成的用户 ID
        Long userId = user.getUserId();

        // 5. 创建 user_info 对象
        UserInfo userInfo = new UserInfo();

        userInfo.setUserId(userId);
        userInfo.setUserName(dto.getUserName());
        userInfo.setGender(dto.getGender() != null ? dto.getGender() : 0);

        // 6. 插入 user_info 表
        userInfoMapper.insert(userInfo);

        // Public registration uses the ordinary-user role; administrative
        // creation may supply roles only after server-side authorization.
        List<Role> roles;
        if (dto.getRole() == null || dto.getRole().isEmpty()) {
            Role defaultRole = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>()
                            .eq(Role::getRoleName, "普通用户")
            );
            if (defaultRole == null) {
                throw new IllegalStateException("默认普通用户角色未配置");
            }
            roles = List.of(defaultRole);
        } else {
            Set<String> roleNames = new HashSet<>(dto.getRole());
            roles = roleMapper.selectList(
                    new LambdaQueryWrapper<Role>()
                            .in(Role::getRoleName, roleNames)
            );
            if (roles.size() != roleNames.size()) {
                throw new IllegalArgumentException("所选角色不存在");
            }
        }

        for (Role role : roles) {
            UserRole userRole = new UserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(role.getRoleId());
            userRoleMapper.insert(userRole);
        }
    }

    /**
     * 用户登录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean login(UserLoginDTO dto) {
        // 1. 检查用户名是否存在
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, dto.getEmail())
        );

        if (user == null) {
            throw new IllegalArgumentException("邮箱不存在");
        }

        // 2. 检查密码是否正确
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        if(!Objects.equals(user.getStatus(), UserStatusEnum.NORMAL.getCode())){
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }

        List<RoleVO> roles = roleService.getRoleListByUserId(user.getUserId());
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("该账号尚未分配可登录职能");
        }

        // 3. Check whether the account has multiple roles.
        if (roles.size() > 1) {
            StpUtil.login(user.getUserId(), setParameter(dto.getRememberMe()));
            StpUtil.getTokenSession().set(
                    "activeRoleId",
                    ""
            );
            return false;
        }

        // 4. 执行登录
        StpUtil.login(user.getUserId(), setParameter(dto.getRememberMe()));
        StpUtil.getTokenSession().set(
                "activeRoleId",
                roles.getFirst().getRoleId()
        );
        return true;
    }

    /**
     * 选择权限登录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void loginWithRole(Long roleId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Long membershipCount = userRoleMapper.selectCount(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
                        .eq(UserRole::getRoleId, roleId)
        );
        if (membershipCount == 0) {
            throw new IllegalArgumentException("当前账号无权选择该职能");
        }
        StpUtil.getTokenSession().set(
                "activeRoleId",
                roleId
        );
    }

    /**
     * 设置登录参数
     */
    @NonNull
    private SaLoginParameter setParameter(Boolean rememberMe) {
        // 根据是否勾选记住我设置登录参数
        SaLoginParameter parameter = new SaLoginParameter()
                .setIsLastingCookie(rememberMe);

        if (Boolean.TRUE.equals(rememberMe)) {
            // 记住我：
            // 30天无操作才失效
            parameter.setTimeout(-1);
            parameter.setActiveTimeout(2592000);
        } else {
            // 不记住我：
            // 24小时后必须重新登录
            parameter.setTimeout(86400);
            parameter.setActiveTimeout(-1);
        }

        return parameter;
    }

    /**
     * 用户权限检查
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean checkRole() {
        if (!StpUtil.isLogin()) {
            return false;
        }

        Object activeRoleId = StpUtil.getTokenSession().get("activeRoleId");
        if (activeRoleId == null) {
            return false;
        }

        final long roleId;
        try {
            roleId = Long.parseLong(activeRoleId.toString());
        } catch (NumberFormatException exception) {
            return false;
        }

        UserRole userRole = userRoleMapper.selectOne(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getRoleId, roleId)
                        .eq(UserRole::getUserId, StpUtil.getLoginIdAsLong())
        );
        return userRole != null;
    }

    /**
     * 校验新邮箱未被其他账号使用后更新登录账号。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEmail(UserEmailUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        String email = dto.getEmail().trim();

        Long duplicateCount = userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
                        .ne(User::getUserId, userId)
        );
        if (duplicateCount > 0) {
            throw new IllegalArgumentException("该邮箱已被其他账号绑定");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户账号不存在");
        }

        user.setEmail(email);
        if (userMapper.updateById(user) == 0) {
            throw new IllegalStateException("绑定邮箱更新失败");
        }
    }

    /**
     * 修改密码前必须验证当前密码，并只保存加密后的新密码。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(UserPasswordUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户账号不存在");
        }
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("当前密码不正确");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPassword())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        if (userMapper.updateById(user) == 0) {
            throw new IllegalStateException("密码更新失败");
        }
    }
}
