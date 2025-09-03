package com.szwl.supportbot.imdemo.controller;

import com.szwl.supportbot.imdemo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 用户控制器
 * 提供登录接口
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户登录
     * @param request 登录请求
     * @return 登录结果
     */
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());
        
        try {
            Long uid = userService.login(request.getUsername());
            return Map.of(
                "success", true,
                "uid", uid,
                "message", "登录成功"
            );
        } catch (Exception e) {
            log.error("登录失败: username={}", request.getUsername(), e);
            return Map.of(
                "success", false,
                "message", "登录失败: " + e.getMessage()
            );
        }
    }

    /**
     * 登录请求模型
     */
    public static class LoginRequest {
        private String username;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}
