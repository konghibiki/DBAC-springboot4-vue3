package com.example.springboot.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.example.springboot.system.common.enums.GenderEnum;
import com.example.springboot.system.common.enums.UserStatusEnum;
import com.example.springboot.system.dto.UserProfileUpdateDTO;
import com.example.springboot.system.dto.UserRegisterDTO;
import com.example.springboot.system.dto.UserUpdateDTO;
import com.example.springboot.system.entity.Role;
import com.example.springboot.system.entity.User;
import com.example.springboot.system.entity.UserInfo;
import com.example.springboot.system.entity.UserRole;
import com.example.springboot.system.mapper.RoleMapper;
import com.example.springboot.system.mapper.UserInfoMapper;
import com.example.springboot.system.mapper.UserMapper;
import com.example.springboot.system.mapper.UserRoleMapper;
import com.example.springboot.system.service.*;
import com.example.springboot.system.util.EnumUtils;
import com.example.springboot.system.vo.UserInfoVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl
        extends CrudRepository<UserMapper, User>
        implements IBaseService<User>, UserService {

    private final AuthService authService;
    private final RoleMapper roleMapper;
    private final UserMapper userMapper;
    private final UserInfoMapper userInfoMapper;
    private final UserRoleMapper userRoleMapper;

    private final RoleService roleService;
    private final AvatarStorageService avatarStorageService;

    private final PasswordEncoder passwordEncoder;

    /**
     * 更新当前登录用户的昵称、性别和可选头像。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UserProfileUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        UserInfo userInfo = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getUserId, userId)
        );
        if (userInfo == null) {
            throw new IllegalArgumentException("用户资料不存在");
        }

        String oldAvatar = userInfo.getAvatar();
        String newAvatar = null;
        userInfo.setUserName(dto.getUserName().trim());
        userInfo.setGender(dto.getGender());
        if (dto.getAvatar() != null && !dto.getAvatar().isEmpty()) {
            newAvatar = avatarStorageService.storePngAvatar(dto.getAvatar());
            userInfo.setAvatar(newAvatar);
        }

        if (newAvatar != null) {
            String storedAvatar = newAvatar;
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_COMMITTED) {
                                avatarStorageService.deleteStoredAvatar(oldAvatar);
                            } else {
                                avatarStorageService.deleteStoredAvatar(storedAvatar);
                            }
                        }
                    }
            );
        }

        if (userInfoMapper.updateById(userInfo) == 0) {
            throw new IllegalStateException("个人资料更新失败");
        }
    }

    /**
     * 获取当前登录用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO getSelfInfo() {
        // 1. 获取当前登录用户 ID
        Long userId = StpUtil.getLoginIdAsLong();

        // 2. 获取用户角色
        Role activeRole = roleMapper.selectById(
                (Serializable) StpUtil.getTokenSession().get("activeRoleId"));
        if (activeRole == null) {
            throw new IllegalArgumentException("当前登录职能不存在");
        }

        UserInfoVO userInfo = userMapper.selectJoinOne(
                UserInfoVO.class,
                new MPJLambdaWrapper<User>()
                        .select(
                                User::getUserId,
                                User::getEmail,
                                User::getStatus
                        )
                        .select(
                                UserInfo::getUserName,
                                UserInfo::getAvatar,
                                UserInfo::getGender,
                                UserInfo::getCreateTime
                        )
                        .leftJoin(
                                UserInfo.class,
                                UserInfo::getUserId,
                                User::getUserId
                        )
                        .eq(User::getUserId, userId)
        );

        if (userInfo == null) {
            throw new IllegalArgumentException("用户资料不存在");
        }
        userInfo.setRole(List.of(activeRole.getRoleName()));

        userInfo.setGender(toGenderDescription(userInfo.getGender()));
        return userInfo;
    }

    /**
     * 获取全部用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Page<UserInfoVO> getAllInfo(Integer pageNum, Integer pageSize) {

        Page<UserInfoVO> userInfoList = userMapper.selectJoinPage(
                new Page<>(pageNum, pageSize),
                UserInfoVO.class,
                new MPJLambdaWrapper<User>()
                        .select(
                                User::getUserId,
                                User::getEmail,
                                User::getStatus
                        )
                        .select(
                                UserInfo::getUserName,
                                UserInfo::getAvatar,
                                UserInfo::getGender,
                                UserInfo::getCreateTime
                        )
                        .leftJoin(
                                UserInfo.class,
                                UserInfo::getUserId,
                                User::getUserId
                        )
        );


        return getUserInfoVOPage(userInfoList);
    }

    /**
     * 获取指定用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Page<UserInfoVO> getUserInfo(String findInfo, String roleName, Integer pageNum, Integer pageSize) {

        // 1. 构建用户信息查询
        MPJLambdaWrapper<User> wrapper = new MPJLambdaWrapper<User>()
                .select(User::getUserId, User::getEmail, User::getStatus)
                .select(UserInfo::getUserName, UserInfo::getAvatar,
                        UserInfo::getGender, UserInfo::getCreateTime)
                .leftJoin(UserInfo.class, UserInfo::getUserId, User::getUserId);

        // 2. Filter by role in SQL instead of loading all matching user IDs
        // into application memory.
        if (roleName != null && !roleName.trim().isEmpty()) {

            Role role = roleMapper.selectOne(
                    new LambdaQueryWrapper<Role>()
                            .eq(Role::getRoleName, roleName.trim())
            );

            if (role == null) {
                // 角色不存在，直接返回空分页
                return new Page<>(pageNum, pageSize);
            }

            wrapper.innerJoin(
                            UserRole.class,
                            UserRole::getUserId,
                            User::getUserId
                    )
                    .eq(UserRole::getRoleId, role.getRoleId());
        }

        // 3. 用户信息搜索
        if (findInfo != null && !findInfo.trim().isEmpty()) {
            String keyword = findInfo.trim();

            wrapper.and(w -> {
                if (keyword.matches("\\d+")) {
                    w.eq(User::getUserId, Long.valueOf(keyword)).or();
                }

                w.like(UserInfo::getUserName, keyword)
                        .or()
                        .like(User::getEmail, keyword);
            });
        }

        // 4. 分页查询
        Page<UserInfoVO> userInfoList = userMapper.selectJoinPage(
                new Page<>(pageNum, pageSize),
                UserInfoVO.class,
                wrapper
        );

        return getUserInfoVOPage(userInfoList);
    }

    @NonNull
    private Page<UserInfoVO> getUserInfoVOPage(Page<UserInfoVO> userInfoList) {
        List<UserInfoVO> records = userInfoList.getRecords();
        if (records.isEmpty()) {
            return userInfoList;
        }

        List<Long> userIds = records.stream()
                .map(UserInfoVO::getUserId)
                .toList();
        Map<Long, List<Long>> roleIdsByUser = new HashMap<>();
        List<UserRole> userRoles = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                        .in(UserRole::getUserId, userIds)
        );
        userRoles.forEach(userRole ->
                roleIdsByUser.computeIfAbsent(userRole.getUserId(), ignored -> new ArrayList<>())
                        .add(userRole.getRoleId())
        );

        Set<Long> roleIds = userRoles.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());
        Map<Long, String> roleNames = roleIds.isEmpty()
                ? Map.of()
                : roleMapper.selectByIds(roleIds).stream()
                        .collect(Collectors.toMap(Role::getRoleId, Role::getRoleName));

        records.forEach(userInfoVO -> {
            if (userInfoVO.getStatus() != null) {
                userInfoVO.setStatus(
                        "1".equals(userInfoVO.getStatus())
                                ? "禁用用户"
                                : "正常用户"
                );
            } else {
                userInfoVO.setStatus("正常用户");
            }

            userInfoVO.setGender(toGenderDescription(userInfoVO.getGender()));
            userInfoVO.setRole(roleIdsByUser
                    .getOrDefault(userInfoVO.getUserId(), List.of())
                    .stream()
                    .map(roleNames::get)
                    .filter(Objects::nonNull)
                    .toList());
        });

        return userInfoList;
    }

    private String toGenderDescription(String gender) {
        if (gender == null || gender.isBlank()) {
            return GenderEnum.SECRET.getDescription();
        }
        return EnumUtils.getDescription(GenderEnum.class, Integer.parseInt(gender));
    }

    /**
     * 添加用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addUser(UserRegisterDTO dto) {
        roleService.validateAssignableRoles(dto.getRole());
        authService.register(dto);
    }

    /**
     * 修改用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {

        User user = userMapper.selectById(dto.getUserId());

        UserInfo userInfo = userInfoMapper.selectOne(
                new LambdaQueryWrapper<UserInfo>()
                        .eq(UserInfo::getUserId, dto.getUserId())
        );

        if (user == null || userInfo == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        Integer adminRoleLevel = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>()
                        .eq(Role::getRoleId, StpUtil.getTokenSession().get("activeRoleId"))
        ).getRoleLevel();

        Integer userRoleLevels = roleMapper.selectByIds(userRoleMapper.selectList(
                        new LambdaQueryWrapper<UserRole>()
                                .eq(UserRole::getUserId, dto.getUserId())
                ).stream()
                .map(UserRole::getRoleId)
                .toList()).stream().map(Role::getRoleLevel).min(Integer::compareTo).orElse(0);

        if(adminRoleLevel >= userRoleLevels){
            throw new IllegalArgumentException("您没有权限修改该用户");
        }

        List<Role> roles = roleService.validateAssignableRoles(dto.getRole());

        if(dto.getEmail() != null) {
            if(userMapper.selectOne(
                    new LambdaQueryWrapper<User>()
                            .ne(User::getUserId, dto.getUserId())
                            .eq(User::getEmail, dto.getEmail())
            ) != null) {
                throw new IllegalArgumentException("邮箱已存在");
            }
            user.setEmail(dto.getEmail());
        }

        if(dto.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        user.setStatus(
            UserStatusEnum.getByDescription(dto.getStatus()).getCode()
        );

        userMapper.updateById(user);


        userInfo.setGender(dto.getGender());

        if(dto.getUserName() != null) {
            userInfo.setUserName(dto.getUserName());
        }

        userInfoMapper.updateById(userInfo);

        // 1. 查询用户当前角色
        List<UserRole> userRoleList = userRoleMapper.selectList(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, dto.getUserId())
        );

        // 2. 获取当前角色ID集合
        Set<Long> oldRoleIds = userRoleList.stream()
                .map(UserRole::getRoleId)
                .collect(Collectors.toSet());

        // 3. 根据前端提交的角色名称查询角色
        // 4. 获取前端提交的角色 ID 集合
        Set<Long> newRoleIds = roles.stream()
                .map(Role::getRoleId)
                .collect(Collectors.toSet());

        // 5. 计算需要删除的角色 ID（旧的有，新的没有）
        Set<Long> deleteRoleIds = new HashSet<>(oldRoleIds);
        deleteRoleIds.removeAll(newRoleIds);

        // 6. 计算需要新增的角色 ID（新的有，旧的没有）
        Set<Long> insertRoleIds = new HashSet<>(newRoleIds);
        insertRoleIds.removeAll(oldRoleIds);

        // 7. 删除不再需要的角色
        if (!deleteRoleIds.isEmpty()) {
            userRoleMapper.delete(
                    new LambdaQueryWrapper<UserRole>()
                            .eq(UserRole::getUserId, dto.getUserId())
                            .in(UserRole::getRoleId, deleteRoleIds)
            );
        }

        // 8. 新增用户角色关系
        if (!insertRoleIds.isEmpty()) {
            insertRoleIds.forEach(roleId -> {
                UserRole userRole = new UserRole();
                userRole.setUserId(dto.getUserId());
                userRole.setRoleId(roleId);

                userRoleMapper.insert(userRole);
            });
        }
        }

    /**
     * 删除用户
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long userId, String password) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        Long adminId = StpUtil.getLoginIdAsLong();

        if (Objects.equals(user.getUserId(), adminId)) {
            throw new IllegalArgumentException("不能删除当前登录用户");
        }
        if (!passwordEncoder.matches(password, userMapper.selectById(adminId).getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        userMapper.deleteById(userId);
        userRoleMapper.delete(
                new LambdaQueryWrapper<UserRole>()
                        .eq(UserRole::getUserId, userId)
        );
    }
}
