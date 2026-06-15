<!-- src/views/ChatHistory.vue -->
<template>
  <div class="chat-history-page">
    <!-- 页面标题 -->
    <div class="page-header glass-effect">
      <div class="header-icon">
        <i class="fa-solid fa-history"></i>
      </div>
      <div class="header-content">
        <h2>聊天历史</h2>
        <p>查看和管理您的聊天记录</p>
      </div>
    </div>

    <!-- 功能选项卡 -->
    <div class="tabs-container glass-effect">
      <el-tabs v-model="activeTab" class="history-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="队伍聊天记录" name="team" />
        <el-tab-pane label="私聊记录" name="private" />
        <el-tab-pane label="统计信息" name="stats" />
      </el-tabs>

      <!-- 队伍聊天记录面板 -->
      <div v-if="activeTab === 'team'" class="history-panel">
        <div class="search-section">
          <div class="search-wrapper">
            <i class="fa-solid fa-search search-icon"></i>
            <el-input
                v-model="teamSearchId"
                placeholder="输入队伍ID"
                class="search-input"
                @keyup.enter="fetchTeamHistory"
            />
          </div>
          <el-button type="primary" class="search-btn" @click="fetchTeamHistory">
            <i class="fa-solid fa-search"></i>
            <span>搜索</span>
          </el-button>
        </div>

        <div v-if="teamHistory.length === 0" class="empty-state">
          <div class="empty-icon">
            <i class="fa-solid fa-comments"></i>
          </div>
          <p v-if="!teamSearchId">请输入队伍ID进行搜索</p>
          <p v-else>暂无相关聊天记录</p>
        </div>

        <div v-else class="history-list">
          <ChatHistoryItem
              v-for="message in teamHistory"
              :key="message.id || message.timestamp"
              :sender-id="message.senderId || message.sender || 'Unknown'"
              :sender-name="getSenderName(message)"
              :content="message.content || message.text || ''"
              :timestamp="message.timestamp || message.createTime || new Date().toISOString()"
              :is-user="isHumanMessage(message)"
          />
        </div>

        <!-- 分页 -->
        <el-pagination
            v-if="totalTeamRecords > 0"
            class="pagination"
            layout="prev, pager, next"
            :total="totalTeamRecords"
            :page-size="teamPageSize"
            v-model:current-page="teamCurrentPage"
            @current-change="handleTeamPageChange"
        />
      </div>

      <!-- 私聊记录面板 -->
      <div v-if="activeTab === 'private'" class="history-panel">
        <div class="search-section">
          <el-select v-model="selectedUserId" placeholder="选择用户" class="search-select" filterable>
            <el-option
                v-for="user in userList"
                :key="user.id"
                :label="user.username || `用户${user.id}`"
                :value="user.id"
            >
              <div class="user-option">
                <div class="user-info">
                  <span class="option-label">{{ user.username || `用户${user.id}` }}</span>
                </div>
              </div>
            </el-option>
          </el-select>
          <el-select v-model="selectedAiId" placeholder="选择智能助手" class="search-select" filterable popper-class="glass-popper">
            <el-option
                v-for="ai in aiList"
                :key="ai.id"
                :label="ai.title"
                :value="ai.id"
            >
              <div class="ai-option">
                <div class="ai-info">
                  <span class="option-label">{{ ai.title }}</span>
                </div>
              </div>
            </el-option>
          </el-select>
          <el-button type="primary" class="search-btn" @click="fetchPrivateHistory">
            <i class="fa-solid fa-search"></i>
            <span>搜索</span>
          </el-button>
        </div>

        <div v-if="privateHistory.length === 0" class="empty-state">
          <div class="empty-icon">
            <i class="fa-solid fa-comments"></i>
          </div>
          <p>暂无私聊记录</p>
          <p class="empty-hint">选择用户和AI助手后进行搜索</p>
        </div>

        <div v-else class="history-list">
          <ChatHistoryItem
              v-for="message in privateHistory"
              :key="message.id || message.timestamp"
              :sender-id="message.senderId || message.sender || 'Unknown'"
              :sender-name="getSenderName(message)"
              :content="message.content || message.text || ''"
              :timestamp="message.timestamp || message.createTime || new Date().toISOString()"
              :is-user="isHumanMessage(message)"
          />
        </div>

        <!-- 分页 -->
        <el-pagination
            v-if="totalPrivateRecords > 0"
            class="pagination"
            layout="prev, pager, next"
            :total="totalPrivateRecords"
            :page-size="privatePageSize"
            v-model:current-page="privateCurrentPage"
            @current-change="handlePrivatePageChange"
        />
      </div>

      <!-- 统计信息面板 -->
      <div v-if="activeTab === 'stats'" class="stats-panel">
        <div class="stat-cards">
          <div class="stat-card">
            <div class="stat-icon">
              <i class="fa-solid fa-users"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userStats.teamCount || 0 }}</div>
              <div class="stat-label">参与队伍数</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon">
              <i class="fa-solid fa-message"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ userStats.messageCount || 0 }}</div>
              <div class="stat-label">我发送的消息数</div>
            </div>
          </div>
        </div>

        <div class="stat-actions">
          <el-button type="danger" class="cleanup-btn" @click="showCleanupDialog = true">
            <i class="fa-solid fa-trash"></i>
            <span>清理过期记录</span>
          </el-button>
        </div>
      </div>
    </div>

    <!-- 清理对话框 -->
    <el-dialog v-model="showCleanupDialog" title="清理过期记录" width="400px" class="cleanup-dialog">
      <p>请输入要保留的天数：</p>
      <el-input-number v-model="retentionDays" :min="1" :max="365" class="days-input" />
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showCleanupDialog = false">取消</el-button>
          <el-button type="danger" @click="performCleanup">确认清理</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ChatHistoryItem from '@/components/ChatHistoryItem.vue'
