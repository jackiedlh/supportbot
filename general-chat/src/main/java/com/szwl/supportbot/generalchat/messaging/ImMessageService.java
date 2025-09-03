package com.szwl.supportbot.generalchat.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * IM消息服务
 * 通过Nacos服务发现获取IM服务的路由信息，然后调用HTTP接口
 */
@Slf4j
@Service
public class ImMessageService {

    @Value("${im.service.name:im-demo}")
    private String imServiceName;

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;

    public ImMessageService(DiscoveryClient discoveryClient, RestTemplate restTemplate) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = restTemplate;
    }

    /**
     * 发送AI回复消息给用户
     * @param userId 用户ID
     * @param content AI回复内容
     * @return 是否发送成功
     */
    public boolean sendAiResponse(String userId, String content) {
        try {
            log.info("准备发送AI回复: userId={}, content={}", userId, content);
            
            // 1. 通过Nacos服务发现获取IM服务实例
            ServiceInstance imInstance = discoverImService();
            if (imInstance == null) {
                log.error("未找到IM服务实例: {}", imServiceName);
                return false;
            }

            // 2. 获取服务实例的网络地址信息
            String host = imInstance.getHost();
            int port = imInstance.getPort(); // HTTP端口
            
            log.info("发现IM服务实例: {}:{} (HTTP)", host, port);
            
            // 3. 调用HTTP服务发送消息
            boolean success = callHttpService(host, port, userId, content);
            
            if (success) {
                log.info("AI回复发送成功: userId={}, target={}:{}", userId, host, port);
            } else {
                log.warn("AI回复发送失败: userId={}, target={}:{}", userId, host, port);
            }
            
            return success;
            
        } catch (Exception e) {
            log.error("发送AI回复异常: userId={}", userId, e);
            return false;
        }
    }

    /**
     * 通过Nacos服务发现获取IM服务实例
     * @return 服务实例，如果未找到返回null
     */
    private ServiceInstance discoverImService() {
        try {
            log.debug("开始服务发现，服务名: {}", imServiceName);
            
            // 调用Nacos客户端接口获取服务实例列表
            List<ServiceInstance> instances = discoveryClient.getInstances(imServiceName);
            
            if (instances.isEmpty()) {
                log.warn("服务 {} 没有可用实例", imServiceName);
                return null;
            }
            
            // 选择第一个可用实例（实际项目中可以实现负载均衡策略）
            ServiceInstance instance = instances.get(0);
            
            log.debug("服务发现成功: {} -> {}:{}", imServiceName, 
                     instance.getHost(), instance.getPort());
            
            return instance;
            
        } catch (Exception e) {
            log.error("服务发现异常: {}", imServiceName, e);
            return null;
        }
    }

    /**
     * 调用HTTP服务发送消息
     * @param host 服务主机地址
     * @param port HTTP端口
     * @param userId 用户ID
     * @param content 消息内容
     * @return 是否调用成功
     */
    private boolean callHttpService(String host, int port, String userId, String content) {
        try {
            log.debug("开始调用HTTP服务: {}:{}, userId={}", host, port, userId);
            
            // 1. 构建请求URL
            String url = String.format("http://%s:%d/api/ai-response/send", host, port);
            
            // 2. 构建请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // 3. 构建请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("userId", userId);
            requestBody.put("content", content);
            requestBody.put("source", "general-chat");
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            
            // 4. 发送HTTP请求
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            
            // 5. 处理响应
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody != null && Boolean.TRUE.equals(responseBody.get("success"))) {
                    log.info("HTTP调用成功: userId={}, message={}", userId, responseBody.get("message"));
                    return true;
                } else {
                    log.warn("HTTP调用失败: userId={}, message={}", userId, 
                            responseBody != null ? responseBody.get("message") : "未知错误");
                    return false;
                }
            } else {
                log.error("HTTP调用失败: userId={}, status={}", userId, response.getStatusCode());
                return false;
            }
            
        } catch (Exception e) {
            log.error("HTTP调用异常: {}:{}, userId={}", host, port, userId, e);
            return false;
        }
    }

    /**
     * 获取服务发现状态信息（用于监控和调试）
     * @return 服务状态信息
     */
    public String getServiceStatus() {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(imServiceName);
            return String.format("服务名: %s, 可用实例数: %d", imServiceName, instances.size());
        } catch (Exception e) {
            return String.format("服务名: %s, 状态检查失败: %s", imServiceName, e.getMessage());
        }
    }
}
