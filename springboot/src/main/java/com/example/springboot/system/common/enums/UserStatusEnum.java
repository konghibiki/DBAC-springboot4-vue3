package com.example.springboot.system.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserStatusEnum implements BaseEnum {

    /**
     * 正常用户
     */
    NORMAL(0, "正常用户"),
    /**
     * 禁用用户
     */
    BANUSER(1, "禁用用户");


    private final Integer code;
    private final String description;

    /**
     * 根据描述获取枚举
     */
    public static UserStatusEnum getByDescription(String description) {
        for (UserStatusEnum status : values()) {
            if (status.getDescription().equals(description)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的用户状态：" + description);
    }
}