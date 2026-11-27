package com.example.springboot.system.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 通用工具类
 * Spring Boot 4 + Spring Data Redis + Jackson 3
 */
@Component
@RequiredArgsConstructor
public class RedisUtil {
    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    // ==================== 普通键值操作 ====================

    //set()	保存字符串
    //get()	获取字符串
    //setObject()	保存 Java 对象
    //getObject()	获取 Java 对象
    //hasKey()	判断 Key 是否存在
    //delete()	删除 Key
    //expire()	设置过期时间
    //getExpire()	获取剩余过期时间
    //increment()	自增
    //decrement()	自减
    //hSet()	保存 Hash
    //hGet()	获取 Hash
    //lPush()	添加 List 元素
    //lPop()	弹出 List 元素

    /**
     * 保存字符串
     */
    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 保存字符串并设置过期时间
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(
                key, value, Duration.ofMillis(unit.toMillis(timeout))
        );
    }

    /**
     * 保存 Java 对象（JSON 格式）
     */
    public void setObject(String key, Object value) {
        redisTemplate.opsForValue().set(key, toJson(value));
    }

    /**
     * 保存 Java 对象并设置过期时间
     */
    public void setObject(
            String key, Object value, long timeout, TimeUnit unit) {

        redisTemplate.opsForValue().set(
                key,
                toJson(value),
                Duration.ofMillis(unit.toMillis(timeout))
        );
    }

    /**
     * 获取字符串
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取 Java 对象
     */
    public <T> T getObject(String key, Class<T> clazz) {
        String value = get(key);

        if (value == null) {
            return null;
        }

        return fromJson(value, clazz);
    }

    // ==================== Key 操作 ====================

    /**
     * 判断 Key 是否存在
     */
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 删除 Key
     */
    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    /**
     * 批量删除 Key
     */
    public long delete(Set<String> keys) {
        Long count = redisTemplate.delete(keys);
        return count == null ? 0 : count;
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        return Boolean.TRUE.equals(
                redisTemplate.expire(
                        key,
                        Duration.ofMillis(unit.toMillis(timeout))
                )
        );
    }

    /**
     * 获取剩余过期时间
     */
    public long getExpire(String key, TimeUnit unit) {
        Long expire = redisTemplate.getExpire(key, unit);
        return expire == null ? -2 : expire;
    }

    /**
     * 移除过期时间
     */
    public boolean persist(String key) {
        return Boolean.TRUE.equals(redisTemplate.persist(key));
    }

    // ==================== 数字操作 ====================

    /**
     * 自增
     */
    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    /**
     * 指定步长自增
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 自减
     */
    public Long decrement(String key) {
        return redisTemplate.opsForValue().decrement(key);
    }

    /**
     * 指定步长自减
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    // ==================== Hash 操作 ====================

    /**
     * 保存 Hash 字段
     */
    public void hSet(String key, String field, Object value) {
        redisTemplate.opsForHash().put(key, field, toJson(value));
    }

    /**
     * 获取 Hash 字段
     */
    public <T> T hGet(String key, String field, Class<T> clazz) {
        Object value = redisTemplate.opsForHash().get(key, field);

        if (value == null) {
            return null;
        }

        return fromJson(value.toString(), clazz);
    }

    /**
     * 获取整个 Hash
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 删除 Hash 字段
     */
    public long hDelete(String key, String... fields) {
        return redisTemplate.opsForHash().delete(key, (Object[]) fields);
    }

    /**
     * 判断 Hash 字段是否存在
     */
    public boolean hHasKey(String key, String field) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForHash().hasKey(key, field)
        );
    }

    // ==================== List 操作 ====================

    /**
     * 从右侧添加元素
     */
    public Long lPush(String key, Object value) {
        return redisTemplate.opsForList().rightPush(key, toJson(value));
    }

    /**
     * 从左侧添加元素
     */
    public Long lLeftPush(String key, Object value) {
        return redisTemplate.opsForList().leftPush(key, toJson(value));
    }

    /**
     * 从左侧弹出元素
     */
    public <T> T lPop(String key, Class<T> clazz) {
        String value = redisTemplate.opsForList().leftPop(key);

        return value == null ? null : fromJson(value, clazz);
    }

    /**
     * 获取 List 指定范围的数据
     */
    public <T> List<T> lRange(
            String key, long start, long end, Class<T> clazz) {

        List<String> values = redisTemplate.opsForList()
                .range(key, start, end);

        if (values == null) {
            return List.of();
        }

        return values.stream()
                .map(value -> fromJson(value, clazz))
                .toList();
    }

    /**
     * 获取 List 长度
     */
    public Long lSize(String key) {
        return redisTemplate.opsForList().size(key);
    }

    // ==================== JSON 转换 ====================

    /**
     * Java 对象转 JSON
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Redis JSON 序列化失败", e);
        }
    }

    /**
     * JSON 转 Java 对象
     */
    private <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Redis JSON 反序列化失败", e);
        }
    }
}
