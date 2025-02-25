package com.example.myproject.Service;

import com.example.myproject.Model.Result;
import com.example.myproject.Model.RoleRoute;
import com.example.myproject.Model.RouteDTO;

import java.util.List;

public interface RouteService {
    List<RouteDTO> getAllRouteTree();
    List<RouteDTO> getRoutesByRoles(List<String> roles);
    Result getRoles();
    void updateRoutes(RoleRoute returnedRoutes, String currentRole);
}
