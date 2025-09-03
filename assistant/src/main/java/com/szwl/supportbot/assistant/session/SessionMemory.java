package com.szwl.supportbot.assistant.session;

import lombok.extern.slf4j.Slf4j;
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

    private static final Duration SESSION_TTL = Duration.ofMinutes(30); // 延长到30分钟
    private static final String SESSION_PREFIX = "chat:";
    
    private final RedisTemplate<String, Object> redisTemplate;

    public SessionMemory(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

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
     * @return 格式化的对话历史字符串，如果没有聊天记录则返回空字符串
     */
    public String getConversationContext(String uid) {
        List<Map<String, Object>> chatHistory = getChatHistory(uid);
        
        if (chatHistory.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("对话历史：\n");
        
        for (Map<String, Object> record : chatHistory) {
            String sender = (String) record.get("sender");
            String content = (String) record.get("content");
            
            if ("user".equals(sender)) {
                context.append("用户：").append(content).append("\n");
            } else if ("assistant".equals(sender)) {
                context.append("助手：").append(content).append("\n");
            }
        }
        
        return context.toString();
    }

    /**
     * 构建对话历史上下文字符串（保持向后兼容）
     * @deprecated 请使用 getConversationContext 方法
     */
    @Deprecated
    public String buildConversationContext(String uid) {
        return getConversationContext(uid);
    }

    /**
     * 获取最近的N条聊天记录
     * 
     * @param uid 用户ID
     * @param count 记录数量
     * @return 最近的聊天记录列表
     */
    public List<Map<String, Object>> getRecentChatHistory(String uid, int count) {
        List<Map<String, Object>> chatHistory = getChatHistory(uid);
        
        if (chatHistory.size() <= count) {
            return new ArrayList<>(chatHistory);
        }
        
        return new ArrayList<>(chatHistory.subList(chatHistory.size() - count, chatHistory.size()));
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
     * 检查用户是否有聊天记录
     * 
     * @param uid 用户ID
     * @return true表示有记录，false表示无记录
     */
    public boolean hasChatHistory(String uid) {
        String key = SESSION_PREFIX + uid;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 获取用户聊天记录剩余生存时间
     * 
     * @param uid 用户ID
     * @return 剩余秒数，-1表示永不过期，-2表示key不存在
     */
    public Long getChatHistoryTTL(String uid) {
        String key = SESSION_PREFIX + uid;
        return redisTemplate.getExpire(key);
    }

    /**
     * 获取聊天记录数量
     * 
     * @param uid 用户ID
     * @return 记录数量
     */
    public int getChatHistorySize(String uid) {
        List<Map<String, Object>> chatHistory = getChatHistory(uid);
        return chatHistory.size();
    }

    // 保持向后兼容的方法
    @Deprecated
    public Map<String, Object> getSessionContext(String uid) {
        List<Map<String, Object>> chatHistory = getChatHistory(uid);
        if (chatHistory.isEmpty()) {
            return null;
        }
        
        // 返回最后两条记录作为兼容格式
        Map<String, Object> context = new java.util.HashMap<>();
        if (chatHistory.size() >= 2) {
            Map<String, Object> lastUser = chatHistory.get(chatHistory.size() - 2);
            Map<String, Object> lastAssistant = chatHistory.get(chatHistory.size() - 1);
            
            if ("user".equals(lastUser.get("sender")) && "assistant".equals(lastAssistant.get("sender"))) {
                context.put("lastQuestion", lastUser.get("content"));
                context.put("lastAnswer", lastAssistant.get("content"));
                context.put("timestamp", lastAssistant.get("timestamp"));
            }
        }
        
        return context;
    }

    @Deprecated
    public void saveUserMessage(String uid, String userMessage) {
        addUserQuestion(uid, userMessage);
    }

    @Deprecated
    public void saveAIResponse(String uid, String aiResponse, String responseType) {
        addAIAnswer(uid, aiResponse);
    }

    @Deprecated
    public String buildConversationHistory(String uid) {
        return buildConversationContext(uid);
    }

    @Deprecated
    public void clearSessionContext(String uid) {
        clearChatHistory(uid);
    }

    @Deprecated
    public boolean hasSession(String uid) {
        return hasChatHistory(uid);
    }

    @Deprecated
    public Long getSessionTTL(String uid) {
        return getChatHistoryTTL(uid);
    }
}
