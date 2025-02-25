package com.example.myproject.Manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class WebSocketSessionManager {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);

    // 存储用户 ID 和 WebSocketSession 的映射
    private static final ConcurrentHashMap<String, WebSocketSession> userIdToSession = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<WebSocketSession, String> sessionToUserId = new ConcurrentHashMap<>();

    private static final ExecutorService executorService = Executors.newFixedThreadPool(10);

    public void addSession(String userId, WebSocketSession session) {
        userIdToSession.put(userId, session);
        sessionToUserId.put(session, userId);
    }

    public String getUserId(WebSocketSession session) {
        return sessionToUserId.get(session);
    }

    public WebSocketSession getSession(String userId) {
        return userIdToSession.get(userId);
    }

    public void removeSession(String userId) {
        WebSocketSession session = userIdToSession.remove(userId);
        if (session != null) {
            sessionToUserId.remove(session);
        }
    }

    public void sendMessageToAll(String message) {
        userIdToSession.values().forEach(session -> executorService.submit(() -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                } else {
                    removeSession(getUserId(session));
                }
            } catch (Exception e) {
                logger.error("Error sending message to session", e);
            }
        }));
    }

    public void sendMessageToUser(String userId, String message) {
        WebSocketSession session = getSession(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                logger.info("Message sent to user {}: {}", userId, message);
            } catch (Exception e) {
                logger.error("Error sending message to user {}", userId, e);
            }
        } else {
            logger.warn("WebSocket session for user {} is not available", userId);
        }
    }
}
