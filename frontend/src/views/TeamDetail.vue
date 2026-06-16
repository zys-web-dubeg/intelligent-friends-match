<template>
  <div class="team-detail-page">
    <!-- 页面头部 -->
    <div class="page-header glass-effect">
      <el-button class="back-btn" @click="$router.push('/teams')">
        <i class="fa-solid fa-arrow-left"></i>
        <span>返回</span>
      </el-button>
      <div class="header-info">
        <h2>{{ team?.name || '队伍详情' }}</h2>
        <p>成员数：{{ visibleMembers.length }}</p>
      </div>
      <el-tag v-if="team" :type="accessLevelTag(team.accessLevel)" class="access-tag">
        {{ accessLevelText(team.accessLevel) }}
      </el-tag>
    </div>

    <el-row :gutter="24">
      <!-- 左侧：队伍信息和成员 -->
      <el-col :span="8">
        <!-- 队伍信息卡片 -->
        <div class="info-card glass-effect team-info-card">
          <div class="card-header info-header">
            <div class="header-icon info-icon">
              <i class="fa-solid fa-info-circle"></i>
            </div>
            <h3>队伍信息</h3>
          </div>
          <div v-if="team" class="info-content">
            <div class="info-item">
              <span class="info-label">名称</span>
              <span class="info-value">{{ team.name }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">访问级别</span>
              <el-tag :type="accessLevelTag(team.accessLevel)" size="small">
                {{ accessLevelText(team.accessLevel) }}
              </el-tag>
            </div>
            <div v-if="team.personalityConstraint" class="info-item">
              <span class="info-label">个性约束</span>
              <span class="info-value">{{ team.personalityConstraint }}</span>
            </div>
          </div>
        </div>

        <!-- 成员列表卡片 -->
        <div class="member-card glass-effect members-list-card">
          <div class="card-header members-header">
            <div class="header-icon members-icon">
              <i class="fa-solid fa-users"></i>
            </div>
            <h3>成员列表</h3>
            <span class="member-count">{{ visibleMembers.length }}</span>
          </div>
          <div v-if="visibleMembers.length === 0" class="empty-state">
            <i class="fa-solid fa-user-slash"></i>
            <p>暂无成员</p>
          </div>
          <ul v-else class="member-list">
            <li v-for="member in visibleMembers" :key="member.id" class="member-item">
              <div class="member-avatar">
                <i v-if="member.senderType === 'AI'" class="fa-solid fa-robot"></i>
                <img v-else-if="member.avatar" :src="member.avatar" alt="avatar" />
                <i v-else class="fa-solid fa-user"></i>
              </div>
              <div class="member-info">
                <span class="member-name">{{ member.nickname || `用户${member.userId}` }}</span>
                <span class="member-role" :class="{ admin: member.role === 'admin', ai: member.senderType === 'AI' }">
                  {{ member.senderType === 'AI' ? 'AI助手' : (member.role === 'admin' ? '管理员' : '成员') }}
                </span>
              </div>
            </li>
          </ul>
        </div>
      </el-col>

      <!-- 右侧：聊天区域 -->
      <el-col :span="16">
        <div class="chat-card glass-effect">
          <div class="card-header">
            <div class="header-icon chat">
              <i class="fa-solid fa-comments"></i>
            </div>
            <h3>队伍聊天</h3>
            <div class="connection-status">
              <span :class="['status-dot', wsConnected ? 'online' : 'offline']"></span>
              <span class="status-text">{{ wsConnected ? '已连接' : '未连接' }}</span>
            </div>
          </div>
          <div class="chat-messages" ref="messageContainer">
            <div 
              v-for="(msg, index) in messages"
              :key="index" 
              :class="['message-item', (msg.type || 'MESSAGE').toLowerCase(), { 'ai-message': msg.senderType === 'AI', 'my-message': msg.isMine }]"
            >
              <div v-if="msg.type === 'SYSTEM'" class="system-message">
                <span>{{ msg.message || msg.content }}</span>
              </div>
              <div v-else class="chat-message">
                <div class="message-avatar">
                  <i v-if="msg.senderType === 'AI'" class="fa-solid fa-robot"></i>
                  <img v-else-if="msg.senderAvatar" :src="msg.senderAvatar" alt="avatar" />
                  <i v-else class="fa-solid fa-user"></i>
                </div>
                <div class="message-content">
                  <div class="message-header">
                    <span class="sender-name">{{ msg.senderType === 'AI' ? '🤖 AI助手' : (msg.senderName || `未知伙伴`) }}</span>
                    <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
                  </div>
                  <div class="message-body" :class="{ 'ai-body': msg.senderType === 'AI', 'my-body': msg.isMine }">
                    {{ msg.content }}
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div class="chat-input">
            <div class="input-wrapper">
              <i class="fa-solid fa-comment input-icon"></i>
              <el-input
                v-model="inputMessage"
                placeholder="输入消息..."
                :disabled="!wsConnected"
                @keyup.enter="sendMessage"
                clearable
                class="message-input"
              >
                <template #append>
                  <el-button class="send-btn" @click="sendMessage" :disabled="!wsConnected || !inputMessage.trim()">
                    <i class="fa-solid fa-paper-plane"></i>
                    <span>发送</span>
                  </el-button>
                </template>
              </el-input>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTeamMembers } from '@/api/teamService'
