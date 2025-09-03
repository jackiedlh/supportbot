package com.szwl.supportbot.imdemo.controller;

import com.szwl.supportbot.imdemo.model.AiResponseRequest;
import com.szwl.supportbot.imdemo.service.AiResponseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AI回复控制器
 * 供assistant、rag、直接问答三个模块调用
 */
@Slf4j
@RestController
@RequestMapping("/api/ai-response")
public class AiResponseController {

    @Autowired
    private AiResponseService aiResponseService;

    @Value("${server.port:11002}")
    private int serverPort;

    /**
     * 接收AI回复并发送给用户
     * @param request AI回复请求
     * @return 处理结果
     */
    @PostMapping("/send")
    public Map<String, Object> sendAiResponse(@RequestBody AiResponseRequest request) {
        log.info("收到AI回复: userId={}, source={}, content={}", 
                 request.getUserId(), request.getSource(), request.getContent());
        
        try {
            boolean success = aiResponseService.sendAiResponse(request);
            return Map.of(
                "success", success,
                "message", success ? "AI回复发送成功" : "AI回复发送失败"
            );
        } catch (Exception e) {
            log.error("发送AI回复失败: userId={}", request.getUserId(), e);
            return Map.of(
                "success", false,
                "message", "发送失败: " + e.getMessage()
            );
        }
    }

    /**
     * 健康检查接口
     * @return 服务状态
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "UP",
            "service", "im-demo",
            "port", serverPort,
            "protocol", "http"
        );
    }
}
