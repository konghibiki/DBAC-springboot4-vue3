package com.example.springboot.system.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import cn.dev33.satoken.filter.SaServletFilter;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import com.example.springboot.system.common.Result;
import com.example.springboot.system.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Objects;

/**
 * [Sa-Token 权限认证] 配置类 
 */
@Slf4j
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {

    private final AuthService authService;

    public SaTokenConfigure(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 注册 [Sa-Token全局过滤器]
     */
    @Bean
    public SaServletFilter getSaServletFilter() {
        return new SaServletFilter()

                // 指定 拦截路由 与 放行路由
                .addInclude("/**").addExclude(
                        "/favicon.ico",
                        "/auth/register"
                )   /* 排除掉 /favicon.ico */

                // 认证函数: 每次请求执行
                .setAuth(obj -> {
                    // 获取当前请求路径
                    String path = SaHolder.getRequest().getRequestPath();

                    // 1. 登录接口放行
                    if ("/auth/login".equals(path) || "/auth/isLogin".equals(path)) {
                        return;
                    }

                    // 2.其他接口必须登录
                    StpUtil.checkLogin();

                    // 3. 只允许访问角色选择相关接口
                    if ((Objects.equals(StpUtil.getTokenSession().get("activeRoleId"), "")
                            && ("/auth/loginWithRole".equals(path) ||
                            "/role/self".equals(path))) ||
                            "/auth/logout".equals(path)) {
                            return;
                    }

                    // 4. 剩余已选择角色才能访问
                    if (!authService.checkRole()) {
                        StpUtil.logout();
                        StpUtil.checkLogin();
                    }

                    // 4. 已登录且已选择角色，继续正常请求
                })

                // 异常处理函数：每次认证函数发生异常时执行此函数
                .setError(e -> {
                    HttpStatus status;
                    String message;

                    if (e instanceof NotLoginException) {

                        status = HttpStatus.UNAUTHORIZED;
                        message = "未登录或登录状态已失效";

                    } else if (
                            e instanceof NotPermissionException
                                    || e instanceof NotRoleException
                    ) {

                        status = HttpStatus.FORBIDDEN;
                        message = "没有权限访问该资源";

                    } else {

                        status = HttpStatus.INTERNAL_SERVER_ERROR;
                        message = "服务器内部错误";
                        log.error("Sa-Token 过滤器异常：", e);
                    }

                    SaHolder.getResponse().setStatus(status.value());

                    return Result.error(
                            status.value(),
                            message
                    );
                })

                // 前置函数：在每次认证函数之前执行（BeforeAuth 不受 includeList 与 excludeList 的限制，所有请求都会进入）
                .setBeforeAuth(r -> {
                    // ---------- 设置一些安全响应头 ----------
                    SaHolder.getResponse()
                            // 服务器名称
                            .setServer("sa-server")
                            // 是否可以在iframe显示视图： DENY=不可以 | SAMEORIGIN=同域下可以 | ALLOW-FROM uri=指定域名下可以
                            .setHeader("X-Frame-Options", "SAMEORIGIN")
                            // 是否启用浏览器默认XSS防护： 0=禁用 | 1=启用 | 1; mode=block 启用, 并在检查到XSS攻击时，停止渲染页面
                            .setHeader("X-XSS-Protection", "1; mode=block")
                            // 禁用浏览器内容嗅探
                            .setHeader("X-Content-Type-Options", "nosniff")
                    ;
                })
                ;
    }

    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }

}