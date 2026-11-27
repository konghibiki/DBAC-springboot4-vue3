package com.example.springboot.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoleVO {

    private Long roleId;

    private String roleName;

    private Integer roleLevel;
}
