package com.example.myproject.Controller;

import com.example.myproject.Model.Result;
import com.example.myproject.Model.RoleRoute;
import com.example.myproject.Model.User;
import com.example.myproject.Service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;


@RestController
@RequestMapping("/api/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    @GetMapping("/getAllRoutes")
    public Result getAllRoutes() {
        return Result.success(routeService.getAllRouteTree());
    }

    @GetMapping("/routesByRoles")
    public Result getRoutesByRoles(@RequestAttribute("user") User user) {
        // 假设我们能从 principal 或 token 中获取用户角色
        // 这里只是硬编码模拟
        List<String> userRoles = Arrays.asList(user.getRole());
        return Result.success(routeService.getRoutesByRoles(userRoles));
    }
    @GetMapping("/getAllRoles")
    public Result getAllRoles() {
        return routeService.getRoles();
    }

    @PutMapping("/role/{id}")
    public Result updateRole(@PathVariable("id") String currentRole, @RequestBody RoleRoute returnedRoutes) {
        try {
            routeService.updateRoutes(returnedRoutes, currentRole);
            return Result.success();
        } catch (Exception e) {
            return Result.failure("无法修改路由");
        }
    }
}


