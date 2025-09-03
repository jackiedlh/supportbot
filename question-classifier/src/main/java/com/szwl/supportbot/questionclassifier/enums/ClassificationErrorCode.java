package com.szwl.supportbot.questionclassifier.enums;

import lombok.Getter;

/**
 * 问题分类错误码
 */
@Getter
public enum ClassificationErrorCode {
    
    SUCCESS(0, "成功"),
    
    // 常见错误
    FAILED(-1, "失败"),
    TIMEOUT(-2, "超时"),
    PARAM_ERROR(-3, "参数错误"),
    
    // AI相关错误
    AI_CALL_FAILED(-10, "AI调用失败"),
    AI_RESULT_EMPTY(-11, "AI返回结果为空"),
    AI_PARSE_ERROR(-12, "AI结果解析失败"),
    
    // 系统错误
    SYSTEM_ERROR(-100, "系统错误"),
    CONFIG_ERROR(-101, "配置错误");
    
    private final int code;
    private final String message;
    
    ClassificationErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
