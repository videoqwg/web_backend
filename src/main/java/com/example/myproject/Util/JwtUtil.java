package com.example.myproject.Util;

import com.example.myproject.Config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {
    private final JwtConfig jwtConfig;

    @Autowired
    public JwtUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    public  String generateToken(String userid, String username, String avatar, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userid", userid);
        claims.put("username", username);
        claims.put("avatar", avatar);
        claims.put("roles", roles);

        return Jwts.builder()
                .setClaims(claims) // 设置负载
                .setSubject("userInfo") // 设置主题
                .setIssuedAt(new Date(System.currentTimeMillis())) // 设置签发时间
                .setExpiration(new Date(System.currentTimeMillis() + 86400000)) // 设置过期时间为1天
                .signWith(SignatureAlgorithm.HS512, jwtConfig.getSecretKey()) // 设置签名算法和密钥
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .setSigningKey(jwtConfig.getSecretKey())
                .parseClaimsJws(token)
                .getBody();
    }
}