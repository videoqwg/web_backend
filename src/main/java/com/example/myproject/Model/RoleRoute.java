package com.example.myproject.Model;

import java.util.List;

@lombok.Data
public class RoleRoute {
    private String name;
    private String key;
    private String description;
    private List<RouteDTO> routes;
}
