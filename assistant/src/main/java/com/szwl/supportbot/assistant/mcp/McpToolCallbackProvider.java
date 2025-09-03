package com.szwl.supportbot.assistant.mcp;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.szwl.supportbot.assistant.config.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP工具回调提供者
 * 实现ToolCallbackProvider接口，基于MCP协议标准的工具发现和调用机制
 */
@Slf4j
public class McpToolCallbackProvider implements ToolCallbackProvider {

    private final WebClient webClient;
    private final String toolsResponse;
    private final Map<String, McpTool> toolsCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AgentConfig.ToolCallback toolCallback;
    private final AgentConfig.Connection connection;
    
    // MCP协议标准路径
    private static final String MCP_TOOLS_PATH = "/tools";

    public McpToolCallbackProvider(WebClient webClient, AgentConfig.Connection connection, String toolsResponse, AgentConfig.ToolCallback toolCallback) {
        this.webClient = webClient;
        this.connection = connection;
        this.toolsResponse = toolsResponse;
        this.toolCallback = toolCallback;
        initializeTools();
    }

    /**
     * 初始化工具列表
     * 基于MCP协议标准的工具发现机制
     */
    private void initializeTools() {
        try {
            log.info("初始化MCP工具，响应内容长度: {}", toolsResponse.length());
            
            // 解析MCP协议的工具列表响应
            parseMcpToolsResponse(toolsResponse);
            
            log.info("成功解析MCP工具，工具数量: {}", toolsCache.size());
            
        } catch (Exception e) {
            log.error("初始化MCP工具失败", e);
        }
    }

    /**
     * 解析MCP协议的工具列表响应
     * 基于MCP协议标准：https://modelcontextprotocol.io/
     */
    private void parseMcpToolsResponse(String response) {
        try {
            // 使用 Jackson 解析 JSON 响应
            JsonNode rootNode = objectMapper.readTree(response);
            
            // 解析MCP协议标准的JSON-RPC响应
            if (rootNode.has("result") && rootNode.get("result").has("tools")) {
                parseMcpProtocolTools(rootNode.get("result").get("tools"));
            } else if (rootNode.has("tools") && rootNode.get("tools").isArray()) {
                // 兼容直接返回tools数组的格式
                parseStandardMcpTools(rootNode);
            } else if (rootNode.has("data") && rootNode.get("data").isArray()) {
                // 解析可能的其他格式
                parseAlternativeFormat(rootNode);
            } else {
                // 尝试解析其他格式
                parseGenericTools(response);
            }
            
        } catch (Exception e) {
            log.error("解析MCP工具响应失败，尝试手动解析", e);
            // 降级到手动解析
            parseGenericTools(response);
        }
    }

    /**
     * 解析MCP协议标准的工具列表
     */
    private void parseMcpProtocolTools(JsonNode toolsArray) {
        try {
            for (JsonNode toolNode : toolsArray) {
                String name = toolNode.path("name").asText();
                String description = toolNode.path("description").asText();
                JsonNode inputSchema = toolNode.path("inputSchema");
                
                if (name != null && !name.trim().isEmpty()) {
                    Map<String, Object> schemaMap = inputSchema.isMissingNode() ? 
                        Map.of("type", "object", "properties", Map.of()) :
                        objectMapper.convertValue(inputSchema, new TypeReference<Map<String, Object>>() {});
                    
                    McpTool tool = new McpTool(name, description, schemaMap);
                    toolsCache.put(name, tool);
                    log.debug("解析到MCP协议工具: {} - {}", name, description);
                }
            }
        } catch (Exception e) {
            log.error("解析MCP协议工具失败", e);
        }
    }