import wsService from '@/api/websocket'
import { getUser } from '@/api/authService'
import apiClient from '@/api/httpClient'
import { ElMessage } from 'element-plus'
import { getUserProfilesBatch } from '@/api/userProfileService'
import { getUsersBatch } from '@/api/userService'

const route = useRoute()
const router = useRouter()
const teamId = route.params.id
const user = getUser()

const team = ref(null)
const members = ref([])
const visibleMembers = computed(() => [
  {
    id: 'ai_assistant',
    userId: 'ai_assistant',
    nickname: 'AI助手',
    role: 'assistant',
    senderType: 'AI'
  },
  ...members.value
])
const messages = ref([])
const inputMessage = ref('')
const wsConnected = ref(false)
const messageContainer = ref(null)

// 用户头像缓存
const userAvatarCache = ref({})

const scrollToBottom = () => {
  nextTick(() => {
    const container = messageContainer.value
    if (container) container.scrollTop = container.scrollHeight
  })
}

const aiThinking = ref(false)
const aiOnline = ref(false) // AI助手在线状态

const showInitialAIMessage = () => {
  if (messages.value.some(msg => msg.senderType === 'AI' && msg.isInitialAI)) return

  aiOnline.value = true
  messages.value.unshift({
    type: 'AI_MESSAGE',
    senderType: 'AI',
    senderId: 'ai_assistant',
    senderName: 'AI助手',
    content: '大家好，我是队伍聊天助手，已经加入当前队伍聊天。',
    timestamp: Date.now(),
    isMine: false,
    isInitialAI: true
  })
  scrollToBottom()
}

// 从路由状态获取队伍信息（从列表页传过来）
if (route.state?.team) {
  team.value = route.state.team
} else {
  // 如果没有传入，使用默认值，等待onMounted中获取真实数据
  team.value = { id: teamId, name: '队伍' + teamId, accessLevel: 0 }
}

const accessLevelText = (level) => {
  const map = { 0: '公开', 1: '审核', 2: '私有' }
  return map[level] || '未知'
}

const accessLevelTag = (level) => {
  const map = { 0: 'success', 1: 'warning', 2: 'danger' }
  return map[level] || 'info'
}

const formatTime = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {hour: '2-digit', minute: '2-digit'})
}

const sendMessage = () => {
  const content = inputMessage.value.trim()
  if (!content || !wsConnected.value) return

  // 立即在界面上显示用户发送的消息
  const userMessage = {
    content: content,
    senderId: user?.id,
    senderName: user?.nickname || user?.username || `用户${user?.id}`,
    senderType: 'USER',
    timestamp: Date.now(),
    type: 'MESSAGE',
    isMine: true
  }
  
  messages.value.push(userMessage)
  scrollToBottom()
  
  const sent = wsService.send(content, 'MESSAGE')
  if (!sent) {
    // 如果发送失败，移除刚添加的消息并显示错误
    messages.value.pop()
    ElMessage.error('消息发送失败，请检查连接')
  } else {
    inputMessage.value = ''
  }
}

// WebSocket 事件处理
let unbindConnect, unbindMessage, unbindSystem, unbindError, unbindClose, unbindAIThinking, unbindAITrigger

