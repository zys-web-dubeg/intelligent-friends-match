import { getToken } from './authService'

class WebSocketService {
  constructor() {
    this.ws = null
    this.teamId = null
    this.userId = null
    this.listeners = {
      message: [],
      system: [],
      connect: [],
      error: [],
      close: [],
      ai_thinking: [],
      ai_trigger: []
    }
    this.reconnectTimer = null
    this.reconnectAttempts = 0
    this.maxReconnectAttempts = 5
  }

  connect(teamId, userId) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      if (this.teamId === teamId && this.userId === userId) return
      this.disconnect()
    }

    this.teamId = teamId
    this.userId = userId
    const token = getToken()
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${protocol}//${window.location.host}/websocket/team/${teamId}/${userId}?token=${encodeURIComponent(token || '')}`

    this.ws = new WebSocket(wsUrl)

    this.ws.onopen = () => {
      this.reconnectAttempts = 0
      this.notify('connect', { userId, teamId })
    }

    this.ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        const msgType = data.type || ''

        // 系统消息
        if (msgType === 'SYSTEM' || msgType === 'CONNECT') {
          this.notify('system', data)
          // 注意：不再在这里重复触发 connect 事件，避免重复显示连接消息
          // connect 事件只在 ws.onopen 时触发一次
        }
        // 错误消息
        else if (msgType === 'ERROR') {
          this.notify('error', data)
        }
        // AI思考状态
        else if (msgType === 'AI_THINKING') {
          this.notify('ai_thinking', data)
        }
        // AI触发状态
        else if (msgType === 'AI_TRIGGER') {
          this.notify('ai_trigger', data)
        }
        // AI消息或普通消息
        else if (data.message || data.content) {
          this.notify('message', data)
        }
      } catch (e) {
        console.error('WebSocket message parse error:', e)
      }
    }

    this.ws.onerror = (error) => {
      this.notify('error', { type: 'ERROR', message: 'WebSocket连接错误' })
    }

    this.ws.onclose = () => {
      this.notify('close', {})
      if (this.reconnectAttempts < this.maxReconnectAttempts) {
        this.reconnectTimer = setTimeout(() => {
          this.reconnectAttempts++
          this.connect(this.teamId, this.userId)
        }, 3000)
      }
    }
  }

  send(content, type = 'MESSAGE') {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const message = { content, type };
      if (type === 'AI_TRIGGER') {
        message.teamId = this.teamId;
      }
      this.ws.send(JSON.stringify(message))
      return true
    }
    return false
  }

  // 检查消息是否包含触发AI的关键字
  triggerAIIfNeeded(messageContent) {
    if (!messageContent) return

    // AI触发关键词列表
    const aiTriggerKeywords = [
      'AI', '助手', '帮忙', '建议', '怎么看', '想法', '分析',
      '帮我', '帮我想', '智能', '机器人', '小智', '咨询', '意见'
    ]

    const containsKeyword = aiTriggerKeywords.some(keyword =>
        messageContent.toLowerCase().includes(keyword.toLowerCase())
    )

    if (containsKeyword) {
      // 发送AI触发通知
      this.sendAIActivationNotice(messageContent)
    }
  }

  // 发送AI激活通知
  sendAIActivationNotice(content) {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      this.send(content, 'AI_TRIGGER');
    }
  }

  disconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
  }

  on(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event].push(callback)
    }
    return () => this.off(event, callback)
  }

  off(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(cb => cb !== callback)
    }
  }

  notify(event, data) {
    if (this.listeners[event]) {
      this.listeners[event].forEach(cb => cb(data))
    }
  }
}

export default new WebSocketService()