    /**
     * 解析标准 MCP 格式的工具
     */
    private void parseStandardMcpTools(JsonNode rootNode) {
        try {
            JsonNode toolsArray = rootNode.get("tools");
            for (JsonNode toolNode : toolsArray) {
                String name = toolNode.path("name").asText();
                String description = toolNode.path("description").asText();
                JsonNode inputSchema = toolNode.path("inputSchema");
                
                if (name != null && !name.trim().isEmpty()) {
                    Map<String, Object> schemaMap = inputSchema.isMissingNode() ? 
                        Map.of("type", "object", "properties", Map.of()) :
                        objectMapper.convertValue(inputSchema, new TypeReference<Map<String, Object>>() {});
                    
                    McpTool tool = new McpTool(name, description, schemaMap);
                    toolsCache.put(name, tool);
                    log.debug("解析到工具: {} - {}", name, description);
                }
            }
        } catch (Exception e) {
            log.error("解析标准MCP工具失败", e);
        }
    }

    /**
     * 解析替代格式的工具
     */
    private void parseAlternativeFormat(JsonNode rootNode) {
        try {
            JsonNode dataArray = rootNode.get("data");
            for (JsonNode itemNode : dataArray) {
                String name = itemNode.path("name").asText();
                String description = itemNode.path("description").asText();
                
                if (name != null && !name.trim().isEmpty()) {
                    Map<String, Object> schemaMap = Map.of("type", "object", "properties", Map.of());
                    McpTool tool = new McpTool(name, description, schemaMap);
                    toolsCache.put(name, tool);
                    log.debug("解析到工具(替代格式): {} - {}", name, description);
                }
            }
        } catch (Exception e) {
            log.error("解析替代格式工具失败", e);
        }
    }

    /**
     * 解析通用格式的工具（降级方案）
     */
    private void parseGenericTools(String response) {
        try {
            if (response.contains("\"tools\"")) {
                parseToolsArray(response);
            } else if (response.contains("tools:")) {
                parseYamlTools(response);
            } else {
                // 尝试从响应中提取工具名称
                parseToolNamesFromResponse(response);
            }
        } catch (Exception e) {
            log.error("通用解析失败，创建默认工具", e);
            createDefaultTools();
        }
    }

    /**
     * 从响应中提取工具名称
     */
    private void parseToolNamesFromResponse(String response) {
        try {
            // 尝试查找可能的工具名称模式
            String[] lines = response.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.contains("name") || line.contains("tool")) {
                    parseToolFromLine(line);
                }
            }
            
