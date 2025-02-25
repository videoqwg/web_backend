package com.example.myproject.Model;

import lombok.Data;

import java.time.LocalDateTime;

@lombok.Data
public class User {
    private String userid;
    private String username;
    private String password;
    private String email;
    private String phone;
    private String introduction;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String avatar;
    private String role;
}