import {
  getTeamChatHistory,
  getUserAiChatHistory,
  getUserChatStats,
  cleanupChatHistory
} from '@/api/chatHistoryService'
import { getUser } from '@/api/authService'
import { getAllUsers } from '@/api/userService'
import { agents } from '@/config/agents'

// 当前激活的标签页
const activeTab = ref('team')

// 队伍聊天记录相关
const teamSearchId = ref('')
const teamHistory = ref([])
const teamCurrentPage = ref(1)
const teamPageSize = ref(20)
const totalTeamRecords = ref(0)

// 私聊记录相关
const selectedUserId = ref('')
const selectedAiId = ref('')
const privateHistory = ref([])
const privateCurrentPage = ref(1)
const privatePageSize = ref(20)
const totalPrivateRecords = ref(0)

// 统计信息相关
const userStats = ref({})
const showCleanupDialog = ref(false)
const retentionDays = ref(30)

// 用户和AI列表
const userList = ref([])
const aiList = ref(agents.map(agent => ({
  id: agent.id,
  name: agent.name,
  title: agent.title
})))
const userNameMap = ref({})

const normalizeSenderType = (message) => {
  return String(message?.senderType || message?.sender || '').toUpperCase()
}

const isHumanMessage = (message) => {
  const senderType = normalizeSenderType(message)
  return senderType === 'HUMAN' || senderType === 'USER'
}

const isAiMessage = (message) => {
  const senderType = normalizeSenderType(message)
  return senderType === 'AI' || senderType === 'ASSISTANT' || String(message?.senderId || '').startsWith('ai_')
}

const getSenderName = (message) => {
  if (message?.senderName) return message.senderName
  if (isAiMessage(message)) return 'AI助手'

  const senderId = message?.senderId || message?.sender
  if (!senderId) return '未知用户'

  return userNameMap.value[String(senderId)] || `用户${senderId}`
}

// 格式化时间戳
const formatDate = (timestamp) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 切换标签页
const handleTabChange = (tabName) => {
  activeTab.value = tabName
  if (tabName === 'team') {
    fetchTeamHistory()
  } else if (tabName === 'private') {
    fetchPrivateHistory()
  } else if (tabName === 'stats') {
    fetchUserStats()
  }
}

