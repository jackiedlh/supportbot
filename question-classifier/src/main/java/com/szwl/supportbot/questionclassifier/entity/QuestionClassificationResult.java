package com.szwl.supportbot.questionclassifier.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 问题分类结果的结构化输出实体
 */
public class QuestionClassificationResult {

    @JsonProperty("questions")
    private List<QuestionItem> questions;

    @JsonProperty("total_questions")
    private Integer totalQuestions;

    public QuestionClassificationResult() {
    }

    public List<QuestionItem> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionItem> questions) {
        this.questions = questions;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    /**
     * 单个问题项
     */
    public static class QuestionItem {
        @JsonProperty("id")
        private String id;

        @JsonProperty("original_text")
        private String originalText;

        @JsonProperty("category")
        private String category;

        @JsonProperty("extracted_info")
        private Map<String, String> extractedInfo;

        @JsonProperty("confidence")
        private Double confidence;

        public QuestionItem() {
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getOriginalText() { return originalText; }
        public void setOriginalText(String originalText) { this.originalText = originalText; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public Map<String, String> getExtractedInfo() { return extractedInfo; }
        public void setExtractedInfo(Map<String, String> extractedInfo) { this.extractedInfo = extractedInfo; }

        public Double getConfidence() { return confidence; }
        public void setConfidence(Double confidence) { this.confidence = confidence; }
    }
}
