package com.example.myproject.Util;

import com.example.myproject.Model.User;
import com.example.myproject.Service.UserService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization"); // 获取请求头中的 JWT

        if (token == null || !token.startsWith("Bearer ")) {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/plain;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("请求头缺少或无效的Token");
            return false;
        }

        token = token.substring(7); // 去掉 "Bearer " 前缀

        try {
            Claims claims = jwtUtil.validateToken(token);  // 验证并解析 JWT
            // 将用户信息存储在请求属性中，供后续使用
            String userid = claims.get("userid", String.class);
            User user = userService.findUser(userid);
            request.setAttribute("user", user);
            return true;
        } catch (Exception e) {
            response.setCharacterEncoding("UTF-8");
            response.setContentType("text/plain;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("token非法或已过期");
            return false;
        }
    }
}
