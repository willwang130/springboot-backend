package com.example.demo.webSocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class WebSocketNotificationHandler extends TextWebSocketHandler {

    // 存储所有连接的 WebSocketSession
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    // 有新连接时添加到 sessions
    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) {
        sessions.add(session);
        log.info("管理员连接 WebSocket: {}", session.getId());
    }

    // 连接关闭后移除 sessions
    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) {
        sessions.remove(session);
        log.info("管理员断开 WebSocket: {}", session.getId());
    }

    // 向所有管理员推送消息
    public void sendNotification(String message) throws IOException {
        for (WebSocketSession session: sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

    // 收到消息后，给所有连接者广播
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, TextMessage message) throws IOException {
        log.info("收到消息: {}", message.getPayload());
        for(WebSocketSession webSocketSession : sessions) {
            webSocketSession.sendMessage(new TextMessage("服务器收到: " + message.getPayload()));
        }
    }

    // 服务器每隔 30 秒发送一次 ping
    @Scheduled(fixedRate = 30000) // 每 30 秒
    public void sendHeartbeat() throws IOException {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage("ping"));
            }
        }
    }
}
