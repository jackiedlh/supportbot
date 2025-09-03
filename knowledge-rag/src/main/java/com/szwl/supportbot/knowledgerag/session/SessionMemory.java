package com.szwl.supportbot.knowledgerag.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 会话内存管理服务
 * 使用Redis存储用户会话上下文，支持追加存储聊天记录
 * 每条记录包含发送者身份(user/assistant)和内容
 */
@Slf4j
@Service
public class SessionMemory {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final Duration SESSION_TTL = Duration.ofMinutes(30); // 延长到30分钟
    private static final String SESSION_PREFIX = "chat:";

    /**
     * 获取用户聊天记录
     * 
     * @param uid 用户ID
     * @return 聊天记录列表，如果不存在返回空列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getChatHistory(String uid) {
        String key = SESSION_PREFIX + uid;
        Object value = redisTemplate.opsForValue().get(key);
        
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    /**
     * 添加聊天记录
     * 
     * @param uid 用户ID
     * @param sender 发送者身份 ("user" 或 "assistant")
     * @param content 消息内容
     */
    public void addChatRecord(String uid, String sender, String content) {
        String key = SESSION_PREFIX + uid;
        
        // 获取现有聊天记录
        List<Map<String, Object>> chatHistory = getChatHistory(uid);
        
        // 创建新的聊天记录
        Map<String, Object> record = Map.of(
            "sender", sender,
            "content", content,
            "timestamp", System.currentTimeMillis()
        );
        
        // 保存到历史记录
        chatHistory.add(record);
        
        // 保存到Redis，设置TTL
        redisTemplate.opsForValue().set(key, chatHistory, SESSION_TTL);
        
        log.info("添加聊天记录: uid={}, sender={}, contentLength={}", uid, sender, content.length());
    }

    /**
     * 添加用户问题记录
     * 
     * @param uid 用户ID
     * @param question 用户问题
     */
    public void addUserQuestion(String uid, String question) {
        addChatRecord(uid, "user", question);
    }

    /**
     * 添加AI回答记录
     * 
     * @param uid 用户ID
     * @param answer AI回答内容
     */
    public void addAIAnswer(String uid, String answer) {
        addChatRecord(uid, "assistant", answer);
    }

    /**
     * 获取对话历史上下文字符串
     * 用于AI模型的上下文输入
     * 
     * @param uid 用户ID
     * @return 格式化的对话历史字符串
     */
    public String getConversationContext(String uid) {
        List<Map<String, Object>> chatHistory = getChatHistory(uid);
        
        if (chatHistory.isEmpty()) {
            return null;
        }
        
        StringBuilder context = new StringBuilder();
        context.append("对话历史:\n");
        
        for (Map<String, Object> record : chatHistory) {
            String sender = (String) record.get("sender");
            String content = (String) record.get("content");
            
            if ("user".equals(sender)) {
                context.append("用户: ").append(content).append("\n");
            } else if ("assistant".equals(sender)) {
                context.append("AI: ").append(content).append("\n");
            }
        }
        
        return context.toString();
    }

    /**
     * 添加对话记录（兼容旧接口）
     * 
     * @param sessionId 会话ID
     * @param userMessage 用户消息
     * @param aiResponse AI回答
     */
    public void addConversationRecord(String sessionId, String userMessage, String aiResponse) {
        addUserQuestion(sessionId, userMessage);
        addAIAnswer(sessionId, aiResponse);
    }

    /**
     * 清除用户聊天记录
     * 
     * @param uid 用户ID
     */
    public void clearChatHistory(String uid) {
        String key = SESSION_PREFIX + uid;
        redisTemplate.delete(key);
        log.info("清除聊天记录: uid={}", uid);
    }

    /**
     * 检查是否有聊天记录
     * 
     * @param uid 用户ID
     * @return 是否有聊天记录
     */
    public boolean hasChatHistory(String uid) {
        return !getChatHistory(uid).isEmpty();
    }

    /**
     * 获取聊天记录数量
     * 
     * @param uid 用户ID
     * @return 聊天记录数量
     */
    public int getChatHistorySize(String uid) {
        return getChatHistory(uid).size();
    }

    /**
     * 获取最近的聊天记录
     * 
     * @param uid 用户ID
     * @param limit 限制数量
     * @return 最近的聊天记录列表
     */
    public List<Map<String, Object>> getRecentChatHistory(String uid, int limit) {
        List<Map<String, Object>> allHistory = getChatHistory(uid);
        if (allHistory.size() <= limit) {
            return allHistory;
        }
        return allHistory.subList(allHistory.size() - limit, allHistory.size());
    }

    /**
     * 获取聊天记录TTL
     * 
     * @param uid 用户ID
     * @return TTL秒数，-1表示永不过期，-2表示key不存在
     */
    public long getChatHistoryTTL(String uid) {
        String key = SESSION_PREFIX + uid;
        return redisTemplate.getExpire(key);
    }
}