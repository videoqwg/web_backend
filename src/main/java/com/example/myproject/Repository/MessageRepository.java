package com.example.myproject.Repository;

import com.example.myproject.Model.MessageDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;


@Repository
public interface MessageRepository extends MongoRepository<MessageDTO, String> {
    // 根据 senderId、receiverId 和 command 查询指令
    Optional<MessageDTO> findBySenderIdAndReceiverIdAndCommand(String senderId, String receiverId, String command);
    List<MessageDTO> findByReceiverIdAndProcessed(String receiverId, boolean processed);
    List<MessageDTO> findByReceiverIdAndCommandTypeAndTimestampGreaterThan(String senderId,String commandType, Instant timestamp);
    List<MessageDTO> findBySenderIdAndCommandTypeAndTimestampGreaterThan(String senderId,String commandType, Instant timestamp);
    List<MessageDTO> findByReceiverIdAndCommandTypeAndExpiryTimeGreaterThan(String senderId,String commandType, Instant timestamp);
    List<MessageDTO> findBySenderIdAndCommandTypeAndExpiryTimeGreaterThan(String senderId,String commandType, Instant timestamp);
    List<MessageDTO> findByReceiverIdAndCommandType(String receiverId, String commandType);
    List<MessageDTO> findBySenderIdAndCommandType(String senderId, String commandType);
}



