package com.szwl.supportbot.questionclassifier.mq;

import com.szwl.supportbot.questionclassifier.config.QuestionCategoryConfig;
import com.szwl.supportbot.questionclassifier.entity.QuestionClassificationResult.QuestionItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 任务生产者 (基于 RocketMQ 5.x API)
 * - 动态根据分类结果投递到不同的 Topic
 * - Producer 生命周期交给 Spring 管理
 */
@Slf4j
@Service
public class TaskProducer {

    private final QuestionCategoryConfig categoryConfig;
    private final String nameServer;
    @Value("${rocketmq.default-topic}")
    private String defaultTopic;
    private Producer producer; // v5 Producer 实例

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public TaskProducer(
            QuestionCategoryConfig categoryConfig,
            @Value("${rocketmq.name-server}") String nameServer
    ) {
        this.categoryConfig = categoryConfig;
        this.nameServer = nameServer;
    }

    /**
     * 初始化 Producer (Spring 启动时调用)
     */
    @PostConstruct
    public void init() {
        try {
            ClientServiceProvider provider = ClientServiceProvider.loadService();
            ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder()
                    .setEndpoints(nameServer)
                    .build();

            this.producer = provider.newProducerBuilder()
                    .setClientConfiguration(clientConfiguration)
                    // 提前声明默认 topic（可以用分类结果覆盖）
                    .setTopics(defaultTopic)
                    .build();

            log.info("RocketMQ Producer 初始化成功, nameServer={}", nameServer);
        } catch (Exception e) {
            log.error("RocketMQ Producer 初始化失败: {}", e.getMessage(), e);
        }
    }

    /**
     * Spring 关闭时释放 Producer
     */
    @PreDestroy
    public void shutdown() {
        if (producer != null) {
            try {
                producer.close();
                log.info("RocketMQ Producer 已关闭");
            } catch (Exception e) {
                log.error("关闭 RocketMQ Producer 失败: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 发送任务到队列
     */
    public void sendTaskToQueue(QuestionItem questionItem, String question, String sessionId) {
        String topic = getTopicByCategory(questionItem.getCategory());
        String messageBody = buildTaskMessage(questionItem, question, sessionId);

        try {
            Message message = ClientServiceProvider.loadService()
                    .newMessageBuilder()
                    .setTopic(topic)
                    .setBody(messageBody.getBytes())
                    .build();

            SendReceipt receipt = producer.send(message);
            log.info("消息发送成功: topic={}, msgId={}", topic, receipt.getMessageId());
        } catch (Exception e) {
            log.error("发送消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 根据分类获取对应的Topic
     */
    private String getTopicByCategory(String category) {
        return categoryConfig.getTopicForCategory(category);
    }

    /**
     * 构建任务消息 (JSON 格式)
     */
    private String buildTaskMessage(QuestionItem questionItem, String question, String sessionId) {
        // 在问题后面追加用户ID信息
        String questionWithUserId = question + "，用户ID是" + sessionId;
        log.info("问题追加用户ID: 原问题='{}', 追加后='{}', 用户ID={}", question, questionWithUserId, sessionId);
        
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("sessionId", sessionId);
        messageMap.put("question", questionWithUserId);
        messageMap.put("category", questionItem.getCategory());
        messageMap.put("timestamp", LocalDateTime.now().format(formatter));

        try {
            return objectMapper.writeValueAsString(messageMap);
        } catch (Exception e) {
            return String.format("sessionId=%s, question=%s, category=%s",
                    sessionId, questionWithUserId, questionItem.getCategory());
        }
    }
}
