package com.example.springboot.system.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration // 告诉Spring Boot这是一个配置类, 会自动扫描并加载配置类
@MapperScan("com.example.springboot.system.mapper") // 扫描Mapper接口, 并自动创建实现类, 告诉MybatisPlus扫描的包路径
public class MybatisPlusConfig {

    /**
     * 添加分页插件
     */
    @Bean // 告诉Spring Boot这是一个Bean, 会自动扫描并加载Bean, 返回的对象会自动放入到Spring容器中
    public MybatisPlusInterceptor mybatisPlusInterceptor() {

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL)); // 如果配置多个插件, 切记分页最后添加
        // 如果有多数据源可以不配具体类型, 否则都建议配上具体的 DbType
        return interceptor;
    }
}
