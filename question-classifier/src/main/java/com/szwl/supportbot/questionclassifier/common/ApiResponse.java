package com.szwl.supportbot.questionclassifier.common;

/**
 * API响应封装类
 */
public class ApiResponse {
    
    private final int code;
    private final String message;

    public ApiResponse(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    /**
     * 成功响应
     */
    public static ApiResponse success() {
        return new ApiResponse(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
    }

    /**
     * 失败响应
     */
    public static ApiResponse failure(ResultCode resultCode) {
        return new ApiResponse(resultCode.getCode(), resultCode.getMessage());
    }

    /**
     * 系统错误响应
     */
    public static ApiResponse systemError(String errorMessage) {
        return new ApiResponse(ResultCode.SYSTEM_ERROR.getCode(), errorMessage);
    }

    /**
     * 参数无效响应
     */
    public static ApiResponse invalidParameter() {
        return new ApiResponse(ResultCode.INVALID_PARAMETER.getCode(), ResultCode.INVALID_PARAMETER.getMessage());
    }
}
