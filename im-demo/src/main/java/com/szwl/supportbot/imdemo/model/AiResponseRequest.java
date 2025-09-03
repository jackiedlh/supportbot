package com.szwl.supportbot.imdemo.model;

import lombok.Data;

/**
 * AI回复请求模型
 * 供assistant、rag、直接问答三个模块调用
 */
@Data
public class AiResponseRequest {
    
    private Long userId;             // 用户ID
    private String content;          // AI回复内容
    private String source;           // 来源模块（assistant/rag/general-chat）
    
    public AiResponseRequest() {}
    
    public AiResponseRequest(Long userId, String content, String source) {
        this.userId = userId;
        this.content = content;
        this.source = source;
    }
}
