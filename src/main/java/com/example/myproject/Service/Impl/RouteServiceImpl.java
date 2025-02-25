package com.example.myproject.Service.Impl;

import com.alibaba.fastjson.JSON; // 仅示例，如果你用 Gson/Jackson，自行替换
import com.example.myproject.Model.*;
import com.example.myproject.Repository.RouteMapper;
import com.example.myproject.Service.RouteService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RouteServiceImpl implements RouteService {

    @Autowired
    private RouteMapper routeMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 不区分角色，直接返回全部树形路由
     * 如果需要按角色过滤，可以自己改成 getRoutesByUser(...) 或 getRoutesByRoles(...)
     */
    public List<RouteDTO> getAllRouteTree() {
        // 1) 查询所有路由
        List<SysRoute> list = routeMapper.findAll();
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) 转成 DTO
        List<RouteDTO> dtoList = convertToDTO(list);

        // 3) 组装成树
        return buildTree(dtoList);
    }


    /**
     * 如果想根据角色过滤，可以写个方法：
     * 1. 先根据角色查询到能访问的路由列表
     * 2. 再组装成树
     * 这里仅做示例
     */
    public List<RouteDTO> getRoutesByRoles(List<String> roles) {
        // 1) 查询所有路由
        List<SysRoute> list = routeMapper.findAll();

        // 2) 转DTO
        List<RouteDTO> dtoList = convertToDTO(list);

        // 3) 在后端过滤 meta.roles
        List<RouteDTO> filtered = filterByRoles(dtoList, roles);

        // 4) 组装成树
        return buildTree(filtered);
    }

    public Result getRoles() {
        List<RoleRoute> roleRoutes = new ArrayList<>();
        RoleRoute admin = new RoleRoute();
        admin.setKey("admin");
        admin.setName("admin");
        admin.setDescription("Super Administrator. Have access to view all pages.");
        admin.setRoutes(getRoutesByRoles(List.of("admin")));
        RoleRoute editor = new RoleRoute();
        editor.setKey("editor");
        editor.setName("editor");
        editor.setDescription("Normal Editor. Can see all pages except permission page");
        editor.setRoutes(getRoutesByRoles(List.of("editor")));
        roleRoutes.add(admin);
        roleRoutes.add(editor);
        return Result.success(roleRoutes);
    }

    /**
     * 将数据库实体转换成前端需要的 RouteDTO
     */
    private List<RouteDTO> convertToDTO(List<SysRoute> sysRoutes) {
        List<RouteDTO> dtoList = new ArrayList<>();
        for (SysRoute sr : sysRoutes) {
            RouteDTO dto = new RouteDTO();
            dto.setId(sr.getId());
            dto.setParentId(sr.getParent_id());
            dto.setPath(sr.getPath());
            dto.setName(sr.getName());
            dto.setComponent(sr.getComponent());
            dto.setRedirect(sr.getRedirect());
            dto.setAlwaysShow(sr.getAlwaysShow() != null && sr.getAlwaysShow() == 1);
            dto.setHidden(sr.getHidden() != null && sr.getHidden() == 1);

            // meta 处理
            RouteMeta meta = new RouteMeta();
            meta.setTitle(sr.getMeta_title());
            meta.setIcon(sr.getMeta_icon());

            // 如果 metaRoles 字段里存的是 JSON 字符串 (如 '["admin","editor"]')
            if (sr.getMeta_roles() != null && !sr.getMeta_roles().isEmpty()) {
                try {
                    List<String> roles = JSON.parseArray(sr.getMeta_roles(), String.class);
                    meta.setRoles(roles);
                } catch (Exception e) {
                    // 如果解析失败，可做个安全降级
                    meta.setRoles(Collections.emptyList());
                }
            }
            dto.setMeta(meta);

            dtoList.add(dto);
        }
        return dtoList;
    }

    /**
     * 构建树形结构
     */
    private List<RouteDTO> buildTree(List<RouteDTO> dtoList) {
        // 先找出所有顶级路由( parentId == 0 )
        List<RouteDTO> root = dtoList.stream()
                .filter(r -> r.getParentId() == 0)
                .collect(Collectors.toList());

        // 给每个 root 找子节点
        for (RouteDTO parent : root) {
            parent.setChildren(getChildren(parent, dtoList));
        }

        return root;
    }

    private List<RouteDTO> getChildren(RouteDTO parent, List<RouteDTO> all) {
        List<RouteDTO> children = all.stream()
                .filter(r -> Objects.equals(r.getParentId(), parent.getId()))
                .collect(Collectors.toList());
        for (RouteDTO child : children) {
            child.setChildren(getChildren(child, all));
        }
        return children;
    }

    /**
     * 根据用户角色，在后端过滤
     */
    private List<RouteDTO> filterByRoles(List<RouteDTO> routes, List<String> userRoles) {
        // 递归过滤: 只有 meta.roles 为空 或 路由 roles 与 userRoles 有交集，才保留
        List<RouteDTO> filtered = new ArrayList<>();
        for (RouteDTO route : routes) {
            // 判断权限
            boolean keep = false;
            List<String> rolesInRoute = route.getMeta().getRoles();
            if (rolesInRoute != null && !rolesInRoute.isEmpty()) {
                // 如果有设置 roles，则看是否跟 userRoles 有交集
                for (String r : rolesInRoute) {
                    if (userRoles.contains(r)) {
                        keep = true;
                        break;
                    }
                }
            }

            if (keep) {
                // 继续递归过滤子路由
                List<RouteDTO> child = route.getChildren();
                if (child != null && !child.isEmpty()) {
                    route.setChildren(filterByRoles(child, userRoles));
                }
                filtered.add(route);
            }
        }
        return filtered;
    }

    @Transactional
    public void updateRoutes(RoleRoute returnedRoutes, String currentRole) {
        // 提取前端返回的所有路由 ID
        List<Long> returnedRouteIds = extractIdsFromReturnedRoutes(returnedRoutes.getRoutes());

        // 查询数据库中所有路由
        List<SysRoute> allRoutes = routeMapper.findAll();

        // 获取数据库中现有的路由 ID
        Set<Long> existingRouteIds = allRoutes.stream()
                .map(SysRoute::getId)
                .collect(Collectors.toSet());

        // 处理新增路由（前端返回但数据库中未包含当前角色）
        List<SysRoute> routesToAddRole = allRoutes.stream()
                .filter(route -> returnedRouteIds.contains(route.getId())) // 前端返回的路由
                .filter(route -> !hasRole(route, currentRole)) // 该路由的 meta_roles 不包含当前角色
                .collect(Collectors.toList());

        // 处理未返回的路由（需要移除当前角色）
        List<SysRoute> routesToRemoveRole = allRoutes.stream()
                .filter(route -> !returnedRouteIds.contains(route.getId())) // 数据库中未返回的路由
                .filter(route -> hasRole(route, currentRole)) // 该路由的 meta_roles 包含当前角色
                .collect(Collectors.toList());

        // 更新 meta_roles 字段
        addRoleToRoutes(routesToAddRole, currentRole);
        removeRoleFromRoutes(routesToRemoveRole, currentRole);
    }

    /**
     * 判断路由是否包含某角色
     */
    private boolean hasRole(SysRoute route, String role) {
        String metaRoles = route.getMeta_roles();
        if (metaRoles != null && !metaRoles.isEmpty()) {
            try {
                List<String> roles = objectMapper.readValue(metaRoles, new TypeReference<List<String>>() {});
                return roles.contains(role);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 为路由添加角色
     */
    private void addRoleToRoutes(List<SysRoute> routes, String role) {
        for (SysRoute route : routes) {
            String metaRoles = route.getMeta_roles();
            try {
                List<String> roles = metaRoles != null && !metaRoles.isEmpty()
                        ? objectMapper.readValue(metaRoles, new TypeReference<List<String>>() {})
                        : new ArrayList<>();
                roles.add(role);
                route.setMeta_roles(objectMapper.writeValueAsString(roles));
                routeMapper.updateMetaRoles(route.getId(), route.getMeta_roles());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 从路由中移除角色
     */
    private void removeRoleFromRoutes(List<SysRoute> routes, String role) {
        for (SysRoute route : routes) {
            String metaRoles = route.getMeta_roles();
            try {
                List<String> roles = metaRoles != null && !metaRoles.isEmpty()
                        ? objectMapper.readValue(metaRoles, new TypeReference<List<String>>() {})
                        : new ArrayList<>();
                roles.remove(role);
                route.setMeta_roles(objectMapper.writeValueAsString(roles));
                routeMapper.updateMetaRoles(route.getId(), route.getMeta_roles());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 提取返回的路由 ID（递归处理子路由）
     */
    private List<Long> extractIdsFromReturnedRoutes(List<RouteDTO> routes) {
        List<Long> ids = new ArrayList<>();
        for (RouteDTO route : routes) {
            ids.add(route.getId());
            if (route.getChildren() != null && !route.getChildren().isEmpty()) {
                ids.addAll(extractIdsFromReturnedRoutes(route.getChildren()));
            }
        }
        return ids;
    }
}
