package com.example.springboot.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 项目配置类
 * 用于从application.yaml文件中读取项目配置,并将其注入到项目中
 */

@Data
@Component // 标注为组件, 会自动扫描并加载
@ConfigurationProperties(prefix = "project") // 从application.yaml文件中读取project配置
public class ProjectConfig {

    /**
     * 项目配置文件路径
     * 用于指定项目配置文件的路径, 例如：./file
     * 可以使用 ProjectConfig.profile 来直接访问
     */
    private String profile;
}
