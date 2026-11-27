# **通用DBAC管理系统**

**- 使用技术栈：**

  前端： Vue3、  Typescript
  
  后端： Spring Boot4、 My sql

**- 注意事项：**
  需要使用nginx代理


**- 需要建立的数据库：**

· 需要Redis数据库对token进行保存。

· mysql数据库具体如下：
  1. user主表
~~~mysql
create table user
(
    user_id  bigint auto_increment comment '用户ID'
        primary key,
    email    varchar(255) charset utf8mb4 not null comment '登录账号',
    password varchar(255) charset utf8mb4 not null comment '登录密码',
    status   tinyint default 0            not null comment '账号状态：0：正常 1：禁用',
    constraint uk_email
        unique (email)
)
    comment '用户主表';
~~~

  2.user_info表
~~~mysql
create table user_info
(
    info_id     bigint auto_increment comment '详情ID'
        primary key,
    user_id     bigint                             not null comment '关联用户ID',
    user_name   varchar(50) charset utf8mb4        null comment '用户昵称',
    avatar      varchar(500) charset utf8mb4       null comment '头像地址',
    gender      tinyint  default 0                 null comment '性别：0保密，1男，2女',
    create_time datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_user_id
        unique (user_id),
    constraint fk_user_info_user
        foreign key (user_id) references user (user_id)
            on update cascade on delete cascade
)
    comment '用户详情表';
~~~

  3.role表
~~~mysql
create table role
(
    role_name  varchar(50) not null comment '权限类型',
    role_id    bigint auto_increment comment '权限ID'
        primary key,
    role_level tinyint     not null comment '职责等级',
    constraint uk_role_type
        unique (role_name)
)
    comment '用户权限表';
~~~

  4.user_role关联表

  5.permission表
~~~mysql
create table permission
(
    permission_id   bigint auto_increment comment '权限主键'
        primary key,
    permission_name varchar(100)                          not null comment '权限名称，如删除用户',
    permission_code varchar(100)                          not null comment '权限标识，如 user:delete',
    permission_type varchar(20) default 'API'             not null comment '权限类型',
    description     varchar(255)                          null comment '权限说明',
    create_time     datetime    default CURRENT_TIMESTAMP null,
    constraint permission_code
        unique (permission_code)
)
    comment '权能表';
~~~
  6.role_permission关联表

  7.menu表
~~~mysql
  create table menu
(
    menu_id         bigint auto_increment comment '菜单ID'
        primary key,
    menu_name       varchar(50)                        not null comment '菜单名称',
    route_path      varchar(255)                       null comment '路由路径',
    route_name      varchar(100)                       null comment '路由名称',
    component_path  varchar(255)                       null comment '前端组件路径',
    permission_code varchar(100)                       null comment '页面访问权限码',
    icon            varchar(100)                       null comment '菜单图标',
    keep_alive      tinyint  default 0                 not null comment '是否缓存页面 0否 1是',
    create_time     datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time     datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    constraint uk_route_name
        unique (route_name)
)
    comment '系统菜单表' charset = utf8mb4;

create index idx_permission_code
    on menu (permission_code);
~~~

### 初始化与部署说明

- 用户列表接口要求 `user.list` 权限。首次部署后，在权限管理中创建该权限并授予需要查看用户列表的角色；角色分配接口会限制只能授予低于当前登录角色等级的角色。
- 生产构建前运行 `pnpm --dir vite-project build`，并将 `vite-project/dist` 内容部署到 Nginx 配置中的 `/usr/share/nginx/html`。生产环境通过 `vite-project/.env.production` 将 API 指向 `/api`；`nginx.conf` 将前端作为静态资源提供，开发时仍可直接使用 Vite。

