package com.szwl.supportbot.assistant.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

/**
 * 配置解析器
 * 用于解析从Nacos获取的YAML配置
 */
@Slf4j
@Component
public class ConfigParser {

    private final Yaml yaml = new Yaml();

    /**
     * 解析YAML配置内容
     * 
     * @param yamlContent YAML配置内容
     * @return 解析后的配置Map
     */
    public Map<String, Object> parseYaml(String yamlContent) {
        try {
            if (yamlContent == null || yamlContent.trim().isEmpty()) {
                log.warn("Empty YAML content provided");
                return Map.of();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> config = yaml.load(yamlContent);
            log.debug("Successfully parsed YAML config with {} top-level keys", 
                     config != null ? config.size() : 0);
            return config != null ? config : Map.of();

        } catch (Exception e) {
            log.error("Failed to parse YAML content", e);
            return Map.of("error", "Failed to parse YAML: " + e.getMessage());
        }
    }

    /**
     * 从配置中提取系统提示词
     * 
     * @param config 解析后的配置
     * @return 系统提示词
     */
    public String extractSystemPrompt(Map<String, Object> config) {
        try {
            if (config == null || config.isEmpty()) {
                return null;
            }

            // 支持多种配置结构
            Object systemPrompt = config.get("systemPrompt");
            if (systemPrompt != null) {
                return systemPrompt.toString();
            }

            // 尝试从嵌套结构中获取
            @SuppressWarnings("unchecked")
            Map<String, Object> agentConfig = (Map<String, Object>) config.get("agent");
            if (agentConfig != null) {
                systemPrompt = agentConfig.get("systemPrompt");
                if (systemPrompt != null) {
                    return systemPrompt.toString();
                }
            }

            log.debug("No systemPrompt found in config");
            return null;

        } catch (Exception e) {
            log.error("Error extracting system prompt from config", e);
            return null;
        }
    }

    /**
     * 从配置中提取MCP配置
     * 
     * @param config 解析后的配置
     * @return MCP配置
     */
    public Map<String, Object> extractMcpConfig(Map<String, Object> config) {
        try {
            if (config == null || config.isEmpty()) {
                return Map.of();
            }

            // 支持多种配置结构
            Object mcpConfig = config.get("mcp");
            if (mcpConfig instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> mcp = (Map<String, Object>) mcpConfig;
                return mcp;
            }

            // 尝试从嵌套结构中获取
            @SuppressWarnings("unchecked")
            Map<String, Object> agentConfig = (Map<String, Object>) config.get("agent");
            if (agentConfig != null) {
                mcpConfig = agentConfig.get("mcp");
                if (mcpConfig instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> mcp = (Map<String, Object>) mcpConfig;
                    return mcp;
                }
            }

            log.debug("No MCP config found in config");
            return Map.of();

        } catch (Exception e) {
            log.error("Error extracting MCP config from config", e);
            return Map.of();
        }
    }

    /**
     * 验证配置是否有效
     * 
     * @param config 解析后的配置
     * @return 是否有效
     */
    public boolean isValidConfig(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return false;
        }

        // 检查是否包含必要的配置项
        String systemPrompt = extractSystemPrompt(config);
        Map<String, Object> mcpConfig = extractMcpConfig(config);

        boolean isValid = systemPrompt != null && !systemPrompt.trim().isEmpty();
        
        if (!isValid) {
            log.warn("Config validation failed: missing or empty systemPrompt");
        }

        return isValid;
    }
}
