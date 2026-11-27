package com.example.springboot.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import com.example.springboot.system.dto.MenuDTO;
import com.example.springboot.system.entity.Menu;
import com.example.springboot.system.entity.User;
import com.example.springboot.system.mapper.MenuMapper;
import com.example.springboot.system.mapper.UserMapper;
import com.example.springboot.system.service.IBaseService;
import com.example.springboot.system.service.MenuService;
import com.example.springboot.system.service.PermissionService;
import com.example.springboot.system.util.PermissionUtils;
import com.example.springboot.system.vo.MenuVO;
import com.github.yulichang.wrapper.MPJLambdaWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 菜单服务实现类
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl
        extends CrudRepository<MenuMapper, Menu>
        implements IBaseService<Menu>, MenuService {

    private final MenuMapper menuMapper;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final PermissionService permissionService;

    /**
     * 获取用户自己的菜单列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<String> listSelf() {

        // 1. 获取当前用户/角色拥有的权限
        Set<String> userPermissionCodes = permissionService.getPermissionListByUserId()
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 2. 获取数据库中所有真实的 list 权限码
        List<String> allListPermissionCodes = permissionService.getAllPermissionListOfList();

        // 3. 通配符匹配，得到真实拥有的 list 权限
        List<String> matchedPermissionCodes = allListPermissionCodes.stream()
                .filter(listCode -> userPermissionCodes.stream()
                        .anyMatch(userCode -> PermissionUtils.matches(userCode, listCode)))
                .toList();

        // 4. 通过具体权限码查询对应 Menu，并返回 RouteName
        if (matchedPermissionCodes.isEmpty()) {
            return List.of();
        }

        return menuMapper.selectList(
                        new LambdaQueryWrapper<Menu>()
                                .in(Menu::getPermissionCode, matchedPermissionCodes)
                ).stream()
                .map(Menu::getRouteName)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /**
     * 获取菜单列表
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Page<MenuVO> list(Integer pageNum, Integer pageSize) {

        return menuMapper.selectJoinPage(
                new Page<>(pageNum, pageSize),
                MenuVO.class,
                new MPJLambdaWrapper<Menu>()
                        .orderByAsc(Menu::getMenuId)
        );
    }

    /**
     * 添加菜单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(MenuDTO menuDTO) {

        Menu menu = menuInfoSet(new Menu(), menuDTO);

        menuMapper.insert(menu);
    }

    /**
     * 修改菜单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(MenuDTO menuDTO) {

        Menu menu = menuMapper.selectById(menuDTO.getMenuId());
        if(menu == null) {
            throw new IllegalArgumentException("菜单不存在");
        }

        menuMapper.updateById(menuInfoSet(menu, menuDTO));
    }

    private Menu menuInfoSet(Menu menu, MenuDTO menuDTO) {
        if( menuMapper.selectOne(
                    new MPJLambdaWrapper<Menu>()
                            .ne(Menu::getMenuId, menuDTO.getMenuId())
                            .eq(Menu::getMenuName, menuDTO.getMenuName())
            ) != null
        ) {
            throw new IllegalArgumentException("菜单名称已存在");
        }

        if( menuMapper.selectOne(
                new MPJLambdaWrapper<Menu>()
                        .ne(Menu::getMenuId, menuDTO.getMenuId())
                        .eq(Menu::getRoutePath, menuDTO.getRoutePath())
        ) != null) {
            throw new IllegalArgumentException("路由路径已存在");
        }

        if( menuMapper.selectOne(
                new MPJLambdaWrapper<Menu>()
                        .ne(Menu::getMenuId, menuDTO.getMenuId())
                        .eq(Menu::getRouteName, menuDTO.getRouteName())
        ) != null) {
            throw new IllegalArgumentException("路由名字已存在");
        }

        if( menuMapper.selectOne(
                new MPJLambdaWrapper<Menu>()
                        .ne(Menu::getMenuId, menuDTO.getMenuId())
                        .eq(Menu::getComponentPath, menuDTO.getComponentPath())
        ) != null) {
            throw new IllegalArgumentException("组件路径已存在");
        }

        menu.setMenuName(menuDTO.getMenuName());
        menu.setRoutePath(menuDTO.getRoutePath());
        menu.setRouteName(menuDTO.getRouteName());
        menu.setComponentPath(menuDTO.getComponentPath());
        menu.setPermissionCode(menuDTO.getPermissionCode());
        menu.setKeepAlive(menuDTO.getKeepAlive());
        if(menuDTO.getIcon() != null) {
            menu.setIcon(menuDTO.getIcon());
        }
        return menu;
    }

    /**
     * 删除菜单
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long menuId, String password) {

        Menu menu = menuMapper.selectById(menuId);
        if(menu == null) {
            throw new IllegalArgumentException("菜单不存在");
        }

        Long adminId = StpUtil.getLoginIdAsLong();

        User user = userMapper.selectById(adminId);
        if(user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if(!passwordEncoder.matches(password, user.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }

        menuMapper.deleteById(menuId);
    }
}