// 获取队伍聊天记录
const fetchTeamHistory = async () => {
  if (!teamSearchId.value) {
    ElMessage.warning('请输入队伍ID')
    return
  }

  try {
    const response = await getTeamChatHistory(
        teamSearchId.value,
        teamCurrentPage.value - 1,
        teamPageSize.value
    )

    if (response.code === 200) {
      teamHistory.value = response.data || []
      // 这里需要根据实际API返回的数据结构进行调整
      // 如果API返回总数，应该类似这样：
      // totalTeamRecords.value = response.total || response.data?.length || 0
    } else {
      ElMessage.error(response.message || '获取队伍聊天记录失败')
    }
  } catch (error) {
    console.error('获取队伍聊天记录失败:', error)
    ElMessage.error('获取队伍聊天记录失败')
  }
}

// 获取私聊记录
const fetchPrivateHistory = async () => {
  if (!selectedUserId.value || !selectedAiId.value) {
    ElMessage.warning('请选择用户和AI助手')
    return
  }

  try {
    const response = await getUserAiChatHistory(
        selectedUserId.value,
        selectedAiId.value,
        privateCurrentPage.value - 1,
        privatePageSize.value
    )

    if (response.code === 200) {
      privateHistory.value = response.data || []
      // 这里需要根据实际API返回的数据结构进行调整
      // totalPrivateRecords.value = response.total || response.data?.length || 0
    } else {
      ElMessage.error(response.message || '获取私聊记录失败')
    }
  } catch (error) {
    console.error('获取私聊记录失败:', error)
    ElMessage.error('获取私聊记录失败')
  }
}

// 获取用户统计信息
const fetchUserStats = async () => {
  const currentUser = getUser()
  if (!currentUser) {
    ElMessage.error('用户未登录')
    return
  }

  try {
    const response = await getUserChatStats(currentUser.id)

    if (response.code === 200) {
      userStats.value = response.data || {}
    } else {
      ElMessage.error(response.message || '获取统计信息失败')
    }
  } catch (error) {
    console.error('获取统计信息失败:', error)
    ElMessage.error('获取统计信息失败')
  }
}

// 处理队伍记录分页
const handleTeamPageChange = (page) => {
  teamCurrentPage.value = page
  fetchTeamHistory()
}

// 处理私聊记录分页
const handlePrivatePageChange = (page) => {
  privateCurrentPage.value = page
  fetchPrivateHistory()
}

// 执行清理操作
const performCleanup = async () => {
  try {
    const response = await cleanupChatHistory(retentionDays.value)

    if (response.code === 200) {
      ElMessage.success(response.message || '清理成功')
      showCleanupDialog.value = false
      retentionDays.value = 30 // 重置为默认值
    } else {
      ElMessage.error(response.message || '清理失败')
    }
  } catch (error) {
    console.error('清理聊天记录失败:', error)
    ElMessage.error('清理聊天记录失败')
  }
}

