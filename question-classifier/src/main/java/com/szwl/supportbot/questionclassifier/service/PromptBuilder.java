package com.szwl.supportbot.questionclassifier.service;

import com.szwl.supportbot.questionclassifier.config.PromptTemplateConfig;
import com.szwl.supportbot.questionclassifier.entity.QuestionClassificationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 提示词构建器
 * 专门负责构建AI分类的提示词
 */
@Slf4j
@Component
public class PromptBuilder {

    private final PromptTemplateConfig promptTemplateConfig;
    private final BeanOutputConverter<QuestionClassificationResult> resultConverter;
    private final String resultFormat;

    public PromptBuilder(PromptTemplateConfig promptTemplateConfig) {
        this.promptTemplateConfig = promptTemplateConfig;
        
        // 初始化结构化输出转换器
        this.resultConverter = new BeanOutputConverter<>(
            new ParameterizedTypeReference<QuestionClassificationResult>() {}
        );
        this.resultFormat = resultConverter.getFormat();
        log.info("结构化输出格式: {}", resultFormat);
    }

    /**
     * 构建分类提示词
     */
    public String buildClassificationPrompt(String question, String context) {
        // 获取Nacos配置的提示词
        var questionConfig = promptTemplateConfig.getQuestionClassification();
        
        if (questionConfig != null && questionConfig.getSystemPrompt() != null) {
            // 使用Nacos配置的提示词
            String systemPrompt = questionConfig.getSystemPrompt();
            
            // 如果有示例，添加到系统提示词中
            if (questionConfig.getExamples() != null && !questionConfig.getExamples().isEmpty()) {
                systemPrompt += "\n\n示例：\n" + formatExamples(questionConfig.getExamples());
            }
            
            // 构建用户提示词，支持聊天记录上下文
            String userPrompt = questionConfig.getUserPromptTemplate() != null ? 
                questionConfig.getUserPromptTemplate()
                    .replace("{user_input}", question)
                    .replace("{conversation_context}", context != null && !context.trim().isEmpty() ? context : "无历史对话记录") :
                buildDefaultUserPrompt(question, context);
            
            // 组合系统提示词和用户提示词，并添加结构化输出格式要求
            String formatRequirement = """
                
                请按照以下JSON格式输出结果，不要包含任何多余的文字：
                {format}
                """;
            
            return systemPrompt + "\n\n" + userPrompt + formatRequirement;
            
        } else {
            // 回退到硬编码的模板
            log.warn("Nacos提示词配置未加载，使用硬编码模板");
            String baseTemplate = promptTemplateConfig.getUnifiedAnalysisTemplate()
                .replace("{context}", context)
                .replace("{question}", question);
            
            // 添加结构化输出格式要求
            String formatRequirement = """
                
                请按照以下JSON格式输出结果，不要包含任何多余的文字：
                {format}
                """;
            
            return baseTemplate + formatRequirement;
        }
    }

    /**
     * 构建默认的用户提示词（当Nacos配置不可用时）
     */
    private String buildDefaultUserPrompt(String question, String context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请对以下用户输入进行分类和信息提取：\n\n");
        
        // 如果有聊天记录上下文，添加到提示词中
        if (context != null && !context.trim().isEmpty()) {
            prompt.append("=== 对话历史上下文 ===\n");
            prompt.append(context).append("\n");
            prompt.append("=== 当前问题 ===\n");
            prompt.append(question).append("\n\n");
            prompt.append("请结合对话历史上下文，对当前问题进行分类和信息提取。");
        } else {
            prompt.append(question).append("\n\n");
            prompt.append("请对当前问题进行分类和信息提取。");
        }
        
        return prompt.toString();
    }

    /**
     * 格式化示例数据
     */
    private String formatExamples(List<PromptTemplateConfig.Example> examples) {
        if (examples == null || examples.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < examples.size(); i++) {
            PromptTemplateConfig.Example example = examples.get(i);
            sb.append("示例").append(i + 1).append(":\n");
            sb.append("用户输入: ").append(example.getUserInput()).append("\n");
            sb.append("期望输出: ").append(example.getExpectedOutput()).append("\n\n");
        }
        return sb.toString();
    }
}
