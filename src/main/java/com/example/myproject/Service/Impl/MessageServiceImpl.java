package com.example.myproject.Service.Impl;

import com.example.myproject.Model.MessageDTO;
import com.example.myproject.Model.Result;
import com.example.myproject.Repository.MessageRepository;
import com.example.myproject.Service.FriendsService;
import com.example.myproject.Service.GroupsService;
import com.example.myproject.Service.MessageService;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.example.myproject.Config.RabbitMQConfig.GROUP_EXCHANGE;
import static com.example.myproject.Config.RabbitMQConfig.INSTRUCTION_EXCHANGE;


@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private FriendsService friendsService;

    @Autowired
    private GroupsService groupsService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 给指定用户发送指令/消息
     */
    public Result sendMessage(MessageDTO message) {
        try {
            String routingKey = "user." + message.getReceiverId();

            rabbitTemplate.convertAndSend(
                    INSTRUCTION_EXCHANGE,
                    routingKey,
                    message
            );

            messageRepository.save(message);
            System.out.println("【指令发送】" + message);
            return Result.success();
        } catch (AmqpException e) {
            return Result.failure(e.getMessage());
        }
    }

    public Result sendGroupMessage(MessageDTO message) {
        // RoutingKey: group.<groupId>.*
        // 所有绑定了 group.<groupId>.<xxx> 的用户队列，均可收到此消息
        try {
            String routingKey = String.format("group.%s.*", message.getReceiverId());

            rabbitTemplate.convertAndSend(GROUP_EXCHANGE, routingKey, message);

            messageRepository.save(message);
            System.out.printf("[群消息] %s -> 群[%s]: %s\n", message.getSenderId(), message.getReceiverId(), message.getCommand());
            return Result.success();
        } catch (AmqpException e) {
            return Result.failure(e.getMessage());
        }
    }

    public Result addFriend(MessageDTO message){
        Optional<MessageDTO> existingMessage = messageRepository.findBySenderIdAndReceiverIdAndCommand(
                message.getSenderId(),
                message.getReceiverId(),
                message.getCommand()
        );
        if (existingMessage.isPresent()) {
            return Result.failure("已发送过好友请求");
        }
        sendMessage(message);
        return friendsService.addFriend(message.getSenderId(), message.getReceiverId());
    }

    public Map<String,List<MessageDTO>> processMessages(String receiverId) {
        // 查询 senderId 为 123 且 processed 为 false 的所有记录
        List<MessageDTO> messages = messageRepository.findByReceiverIdAndProcessed(receiverId, false);

        // 根据 commandType 分组
        List<MessageDTO> Commands = messages.stream()
                .filter(message -> "command".equals(message.getCommandType()))
                .toList();

        List<MessageDTO> Chats = messages.stream()
                .filter(message -> "chat".equals(message.getCommandType()))
                .toList();

        Map<String,List<MessageDTO>> data = Map.of(
                "commands",Commands,
                "chats",Chats
        );

        return data;
    }

    @Override
    public Result syncMessages(String timestamp, String userId, String commandType) {
        List<MessageDTO> receivedMessages;
        List<MessageDTO> sendMessages;
        if(timestamp == null){
            try {
                if(Objects.equals(commandType, "notification")){
                    Instant currentTime = Instant.now();
                    // 时区问题需要加8小时
                    Instant newTime = currentTime.plus(Duration.ofHours(8));
                    // 查询用户接收到的消息
                    receivedMessages = messageRepository.findByReceiverIdAndCommandTypeAndExpiryTimeGreaterThan(userId, commandType, newTime);
                    // 查询用户发送的消息
                    sendMessages = messageRepository.findBySenderIdAndCommandTypeAndExpiryTimeGreaterThan(userId, commandType, newTime);
                }else {
                    // 查询用户接收到的消息
                    receivedMessages = messageRepository.findByReceiverIdAndCommandType(userId, commandType);
                    // 查询用户发送的消息
                    sendMessages = messageRepository.findBySenderIdAndCommandType(userId, commandType);
                }
                // 合并消息并按时间戳排序
                List<MessageDTO> allMessages = new ArrayList<>();
                allMessages.addAll(receivedMessages);
                allMessages.addAll(sendMessages);

                // 按时间戳排序消息
                allMessages.sort(Comparator.comparing(MessageDTO::getTimestamp));

                return Result.success(allMessages);
            } catch (Exception e) {
                e.printStackTrace();
                return Result.failure("同步消息失败" + e.getMessage());
            }
        }else {
            try {
                Instant timestampDate = Instant.parse(timestamp);
                receivedMessages = messageRepository.findByReceiverIdAndCommandTypeAndTimestampGreaterThan(userId, commandType, timestampDate);
                // 查询时间戳之后用户发送的消息
                sendMessages = messageRepository.findBySenderIdAndCommandTypeAndTimestampGreaterThan(userId, commandType, timestampDate);
                // 合并消息并按时间戳排序
                List<MessageDTO> allMessages = new ArrayList<>();
                allMessages.addAll(receivedMessages);
                allMessages.addAll(sendMessages);

                // 按时间戳排序消息
                allMessages.sort(Comparator.comparing(MessageDTO::getTimestamp));

                return Result.success(allMessages);
            } catch (Exception e) {
                e.printStackTrace();
                return Result.failure("同步消息失败" + e.getMessage());
            }
        }
    }
}
