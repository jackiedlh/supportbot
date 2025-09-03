package com.szwl.supportbot.assistant.constant;

/**
 * 通用常量类
 * 定义系统中使用的各种常量
 */
public class CommonConstants {

    /**
     * 默认业务类型
     */
    public static final String DEFAULT_BUSINESS_TYPE = "default";

    /**
     * 默认聊天ID
     */
    public static final String DEFAULT_CHAT_ID = "default";

    /**
     * 默认超时时间（毫秒）
     */
    public static final long DEFAULT_TIMEOUT_MS = 30000;

    /**
     * 最大重试次数
     */
    public static final int MAX_RETRY_ATTEMPTS = 3;

    /**
     * 重试延迟（毫秒）
     */
    public static final long RETRY_DELAY_MS = 1000;

    /**
     * 缓存过期时间（秒）
     */
    public static final long CACHE_EXPIRE_SECONDS = 3600;

    /**
     * 分页默认大小
     */
    public static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 最大分页大小
     */
    public static final int MAX_PAGE_SIZE = 100;

    /**
     * 成功状态码
     */
    public static final String SUCCESS_CODE = "200";

    /**
     * 失败状态码
     */
    public static final String FAILURE_CODE = "500";

    /**
     * 业务错误状态码
     */
    public static final String BUSINESS_ERROR_CODE = "400";

    /**
     * 未授权状态码
     */
    public static final String UNAUTHORIZED_CODE = "401";

    /**
     * 禁止访问状态码
     */
    public static final String FORBIDDEN_CODE = "403";

    /**
     * 未找到状态码
     */
    public static final String NOT_FOUND_CODE = "404";

    /**
     * 默认系统提示词
     */
    public static final String DEFAULT_SYSTEM_PROMPT = "你是一名专业的AI助手，可以帮助用户处理各种业务需求。";

    /**
     * 默认MCP工具名称
     */
    public static final String DEFAULT_MCP_TOOL_NAME = "mcp_default_tool";

    /**
     * 默认MCP工具描述
     */
    public static final String DEFAULT_MCP_TOOL_DESCRIPTION = "MCP默认工具";

    /**
     * 私有构造函数，防止实例化
     */
    private CommonConstants() {
        throw new UnsupportedOperationException("常量类不能实例化");
    }
}
