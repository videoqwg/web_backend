package com.example.myproject.Manager;

import com.example.myproject.Model.MessageDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自定义的消息监听器，用于接收 JSON 格式的消息，并通过 WebSocket 推送给指定客户端。
 */
public class DynamicUserMessageListener implements ChannelAwareMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(DynamicUserMessageListener.class);

    private final String userId;
    private final MessageConverter messageConverter;
    private final WebSocketSessionManager webSocketSessionManager;

    public DynamicUserMessageListener(String userId, MessageConverter messageConverter, WebSocketSessionManager webSocketSessionManager) {
        this.userId = userId;
        this.messageConverter = messageConverter;
        this.webSocketSessionManager = webSocketSessionManager;
    }

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            // 将消息转换为 MessageDTO 对象
            MessageDTO instruction = (MessageDTO) messageConverter.fromMessage(message);
            logger.info("【用户 {}】收到消息: {}", userId, instruction);

            if (instruction != null && instruction.getCommandType() != null) {
                // 使用 WebSocket 推送消息到前端
                if(!sendMessageToClient(userId, instruction)){
                    channel.basicNack(deliveryTag, false, true); // 重新入队
                }
            } else {
                logger.warn("收到无效的消息或类型为空: {}", instruction);
            }

            // 手动确认消息
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            logger.error("处理消息时发生错误：{}", e.getMessage(), e);
            // channel.basicNack(deliveryTag, false, true); // 重新入队
            channel.basicAck(deliveryTag, false);
        }
    }

    private boolean sendMessageToClient(String userId, MessageDTO instruction) {
        try {
            // 创建 Jackson 的 ObjectMapper 实例
            ObjectMapper objectMapper = new ObjectMapper();
            // 注册 JavaTimeModule 模块以支持 Java 8 日期时间
            objectMapper.registerModule(new JavaTimeModule());
            // 禁用将日期写为 [year, month, day, ...] 的数组形式
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            // 将 MessageDTO 对象序列化为 JSON 字符串
            String messageJson = objectMapper.writeValueAsString(instruction);

            // 检查用户的 WebSocket 会话是否存在
            if (webSocketSessionManager.getSession(userId) != null) {
                // 通过 WebSocket 发送消息
                webSocketSessionManager.sendMessageToUser(userId, messageJson);
                logger.info("已向用户 {} 发送 WebSocket 消息: {}", userId, messageJson);
                return true;
            } else {
                logger.warn("WebSocket 会话未找到，无法向用户 {} 发送消息", userId);
                return false;
            }
        } catch (Exception e) {
            logger.error("向用户 {} 发送 WebSocket 消息失败", userId, e);
            return false;
        }
    }

}