onMounted(async () => {
  showInitialAIMessage()

  // 先加载队伍信息（如果路由状态中没有传入的话）
  if (!route.state?.team) {
    try {
      const teamRes = await apiClient.get(`/api/v1/teams/${teamId}`)
      team.value = teamRes.data
    } catch (e) {
      console.error('获取队伍信息失败:', e)
      // 保持默认值
      team.value = { id: teamId, name: '队伍' + teamId, accessLevel: 0 }
      ElMessage.error(e.message || '获取队伍信息失败')
    }
  }

  // 加载成员列表
  try {
    const res = await getTeamMembers(teamId)
    const teamMembers = res.data || []
    
    // 获取所有成员的用户资料以获取头像信息
    const userIds = teamMembers.map(member => member.userId)
    if (userIds.length > 0) {
      try {
        const profilesRes = await getUserProfilesBatch(userIds)
        const profilesMap = {}
        if (profilesRes.code === 200 && Array.isArray(profilesRes.data)) {
          profilesRes.data.forEach(profile => {
            profilesMap[profile.userId] = profile
          })
        }
        
        // 同时获取用户基本信息（包括username）
        const usersRes = await getUsersBatch(userIds)
        const usersMap = {}
        if (usersRes.code === 200 && Array.isArray(usersRes.data)) {
          usersRes.data.forEach(user => {
            usersMap[user.id] = user
          })
        }
        
        // 将头像和用户名信息合并到成员列表中
        members.value = teamMembers.map(member => {
          const profile = profilesMap[member.userId]
          const userInfo = usersMap[member.userId]
          return {
            ...member,
            avatar: profile?.avatar || null,
            nickname: userInfo?.username || `用户${member.userId}`
          }
        })
      } catch (profileErr) {
        console.error('获取成员信息失败:', profileErr)
        // 如果获取信息失败，仍然显示基本信息
        members.value = teamMembers
      }
    } else {
      members.value = teamMembers
    }
  } catch (e) {
    ElMessage.error(e.message || '获取成员列表失败')
  }

  // 注册WebSocket事件
  unbindConnect = wsService.on('connect', (data) => {
    wsConnected.value = true
    console.log('已连接到聊天服务器')
    // 可选：如果需要显示连接成功的系统消息，取消下面的注释
    // messages.value.push({type: 'SYSTEM', message: '已连接到聊天服务器', timestamp: Date.now()})
    // scrollToBottom()
  })

  unbindMessage = wsService.on('message', (data) => {
    console.log('Received message data:', data)  // 调试日志
    console.log('Current user id:', user?.id)    // 调试日志
    
    // 使用多种可能的字段名来判断是否是自己的消息
    const isMine = String(data.senderUserId) === String(user?.id) || 
                   String(data.senderId) === String(user?.id) ||
                   String(data.sender_user_id) === String(user?.id) ||
                   String(data.sender_id) === String(user?.id)
    
    console.log('Is mine:', isMine)  // 调试日志
    
    // 检查是否是刚刚发送的消息（避免重复显示）
    const lastMessage = messages.value[messages.value.length - 1]
    if (lastMessage && lastMessage.content === data.content && isMine) {
      // 如果是重复消息，只更新时间戳和类型，不添加新条目
      lastMessage.timestamp = data.timestamp || Date.now()
      lastMessage.type = data.type || 'MESSAGE'
      return
    }
    
    // 构建消息对象
    let processedMessage = {
      ...data,
      isMine,
      senderType: data.senderType || (isMine ? 'USER' : 'USER'),
      senderName: data.senderName || (isMine ? (user?.nickname || user?.username || `用户${user?.id}`) : `未知伙伴`)
    }
    
    // 如果是用户消息且不是自己的消息，尝试从缓存获取头像和用户名
    if (!isMine && data.senderId && data.senderType !== 'AI') {
      const cachedAvatar = userAvatarCache.value[data.senderId]
      if (cachedAvatar) {
        processedMessage.senderAvatar = cachedAvatar
      }
      
      // 如果没有用户名，尝试从成员列表中获取
      if (!processedMessage.senderName || processedMessage.senderName === '未知伙伴') {
        const member = members.value.find(m => m.userId === data.senderId)
        if (member) {
          processedMessage.senderName = member.nickname || member.username || `用户${member.userId}`
        } else {
          // 如果成员列表中没有找到，尝试单独获取用户信息
          getUserById(data.senderId).then(userRes => {
            if (userRes.code === 200 && userRes.data) {
              const user = userRes.data
              // 更新消息中的用户名
              const messageIndex = messages.value.findIndex(msg => msg === processedMessage)
              if (messageIndex !== -1) {
                messages.value[messageIndex].senderName = user.username || `用户${user.id}`
              }
            }
          }).catch(err => {
            console.error('获取用户信息失败:', err)
          })
        }
      }
    }

    // 更新AI在线状态
    if (data.senderType === 'AI') {
      aiOnline.value = true
    }

    messages.value.push(processedMessage)
    scrollToBottom()
  })

  unbindSystem = wsService.on('system', (data) => {
    messages.value.push(data)

    // 处理AI相关系统消息
    if (data.message?.includes('AI') || data.message?.includes('助手')) {
      aiOnline.value = true
    }

    scrollToBottom()
  })

  unbindError = wsService.on('error', (data) => {
    ElMessage.error(data.message || '连接出错')
  })

  unbindClose = wsService.on('close', () => {
    wsConnected.value = false
    messages.value.push({type: 'SYSTEM', message: '连接已断开', timestamp: Date.now()})
    scrollToBottom()
  })

  // AI思考状态
  unbindAIThinking = wsService.on('ai_thinking', (data) => {
    aiThinking.value = data.thinking || false
    if (data.thinking) {
      aiOnline.value = true // AI开始思考表示在线
    }
  })

  // AI触发状态
  unbindAITrigger = wsService.on('ai_trigger', (data) => {
    if (data.type === 'AI_TRIGGER') {
      messages.value.push({
        type: 'SYSTEM',
        message: 'AI助手已收到触发信号，正在准备参与对话...',
        timestamp: Date.now()
      })
      scrollToBottom()
    }
  })

  // 建立WebSocket连接
  if (user?.id) {
    wsService.connect(teamId, user.id)
  }
})

