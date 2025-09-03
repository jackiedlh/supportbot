package com.szwl.supportbot.imdemo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户服务
 * 处理用户登录，用户名和UID映射存储在本地文件
 */
@Slf4j
@Service
public class UserService {

    private static final String USER_FILE = "users.txt";
    private final Map<String, Long> usernameToUid = new HashMap<>();
    private final Map<Long, String> uidToUsername = new HashMap<>();

    public UserService() {
        loadUsers();
    }

    /**
     * 用户登录
     * @param username 用户名
     * @return UID
     */
    public Long login(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }

        Long uid = usernameToUid.get(username);
        if (uid == null) {
            // 生成新的UID
            uid = generateUid();
            usernameToUid.put(username, uid);
            uidToUsername.put(uid, username);
            saveUsers();
            log.info("新用户注册: username={}, uid={}", username, uid);
        } else {
            log.info("用户登录: username={}, uid={}", username, uid);
        }

        return uid;
    }

    /**
     * 生成UID
     * 使用UUID的hashCode作为long类型的UID
     */
    private Long generateUid() {
        // 使用UUID的hashCode生成long类型的UID
        long uid = Math.abs(UUID.randomUUID().hashCode());
        return uid;
    }

    /**
     * 加载用户数据
     */
    private void loadUsers() {
        try {
            Path path = Paths.get(USER_FILE);
            if (Files.exists(path)) {
                try (BufferedReader reader = Files.newBufferedReader(path)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("=");
                        if (parts.length == 2) {
                            String username = parts[0].trim();
                            try {
                                Long uid = Long.parseLong(parts[1].trim());
                                usernameToUid.put(username, uid);
                                uidToUsername.put(uid, username);
                            } catch (NumberFormatException e) {
                                log.warn("跳过无效的UID格式: {}", parts[1]);
                            }
                        }
                    }
                }
                log.info("加载用户数据: {} 个用户", usernameToUid.size());
            }
        } catch (IOException e) {
            log.warn("加载用户数据失败，将创建新文件", e);
        }
    }

    /**
     * 保存用户数据
     */
    private void saveUsers() {
        try {
            Path path = Paths.get(USER_FILE);
            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                for (Map.Entry<String, Long> entry : usernameToUid.entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue());
                    writer.newLine();
                }
            }
            log.info("保存用户数据: {} 个用户", usernameToUid.size());
        } catch (IOException e) {
            log.error("保存用户数据失败", e);
        }
    }

    /**
     * 获取用户名
     */
    public String getUsername(Long uid) {
        return uidToUsername.get(uid);
    }

    /**
     * 获取用户数量
     */
    public int getUserCount() {
        return usernameToUid.size();
    }
}
