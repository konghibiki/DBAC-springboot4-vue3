package com.example.springboot.system.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GenderEnum implements BaseEnum {

    SECRET(0, "保密"),
    MALE(1, "男"),
    FEMALE(2, "女");

    private final Integer code;
    private final String description;
}