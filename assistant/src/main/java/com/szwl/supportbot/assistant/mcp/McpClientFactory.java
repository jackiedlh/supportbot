package com.szwl.supportbot.assistant.mcp;

import com.szwl.supportbot.assistant.config.AgentConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.ArrayList;
import java.util.List;

/**
 * MCP客户端工厂服务
 * 根据配置动态创建MCP客户端连接，实现正确的MCP协议
 */
@Slf4j
@Service
public class McpClientFactory {

    // WebClient缓存 - 只缓存连接，不缓存工具列表
    private final Map<String, WebClient> webClientCache = new ConcurrentHashMap<>();
    
    // 连接失败计数，用于重试逻辑
    private final Map<String, AtomicInteger> connectionFailureCount = new ConcurrentHashMap<>();
    
    // 最大重试次数
    private static final int MAX_RETRY_ATTEMPTS = 3;
    
    // 重试延迟（毫秒）
    private static final long RETRY_DELAY_MS = 1000;
    
    // MCP协议标准路径
    private static final String MCP_TOOLS_PATH = "/tools";
    private static final String MCP_TOOLS_LIST_PATH = "/tools";
    private static final String MCP_TOOLS_CALL_PATH = "/tools";

    /**
     * 根据MCP配置获取或创建ToolCallbackProvider
     * 连接可以缓存，但每次都重新拉取工具列表
     */
    public ToolCallbackProvider getOrCreateMcpClient(AgentConfig.McpConfig mcpConfig) {
        if (mcpConfig == null || mcpConfig.getClient() == null) {
            log.debug("MCP配置为空，返回null");
            return null;
        }

        // 每次都重新创建ToolCallbackProvider，但复用WebClient连接
        return createMcpClient(mcpConfig);
    }

    /**
     * 创建MCP客户端
     * 支持多个MCP服务器连接，聚合所有服务器的工具
     */
    private ToolCallbackProvider createMcpClient(AgentConfig.McpConfig mcpConfig) {
        try {
            if (mcpConfig.getClient() == null) {
                log.warn("MCP配置中缺少client信息");
                return null;
            }
            
            // 检查toolcallback配置
            AgentConfig.ToolCallback toolCallback = mcpConfig.getClient().getToolcallback();
            if (toolCallback != null && !toolCallback.isEnabled()) {
                log.info("toolcallback已禁用，跳过MCP客户端创建");
                return null;
            }
            
            if (mcpConfig.getClient().getSse() == null || 
                mcpConfig.getClient().getSse().getConnections() == null || 
                mcpConfig.getClient().getSse().getConnections().isEmpty()) {
                log.warn("MCP配置中缺少SSE连接信息");
                return null;
            }

            // 获取所有连接配置
            Map<String, AgentConfig.Connection> connections = mcpConfig.getClient().getSse().getConnections();
            log.info("发现 {} 个MCP服务器连接", connections.size());
            
            // 记录toolcallback配置信息
            if (toolCallback != null) {
                log.info("toolcallback配置: enabled={}, options={}", 
                        toolCallback.isEnabled(), toolCallback.getOptions());
            }

            // 存储所有服务器的工具提供者
            List<ToolCallbackProvider> allToolProviders = new ArrayList<>();
            
            // 遍历所有连接，创建对应的MCP客户端
            for (Map.Entry<String, AgentConfig.Connection> entry : connections.entrySet()) {
                String connectionName = entry.getKey();
                AgentConfig.Connection connection = entry.getValue();
                
                try {
                    log.info("处理MCP连接: {}, URL: {}", connectionName, connection.getUrl());
                    
                    // 创建WebClient
                    WebClient webClient = getOrCreateWebClient(connection);
                    
                    // 创建MCP工具提供者，传入toolcallback配置
                    ToolCallbackProvider toolProvider = createMcpToolProvider(webClient, connection, toolCallback);
                    
                    if (toolProvider != null && toolProvider.getToolCallbacks().length > 0) {
                        allToolProviders.add(toolProvider);
                        log.info("成功创建MCP连接 {} 的工具提供者，工具数量: {}", 
                                connectionName, toolProvider.getToolCallbacks().length);
                    } else {
                        log.warn("MCP连接 {} 未提供有效工具", connectionName);
                    }
                    
                } catch (Exception e) {
                    log.error("处理MCP连接 {} 失败", connectionName, e);
                    // 继续处理其他连接，不中断整个流程
                }
            }
            
            if (allToolProviders.isEmpty()) {
                log.warn("所有MCP连接都失败，无法创建工具提供者");
                return null;
            }
            
            // 如果只有一个工具提供者，直接返回
            if (allToolProviders.size() == 1) {
                return allToolProviders.get(0);
            }
            
            // 如果有多个工具提供者，创建聚合的工具提供者
            log.info("创建聚合工具提供者，包含 {} 个MCP服务器的工具", allToolProviders.size());
            return createAggregatedToolProvider(allToolProviders);
            
        } catch (Exception e) {
            log.error("创建MCP客户端失败", e);
            return null;
        }
    }

