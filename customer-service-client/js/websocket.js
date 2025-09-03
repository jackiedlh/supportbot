/**
 * STOMP WebSocket连接管理
 * 负责与im-demo后端建立STOMP连接，处理消息收发
 */
class WebSocketManager {
    constructor() {
        this.stompClient = null;
        this.sockJs = null;
        this.reconnectAttempts = 0;
        this.maxReconnectAttempts = 5;
        this.reconnectInterval = 3000; // 3秒
        this.heartbeatInterval = null;
        this.isConnected = false;
        this.userId = null;
        this.sessionId = null;
        
        // 绑定事件处理器
        this.onMessageCallback = null;
        this.onConnectCallback = null;
        this.onDisconnectCallback = null;
        
        // 用于处理请求-响应模式的回调
        this.pendingResponses = new Map(); // messageId -> {resolve, reject, timeout}

    }

    /**
     * 连接到STOMP WebSocket服务器
     * @param {string|number} userId 用户ID
     */
    connect(userId) {
        if (this.stompClient && this.stompClient.connected) {
            console.log('STOMP已连接');
            return;
        }

        // 确保userId是数字类型，如果传入的是字符串则转换为数字
        this.userId = typeof userId === 'string' ? parseInt(userId, 10) : userId;
        
        // 验证userId是否为有效数字
        if (isNaN(this.userId)) {
            console.error('无效的用户ID:', userId);
            return;
        }
        
        // 使用配置文件中的服务地址，传递uid参数
        console.log('准备构建WebSocket URL，当前userId:', this.userId, '类型:', typeof this.userId);
        const wsUrl = window.getWebSocketUrl(this.userId);
        console.log('正在连接STOMP WebSocket:', wsUrl);
        console.log('用户ID (数字类型):', this.userId);

        try {
            // 创建SockJS连接
            this.sockJs = new SockJS(wsUrl);
            this.stompClient = Stomp.over(this.sockJs);
            
            // 禁用STOMP调试日志
            this.stompClient.debug = null;
            
            this.setupEventHandlers();
        } catch (error) {
            console.error('STOMP连接失败:', error);
            this.scheduleReconnect();
        }
    }

    /**
     * 设置STOMP事件处理器
     */
    setupEventHandlers() {
        try {
            // 设置连接参数，包含用户身份信息
            const connectHeaders = {
                'user': this.userId.toString(),  // 设置用户ID
                'login': this.userId.toString()  // 设置登录名
            };
            
            console.log('连接参数:', connectHeaders);
            
            this.stompClient.connect(connectHeaders, 
                // 连接成功回调
                (frame) => {
                    console.log('=== STOMP连接成功 ===');
                    console.log('连接帧信息:', frame);
                    console.log('连接头信息:', frame.headers);
                    console.log('用户ID:', this.userId);
                    
                    this.isConnected = true;
                    this.reconnectAttempts = 0;
                    
                    // 不在这里获取sessionId，让Spring自动处理
                    // 用户发送消息时，服务端会自动建立会话
                    console.log('STOMP连接已建立，等待用户发送消息...');
                    
                    // 订阅通用队列，确保能接收到消息
                    this.subscribeToUserMessages();
                    
                    console.log('连接状态已更新:', this.isConnected);
                    
                    // 开始心跳
                    this.startHeartbeat();
                    
                    if (this.onConnectCallback) {
                        this.onConnectCallback();
                    }
                    
                    console.log('=== STOMP连接初始化完成 ===');
                },
                // 连接失败回调
                (error) => {
                    console.error('STOMP连接失败:', error);
                    this.isConnected = false;
                    this.stopHeartbeat();
                    
                    if (this.onDisconnectCallback) {
                        this.onDisconnectCallback();
                    }
                    
                    this.scheduleReconnect();
                }
            );
        } catch (error) {
            console.error('设置STOMP事件处理器失败:', error);
            this.isConnected = false;
            this.scheduleReconnect();
        }
    }

    /**
     * 订阅用户消息队列
     */
    subscribeToUserMessages() {
        if (this.stompClient && this.stompClient.connected) {
            console.log('开始订阅消息队列...');
            console.log('当前用户ID:', this.userId);
            console.log('STOMP客户端状态:', this.stompClient.connected);
            
            // 订阅用户专属队列 - 使用Spring自动路由
            const userQueue = '/user/queue/messages';
            console.log('订阅用户专属队列:', userQueue);
            console.log('Spring会自动路由到: /user/{sessionId}/queue/messages');
            
            this.stompClient.subscribe(userQueue, (message) => {
                console.log('=== 收到消息 ===');
                console.log('订阅队列:', userQueue);
                console.log('实际接收队列:', message.headers.destination);
                console.log('原始消息:', message);
                console.log('消息体:', message.body);
                
                try {
                    const data = JSON.parse(message.body);
                    console.log('解析后的消息数据:', data);
                    this.handleMessage(data);
                } catch (error) {
                    console.error('解析消息失败:', error);
                    console.error('原始消息体:', message.body);
                }
            });
            
            console.log('消息队列订阅完成');
        } else {
            console.error('STOMP客户端未连接，无法订阅消息队列');
        }
    }







