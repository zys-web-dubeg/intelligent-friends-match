<template>
  <AppLayout
      :agents="agents"
      :currentAgentId="currentAgentId"
      @switch-agent="onSwitchAgent"
      @new-chat="newChat"
  >
    <div class="chat-container">
      <div class="chat-header">
        <div class="header-left">
          <div class="agent-avatar">
            <img :src="currentAgent?.image" alt="avatar" />
          </div>
          <div class="agent-info">
            <span class="agent-name">{{ currentAgent?.name }}</span>
            <span class="agent-status">{{ currentAgent?.title }}</span>
          </div>
        </div>
        <el-button size="small" class="new-chat-btn" @click="newChat">
          <i class="fa-solid fa-plus"></i>
          <span>新会话</span>
        </el-button>
      </div>
      <div class="message-list" ref="messaggListRef">
        <div
            v-for="(message, index) in messages"
            :key="index"
            :class="
            message.isUser ? 'message user-message' : 'message bot-message'
          "
        >
          <div class="message-avatar">
            <i
                :class="
                message.isUser
                  ? 'fa-solid fa-user'
                  : 'fa-solid fa-robot'
              "
            ></i>
          </div>
          <div class="message-content">
            <div class="message-bubble">
              <span v-html="message.content"></span>
              <span
                  class="loading-dots"
                  v-if="message.isThinking || message.isTyping"
              >
                <span class="dot"></span>
                <span class="dot"></span>
                <span class="dot"></span>
              </span>
            </div>
          </div>
        </div>
      </div>
      <div class="input-container">
        <el-input
            v-model="inputMessage"
            placeholder="请输入消息..."
            @keyup.enter="sendMessage"
            class="chat-input"
        ></el-input>
        <el-button @click="sendMessage" :disabled="isSending || !inputMessage.trim()" type="primary" class="send-btn"
        ><i class="fa-solid fa-paper-plane"></i></el-button
        >
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import apiClient from '@/api/httpClient'
import { v4 as uuidv4 } from 'uuid'
import { ElMessage } from 'element-plus'
import { logout } from '@/api/authService'
import AppLayout from '@/layouts/AppLayout.vue'
import { agents, defaultAgentId } from '@/config/agents'

const router = useRouter()
const route = useRoute()
const messaggListRef = ref(null)
const isSending = ref(false)
const inputMessage = ref('')

const currentAgentId = ref(defaultAgentId)

// reactive objects - Vue can track property additions/deletions
const agentMessages = reactive({})
const agentUUIDs = reactive({})

const uuidToNumber = (uuid) => {
  let number = 0
  for (let i = 0; i < uuid.length && i < 6; i++) {
    const hexValue = uuid[i]
    number = number * 16 + (parseInt(hexValue, 16) || 0)
  }
  return number % 1000000
}

// Initialize all agents
const allIds = agents.map(a => a.id)
allIds.forEach(id => {
  agentMessages[id] = []
  let stored = localStorage.getItem(`user_uuid_${id}`)
  if (!stored) {
    stored = uuidToNumber(uuidv4())
    localStorage.setItem(`user_uuid_${id}`, stored)
  }
  agentUUIDs[id] = stored
})

// currentAgent is a computed based on currentAgentId
const currentAgent = computed(() => agents.find(a => a.id === currentAgentId.value))

// messages is a computed that returns the current agent's messages
const messages = computed(() => {
  return agentMessages[currentAgentId.value] || []
})

// Watch messages for scroll-to-bottom
watch(messages, () => {
  nextTick(() => {
    if (messaggListRef.value) {
      messaggListRef.value.scrollTop = messaggListRef.value.scrollHeight
    }
  })
}, { deep: true })

// When switching agent
watch(currentAgentId, (newId) => {
  if (agentMessages[newId].length === 0) {
    hello()
  }
})

const scrollToBottom = () => {
  nextTick(() => {
    if (messaggListRef.value) {
      messaggListRef.value.scrollTop = messaggListRef.value.scrollHeight
    }
  })
}

const hello = () => {
  sendRequest('你好')
}

const sendMessage = () => {
  if (inputMessage.value.trim()) {
    sendRequest(inputMessage.value.trim())
    inputMessage.value = ''
  }
}

