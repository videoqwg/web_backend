package com.example.myproject.Model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;


@lombok.Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "messages")
public class MessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id // 指定主键字段
    private String id;
    private String senderId;
    private String receiverId;
    private String command;
    private String commandType;
    private Instant timestamp;

    // 新增字段：用于表示是否已处理
    private boolean processed = false;

    // 新增字段：用于设置过期时间，支持 null 值表示永久存储

    @Indexed(expireAfter = "0s")  // 设置 TTL 索引
    private Instant expiryTime;



    @Override
    public String toString() {
        return "InstructionMessage{" +
                "Id='" + id + '\'' +
                "senderId='" + senderId + '\'' +
                ", receiverId='" + receiverId + '\'' +
                ", command='" + command + '\'' +
                ", commandType='" + commandType + '\'' +
                ", timestamp=" + timestamp +
                ", processed=" + processed +
                ", expiryTime=" + expiryTime +
                '}';
    }
}


