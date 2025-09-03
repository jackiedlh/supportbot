package com.szwl.supportbot.questionclassifier.entity;

import com.szwl.supportbot.questionclassifier.enums.ClassificationErrorCode;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 分类结果返回类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClassificationResult {
    
    /**
     * 错误码：0表示成功，负数表示失败
     */
    private int code;
    
    /**
     * 错误原因描述
     */
    private String message;
    
    /**
     * 成功结果
     */
    public static ClassificationResult success() {
        return new ClassificationResult(
            ClassificationErrorCode.SUCCESS.getCode(),
            ClassificationErrorCode.SUCCESS.getMessage()
        );
    }
    
    /**
     * 失败结果
     */
    public static ClassificationResult failure(int code, String message) {
        return new ClassificationResult(code, message);
    }
    
    /**
     * 失败结果（使用枚举）
     */
    public static ClassificationResult failure(ClassificationErrorCode errorCode) {
        return new ClassificationResult(
            errorCode.getCode(),
            errorCode.getMessage()
        );
    }
}
