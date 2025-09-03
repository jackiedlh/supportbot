import { useState, useCallback, useRef } from 'react'
import SockJS from 'sockjs-client'
import { Client } from 'stompjs'

export const useWebSocket = () => {
  const [connectionStatus, setConnectionStatus] = useState('disconnected')
  const stompClientRef = useRef(null)
  const reconnectTimeoutRef = useRef(null)

  const connect = useCallback(async (userId) => {
    try {
      setConnectionStatus('connecting')
      
      // 创建SockJS连接
      const socket = new SockJS('/ws')
      const client = Client.over(socket)
      
      // 连接配置
      client.reconnect_delay = 5000
      client.heartbeat.outgoing = 20000
      client.heartbeat.incoming = 20000
      
      // 连接成功回调
      client.onConnect = () => {
        console.log('WebSocket connected successfully')
        setConnectionStatus('connected')
        
        // 订阅个人消息队列
        client.subscribe('/user/queue/messages', (message) => {
          try {
            const messageData = JSON.parse(message.body)
            console.log('Received message:', messageData)
            // 这里需要通知ChatContext处理消息
            // 暂时使用全局事件
            window.dispatchEvent(new CustomEvent('websocket-message', {
              detail: messageData
            }))
          } catch (error) {
            console.error('Failed to parse message:', error)
          }
        })
        
        // 订阅广播消息
        client.subscribe('/topic/broadcast', (message) => {
          try {
            const messageData = JSON.parse(message.body)
            console.log('Received broadcast:', messageData)
          } catch (error) {
            console.error('Failed to parse broadcast:', error)
          }
        })
      }
      
      // 连接错误回调
      client.onStompError = (frame) => {
        console.error('WebSocket STOMP error:', frame)
        setConnectionStatus('error')
      }
      
      // 连接失败回调
      client.onWebSocketError = (error) => {
        console.error('WebSocket error:', error)
        setConnectionStatus('error')
      }
      
      // 连接断开回调
      client.onWebSocketClose = () => {
        console.log('WebSocket connection closed')
        setConnectionStatus('disconnected')
        
        // 尝试重连
        if (reconnectTimeoutRef.current) {
          clearTimeout(reconnectTimeoutRef.current)
        }
        reconnectTimeoutRef.current = setTimeout(() => {
          if (connectionStatus !== 'connected') {
            console.log('Attempting to reconnect...')
            connect(userId)
          }
        }, 5000)
      }
      
      // 建立连接
      client.connect({}, () => {
        console.log('STOMP connection established')
      })
      
      stompClientRef.current = client
      
    } catch (error) {
      console.error('Failed to connect WebSocket:', error)
      setConnectionStatus('error')
      throw error
    }
  }, [connectionStatus])

  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current)
      reconnectTimeoutRef.current = null
    }
    
    if (stompClientRef.current) {
      stompClientRef.current.disconnect()
      stompClientRef.current = null
    }
    
    setConnectionStatus('disconnected')
  }, [])

  const sendMessage = useCallback((message) => {
    if (stompClientRef.current && connectionStatus === 'connected') {
      try {
        stompClientRef.current.send('/app/chat', {}, JSON.stringify(message))
        console.log('Message sent:', message)
      } catch (error) {
        console.error('Failed to send message:', error)
        throw error
      }
    } else {
      throw new Error('WebSocket not connected')
    }
  }, [connectionStatus])

  return {
    connect,
    disconnect,
    sendMessage,
    connectionStatus
  }
}
