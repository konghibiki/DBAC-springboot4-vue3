package com.example.springboot.system.util;

import com.example.springboot.system.common.enums.BaseEnum;

import java.util.Arrays;

public class EnumUtils {

    /**
     * 根据数字获取枚举描述
     */
    public static <E extends Enum<E> & BaseEnum>
    String getDescription(Class<E> enumClass, Integer code) {

        if (code == null) {
            return "未知";
        }

        return Arrays.stream(enumClass.getEnumConstants())
                .filter(item -> item.getCode().equals(code))
                .map(BaseEnum::getDescription)
                .findFirst()
                .orElse("未知");
    }
}