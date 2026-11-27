package com.example.springboot.system.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 资源配置类
 * 1. 配置静态资源访问路径
 * 2. 配置跨域访问规则
 */

@Configuration // 标注为配置类, 会自动扫描并加载配置类
public class ResourcesConfig implements WebMvcConfigurer {

    private final ProjectConfig projectConfig;

    public ResourcesConfig(ProjectConfig projectConfig) {
        this.projectConfig = projectConfig;
    }

    /**
     * 配置静态资源处理器
     * 负责告诉Spring Boot，当用户请求路径时，应该去哪个文件查找文件
     * @param registry 资源处理注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 本地文件上传路径配置
        // 当请求/profile/**路径时, 会去projectConfig.getProfile()路径下查找文件
        registry.addResourceHandler("/profile/**")
                // file: 表示从本地文件系统读取文件
                .addResourceLocations("file:" + projectConfig.getProfile() + "/");
    }

    /**
     * 配置跨域访问规则，允许本地前端开发服务器调用后端接口。
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                // 兼容 Vite 默认端口以及端口被占用后自动切换的情况
                .allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(1800L);
    }

}
