package net.map591.configurtion;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.map591.service.impl.TrackWebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
@Component
@Slf4j
public class TrackWebSocketHandler extends TextWebSocketHandler {

    // 存储联单号与WebSocket会话的映射关系
    // key: ldbh (联单号), value: WebSocketSession
    private final Map<String, WebSocketSession> trackSessions = new ConcurrentHashMap<>();

    // 存储会话ID与联单号的映射关系，用于关闭连接时清理
    private final Map<String, String> sessionToLdbhMap = new ConcurrentHashMap<>();

    @Autowired
    private TrackWebSocketService trackWebSocketService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("轨迹WebSocket连接已建立: {}", session.getId());
    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            log.info("收到轨迹WebSocket消息: {}", payload);

            // 解析消息，获取联单号
            // 消息格式: {"action": "subscribe", "ldbh": "LD341181202510300001"}
            // 或 {"action": "unsubscribe", "ldbh": "LD341181202510300001"}
            Map<String, String> messageData = parseMessage(payload);
            String action = messageData.get("action");
            String ldbh = messageData.get("ldbh");

            if (action == null || ldbh == null || ldbh.trim().isEmpty()) {
                log.warn("无效的轨迹WebSocket消息格式: {}", payload);
                session.sendMessage(new TextMessage("{\"error\": \"无效的消息格式\"}"));
                return;
            }

            if ("subscribe".equals(action)) {
                // 订阅特定联单的轨迹
                subscribeTrack(session, ldbh);
            } else if ("unsubscribe".equals(action)) {
                // 取消订阅特定联单的轨迹
                unsubscribeTrack(session, ldbh);
            } else {
                log.warn("未知的轨迹WebSocket动作: {}", action);
                session.sendMessage(new TextMessage("{\"error\": \"未知的动作\"}"));
            }
        } catch (Exception e) {
            log.error("处理轨迹WebSocket消息失败", e);
            session.sendMessage(new TextMessage("{\"error\": \"处理消息失败\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("轨迹WebSocket连接已关闭: {}, 状态: {}", session.getId(), status);

        // 清理会话相关的映射
        String ldbh = sessionToLdbhMap.remove(session.getId());
        if (ldbh != null) {
            trackSessions.remove(ldbh, session);
            log.info("已清理联单号 {} 的WebSocket会话", ldbh);
        }
    }


    /**
     * 订阅特定联单的轨迹
     */
    private void subscribeTrack(WebSocketSession session, String ldbh) throws Exception {
        // 如果该联单号已有其他会话订阅，先关闭旧会话
        WebSocketSession oldSession = trackSessions.put(ldbh, session);
        if (oldSession != null && oldSession.isOpen() && !oldSession.getId().equals(session.getId())) {
            try {
                oldSession.close();
                sessionToLdbhMap.remove(oldSession.getId());
                log.info("已关闭联单号 {} 的旧WebSocket会话", ldbh);
            } catch (Exception e) {
                log.error("关闭旧WebSocket会话失败", e);
            }
        }

        // 建立会话ID与联单号的映射
        sessionToLdbhMap.put(session.getId(), ldbh);

        // 发送订阅成功消息
        session.sendMessage(new TextMessage("{\"success\": true, \"message\": \"已成功订阅联单号 " + ldbh + " 的轨迹\", \"ldbh\": \"" + ldbh + "\"}"));
        log.info("WebSocket会话 {} 已订阅联单号 {} 的轨迹", session.getId(), ldbh);
        // 订阅后立即推送当前轨迹
        trackWebSocketService.pushTrackUpdate(ldbh, null); // locationData 可为 null，Service 内部会重新查询
    }

    /**
     * 取消订阅特定联单的轨迹
     */
    private void unsubscribeTrack(WebSocketSession session, String ldbh) throws Exception {
        // 检查该会话是否订阅了该联单号
        String sessionLdbh = sessionToLdbhMap.get(session.getId());
        if (sessionLdbh != null && sessionLdbh.equals(ldbh)) {
            // 移除映射
            trackSessions.remove(ldbh, session);
            sessionToLdbhMap.remove(session.getId());

            // 发送取消订阅成功消息
            session.sendMessage(new TextMessage("{\"success\": true, \"message\": \"已成功取消订阅联单号 " + ldbh + " 的轨迹\"}"));
            log.info("WebSocket会话 {} 已取消订阅联单号 {} 的轨迹", session.getId(), ldbh);
        } else {
            session.sendMessage(new TextMessage("{\"error\": \"未找到该联单号的订阅记录\"}"));
            log.warn("WebSocket会话 {} 尝试取消订阅未订阅的联单号 {}", session.getId(), ldbh);
        }
    }

    /**
     * 向特定联单的订阅者发送轨迹更新
     */
    public void sendTrackUpdate(String ldbh, String trackData) {
        WebSocketSession session = trackSessions.get(ldbh);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(trackData));
                log.debug("已向联单号 {} 的订阅者发送轨迹更新", ldbh);
            } catch (Exception e) {
                log.error("向联单号 {} 的订阅者发送轨迹更新失败", ldbh, e);
                try {
                    session.close();
                } catch (Exception ex) {
                    log.error("关闭WebSocket会话失败", ex);
                }
                // 清理映射
                sessionToLdbhMap.remove(session.getId());
                trackSessions.remove(ldbh);
            }
        }
    }

    /**
     * 解析WebSocket消息
     */
    private Map<String, String> parseMessage(String payload) {
        Map<String, String> result = new ConcurrentHashMap<>();

        // 简单的JSON解析，实际应用中建议使用Jackson等库
        payload = payload.trim();
        if (payload.startsWith("{") && payload.endsWith("}")) {
            payload = payload.substring(1, payload.length() - 1);
            String[] pairs = payload.split(",");
            for (String pair : pairs) {
                String[] keyValue = pair.split(":", 2);
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    /**
     * 获取当前订阅的联单号列表
     */
    public Map<String, WebSocketSession> getTrackSessions() {
        return new ConcurrentHashMap<>(trackSessions);
    }
}