onUnmounted(() => {
  unbindConnect?.()
  unbindMessage?.()
  unbindSystem?.()
  unbindError?.()
  unbindClose?.()
  unbindAIThinking?.()
  unbindAITrigger?.()
  wsService.disconnect()
})
</script>

<style scoped>
.team-detail-page {
  padding: 24px;
  min-height: 100vh;
  background: #0f172a;
}

.team-detail-content {
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 24px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  color: #94a3b8;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #f1f5f9;
}

.header-info h2 {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 700;
  color: #f1f5f9;
}

.header-info p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.access-tag {
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
}

:deep(.access-tag.el-tag--success) {
  background: rgba(34, 197, 94, 0.15);
  border-color: rgba(34, 197, 94, 0.3);
  color: #4ade80;
}

:deep(.access-tag.el-tag--warning) {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.3);
  color: #fbbf24;
}

:deep(.access-tag.el-tag--danger) {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.3);
  color: #f87171;
}

/* 卡片通用样式 */
.info-card,
.member-card,
.chat-card {
  padding: 24px;
  border-radius: 16px;
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-icon {
  width: 36px;
  height: 36px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
  font-size: 16px;
}

.header-icon.chat {
  background: linear-gradient(135deg, rgba(236, 72, 153, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  color: #ec4899;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.member-count {
  margin-left: auto;
  padding: 4px 12px;
  background: rgba(14, 165, 233, 0.15);
  border-radius: 20px;
  font-size: 12px;
  color: #0ea5e9;
}

/* 队伍信息卡片 */
.team-info-card {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.08) 0%, rgba(59, 130, 246, 0.05) 100%);
  border: 1px solid rgba(14, 165, 233, 0.25);
  box-shadow: 0 8px 32px rgba(14, 165, 233, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1);
  margin-bottom: 24px;
}

.info-header {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.15) 0%, rgba(59, 130, 246, 0.1) 100%);
  border-bottom: 2px solid rgba(14, 165, 233, 0.3);
  padding: 18px 20px;
}

.info-icon {
  background: linear-gradient(135deg, #0ea5e9 0%, #3b82f6 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(14, 165, 233, 0.4);
}

.info-content {
  padding: 8px 0;
}

.info-content .info-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 14px 20px;
  border-bottom: 1px solid rgba(14, 165, 233, 0.1);
  transition: background 0.2s ease;
}

.info-content .info-item:hover {
  background: rgba(14, 165, 233, 0.05);
}

.info-content .info-item:last-child {
  border-bottom: none;
}

.info-content .info-label {
  color: #94a3b8;
  font-size: 13px;
  font-weight: 500;
}