// 初始化数据
const loadUsers = async () => {
  try {
    const res = await getAllUsers()
    if (res.code === 200 && Array.isArray(res.data)) {
      userList.value = res.data
      userNameMap.value = res.data.reduce((map, user) => {
        map[String(user.id)] = user.username || `用户${user.id}`
        return map
      }, {})
    } else {
      userList.value = []
      userNameMap.value = {}
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    userList.value = []
    userNameMap.value = {}
  }
}

onMounted(() => {
  const currentUser = getUser()
  if (currentUser) {
    loadUsers()
    fetchUserStats()
  }
})
</script>

<style scoped>
.chat-history-page {
  padding: 24px;
  min-height: 100vh;
  background: #0f172a;
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

.header-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
  font-size: 24px;
}

.header-content h2 {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 700;
  color: #f1f5f9;
}

.header-content p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

/* 标签容器 */
.tabs-container {
  border-radius: 16px;
  padding: 24px;
}

/* 标签页样式 */
:deep(.history-tabs) {
  .el-tabs__header {
    margin-bottom: 24px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .el-tabs__item {
    color: #64748b;
    font-weight: 500;
    padding: 0 24px;
    margin-right: 16px;
    border-radius: 8px 8px 0 0;
    transition: all 0.3s ease;
  }

  .el-tabs__item:hover {
    color: #94a3b8;
  }

  .el-tabs__item.is-active {
    color: #0ea5e9;
    background: rgba(14, 165, 233, 0.1);
  }

  .el-tabs__active-bar {
    background: linear-gradient(90deg, #0ea5e9 0%, #8b5cf6 100%);
    height: 3px;
  }
}

/* 搜索区域 */
.search-section {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.search-wrapper {
  position: relative;
  flex: 1;
  min-width: 200px;
  max-width: 300px;
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #64748b;
}

:deep(.search-input .el-input__wrapper) {
  padding-left: 44px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
}

:deep(.search-input .el-input__wrapper:focus) {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
}

:deep(.search-input .el-input__inner) {
  color: #f1f5f9;
}

.search-select {
  min-width: 180px;
  border-radius: 12px;
}

:deep(.search-select .el-select__wrapper) {
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: none;
  min-height: 36px;
  padding: 4px 12px;
}

:deep(.search-select .el-select__wrapper:hover) {
  border-color: rgba(14, 165, 233, 0.4);
}

:deep(.search-select .el-select__wrapper.is-focused) {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
}

:deep(.search-select .el-select__placeholder) {
  color: #64748b;
}

:deep(.search-select .el-select__text) {
  color: #f1f5f9;
}

:deep(.search-select .el-select__caret) {
  color: #64748b;
  font-size: 14px;
}

:deep(.search-select .el-select__tags-text) {
  color: #f1f5f9;
}

/* 下拉弹出层 */
:deep(.search-select .el-select-dropdown) {
  background: rgba(30, 41, 59, 0.95) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(14, 165, 233, 0.3) !important;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  padding: 6px;
}

:deep(.search-select .el-select-dropdown__list) {
  padding: 0;
}

:deep(.search-select .el-select-dropdown__item) {
  color: #e2e8f0 !important;
  background: transparent;
  border-radius: 8px;
  padding: 8px 12px;
  margin: 2px 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  line-height: 1.4;
  min-height: auto;
}

:deep(.search-select .el-select-dropdown__item span) {
  color: #e2e8f0 !important;
}

:deep(.search-select .el-popper .el-select-dropdown__item span) {
  color: #e2e8f0 !important;
}

:deep(.search-select .el-select-dropdown__item.hover) {
  background: rgba(14, 165, 233, 0.3) !important;
  color: #38bdf8;
  border-radius: 8px;
  margin: 0 4px;
  backdrop-filter: blur(5px);
}

:deep(.search-select .el-select-dropdown__item:hover) {
  background: rgba(14, 165, 233, 0.3) !important;
  color: #38bdf8;
  border-radius: 8px;
  margin: 0 4px;
  backdrop-filter: blur(5px);
}

:deep(.search-select .el-select-dropdown__item.selected) {
  background: rgba(14, 165, 233, 0.4) !important;
  color: #fff;
}

:deep(.search-select .el-select-dropdown__item.selected) {
  color: #0ea5e9;
  background: rgba(14, 165, 233, 0.15);
  font-weight: 600;
}

:deep(.search-select .el-popper.is-light) {
  border: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.search-select .el-popper.is-light .el-popper__arrow::before) {
  background: #1e293b;
  border-color: rgba(255, 255, 255, 0.1);
}

/* 可搜索模式下输入框 */
:deep(.search-select .el-select-dropdown__search) {
  border-bottom-color: rgba(255, 255, 255, 0.1);
}

:deep(.search-select .el-select-dropdown__search .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}

:deep(.search-select .el-select-dropdown__search .el-input__inner) {
  color: #f1f5f9;
}

/* 选项自定义模板 */
.user-option,
.ai-option {
  width: 100%;
}

.user-info,
.ai-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.option-label {
  font-size: 14px;
  font-weight: 500;
  color: #f1f5f9 !important;
}

.option-id {
  font-size: 11px;
  color: #64748b !important;
}

/* 强制覆盖element-plus默认样式 - 应用到所有下拉框 */
:deep(.el-popper),
:deep(.el-select-dropdown) {
  background: rgba(30, 41, 59, 0.98) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(14, 165, 233, 0.3) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4) !important;
}

:deep(.el-popper .el-select-dropdown__item),
:deep(.el-select-dropdown__item) {
  color: #f1f5f9 !important;
  background: transparent !important;
}

:deep(.el-popper .el-select-dropdown__item:hover),
:deep(.el-select-dropdown__item:hover) {
  background: rgba(14, 165, 233, 0.3) !important;
  color: #38bdf8 !important;
}

:deep(.el-popper .el-select-dropdown__item.selected),
:deep(.el-select-dropdown__item.selected) {
  background: rgba(14, 165, 233, 0.4) !important;
  color: #fff !important;
  font-weight: 600;
}

:deep(.el-popper .el-select-dropdown__item span),
:deep(.el-select-dropdown__item span) {
  color: #f1f5f9 !important;
}

.search-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
  border-radius: 12px;
  font-weight: 500;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 48px 20px;
}

.empty-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 28px;
}

.empty-state p {
  margin: 0 0 8px 0;
  color: #64748b;
  font-size: 14px;
}

.empty-hint {
  font-size: 12px !important;
  color: #475569 !important;
}

/* 历史记录列表 */
.history-list {
  max-height: 50vh;
  overflow-y: auto;
  padding-right: 8px;
}

.history-list::-webkit-scrollbar {
  width: 6px;
}

.history-list::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.history-list::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.3);
  border-radius: 3px;
}

