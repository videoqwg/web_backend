package com.example.myproject.Model;


import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@lombok.Data
@Document(collection = "friends")
public class Friends {
    @Id
    private String id; // MongoDB 自动生成的唯一ID
    private String userId; // 用户ID（对应 MySQL 的 User.userid）
    private List<Friend> friends; // 初始好友列表为空

    public Friends(String userId) {
        this.userId = userId;
        this.friends = new ArrayList<>();
    }

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Friend {
        private String friendId; // 好友的用户ID
        private String status; // 好友关系状态（pending/accepted/blocked）
        private LocalDateTime createdAt; // 添加好友的时间
    }
}
