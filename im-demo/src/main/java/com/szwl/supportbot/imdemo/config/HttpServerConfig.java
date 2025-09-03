package com.szwl.supportbot.imdemo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP服务器配置
 * 简化版本，只负责HTTP服务器配置，不进行复杂的服务注册
 */
@Slf4j
@Component
public class HttpServerConfig {

    @Value("${server.port:8084}")
    private int httpPort;

    @Value("${spring.application.name:im-demo}")
    private String serviceName;

    /**
     * 启动HTTP服务器
     */
    @PostConstruct
    public void startHttpServer() {
        try {
            log.info("HTTP服务器启动成功，监听端口: {}", httpPort);
            log.info("服务名称: {}, 本地地址: {}:{}", serviceName, getLocalHost(), httpPort);

        } catch (Exception e) {
            log.error("启动HTTP服务器失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取本地主机地址
     */
    private String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.warn("无法获取本地主机地址，使用默认值", e);
            return "127.0.0.1";
        }
    }

    /**
     * 停止HTTP服务器
     */
    @PreDestroy
    public void stopHttpServer() {
        log.info("HTTP服务器已停止");
    }

    /**
     * 获取HTTP服务器端口
     */
    public int getHttpPort() {
        return httpPort;
    }

    /**
     * 获取服务名称
     */
    public String getServiceName() {
        return serviceName;
    }
}
