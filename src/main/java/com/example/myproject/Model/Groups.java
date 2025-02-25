package com.example.myproject.Model;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "groups")
public class Groups {
    @Id
    private String id;  // MongoDB 自动生成的唯一ID
    private String userId;  // 创建者的用户ID
    private String groupId;
    private String groupName;
    private List<Member> members;
    private LocalDateTime createdAt;

    @lombok.Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Member {
        private String userId;
        private String role;
        private String status;
        private LocalDateTime joinedAt;
    }
}

