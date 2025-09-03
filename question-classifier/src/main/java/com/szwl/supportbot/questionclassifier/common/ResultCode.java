package com.szwl.supportbot.questionclassifier.common;

/**
 * 结果代码枚举
 */
public enum ResultCode {
    SUCCESS(0, "成功"),
    INVALID_PARAMETER(-100, "参数无效"),
    AI_MODEL_ERROR(-200, "AI模型调用失败"),
    REDIS_ERROR(-300, "Redis操作失败"),
    ROCKETMQ_ERROR(-400, "消息队列操作失败"),
    SYSTEM_ERROR(-500, "系统内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
