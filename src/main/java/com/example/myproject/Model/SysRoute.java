package com.example.myproject.Model;

@lombok.Data
public class SysRoute {
    private Long id;
    private Long parent_id;
    private String path;
    private String name;
    private String component;
    private String redirect;
    private Integer alwaysShow; // 0 or 1
    private Integer hidden;     // 0 or 1
    private String meta_title;
    private String meta_icon;
    private String meta_roles;   // 存储 ["admin","editor"] 的 JSON字符串或其他形式

    // 省略 getter/setter
}

