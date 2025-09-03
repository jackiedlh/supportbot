package com.szwl.supportbot.questionclassifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 提示词模板配置
 * 从Nacos配置中心获取，支持动态刷新
 */
@Slf4j
@Component
@RefreshScope
@ConfigurationProperties(prefix = "ai-prompts")
public class PromptTemplateConfig {

    /**
     * 问题分类提示词配置
     */
    private QuestionClassification questionClassification;

    /**
     * 统一分析提示词模板
     */
    private String unifiedAnalysisTemplate = "你是一个专业的客服问题分析助手...";

    /**
     * 分类提示词模板
     */
    private String classificationTemplate = "你是一个专业的客服问题分类助手...";

    /**
     * 信息提取提示词模板
     */
    private String extractionTemplate = "你是一个专业的信息提取助手...";

    @PostConstruct
    public void init() {
        log.info("=== PromptTemplateConfig 初始化开始 ===");
        log.info("配置前缀: ai-prompts");
        
        // 检查从Nacos加载的配置
        if (questionClassification != null) {
            log.info("✅ 成功加载 questionClassification 配置");
            log.info("系统提示词长度: {}", 
                questionClassification.getSystemPrompt() != null ? 
                questionClassification.getSystemPrompt().length() : "null");
            log.info("用户提示词模板长度: {}", 
                questionClassification.getUserPromptTemplate() != null ? 
                questionClassification.getUserPromptTemplate().length() : "null");
        } else {
            log.warn("❌ questionClassification 配置为 null");
        }
        
        // 检查硬编码的默认值
        log.info("硬编码默认值:");
        log.info("  unifiedAnalysisTemplate 长度: {}", unifiedAnalysisTemplate.length());
        log.info("  classificationTemplate 长度: {}", classificationTemplate.length());
        log.info("  extractionTemplate 长度: {}", extractionTemplate.length());
        
        log.info("=== PromptTemplateConfig 初始化完成 ===");
    }

    // 内部类，对应Nacos配置结构
    public static class QuestionClassification {
        private String systemPrompt;        // 对应 system-prompt
        private String userPromptTemplate;  // 对应 user-prompt-template
        private List<Example> examples;     // 对应 examples

        // Getters and Setters
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
        
        public String getUserPromptTemplate() { return userPromptTemplate; }
        public void setUserPromptTemplate(String userPromptTemplate) { this.userPromptTemplate = userPromptTemplate; }
        
        public List<Example> getExamples() { return examples; }
        public void setExamples(List<Example> examples) { this.examples = examples; }
    }

    // 示例数据结构
    public static class Example {
        private String userInput;           // 对应 user_input
        private String expectedOutput;      // 对应 expected_output

        // Getters and Setters
        public String getUserInput() { return userInput; }
        public void setUserInput(String userInput) { this.userInput = userInput; }
        
        public String getExpectedOutput() { return expectedOutput; }
        public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }
    }

    // Getters and Setters
    public QuestionClassification getQuestionClassification() {
        return questionClassification;
    }

    public void setQuestionClassification(QuestionClassification questionClassification) {
        this.questionClassification = questionClassification;
    }

    public String getUnifiedAnalysisTemplate() {
        return unifiedAnalysisTemplate;
    }

    public void setUnifiedAnalysisTemplate(String unifiedAnalysisTemplate) {
        this.unifiedAnalysisTemplate = unifiedAnalysisTemplate;
    }

    public String getClassificationTemplate() {
        return classificationTemplate;
    }

    public void setClassificationTemplate(String classificationTemplate) {
        this.classificationTemplate = classificationTemplate;
    }

    public String getExtractionTemplate() {
        return extractionTemplate;
    }

    public void setExtractionTemplate(String extractionTemplate) {
        this.extractionTemplate = extractionTemplate;
    }
}
