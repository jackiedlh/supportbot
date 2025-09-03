package com.szwl.supportbot.questionclassifier.controller;


import com.szwl.supportbot.questionclassifier.entity.ClassificationResult;
import com.szwl.supportbot.questionclassifier.service.QuestionClassifierService;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 问题分类控制器
 */
@RestController
@RequestMapping("/api")
public class QuestionController {

    private final QuestionClassifierService questionClassifierService;

    public QuestionController(QuestionClassifierService questionClassifierService) {
        this.questionClassifierService = questionClassifierService;
    }


    /**
     * 问题分类（同步处理，返回分类状态）
     */
    @GetMapping("/classify")
    public ResponseEntity<Map<String, Object>> classifyQuestion(
            @RequestParam String question,
            @RequestParam String uid) {
        // 参数验证
        if (!StringUtils.hasText(question) || !StringUtils.hasText(uid)) {
            return ResponseEntity.badRequest().body(Map.of(
                "code", 400,
                "message", "问题内容和用户ID不能为空"
            ));
        }

        try {
            // 调用问题分类服务，使用uid作为sessionId
            ClassificationResult result = questionClassifierService.classifyQuestion(question, uid);
            
            // 构建响应数据
            Map<String, Object> responseData = Map.of(
                "code", result.getCode(),
                "message", result.getMessage()
            );
            
            return ResponseEntity.ok(responseData);
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "code", 500,
                "message", "分类失败: " + e.getMessage()
            ));
        }
    }
}
