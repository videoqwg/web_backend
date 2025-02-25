package com.example.myproject.Model;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonInclude;

@lombok.Data
public class RouteDTO {
    private Long id;
    private Long parentId;
    private String path;
    private String name;
    private String component;
    private String redirect;
    private Boolean alwaysShow;
    private Boolean hidden;
    private RouteMeta meta;
    // 必须添加一下注解，否则当 children 为空时，仍会输出一个空数组，前端会报错
    @JsonInclude(JsonInclude.Include.NON_EMPTY) // 仅当 List 不为空时才会输出
    private List<RouteDTO> children;
}