    /**
     * 获取或创建WebClient
     */
    private WebClient getOrCreateWebClient(AgentConfig.Connection connection) {
        String cacheKey = generateWebClientCacheKey(connection);
        
        return webClientCache.computeIfAbsent(cacheKey, k -> WebClient.builder()
                .baseUrl(connection.getUrl())
                .defaultHeader("Authorization", "Bearer " + connection.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json") // 改为JSON格式
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
                .build());
    }

    /**
     * 创建MCP工具提供者
     */
    private ToolCallbackProvider createMcpToolProvider(WebClient webClient, AgentConfig.Connection connection, AgentConfig.ToolCallback toolCallback) {
        String connectionKey = connection.getUrl();
        AtomicInteger failureCount = connectionFailureCount.computeIfAbsent(connectionKey, k -> new AtomicInteger(0));
        
        try {
            // 构建正确的MCP协议路径
            String baseUrl = connection.getUrl();
            String mcpToolsUrl = buildMcpUrl(baseUrl, MCP_TOOLS_LIST_PATH);
            
            log.debug("获取MCP工具列表，URL: {}", mcpToolsUrl);
            
            // 发送MCP协议标准的listTools请求
            String toolsResponse = sendMcpListToolsRequest(webClient, mcpToolsUrl);
            
            if (toolsResponse == null || toolsResponse.trim().isEmpty()) {
                log.warn("MCP服务器未返回工具列表");
                failureCount.incrementAndGet();
                return null;
            }
            
            log.debug("MCP服务器返回工具列表，响应长度: {}", toolsResponse.length());
            failureCount.set(0); // Reset on success
            return createToolCallbackProviderFromResponse(toolsResponse, webClient, connection, toolCallback);
            
        } catch (WebClientResponseException e) {
            failureCount.incrementAndGet();
            log.error("MCP服务器HTTP错误: {} - {}", e.getStatusCode(), e.getStatusText());
            log.debug("尝试的URL: {}", buildMcpUrl(connection.getUrl(), MCP_TOOLS_LIST_PATH));
            handleConnectionFailure(connectionKey, failureCount.get());
            return null;
        } catch (Exception e) {
            failureCount.incrementAndGet();
            log.error("连接MCP服务器失败: {}", connection.getUrl(), e);
            log.debug("尝试的URL: {}", buildMcpUrl(connection.getUrl(), MCP_TOOLS_LIST_PATH));
            handleConnectionFailure(connectionKey, failureCount.get());
            return null;
        }
    }

    /**
     * 构建MCP协议URL
     */
    private String buildMcpUrl(String baseUrl, String path) {
        // 确保baseUrl不以斜杠结尾
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        
        // 拼接MCP协议路径
        return baseUrl + path;
    }

    /**
     * 发送MCP协议标准的listTools请求
     */
    private String sendMcpListToolsRequest(WebClient webClient, String mcpToolsUrl) {
        try {
            // 构建MCP协议标准的listTools请求
            Map<String, Object> listToolsRequest = Map.of(
                "jsonrpc", "2.0",
                "id", "list-tools-" + System.currentTimeMillis(),
                "method", "tools/list",
                "params", Map.of()
            );
            
            log.debug("发送MCP listTools请求: {}", listToolsRequest);
            
            String response = webClient.post()
                .uri(mcpToolsUrl)
                .bodyValue(listToolsRequest)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .retryWhen(reactor.util.retry.Retry.fixedDelay(MAX_RETRY_ATTEMPTS, Duration.ofMillis(RETRY_DELAY_MS))
                    .filter(throwable -> {
                        if (throwable instanceof WebClientResponseException) {
                            WebClientResponseException wcre = (WebClientResponseException) throwable;
                            return wcre.getStatusCode().is5xxServerError() || wcre.getStatusCode().is4xxClientError();
                        }
                        return throwable instanceof java.net.ConnectException || 
                               throwable instanceof java.net.SocketTimeoutException;
                    }))
                .block();
            
            log.debug("MCP listTools响应: {}", response);
            return response;
            
        } catch (Exception e) {
            log.error("发送MCP listTools请求失败", e);
            throw e;
        }
    }

    /**
     * 从响应创建工具回调提供者
     */
    private ToolCallbackProvider createToolCallbackProviderFromResponse(String toolsResponse, WebClient webClient, AgentConfig.Connection connection, AgentConfig.ToolCallback toolCallback) {
        try {
            return new McpToolCallbackProvider(webClient, connection, toolsResponse, toolCallback);
        } catch (Exception e) {
            log.error("创建工具回调提供者失败", e);
            return null;
        }
    }

    /**
     * 创建聚合工具提供者
     */
    private ToolCallbackProvider createAggregatedToolProvider(List<ToolCallbackProvider> toolProviders) {
        try {
            // 计算总工具数量
            int totalToolCount = 0;
            for (ToolCallbackProvider provider : toolProviders) {
                if (provider != null && provider.getToolCallbacks() != null) {
                    totalToolCount += provider.getToolCallbacks().length;
                }
            }
            
            if (totalToolCount == 0) {
                log.warn("没有可用的工具");
                return null;
            }
            
            // 创建聚合的工具回调数组
            org.springframework.ai.tool.ToolCallback[] allToolCallbacks = new org.springframework.ai.tool.ToolCallback[totalToolCount];
            int currentIndex = 0;
            
            for (ToolCallbackProvider provider : toolProviders) {
                if (provider != null && provider.getToolCallbacks() != null) {
                    org.springframework.ai.tool.ToolCallback[] tools = provider.getToolCallbacks();
                    System.arraycopy(tools, 0, allToolCallbacks, currentIndex, tools.length);
                    currentIndex += tools.length;
                }
            }
            
            log.info("成功聚合 {} 个MCP服务器的工具，总工具数量: {}", toolProviders.size(), totalToolCount);
            
            // 创建新的聚合工具提供者
            return new org.springframework.ai.tool.ToolCallbackProvider() {
                @Override
                public org.springframework.ai.tool.ToolCallback[] getToolCallbacks() {
                    return allToolCallbacks;
                }
            };
            
        } catch (Exception e) {
            log.error("创建聚合工具提供者失败", e);
            return null;
        }
    }

    /**
     * 生成WebClient缓存键
     */
    private String generateWebClientCacheKey(AgentConfig.Connection connection) {
        try {
            return connection.getUrl() + "_" + connection.getApiKey();
        } catch (Exception e) {
            log.error("生成WebClient缓存key失败", e);
            return "default_" + System.currentTimeMillis();
        }
    }

    /**
     * 处理连接失败
     */
    private void handleConnectionFailure(String connectionKey, int failureCount) {
        if (failureCount >= MAX_RETRY_ATTEMPTS) {
            log.error("连接失败次数过多，清理连接: {}", connectionKey);
            cleanupFailedConnection(connectionKey);
        }
    }

    /**
     * 清理失败的连接
     */
    private void cleanupFailedConnection(String connectionKey) {
        try {
            // 清理相关的缓存
            webClientCache.remove(connectionKey);
            connectionFailureCount.remove(connectionKey);
            log.info("已清理失败的连接: {}", connectionKey);
        } catch (Exception e) {
            log.error("清理失败连接时出错: {}", connectionKey, e);
        }
    }

    /**
     * 清理缓存
     */
    public void clearCache() {
        webClientCache.clear();
        connectionFailureCount.clear();
        log.info("MCP客户端缓存已清理");
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("webClientCount", webClientCache.size());
        stats.put("connectionFailureCount", connectionFailureCount.size());
        stats.put("webClientKeys", webClientCache.keySet());
        stats.put("failedConnections", connectionFailureCount.entrySet().stream()
            .filter(entry -> entry.getValue().get() > 0)
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().get()
            )));
        return stats;
    }

    /**
     * 健康检查
     */
    public boolean isHealthy() {
        try {
            return !webClientCache.isEmpty();
        } catch (Exception e) {
            log.error("健康检查失败", e);
            return false;
        }
    }

    /**
     * 刷新连接
     */
    public void refreshConnection(String connectionKey) {
        try {
            log.info("刷新连接: {}", connectionKey);
            cleanupFailedConnection(connectionKey);
        } catch (Exception e) {
            log.error("刷新连接失败: {}", connectionKey, e);
        }
    }
}
