package com.szwl.supportbot.knowledgerag.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * JSON 工具类
 * 提供 JSON 序列化和反序列化功能
 */
@Slf4j
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 对象转 JSON 字符串
     * @param obj 对象
     * @return JSON 字符串
     */
    public static String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败", e);
            return null;
        }
    }

    /**
     * JSON 字符串转对象
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败: {}", json, e);
            return null;
        }
    }

    /**
     * JSON 字符串转对象列表
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @param <T> 泛型类型
     * @return 对象列表
     */
    public static <T> List<T> fromJsonList(String json, Class<T> clazz) {
        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(json, listType);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象列表失败: {}", json, e);
            return null;
        }
    }

    /**
     * 格式化 JSON 字符串
     * @param json JSON 字符串
     * @return 格式化后的 JSON 字符串
     */
    public static String formatJson(String json) {
        try {
            Object obj = objectMapper.readValue(json, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("格式化JSON失败: {}", json, e);
            return json;
        }
    }

    /**
     * 检查字符串是否为有效的 JSON
     * @param json JSON 字符串
     * @return 是否有效
     */
    public static boolean isValidJson(String json) {
        try {
            objectMapper.readValue(json, Object.class);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
