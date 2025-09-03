package com.szwl.supportbot.assistant.mcp;

import com.szwl.supportbot.assistant.config.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 工具提供者服务
 * 专门负责根据配置获取和管理不同类型的工具
 * 支持MCP工具和默认工具的动态切换
 */
@Slf4j
@Service
public class ToolProviderService {

    @Autowired
    private McpClientFactory mcpClientFactory;

    /**
     * 根据MCP配置获取对应的工具
     */
    public ToolCallbackProvider getToolsForConfig(AgentConfig.McpConfig mcpConfig) {
        if (mcpConfig == null || mcpConfig.getClient() == null) {
            log.debug("使用默认工具配置");
            return null;
        }
        
        try {
            // 使用动态MCP客户端工厂创建MCP客户端
            ToolCallbackProvider mcpTools = mcpClientFactory.getOrCreateMcpClient(mcpConfig);
            
            if (mcpTools != null && mcpTools.getToolCallbacks().length > 0) {
                log.info("成功获取MCP工具，工具数量: {}", mcpTools.getToolCallbacks().length);
                return mcpTools;
            } else {
                log.warn("无法获取MCP工具");
                return null;
            }
            
        } catch (Exception e) {
            log.error("获取MCP工具失败", e);
            return null;
        }
    }

    /**
     * 获取默认工具
     */
    public ToolCallbackProvider getDefaultTools() {
        // 返回系统默认工具（如文件操作、时间查询等基础工具）
        log.debug("返回默认工具");
        return null; // 当前未配置默认工具
    }

    /**
     * 获取工具统计信息
     */
    public ToolStats getToolStats(AgentConfig.McpConfig mcpConfig) {
        ToolStats stats = new ToolStats();
        
        try {
            ToolCallbackProvider mcpTools = getToolsForConfig(mcpConfig);
            if (mcpTools != null) {
                stats.setMcpToolCount(mcpTools.getToolCallbacks().length);
                stats.setMcpToolsAvailable(true);
            }
            
            ToolCallbackProvider defaultTools = getDefaultTools();
            if (defaultTools != null) {
                stats.setDefaultToolCount(defaultTools.getToolCallbacks().length);
                stats.setDefaultToolsAvailable(true);
            }
            
        } catch (Exception e) {
            log.error("获取工具统计信息失败", e);
        }
        
        return stats;
    }

    /**
     * 工具统计信息
     */
    public static class ToolStats {
        private int mcpToolCount = 0;
        private int defaultToolCount = 0;
        private boolean mcpToolsAvailable = false;
        private boolean defaultToolsAvailable = false;

        // Getters and Setters
        public int getMcpToolCount() { return mcpToolCount; }
        public void setMcpToolCount(int mcpToolCount) { this.mcpToolCount = mcpToolCount; }
        
        public int getDefaultToolCount() { return defaultToolCount; }
        public void setDefaultToolCount(int defaultToolCount) { this.defaultToolCount = defaultToolCount; }
        
        public boolean isMcpToolsAvailable() { return mcpToolsAvailable; }
        public void setMcpToolsAvailable(boolean mcpToolsAvailable) { this.mcpToolsAvailable = mcpToolsAvailable; }
        
        public boolean isDefaultToolsAvailable() { return defaultToolsAvailable; }
        public void setDefaultToolsAvailable(boolean defaultToolsAvailable) { this.defaultToolsAvailable = defaultToolsAvailable; }
        
        public int getTotalToolCount() { return mcpToolCount + defaultToolCount; }
        
        @Override
        public String toString() {
            return String.format("ToolStats{mcpTools=%d, defaultTools=%d, total=%d, mcpAvailable=%s, defaultAvailable=%s}", 
                mcpToolCount, defaultToolCount, getTotalToolCount(), mcpToolsAvailable, defaultToolsAvailable);
        }
    }
}
