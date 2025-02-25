package com.example.myproject.Repository;

import com.example.myproject.Model.SysRoute;
import org.apache.ibatis.annotations.*;



import java.util.List;

@Mapper
public interface RouteMapper {

    /**
     * 查询所有路由
     */
    @Select("SELECT * FROM sys_route")
    List<SysRoute> findAll();

    /**
     * 更新路由的 meta_roles 字段
     */
    @Update("UPDATE sys_route SET meta_roles = #{metaRoles} WHERE id = #{id}")
    void updateMetaRoles(@Param("id") Long id, @Param("metaRoles") String metaRoles);
}

