package com.admin.common.utils;


import com.admin.entity.Node;
import com.admin.service.NodeService;
import com.alibaba.fastjson.JSONObject;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.annotation.Resource;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
public class WebSocketServer extends TextWebSocketHandler {

    @Resource
    NodeService nodeService;

    // 存储所有活跃的 WebSocket 连接
    private static final CopyOnWriteArraySet<WebSocketSession> activeSessions = new CopyOnWriteArraySet<>();
    
    // 为每个session提供锁对象，防止并发发送消息
    private static final ConcurrentHashMap<String, Object> sessionLocks = new ConcurrentHashMap<>();

    //接受客户端消息
    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            if (StringUtils.isNoneBlank(message.getPayload())) {
                //log.info("收到消息: {}", message.getPayload());
                
                String id = session.getAttributes().get("id").toString();
                String type = session.getAttributes().get("type").toString();

                // 先发送确认消息
                sendToUser(session, "ok");
                
                // 如果是节点类型，转发消息给其他会话
                if (Objects.equals(type, "1")) {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", id);
                    jsonObject.put("type", "info");
                    jsonObject.put("data", message.getPayload());
                    String broadcastMessage = jsonObject.toJSONString();
                    
                    // 异步处理广播消息，避免阻塞当前线程
                    for (WebSocketSession targetSession : activeSessions) {
                        if (targetSession != null && targetSession.isOpen() && !targetSession.equals(session)) {
                            sendToUser(targetSession, broadcastMessage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息时发生异常: {}", e.getMessage(), e);
        }
    }

    // 建立连接
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            String id = session.getAttributes().get("id").toString();
            String type = session.getAttributes().get("type").toString();
            if (!Objects.equals(type, "1")) {
                activeSessions.add(session);
            }else {
                Node byId = nodeService.getById(id);
                if (byId != null) {
                    byId.setStatus(1);
                    nodeService.updateById(byId);
                    JSONObject res = new JSONObject();
                    res.put("id", id);
                    res.put("type", "status");
                    res.put("data", 1);
                    broadcastMessage(res.toJSONString());
                }
            }
            log.info("WebSocket 连接建立成功 - id: {}, type: {}, 当前连接数: {}", id, type, activeSessions.size());

        } catch (Exception e) {
            log.error("建立连接时发生异常: {}", e.getMessage(), e);
        }
    }

    // 连接关闭后
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            String id = session.getAttributes().get("id").toString();
            String type = session.getAttributes().get("type").toString();
            String sessionId = session.getId();
            
            if (!Objects.equals(type, "1")) {
                activeSessions.remove(session);
            }else {
                Node byId = nodeService.getById(id);
                if (byId != null) {
                    byId.setStatus(0);
                    nodeService.updateById(byId);
                    JSONObject res = new JSONObject();
                    res.put("id", id);
                    res.put("type", "status");
                    res.put("data", 0);
                    broadcastMessage(res.toJSONString());
                }
            }
            
            // 清理session锁对象
            sessionLocks.remove(sessionId);

            log.info("WebSocket 连接关闭 - id: {}, sessionId: {}, 关闭状态: {}, 当前连接数: {}",
                    id, sessionId, status, activeSessions.size());

        } catch (Exception e) {
            log.error("关闭连接时发生异常: {}", e.getMessage(), e);
        }
    }

    // 点对点发送消息
    @SneakyThrows
    public static void sendToUser(WebSocketSession socketSession, String message) {
        if (socketSession != null && socketSession.isOpen()) {
            String sessionId = socketSession.getId();
            Object lock = sessionLocks.computeIfAbsent(sessionId, k -> new Object());
            
            synchronized (lock) {
                try {
                    if (socketSession.isOpen()) {
                        socketSession.sendMessage(new TextMessage(message));
                    }
                } catch (Exception e) {
                    log.error("发送WebSocket消息失败 [sessionId={}]: {}", sessionId, e.getMessage());
                    activeSessions.remove(socketSession);
                    sessionLocks.remove(sessionId);
                }
            }
        } else {
            activeSessions.remove(socketSession);
            if (socketSession != null) {
                sessionLocks.remove(socketSession.getId());
            }
        }
    }

    // 广播消息
    public static void broadcastMessage(String message) {
        for (WebSocketSession session : activeSessions) {
            sendToUser(session, message);
        }
    }
}
