package com.example.springboot.system.vo;


import lombok.Data;

@Data
public class MenuVO {

    private Long menuId;

    private String menuName;

    private String routePath;

    private String routeName;

    private String componentPath;

    private String icon;

    private String permissionCode;

    private Integer keepAlive;
}
