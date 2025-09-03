package com.szwl.supportbot.knowledgerag.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.message.MessageView;

import java.nio.charset.StandardCharsets;

/**
 * 消息体提取工具类
 * 安全地处理各种类型的消息体（ByteBuffer、byte[]等）
 * 
 * @author miku
 */
@Slf4j
public class MessageBodyExtractor {

    /**
     * 提取消息体内容为字符串
     * 
     * @param messageView 消息视图
     * @return 消息体字符串，如果提取失败返回null
     */
    public static String extractAsString(MessageView messageView) {
        try {
            Object body = messageView.getBody();
            log.info("消息体类型: {}", body != null ? body.getClass().getSimpleName() : "null");
            
            byte[] bodyBytes = extractAsBytes(body);
            
            if (bodyBytes != null && bodyBytes.length > 0) {
                String message = new String(bodyBytes, StandardCharsets.UTF_8);
                log.info("消息体提取成功，长度: {}", message.length());
                return message;
            } else {
                log.warn("消息体为空");
                return null;
            }
            
        } catch (Exception e) {
            log.error("消息体提取失败: messageId={}, error={}", 
                    messageView.getMessageId(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * 提取消息体内容为字节数组
     * 
     * @param body 消息体对象
     * @return 字节数组，如果提取失败返回null
     */
    public static byte[] extractAsBytes(Object body) {
        if (body == null) {
            return null;
        }

        try {
            // 处理 ByteBuffer
            if (body instanceof java.nio.ByteBuffer) {
                java.nio.ByteBuffer buffer = (java.nio.ByteBuffer) body;
                byte[] bodyBytes = new byte[buffer.remaining()];
                buffer.get(bodyBytes);
                buffer.rewind(); // 重置位置，避免影响后续操作
                return bodyBytes;
            }
            
            // 处理字节数组
            if (body instanceof byte[]) {
                return (byte[]) body;
            }
            
            // 尝试调用 array() 方法
            try {
                return (byte[]) body.getClass().getMethod("array").invoke(body);
            } catch (Exception e) {
                log.warn("无法获取消息体数组: {}", e.getMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.error("消息体字节数组提取失败: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 检查消息体是否为空
     * 
     * @param messageView 消息视图
     * @return true表示消息体为空，false表示有内容
     */
    public static boolean isEmpty(MessageView messageView) {
        try {
            Object body = messageView.getBody();
            if (body == null) {
                return true;
            }
            
            byte[] bodyBytes = extractAsBytes(body);
            return bodyBytes == null || bodyBytes.length == 0;
            
        } catch (Exception e) {
            log.warn("检查消息体是否为空时发生异常: {}", e.getMessage());
            return true;
        }
    }

    /**
     * 获取消息体大小
     * 
     * @param messageView 消息视图
     * @return 消息体大小（字节数），如果获取失败返回-1
     */
    public static int getSize(MessageView messageView) {
        try {
            Object body = messageView.getBody();
            if (body == null) {
                return 0;
            }
            
            byte[] bodyBytes = extractAsBytes(body);
            return bodyBytes != null ? bodyBytes.length : 0;
            
        } catch (Exception e) {
            log.warn("获取消息体大小时发生异常: {}", e.getMessage());
            return -1;
        }
    }
}
