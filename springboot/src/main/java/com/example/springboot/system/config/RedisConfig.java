package com.example.springboot.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

/**
 * Redis配置类
 * 配置RedisTemplate对象， 用于操作Redis数据库
 * 将Redis转换为JSON格式存储， 解决对象类型转换问题
 */
@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory factory) {

        // 1. 实例化RedisTemplate对象
        RedisTemplate<String, Object> template =
                new RedisTemplate<>();

        // 2. 注入自动装配的Redis连接工厂 （内置连接池, redis地址配置）
        template.setConnectionFactory(factory);

        // 3. 配置key、hashkey序列化器: 字符串序列化，Redis客户端查看明文key
        StringRedisSerializer stringRedisSerializer =
                new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // 4. 配置value、hashValue序列化器: Builder模式开启类型存储（解决强转异常）
        // GenericJacksonJsonRedisSerializer 是一个通用的JSON序列化器， 支持自动检测对象类型并进行序列化
        GenericJacksonJsonRedisSerializer jsonRedisSerializer =
                GenericJacksonJsonRedisSerializer.builder()
                        .enableDefaultTyping(
                                BasicPolymorphicTypeValidator.builder()
                                        .allowIfBaseType(Object.class)
                                        .allowIfSubType((ctx, clazz) -> true)
                                        .build()
                        )
                        .build();
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        // 5. 初始化序列化配置， 必须调用， 否则序列化规则不生效
        template.afterPropertiesSet();
        return template;
    }
}