            // 如果没有找到任何工具，创建默认工具
            if (toolsCache.isEmpty()) {
                createDefaultTools();
            }
        } catch (Exception e) {
            log.error("从响应提取工具名称失败", e);
            createDefaultTools();
        }
    }

    /**
     * 解析JSON格式的工具数组（降级方案）
     */
    private void parseToolsArray(String response) {
        try {
            // 查找工具数组的开始和结束位置
            int toolsStart = response.indexOf("\"tools\"");
            if (toolsStart == -1) return;
            
            int arrayStart = response.indexOf("[", toolsStart);
            if (arrayStart == -1) return;
            
            int arrayEnd = findMatchingBracket(response, arrayStart);
            if (arrayEnd == -1) return;
            
            String toolsSection = response.substring(arrayStart + 1, arrayEnd);
            parseToolDefinitions(toolsSection);
            
        } catch (Exception e) {
            log.error("解析工具数组失败", e);
        }
    }

    /**
     * 解析工具定义
     */
    private void parseToolDefinitions(String toolsSection) {
        try {
            String[] toolDefinitions = toolsSection.split("\\{");
            for (String toolDefinition : toolDefinitions) {
                if (toolDefinition.trim().isEmpty()) continue;
                
                String name = extractToolName(toolDefinition);
                String description = extractToolDescription(toolDefinition);
                
                if (name != null && !name.trim().isEmpty()) {
                    McpTool tool = new McpTool(name, description, 
                        Map.of("type", "object", "properties", Map.of()));
                    toolsCache.put(name, tool);
                }
            }
        } catch (Exception e) {
            log.error("解析工具定义失败", e);
        }
    }

    /**
     * 从工具定义中提取工具名称
     */
    private String extractToolName(String toolDefinition) {
        try {
            if (toolDefinition.contains("\"name\"")) {
                int nameStart = toolDefinition.indexOf("\"name\"");
                int valueStart = toolDefinition.indexOf("\"", nameStart + 6);
                if (valueStart != -1) {
                    int valueEnd = toolDefinition.indexOf("\"", valueStart + 1);
                    if (valueStart != -1 && valueEnd != -1) {
                        return toolDefinition.substring(valueStart + 1, valueEnd);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("提取工具名称失败", e);
            return null;
        }
    }

    /**
     * 从工具定义中提取工具描述
     */
    private String extractToolDescription(String toolDefinition) {
        try {
            if (toolDefinition.contains("\"description\"")) {
                int descStart = toolDefinition.indexOf("\"description\"");
                int valueStart = toolDefinition.indexOf("\"", descStart + 14);
                if (valueStart != -1) {
                    int valueEnd = toolDefinition.indexOf("\"", valueStart + 1);
                    if (valueStart != -1 && valueEnd != -1) {
                        return toolDefinition.substring(valueStart + 1, valueEnd);
                    }
                }
            }
            return "MCP工具";
        } catch (Exception e) {
            log.debug("提取工具描述失败", e);
            return "MCP工具";
        }
    }

    /**
     * 解析YAML工具定义（降级方案）
     */
    private void parseYamlTools(String response) {
        try {
            String[] lines = response.split("\n");
            for (String line : lines) {
                if (line.contains("name:")) {
                    String name = line.substring(line.indexOf("name:") + 5).trim();
                    if (!name.isEmpty()) {
                        McpTool tool = new McpTool(name, "MCP工具", 
                            Map.of("type", "object", "properties", Map.of()));
                        toolsCache.put(name, tool);
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析YAML工具失败", e);
        }
    }

    /**
     * 创建默认工具（最后的降级方案）
     */
    private void createDefaultTools() {
        try {
            log.warn("创建默认MCP工具");
            McpTool defaultTool = new McpTool("mcp_tool", "默认MCP工具", 
                Map.of("type", "object", "properties", Map.of()));
            toolsCache.put("mcp_tool", defaultTool);
        } catch (Exception e) {
            log.error("创建默认工具失败", e);
        }
    }

    /**
     * 查找匹配的括号
     */
    private int findMatchingBracket(String text, int startIndex) {
        int count = 0;
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '[') count++;
            else if (c == ']') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    /**
     * 从行中解析工具信息
     */
    private void parseToolFromLine(String line) {
        try {
            if (line.contains("name")) {
                String name = line.substring(line.indexOf("name") + 4).trim();
                if (name.startsWith(":")) {
                    name = name.substring(1).trim();
                }
                if (!name.isEmpty()) {
                    McpTool tool = new McpTool(name, "MCP工具", 
                        Map.of("type", "object", "properties", Map.of()));
                    toolsCache.put(name, tool);
                }
            }
        } catch (Exception e) {
            log.debug("从行解析工具失败: {}", line, e);
        }
    }

    @Override
    public ToolCallback[] getToolCallbacks() {
        if (toolsCache.isEmpty()) {
            log.warn("没有可用的MCP工具");
            return new ToolCallback[0];
        }

        return toolsCache.values().stream()
            .map(this::createToolCallback)
            .toArray(ToolCallback[]::new);
    }

    /**
     * 创建工具回调
     */
    private ToolCallback createToolCallback(McpTool tool) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                try {
                    return ToolDefinition.builder()
                        .name(tool.getName())
                        .description(tool.getDescription())
                        .inputSchema(objectMapper.writeValueAsString(tool.getInputSchema()))
                        .build();
                } catch (Exception e) {
                    log.error("构建工具定义失败: {}", tool.getName(), e);
                    // 降级到简单格式
                    return ToolDefinition.builder()
                        .name(tool.getName())
                        .description(tool.getDescription())
                        .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                        .build();
                }
            }

            @Override
            public String call(String arguments) {
                try {
                    log.info("调用MCP工具: {}, 参数: {}", tool.getName(), arguments);
                    
                    // 调用MCP服务器执行工具
                    Map<String, Object> argsMap = parseArguments(arguments);
                    Object result = executeMcpTool(tool.getName(), argsMap);
                    
                    if (result instanceof String) {
                        return (String) result;
                    } else {
                        return objectMapper.writeValueAsString(result);
                    }
                    
                } catch (Exception e) {
                    log.error("执行MCP工具失败: {}", tool.getName(), e);
                    return "{\"error\":\"" + e.getMessage() + "\"}";
                }
            }

            @Override
            public String call(String toolArguments, org.springframework.ai.chat.model.ToolContext toolContext) {
                return this.call(toolArguments);
            }
        };
    }

    /**
     * 解析参数JSON字符串为Map
     */
    private Map<String, Object> parseArguments(String arguments) {
        try {
            if (arguments == null || arguments.trim().isEmpty()) {
                return Map.of();
            }
            
            // 使用 Jackson 解析 JSON 参数
            return objectMapper.readValue(arguments, new TypeReference<Map<String, Object>>() {});
            
        } catch (Exception e) {
            log.warn("解析参数失败: {}", arguments, e);
            // 尝试解析简单的键值对格式
            return parseSimpleArguments(arguments);
        }
    }

    /**
     * 解析简单参数格式（降级方案）
     */
    private Map<String, Object> parseSimpleArguments(String arguments) {
        try {
            Map<String, Object> result = new ConcurrentHashMap<>();
            String[] pairs = arguments.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim().replaceAll("[\"{}]", "");
                    String value = kv[1].trim().replaceAll("[\"{}]", "");
                    result.put(key, value);
                }
            }
            return result;
        } catch (Exception e) {
            log.warn("解析简单参数失败: {}", arguments, e);
            return Map.of();
        }
    }

    /**
     * 执行MCP工具
     * 使用MCP协议标准的callTool方法
     */
    private Object executeMcpTool(String toolName, Map<String, Object> arguments) {
        try {
            // 构建MCP协议标准的callTool请求
            Map<String, Object> callToolRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "call-tool-" + System.currentTimeMillis(),
                "method", "tools/call",
                "params", Map.of(
                    "name", toolName,
                    "arguments", arguments
                )
            );

            log.debug("发送MCP callTool请求: {}", callToolRequest);

            // 构建正确的MCP协议URL
            String baseUrl = connection.getUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            String mcpCallToolUrl = baseUrl + MCP_TOOLS_PATH;

            log.debug("调用MCP工具，URL: {}", mcpCallToolUrl);

            Object result = webClient.post()
                .uri(MCP_TOOLS_PATH)
                .bodyValue(callToolRequest)
                .retrieve()
                .bodyToMono(Object.class)
                .block();

            log.debug("MCP工具调用成功: {} -> {}", toolName, result);
            return result;

        } catch (Exception e) {
            log.error("调用MCP工具失败: {}", toolName, e);
            return Map.of("error", "MCP工具调用失败: " + e.getMessage());
        }
    }

    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return toolsCache.size();
    }

    /**
     * 获取工具名称列表
     */
    public List<String> getToolNames() {
        return List.copyOf(toolsCache.keySet());
    }

    /**
     * 获取工具缓存信息
     */
    public Map<String, McpTool> getToolsCache() {
        return new ConcurrentHashMap<>(toolsCache);
    }

    /**
     * MCP工具内部类
     */
    private static class McpTool {
        private final String name;
        private final String description;
        private final Map<String, Object> inputSchema;

        public McpTool(String name, String description, Map<String, Object> inputSchema) {
            this.name = name;
            this.description = description;
            this.inputSchema = inputSchema;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Map<String, Object> getInputSchema() {
            return inputSchema;
        }
    }
}