.info-content .info-value {
  color: #f1f5f9;
  font-size: 14px;
  font-weight: 500;
}

/* 成员列表卡片 */
.members-list-card {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.08) 0%, rgba(168, 85, 247, 0.05) 100%);
  border: 1px solid rgba(139, 92, 246, 0.25);
  box-shadow: 0 8px 32px rgba(139, 92, 246, 0.15), inset 0 1px 0 rgba(255, 255, 255, 0.1);
}

.members-header {
  background: linear-gradient(135deg, rgba(139, 92, 246, 0.15) 0%, rgba(168, 85, 247, 0.1) 100%);
  border-bottom: 2px solid rgba(139, 92, 246, 0.3);
  padding: 18px 20px;
}

.members-icon {
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.4);
}

/* 成员列表 */
.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #94a3b8;
  background: rgba(139, 92, 246, 0.05);
  border-radius: 12px;
  margin: 16px 0;
}

.empty-state i {
  font-size: 40px;
  margin-bottom: 16px;
  color: #8b5cf6;
  opacity: 0.6;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
  color: #94a3b8;
}

.member-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
  background: rgba(139, 92, 246, 0.05);
  border: 1px solid rgba(139, 92, 246, 0.1);
}

.member-item:hover {
  background: rgba(139, 92, 246, 0.12);
  border-color: rgba(139, 92, 246, 0.25);
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.15);
}

.member-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ffffff;
  font-size: 16px;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
  overflow: hidden;
}

.member-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.member-info {
  flex: 1;
}

.member-name {
  display: block;
  color: #f1f5f9;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 2px;
}

.member-role {
  display: inline-block;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(139, 92, 246, 0.15);
  color: #a78bfa;
  font-weight: 500;
}

.member-role.admin {
  background: linear-gradient(135deg, rgba(248, 113, 113, 0.2) 0%, rgba(239, 68, 68, 0.15) 100%);
  color: #f87171;
  border: 1px solid rgba(248, 113, 113, 0.3);
}

.member-role.ai {
  background: rgba(14, 165, 233, 0.18);
  color: #38bdf8;
  border: 1px solid rgba(14, 165, 233, 0.35);
}

/* 聊天卡片 */
.chat-card {
  height: calc(100vh - 200px);
  display: flex;
  flex-direction: column;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ef4444;
}

.status-dot.online {
  background: #22c55e;
}

.status-text {
  font-size: 12px;
  color: #64748b;
}

/* 聊天消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: rgba(255, 255, 255, 0.02);
  border-radius: 12px;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 3px;
}

/* 消息项 */
.message-item {
  margin-bottom: 16px;
}

/* 系统消息 */
.system-message {
  text-align: center;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  font-size: 12px;
  color: #64748b;
}

/* 聊天消息 */
.chat-message {
  display: flex;
  gap: 12px;
}

.my-message .chat-message {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.my-message .message-avatar {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.3) 0%, rgba(139, 92, 246, 0.3) 100%);
  color: #0ea5e9;
}

.my-message .message-content {
  text-align: right;
}

.my-message .message-header {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 14px;
  flex-shrink: 0;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.ai-message .message-avatar {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.3) 0%, rgba(139, 92, 246, 0.3) 100%);
  color: #0ea5e9;
}

.message-content {
  max-width: 70%;
  min-width: 0;
}

.my-message .message-content {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.message-header {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
}

.my-message .message-header {
  flex-direction: row-reverse;
}

.sender-name {
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
}

.ai-message .sender-name {
  color: #0ea5e9;
}

.message-time {
  font-size: 12px;
  color: #475569;
}

.message-body {
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  word-break: break-all;
  font-size: 14px;
  color: #f1f5f9;
}

.ai-body {
  background: rgba(14, 165, 233, 0.1);
  border-color: rgba(14, 165, 233, 0.2);
}

.my-body {
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border-color: transparent;
}

/* 聊天输入区域 */
.chat-input {
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #64748b;
  z-index: 1;
}

:deep(.message-input .el-input__wrapper) {
  padding-left: 44px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
}

:deep(.message-input .el-input__wrapper:focus) {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
}

:deep(.message-input .el-input__inner) {
  color: #f1f5f9;
  background: transparent;
}

:deep(.message-input .el-input__inner::placeholder) {
  color: #64748b;
}

.send-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.send-btn:hover:not([disabled]) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(14, 165, 233, 0.4);
}

.send-btn[disabled] {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