/* 分页 */
.pagination {
  margin-top: 20px;
  text-align: center;
}

:deep(.pagination .el-pager li) {
  background: rgba(255, 255, 255, 0.05);
  color: #64748b;
  border-radius: 8px;
}

:deep(.pagination .el-pager li:hover) {
  background: rgba(14, 165, 233, 0.2);
  color: #0ea5e9;
}

:deep(.pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  color: #fff;
}

/* 统计卡片 */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  background: rgba(14, 165, 233, 0.08);
  border-color: rgba(14, 165, 233, 0.3);
}

.stat-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.3) 0%, rgba(139, 92, 246, 0.3) 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
  font-size: 20px;
}

.stat-icon.messages {
  background: linear-gradient(135deg, rgba(236, 72, 153, 0.3) 0%, rgba(139, 92, 246, 0.3) 100%);
  color: #ec4899;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #f1f5f9;
}

.stat-label {
  font-size: 13px;
  color: #64748b;
}

/* 统计操作 */
.stat-actions {
  text-align: center;
}

.cleanup-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 12px;
}

:deep(.cleanup-btn.el-button--danger) {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.3);
  color: #f87171;
}

:deep(.cleanup-btn.el-button--danger:hover) {
  background: rgba(239, 68, 68, 0.25);
}

/* 对话框样式 */
:deep(.cleanup-dialog) {
  background: #1e293b;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.cleanup-dialog .el-dialog__header) {
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.cleanup-dialog .el-dialog__title) {
  color: #f1f5f9;
}

:deep(.cleanup-dialog p) {
  color: #94a3b8;
}

:deep(.days-input .el-input-number) {
  width: 100%;
}

:deep(.days-input .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
}

:deep(.days-input .el-input__inner) {
  color: #f1f5f9;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>

<style>
/* 全局样式 - 强制覆盖 Element Plus 下拉框白色背景 */
.el-select-dropdown {
  background-color: rgba(30, 41, 59, 0.98) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(14, 165, 233, 0.3) !important;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4) !important;
}

.el-select-dropdown__item {
  color: #f1f5f9 !important;
  background-color: transparent !important;
}

.el-select-dropdown__item:hover,
.el-select-dropdown__item.hover {
  background-color: rgba(14, 165, 233, 0.3) !important;
  color: #38bdf8 !important;
}

.el-select-dropdown__item.selected {
  background-color: rgba(14, 165, 233, 0.4) !important;
  color: #ffffff !important;
  font-weight: 600;
}

.el-select-dropdown__item span {
  color: #f1f5f9 !important;
}

/* 确保箭头也是深色 */
.el-popper.is-light .el-popper__arrow::before {
  background-color: rgba(30, 41, 59, 0.98) !important;
  border-color: rgba(14, 165, 233, 0.3) !important;
}
</style>