    /**
     * 处理收到的消息
     * @param {Object} data 消息数据
     */
    handleMessage(data) {
        console.log('收到消息:', data);
        
        // 检查是否是响应消息（CHAT_RESPONSE类型）
        if (data.type === 'CHAT_RESPONSE') {
            console.log('识别为响应消息，调用handleResponse');
            this.handleResponse(data);
            return;
        }
        
        // 处理ChatMessage对象（来自后端的消息）
        if (data.type === 'CHAT' || data.type === 'SYSTEM' || data.type === 'AI_RESPONSE') {
            // 直接转发给聊天管理器处理
            if (this.onMessageCallback) {
                this.onMessageCallback(data);
            }
            return;
        }
        
        // 处理其他类型的消息
        switch (data.type) {
            case 'CHAT':
                this.handleChatMessage(data);
                break;
            case 'CHAT_MESSAGE':
                this.handleChatMessage(data);
                break;
            case 'SYSTEM':
                this.handleSystemMessage(data);
                break;
            case 'SYSTEM_MESSAGE':
                this.handleSystemMessage(data);
                break;
            case 'HEARTBEAT_RESPONSE':
                this.handleHeartbeatResponse(data);
                break;
            case 'USER_CONNECT_RESPONSE':
                this.handleUserConnectResponse(data);
                break;
            default:
                console.log('未知消息类型:', data.type);
        }
    }

    /**
     * 处理聊天消息
     * @param {Object} data 消息数据
     */
    handleChatMessage(data) {
        if (this.onMessageCallback) {
            this.onMessageCallback(data);
        }
    }

    /**
     * 处理系统消息
     * @param {Object} data 消息数据
     */
    handleSystemMessage(data) {
        console.log('系统消息:', data.message);
        // 可以在这里处理系统通知等
    }

    /**
     * 处理心跳响应
     * @param {Object} data 响应数据
     */
    handleHeartbeatResponse(data) {
        // 心跳响应，可以更新连接状态
    }

    /**
     * 处理用户连接响应
     * @param {Object} data 响应数据
     */
    handleUserConnectResponse(data) {
        if (data.success) {
            this.sessionId = data.sessionId;
            console.log('用户连接成功，会话ID:', this.sessionId);
        } else {
            console.error('用户连接失败:', data.message);
        }
    }

    /**
     * 发送用户连接消息
     */
    sendUserConnect() {
        if (this.isConnected && this.userId) {
            const message = {
                type: 'USER_CONNECT',
                userId: this.userId,  // 确保是数字类型
                timestamp: Date.now()
            };
            this.send(message);
        }
    }

    /**
     * 发送聊天消息
     * @param {string} content 消息内容
     */
    sendChatMessage(content) {
        if (this.isConnected && this.userId) {
            const messageId = this.generateMessageId();
            const message = {
                messageId: messageId,
                type: 'CHAT',
                content: content,
                sender: this.userId,
                timestamp: Date.now()
            };
            
            console.log('发送的消息对象:', message);
            console.log('发送的消息ID:', messageId);
            
            // 返回Promise，等待服务器响应
            return this.sendWithResponse(message, messageId);
        } else {
            console.error('无法发送消息: WebSocket未连接或用户未登录');
            return Promise.reject(new Error('WebSocket未连接或用户未登录'));
        }
    }

         /**
      * 发送心跳消息
      */
     sendHeartbeat() {
         if (this.isConnected && this.userId) {
             const message = {
                 userId: this.userId,  // 改为userId字段，匹配后端期望
                 timestamp: Date.now()
             };
             // 心跳消息发送到专门的 /app/heartbeat 端点
             this.sendToEndpoint('/app/heartbeat', message);
         }
     }

    /**
     * 发送消息到服务器
     * @param {Object} message 消息对象
     */
    send(message) {
        // 默认发送到 /app/chat 端点
        this.sendToEndpoint('/app/chat', message);
    }

