package com.example.myproject.Manager;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    // 用于管理 WebSocket 会话
    private final WebSocketSessionManager sessionManager = new WebSocketSessionManager();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            var params = UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().toSingleValueMap();
            String userId = params.get("userId");

            if (userId != null) {
                sessionManager.addSession(userId, session);
                logger.info("User connected: {}", userId);
            } else {
                logger.warn("User ID is missing in the WebSocket request");
                session.close();
            }
        } catch (Exception e) {
            logger.error("Error during WebSocket connection establishment", e);
            try {
                session.close();
            } catch (Exception closeEx) {
                logger.error("Error closing WebSocket session", closeEx);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        String userId = sessionManager.getUserId(session);
        if (userId != null) {
            sessionManager.removeSession(userId);
            logger.info("Connection closed for user: {}", userId);
        } else {
            logger.warn("Unable to find user ID for the closed session");
        }
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        logger.info("Message received: {}", message.getPayload());
        // 可根据业务需求实现具体的消息处理逻辑
    }
}