const sendRequest = (message) => {
  isSending.value = true
  const agentId = currentAgentId.value
  const agent = currentAgent.value
  const msgs = agentMessages[agentId]

  // Add user message
  msgs.push({
    isUser: true,
    content: message,
    isTyping: false,
    isThinking: false,
  })

  // Add bot placeholder
  const botMsg = {
    isUser: false,
    content: '',
    isTyping: true,
    isThinking: false,
  }
  msgs.push(botMsg)
  const lastMsg = msgs[msgs.length - 1]

  scrollToBottom()

  // 使用正确的API端点
  apiClient
      .post(
          agent.api,
          { memoryId: agentUUIDs[agentId], message },
          {
            timeout: 30000, // 设置30秒超时
            responseType: 'stream',
            onDownloadProgress: (e) => {
              try {
                // 检查HTTP状态码，如是错误响应则跳过进度处理，让catch处理
                const httpStatus = e.event.target.status
                if (httpStatus >= 400) {
                  return
                }

                const fullText = e.event.target.responseText
                let newText = fullText.substring(lastMsg.content.length)
                // 过滤掉可能的JSON响应头部
                if (newText.startsWith('{') || newText.includes('{"code"')) {
                  // 如果是JSON响应，说明不是流式数据，而是完整响应
                  try {
                    const jsonData = JSON.parse(newText)
                    if (jsonData.code === 200) {
                      lastMsg.content = jsonData.data || jsonData.message || '收到消息'
                    } else {
                      lastMsg.content = jsonData.message || '服务响应异常'
                    }
                  } catch (parseError) {
                    // 如果解析失败，使用原始文本
                    lastMsg.content = newText
                  }
                } else {
                  // 正常流式响应
                  lastMsg.content += newText
                }
                scrollToBottom()
              } catch (error) {
                console.error('处理响应进度错误:', error)
                // 出错时停止处理，显示错误信息
                lastMsg.content = '处理响应时发生错误'
                lastMsg.isTyping = false
              }
            },
          }
      )
      .then(() => {
        lastMsg.isTyping = false
        isSending.value = false
      })
      .catch((error) => {
        console.error('智能助手API错误:', error)

        // 更详细的错误处理
        if (error.code === 'ECONNABORTED' || error.message.includes('timeout')) {
          lastMsg.content = '请求超时，请稍后重试'
        } else if (error.response) {
          // 服务器响应了错误状态码
          if (error.response.status === 401) {
            lastMsg.content = '登录已过期，请重新登录'
            setTimeout(() => {
              logout()
              router.push('/login')
            }, 1500)
          } else if (error.response.status === 403) {
            lastMsg.content = '无权访问该服务'
          } else if (error.response.status === 404) {
            lastMsg.content = '服务端点不存在，请联系管理员'
          } else if (error.response.status >= 500) {
            lastMsg.content = `服务器内部错误 (${error.response.status})，请稍后重试`
          } else {
            // 尝试解析错误响应
            const errorData = error.response.data
            if (errorData && typeof errorData === 'object') {
              lastMsg.content = errorData.message || `请求失败 (${error.response.status})`
            } else {
              lastMsg.content = `请求失败 (${error.response.status}): ${error.response.statusText}`
            }
          }
        } else if (error.request) {
          // 请求已发出但没有收到响应
          lastMsg.content = '网络连接失败，请检查网络连接或服务是否可用'
        } else {
          // 其他错误
          lastMsg.content = `请求失败: ${error.message}`
        }

        lastMsg.isTyping = false
        isSending.value = false
      })
}

const onSwitchAgent = (agentId) => {
  if (agentId !== currentAgentId.value) {
    currentAgentId.value = agentId
  }
}

const newChat = () => {
  const agentId = currentAgentId.value
  const newUuid = uuidToNumber(uuidv4())
  localStorage.setItem(`user_uuid_${agentId}`, newUuid)
  agentUUIDs[agentId] = newUuid
  agentMessages[agentId] = []
  hello()
}

onMounted(() => {
  const agentFromQuery = route.query.agent
  if (agentFromQuery && agents.find(a => a.id === agentFromQuery)) {
    currentAgentId.value = agentFromQuery
  }
})

// Say hello on initial load
hello()
</script>

<style scoped>
.chat-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-height: calc(100vh - 120px);
  background: linear-gradient(180deg, #1e1e2e 0%, #2d2d44 100%);
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.2);
  overflow: hidden;
  margin: 0 auto;
  width: 100%;
  max-width: 1200px; /* 增加最大宽度，使其不会占满整个屏幕 */
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.agent-avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  overflow: hidden;
  border: 2px solid #e2e8f0;
  flex-shrink: 0; /* 防止头像被压缩 */
}

.agent-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.agent-info {
  display: flex;
  flex-direction: column;
}

.agent-name {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
}

.agent-status {
  font-size: 12px;
  color: #a1a1aa;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
  border: 1px solid rgba(255, 255, 255, 0.2);
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.new-chat-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  color: #ffffff;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  background: linear-gradient(180deg, rgba(30, 30, 46, 0.5) 0%, rgba(45, 45, 68, 0.5) 100%);
}

.message {
  display: flex;
  gap: 12px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.1);
  color: #a1a1aa;
  font-size: 14px;
}

.user-message .message-avatar {
  background: rgba(74, 144, 226, 0.2);
  color: #4a90e2;
}

.bot-message .message-avatar {
  background: rgba(102, 126, 234, 0.2);
  color: #667eea;
}

.message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.1);
  color: #ffffff;
  max-width: 70%;
  word-wrap: break-word;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-message .message-bubble {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  align-self: flex-end;
  margin-left: auto;
}

.bot-message .message-bubble {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.loading-dots {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 8px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #a1a1aa;
  animation: pulse 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes pulse {
  0%, 80%, 100% {
    transform: scale(0.75);
    opacity: 0.5;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.input-container {
  display: flex;
  gap: 12px;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.05);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.chat-input {
  flex: 1;
}

.send-btn {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .chat-container {
    max-height: calc(100vh - 80px);
    margin: 10px;
    border-radius: 12px;
  }

  .message-bubble {
    max-width: 85%;
  }

  .input-container {
    padding: 16px;
  }

  .agent-avatar {
    width: 36px;
    height: 36px;
  }
}
</style>