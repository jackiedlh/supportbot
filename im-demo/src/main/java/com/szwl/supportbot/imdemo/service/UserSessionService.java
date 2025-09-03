package com.szwl.supportbot.imdemo.service;

import com.szwl.supportbot.imdemo.model.UserSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 用户会话管理服务
 * 维护用户ID和路由信息的映射关系
 */
@Slf4j
@Service
public class UserSessionService {

    private final SimpMessagingTemplate messagingTemplate;
    
    // 用户会话存储：userId -> UserSession
    private final Map<Long, UserSession> userSessions = new ConcurrentHashMap<>();
    
    // 会话ID映射：sessionId -> userId
    private final Map<String, Long> sessionToUser = new ConcurrentHashMap<>();
    
    // 定时任务执行器，用于心跳检查和会话清理
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public UserSessionService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        
        // 启动定时心跳检查任务，每30秒检查一次
        scheduler.scheduleAtFixedRate(this::checkHeartbeats, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * 用户连接时注册会话
     * @param userId 用户ID
     * @param username 用户名
     * @param sessionId 会话ID
     * @param clientIp 客户端IP
     * @param clientPort 客户端端口
     * @return 用户会话对象
     */
    public UserSession registerUser(Long userId, String username, String sessionId, String clientIp, Integer clientPort) {
        // 如果用户已存在，先清理旧连接
        UserSession existingSession = userSessions.get(userId);
        if (existingSession != null) {
            log.info("用户 {} 重新连接，清理旧会话: {}", userId, existingSession.getSessionId());
            removeUserSession(userId);
        }
        
        // 创建新会话
        UserSession userSession = new UserSession(userId, username, sessionId, clientIp, clientPort);
        userSessions.put(userId, userSession);
        sessionToUser.put(sessionId, userId);
        
        log.info("用户 {} 注册成功，会话ID: {}, IP:{}:{}", userId, sessionId, clientIp, clientPort);
        return userSession;
    }

    /**
     * 用户断开连接时移除会话
     * @param sessionId 会话ID
     */
    public void removeUserBySessionId(String sessionId) {
        Long userId = sessionToUser.remove(sessionId);
        if (userId != null) {
            removeUserSession(userId);
            log.info("用户 {} 断开连接，会话ID: {}", userId, sessionId);
        }
    }

    /**
     * 根据用户ID移除会话
     * @param userId 用户ID
     */
    public void removeUserSession(Long userId) {
        UserSession session = userSessions.remove(userId);
        if (session != null) {
            sessionToUser.remove(session.getSessionId());
            log.info("用户 {} 会话已移除", userId);
        }
    }

    /**
     * 获取用户会话
     * @param userId 用户ID
     * @return 用户会话对象
     */
    public UserSession getUserSession(Long userId) {
        return userSessions.get(userId);
    }

    /**
     * 处理用户心跳
     * @param userId 用户ID
     * @return 是否处理成功
     */
    public boolean handleHeartbeat(Long userId) {
        UserSession session = userSessions.get(userId);
        if (session != null) {
            // 记录心跳时间
            session.updateHeartbeat();
            log.debug("用户 {} 心跳记录", userId);
            return true;
        } else {
            log.warn("用户 {} 心跳记录失败，会话不存在", userId);
            return false;
        }
    }

    /**
     * 向指定用户发送消息
     * @param userId 用户ID
     * @param message 消息内容
     * @return 是否发送成功
     */
    public boolean sendMessageToUser(Long userId, Object message) {
        UserSession session = userSessions.get(userId);
        if (session != null) {
            try {
                log.info("准备发送消息: userId={}, username={}, sessionId={}, message={}", userId, session.getUsername(), session.getSessionId(), message);

                // 使用convertAndSendToUser发送到用户的个人队列
                // 使用用户ID作为用户标识，Spring会自动构建为 /user/{userId}/queue/messages
                messagingTemplate.convertAndSendToUser(
                    session.getUserId().toString(),  // 使用用户ID作为用户标识
                    "/queue/messages",  // 目标队列，Spring会自动构建为/user/{userId}/queue/messages
                    message
                );
                
                log.info("消息发送成功: userId={}, username={}, sessionId={}, 目标队列=/user/{}/queue/messages", 
                         userId, session.getUsername(), session.getSessionId(), session.getUserId());
                return true;
            } catch (Exception e) {
                log.error("发送消息失败: userId={}, sessionId={}, message={}", userId, session.getSessionId(), message, e);
                return false;
            }
        } else {
            log.warn("发送消息失败，用户会话不存在: userId={}", userId);
            return false;
        }
    }

    /**
     * 检查心跳并清理无效会话
     * 每30秒执行一次
     */
    private void checkHeartbeats() {
        long currentTime = System.currentTimeMillis();
        long heartbeatTimeout = 60 * 1000; // 60秒心跳超时
        
        userSessions.entrySet().removeIf(entry -> {
            UserSession session = entry.getValue();
            long timeSinceLastHeartbeat = currentTime - session.getLastHeartbeat();
            
            if (timeSinceLastHeartbeat > heartbeatTimeout) {
                // 增加丢失心跳次数
                session.incrementMissedHeartbeats();
                
                if (session.shouldBeCleaned()) {
                    log.info("清理无效会话: userId={}, IP:{}:{}, 丢失心跳次数: {}", 
                             session.getUserId(), session.getClientIp(), session.getClientPort(), 
                             session.getMissedHeartbeats());
                    sessionToUser.remove(session.getSessionId());
                    return true;
                } else {
                    log.warn("用户 {} 心跳丢失，当前丢失次数: {}", 
                             session.getUserId(), session.getMissedHeartbeats());
                }
            }
            return false;
        });
    }
    

}