    /**
     * 发送消息到指定端点
     * @param {string} endpoint 目标端点
     * @param {Object} message 消息对象
     */
    sendToEndpoint(endpoint, message) {
        console.log('=== 开始发送消息到服务器 ===');
        console.log('目标端点:', endpoint);
        console.log('消息内容:', message);
        console.log('STOMP客户端状态:', {
            exists: !!this.stompClient,
            connected: this.stompClient ? this.stompClient.connected : false,
            readyState: this.sockJs ? this.sockJs.readyState : 'N/A'
        });
        
        if (this.stompClient && this.stompClient.connected) {
            try {
                this.stompClient.send(endpoint, {}, JSON.stringify(message));
                console.log('消息已发送到', endpoint, '端点');
                console.log('=== 消息发送到服务器完成 ===');
            } catch (error) {
                console.error('发送消息失败:', error);
                console.error('错误详情:', {
                    message: error.message,
                    stack: error.stack
                });
            }
        } else {
            console.error('STOMP未连接，无法发送消息');
            console.error('连接状态详情:', {
                stompClient: !!this.stompClient,
                stompConnected: this.stompClient ? this.stompClient.connected : false,
                sockJs: !!this.sockJs,
                sockJsReadyState: this.sockJs ? this.sockJs.readyState : 'N/A'
            });
        }
    }

    /**
     * 开始心跳
     */
    startHeartbeat() {
        this.heartbeatInterval = setInterval(() => {
            this.sendHeartbeat();
        }, 30000); // 30秒发送一次心跳
    }

    /**
     * 停止心跳
     */
    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
        }
    }

    /**
     * 生成消息ID
     * @returns {string} 唯一的消息ID
     */
    generateMessageId() {
        return 'msg_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }

    /**
     * 发送消息并等待响应
     * @param {Object} message 消息对象
     * @param {string} messageId 消息ID
     * @returns {Promise<Object>} 返回Promise，resolve时包含响应结果
     */
    sendWithResponse(message, messageId) {
        return new Promise((resolve, reject) => {
            // 设置超时（30秒）
            const timeout = setTimeout(() => {
                this.pendingResponses.delete(messageId);
                reject(new Error('请求超时'));
            }, 30000);
            
            // 存储Promise的resolve和reject函数
            this.pendingResponses.set(messageId, {
                resolve: resolve,
                reject: reject,
                timeout: timeout
            });
            
            // 发送消息
            try {
                this.send(message);
                console.log('消息已发送，等待响应: messageId=', messageId);
            } catch (error) {
                this.pendingResponses.delete(messageId);
                clearTimeout(timeout);
                reject(error);
            }
        });
    }

    /**
     * 处理响应消息
     * @param {Object} response 响应数据
     */
    handleResponse(response) {
        const { messageId, code, message } = response;
        console.log('=== 收到服务器响应 ===');
        console.log('响应的消息ID:', messageId);
        console.log('响应状态码:', code);
        console.log('响应消息:', message);
        
        // 查找对应的Promise
        const pendingResponse = this.pendingResponses.get(messageId);
        if (pendingResponse) {
            console.log('找到匹配的待响应消息，开始处理...');
            // 清除超时
            clearTimeout(pendingResponse.timeout);
            this.pendingResponses.delete(messageId);
            
            // 根据状态码决定resolve还是reject
            if (code === 0) {
                console.log('消息发送成功，调用resolve');
                pendingResponse.resolve({
                    success: true,
                    code: code,
                    message: message,
                    messageId: messageId
                });
            } else {
                console.log('消息发送失败，调用reject');
                pendingResponse.reject({
                    success: false,
                    code: code,
                    message: message,
                    messageId: messageId
                });
            }
        } else {
            console.warn('未找到匹配的待响应消息ID:', messageId);
        }
    }

    /**
     * 安排重连
     */
    scheduleReconnect() {
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            
            setTimeout(() => {
                if (this.userId) {
                    this.connect(this.userId);
                }
            }, this.reconnectInterval);
        } else {
            console.error('达到最大重连次数，停止重连');
        }
    }

    /**
     * 断开连接
     */
    disconnect() {
        if (this.stompClient && this.stompClient.connected) {
            this.stompClient.disconnect();
        }
        if (this.sockJs) {
            this.sockJs.close();
        }
        this.isConnected = false;
        this.stopHeartbeat();
        this.userId = null;
        this.sessionId = null;
    }

    /**
     * 设置消息回调
     * @param {Function} callback 回调函数
     */
    onMessage(callback) {
        this.onMessageCallback = callback;
    }

    /**
     * 设置连接回调
     * @param {Function} callback 回调函数
     */
    onConnect(callback) {
        this.onConnectCallback = callback;
    }

    /**
     * 设置断开连接回调
     * @param {Function} callback 回调函数
     */
    onDisconnect(callback) {
        this.onDisconnectCallback = callback;
    }

    /**
     * 获取连接状态
     * @returns {boolean} 是否已连接
     */
    getConnectionStatus() {
        return this.stompClient && this.stompClient.connected;
    }
}

// 创建全局WebSocket管理器实例
window.wsManager = new WebSocketManager();
