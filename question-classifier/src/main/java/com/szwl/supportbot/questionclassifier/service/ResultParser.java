package com.szwl.supportbot.questionclassifier.service;

import com.szwl.supportbot.questionclassifier.entity.QuestionClassificationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * 结果解析器
 * 专门负责解析AI返回的结果
 */
@Slf4j
@Component
public class ResultParser {

    private final BeanOutputConverter<QuestionClassificationResult> resultConverter;

    public ResultParser() {
        // 初始化结构化输出转换器
        this.resultConverter = new BeanOutputConverter<>(
            new ParameterizedTypeReference<QuestionClassificationResult>() {}
        );
    }

    /**
     * 解析AI返回的结果
     */
    public QuestionClassificationResult parseResult(String result) {
        try {
            // 使用 Spring AI 的结构化输出转换器
            QuestionClassificationResult classificationResult = resultConverter.convert(result);
            log.info("结构化输出解析成功: {}", classificationResult);
            return classificationResult;
            
        } catch (Exception e) {
            log.warn("结构化输出解析失败: {}, 原始结果: {}", e.getMessage(), result);
            return null;
        }
    }
}
