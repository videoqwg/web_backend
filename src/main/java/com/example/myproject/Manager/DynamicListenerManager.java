package com.example.myproject.Manager;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.example.myproject.Service.UserService.getQueueNameByUserId;

@Component
public class DynamicListenerManager {

    private final ConnectionFactory connectionFactory;
    private final MessageConverter messageConverter;
    private final WebSocketSessionManager webSocketSessionManager;

    // 存放 userId -> container
    private final Map<String, SimpleMessageListenerContainer> containerMap = new ConcurrentHashMap<>();

    public DynamicListenerManager(ConnectionFactory connectionFactory,
                                  MessageConverter messageConverter,
                                  WebSocketSessionManager webSocketSessionManager) {
        this.connectionFactory = connectionFactory;
        this.messageConverter = messageConverter;
        this.webSocketSessionManager = webSocketSessionManager;
    }

    /**
     * 启动指定用户的监听容器
     */
    public void startListenerForUser(String userId) {
        if (containerMap.containsKey(userId)) {
            return; // 已存在则不重复创建
        }

        String queueName = getQueueNameByUserId(userId);

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(5);

        // 设置为手动确认模式
        container.setAcknowledgeMode(org.springframework.amqp.core.AcknowledgeMode.MANUAL);

        // 设置自定义监听器，并注入 messageConverter 和 webSocketSessionManager
        container.setMessageListener(new DynamicUserMessageListener(userId, messageConverter, webSocketSessionManager));

        container.start();
        containerMap.put(userId, container);
    }

    /**
     * 停止指定用户的监听容器
     */
    public void stopListenerForUser(String userId) {
        SimpleMessageListenerContainer container = containerMap.remove(userId);
        if (container != null) {
            container.stop();
        }
    }
}
