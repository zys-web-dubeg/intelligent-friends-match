<template>
  <AppLayout>
    <div class="partner-page">
      <!-- 页面头部 -->
      <div class="page-header glass-effect">
        <div class="header-icon">
          <i class="fa-solid fa-handshake"></i>
        </div>
        <div class="header-content">
          <h2>人人伙伴匹配</h2>
          <p>发现志同道合的伙伴，开启新的旅程</p>
        </div>
        <div class="header-stats">
          <div class="header-stat">
            <span>AI匹配</span>
            <strong>对话推荐</strong>
          </div>
          <div class="header-stat">
            <span>画像维度</span>
            <strong>多维评分</strong>
          </div>
          <div class="header-stat">
            <span>请求流转</span>
            <strong>集中处理</strong>
          </div>
        </div>
      </div>

      <!-- 匹配类型选择器（推荐页使用） -->
      <div v-if="activeTab === 'recommend'" class="match-type-bar glass-effect">
        <div
          v-for="type in matchTypes"
          :key="type.value"
          class="match-type-chip"
          :class="{ active: currentMatchType === type.value }"
          @click="switchMatchType(type.value)"
        >
          <i :class="type.icon"></i>
          <span>{{ type.label }}</span>
        </div>
      </div>

      <!-- 标签页 -->
      <div class="tabs-container glass-effect">
        <el-tabs v-model="activeTab" class="partner-tabs" @tab-change="onTabChange">
          <!-- AI智能匹配 -->
          <el-tab-pane label="AI智能匹配" name="ai-match">
            <div class="ai-chat-container">
              <!-- 聊天头部 -->
              <div class="ai-chat-header">
                <div class="ai-chat-header-left">
                  <div class="ai-chat-avatar">
                    <i class="fa-solid fa-robot"></i>
                  </div>
                  <div class="ai-chat-info">
                    <span class="ai-chat-name">智能伙伴匹配助手</span>
                    <span class="ai-chat-desc">根据画像、兴趣和目标推荐伙伴与队伍</span>
                  </div>
                </div>
                <div class="ai-chat-actions">
                  <span class="ai-status-pill">
                    <i class="fa-solid fa-circle"></i>
                    在线
                  </span>
                  <el-button size="small" class="ai-chat-new-btn" @click="newAiChat">
                    <i class="fa-solid fa-plus"></i>
                    <span>新会话</span>
                  </el-button>
                </div>
              </div>

              <!-- 消息区域 -->
              <div class="ai-chat-messages" ref="chatMessageRef">
                <div v-if="aiMessages.length === 0" class="ai-chat-empty">
                  <div class="ai-chat-empty-icon">
                    <i class="fa-solid fa-robot"></i>
                  </div>
                  <p class="ai-chat-empty-title">你好！我是智能伙伴匹配助手</p>
                  <p class="ai-chat-empty-desc">选择一个方向开始，或直接输入你的匹配需求。</p>
                  <div class="ai-chat-suggestions">
                    <div class="suggestion-chip" @click="quickSend('帮我推荐几个适合一起学习的伙伴')">
                      <i class="fa-solid fa-graduation-cap"></i>
                      <span>学习伙伴</span>
                    </div>
                    <div class="suggestion-chip" @click="quickSend('帮我推荐适合我的队伍')">
                      <i class="fa-solid fa-users"></i>
                      <span>适合队伍</span>
                    </div>
                    <div class="suggestion-chip" @click="quickSend('看看我有哪些已匹配的伙伴')">
                      <i class="fa-solid fa-handshake"></i>
                      <span>我的伙伴</span>
                    </div>
                    <div class="suggestion-chip" @click="quickSend('谁给我发了匹配请求')">
                      <i class="fa-solid fa-bell"></i>
                      <span>待处理请求</span>
                    </div>
                  </div>
                </div>
                <div v-for="(msg, index) in aiMessages" :key="index"
                  :class="['ai-message', msg.isUser ? 'user-msg' : 'bot-msg']">
                  <div class="ai-message-avatar">
                    <i :class="msg.isUser ? 'fa-solid fa-user' : 'fa-solid fa-robot'"></i>
                  </div>
                  <div class="ai-message-content">
                    <div class="ai-message-bubble" v-html="msg.content"></div>
                    <div v-if="msg.isTyping" class="ai-typing-dots">
                      <span class="dot"></span>
                      <span class="dot"></span>
                      <span class="dot"></span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 输入区域 -->
              <div class="ai-chat-input-area">
                <el-input
                  v-model="aiInput"
                  placeholder="输入匹配需求，例如：找一个适合一起学习 Vue 的伙伴"
                  class="ai-chat-input"
                  @keyup.enter="sendAiMessage"
                  :disabled="aiSending"
                  type="textarea"
                  :autosize="{ minRows: 1, maxRows: 3 }"
                  resize="none"
                />
                <el-button
                  class="ai-chat-send-btn"
                  @click="sendAiMessage"
                  :disabled="aiSending || !aiInput.trim()"
                >
                  <i class="fa-solid fa-paper-plane"></i>
                </el-button>
              </div>
            </div>
          </el-tab-pane>
          <!-- 推荐伙伴 -->
          <el-tab-pane label="推荐伙伴" name="recommend">
            <div v-if="recommendLoading" class="loading-state">
              <div class="loading-spinner">
                <i class="fa-solid fa-spinner fa-spin"></i>
              </div>
              <p>正在为你寻找最匹配的伙伴...</p>
            </div>
            <div v-else-if="recommendList.length === 0" class="empty-state">
              <div class="empty-icon">
                <i class="fa-solid fa-compass"></i>
              </div>
              <p>暂无推荐伙伴</p>
              <p class="empty-hint">完善伙伴画像可获得更好的推荐</p>
            </div>
            <div v-if="!recommendLoading && recommendList.length > 0" class="batch-bar">
              <el-checkbox
                v-model="allSelected"
                :indeterminate="selectedIds.length > 0 && selectedIds.length < recommendList.length"
                @change="toggleAll"
              >
                全选
              </el-checkbox>
              <span class="batch-info">已选 {{ selectedIds.length }} / {{ recommendList.length }} 人</span>
              <el-button
                size="small"
                class="batch-btn"
                :disabled="selectedIds.length === 0"
                :loading="batchSending"
                @click="handleBatchSend"
              >
                <i class="fa-solid fa-paper-plane"></i>
                <span>批量邀请 ({{ selectedIds.length }})</span>
              </el-button>
            </div>
            <div v-if="recommendList.length > 0" class="partner-grid">
              <div v-for="item in recommendList" :key="item.userProfile?.userId" class="partner-card" :class="{ 'is-selected': selectedIds.includes(item.userProfile?.userId) }">
                <div class="card-top">
                  <div class="user-avatar">
                    <img v-if="item.userProfile?.avatar" :src="item.userProfile.avatar" alt="avatar" />
                    <i v-else class="fa-solid fa-user"></i>
                  </div>
                  <div class="score-badge" :title="'综合评分: ' + item.matchScore">
                    {{ item.matchScore?.toFixed(2) }}
                  </div>
                </div>
                <div class="user-info">
                  <div class="user-id-row">
                    <el-checkbox
                      v-model="selectedIds"
                      :label="item.userProfile?.userId"
                      class="select-checkbox"
                      @click.stop
                    />
                    <span class="user-id">{{ item.userProfile?.username || `伙伴 ${item.userProfile?.userId}` }}</span>
                  </div>
                  <div v-if="item.userProfile?.mbtiType" class="mbti-row">
                    <el-tag size="small" class="mbti-tag">{{ item.userProfile.mbtiType }}</el-tag>
                  </div>
                  <div v-if="item.userProfile?.tags" class="tags-row">
                    <el-tag
                      v-for="(tag, i) in parseTags(item.userProfile.tags)"
                      :key="i"
                      size="small"
                      class="tag-item"
                    >
                      {{ tag }}
                    </el-tag>
                  </div>
                  <div v-if="item.userProfile?.interests" class="info-line">
                    <i class="fa-solid fa-heart"></i>
                    <span>{{ item.userProfile.interests }}</span>
                  </div>
                  <!-- 维度评分 -->
                  <div v-if="item.dimensionScores" class="dimension-scores">
                    <div
                      v-for="(score, key) in item.dimensionScores"
                      :key="key"
                      class="dimension-row"
                    >
                      <span class="dimension-label">{{ key }}</span>
                      <div class="dimension-bar-bg">
                        <div
                          class="dimension-bar-fill"
                          :style="{ width: (score * 100).toFixed(0) + '%' }"
                        ></div>
                      </div>
                      <span class="dimension-value">{{ (score * 100).toFixed(0) }}</span>
                    </div>
                  </div>
                </div>
                <div class="card-actions">
                  <el-button
                    size="small"
                    class="action-btn send-btn"
                    @click="handleSendRequest(item.userProfile.userId)"
                  >
                    <i class="fa-solid fa-handshake"></i>
                    <span>发送邀请</span>
                  </el-button>
                </div>
              </div>
            </div>
            <!-- 底部批量操作栏（粘性） -->
            <div v-if="selectedIds.length > 0 && !recommendLoading" class="sticky-batch-bar">
              <div class="sticky-batch-inner">
                <span class="sticky-batch-info">已选择 <em>{{ selectedIds.length }}</em> 位伙伴</span>
                <el-button
                  class="sticky-batch-btn"
                  :loading="batchSending"
                  @click="handleBatchSend"
                >
                  <i class="fa-solid fa-paper-plane"></i>
                  <span>批量发送邀请</span>
                </el-button>
              </div>
            </div>
          </el-tab-pane>

          <!-- 我的伙伴 -->
          <el-tab-pane label="我的伙伴" name="my-partners">
            <div v-if="partnersLoading" class="loading-state">
              <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i></div>
              <p>加载中...</p>
            </div>
            <div v-else-if="partnerList.length === 0" class="empty-state">
              <div class="empty-icon"><i class="fa-solid fa-users"></i></div>
              <p>暂无伙伴</p>
              <p class="empty-hint">去推荐页面寻找志同道合的伙伴吧</p>
            </div>
            <div v-else class="partner-grid">
              <div v-for="item in partnerList" :key="item.relationId" class="partner-card">
                <div class="card-top">
                  <div class="user-avatar">
                    <img v-if="item.avatar" :src="item.avatar" alt="avatar" />
                    <i v-else class="fa-solid fa-user"></i>
                  </div>
                  <el-tag size="small" class="match-type-tag">{{ matchTypeLabel(item.matchType) }}</el-tag>
                </div>
                <div class="user-info">
                  <div class="user-id">{{ item.username || `伙伴 ${item.partnerUserId}` }}</div>
                  <div v-if="item.message" class="info-line">
                    <i class="fa-solid fa-message"></i>
                    <span>{{ item.message }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <el-button size="small" class="action-btn unmatch-btn" @click="handleUnmatch(item.relationId)">
                    <i class="fa-solid fa-link-slash"></i>
                    <span>解除</span>
                  </el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 收到的请求 -->
          <el-tab-pane :label="`收到的请求${pendingCount > 0 ? ' (' + pendingCount + ')' : ''}`" name="received">
            <div v-if="receivedLoading" class="loading-state">
              <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i></div>
              <p>加载中...</p>
            </div>
            <div v-else-if="receivedList.length === 0" class="empty-state">
              <div class="empty-icon"><i class="fa-solid fa-inbox"></i></div>
              <p>暂无收到的请求</p>
            </div>
            <div v-else class="partner-grid">
              <div v-for="item in receivedList" :key="item.id" class="partner-card request-card">
                <div class="card-top">
                  <div class="user-avatar">
                    <img v-if="item.avatar" :src="item.avatar" alt="avatar" />
                    <i v-else class="fa-solid fa-user"></i>
                  </div>
                  <el-tag size="small" class="status-tag pending-tag">待处理</el-tag>
                </div>
                <div class="user-info">
                  <div class="user-id">来自 {{ item.username || `伙伴 ${item.fromUserId}` }}</div>
                  <div v-if="item.matchType" class="info-line">
                    <i class="fa-solid fa-tag"></i>
                    <span>{{ matchTypeLabel(item.matchType) }}</span>
                  </div>
                  <div v-if="item.message" class="info-line">
                    <i class="fa-solid fa-message"></i>
                    <span>{{ item.message }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <el-button size="small" class="action-btn accept-btn" @click="handleAccept(item.id)">
                    <i class="fa-solid fa-check"></i>
                    <span>接受</span>
                  </el-button>
                  <el-button size="small" class="action-btn reject-btn" @click="handleReject(item.id)">
                    <i class="fa-solid fa-xmark"></i>
                    <span>拒绝</span>
                  </el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 发出的请求 -->
          <el-tab-pane label="发出的请求" name="sent">
            <div v-if="sentLoading" class="loading-state">
              <div class="loading-spinner"><i class="fa-solid fa-spinner fa-spin"></i></div>
              <p>加载中...</p>
            </div>
            <div v-else-if="sentList.length === 0" class="empty-state">
              <div class="empty-icon"><i class="fa-solid fa-paper-plane"></i></div>
              <p>暂无发出的请求</p>
            </div>
            <div v-else class="partner-grid">
              <div v-for="item in sentList" :key="item.id" class="partner-card request-card">
                <div class="card-top">
                  <div class="user-avatar">
                    <img v-if="item.avatar" :src="item.avatar" alt="avatar" />
                    <i v-else class="fa-solid fa-user"></i>
                  </div>
                  <el-tag size="small" class="status-tag pending-tag">等待回应</el-tag>
                </div>
                <div class="user-info">
                  <div class="user-id">发给 {{ item.username || `伙伴 ${item.toUserId}` }}</div>
                  <div v-if="item.matchType" class="info-line">
                    <i class="fa-solid fa-tag"></i>
                    <span>{{ matchTypeLabel(item.matchType) }}</span>
                  </div>
                  <div v-if="item.message" class="info-line">
                    <i class="fa-solid fa-message"></i>
                    <span>{{ item.message }}</span>
                  </div>
                </div>
                <div class="card-actions">
                  <el-button size="small" class="action-btn cancel-btn" @click="handleCancel(item.id)">
                    <i class="fa-solid fa-ban"></i>
                    <span>取消</span>
                  </el-button>
                </div>
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { v4 as uuidv4 } from 'uuid'
import AppLayout from '@/layouts/AppLayout.vue'
import {
  getRecommendations,
  sendMatchRequest,
  acceptMatchRequest,
  rejectMatchRequest,
  getMyPartners,
  getPendingRequests,
  getSentRequests,
  cancelMatchRequest,
  unmatchPartner,
  chatWithPartnerAgent,
} from '@/api/partnerService'
import { getUserProfilesBatch } from '@/api/userProfileService'
import { getUsersBatch } from '@/api/userService'
import { useRoute } from 'vue-router'

const route = useRoute()

const activeTab = ref('recommend')
const currentMatchType = ref('friend')

const matchTypes = [
  { value: 'friend', label: '交友', icon: 'fa-solid fa-face-smile' },
  { value: 'study',  label: '学习', icon: 'fa-solid fa-book-open' },
  { value: 'hobby',  label: '兴趣', icon: 'fa-solid fa-star' },
  { value: 'sports', label: '运动', icon: 'fa-solid fa-dumbbell' },
  { value: 'game',   label: '游戏', icon: 'fa-solid fa-gamepad' },
  { value: 'date',   label: '恋爱', icon: 'fa-solid fa-heart' },
]

// 推荐
const recommendList = ref([])
const recommendLoading = ref(false)

// 我的伙伴
const partnerList = ref([])
const partnersLoading = ref(false)

// 收到的请求
const receivedList = ref([])
const receivedLoading = ref(false)
const pendingCount = ref(0)

// 发出的请求
const sentList = ref([])
const sentLoading = ref(false)

// 批量选择
const selectedIds = ref([])
const allSelected = ref(false)
const batchSending = ref(false)

// AI智能匹配对话
const aiInput = ref('')
const aiMessages = ref([])
const aiSending = ref(false)
const aiMemoryId = ref(null)
const chatMessageRef = ref(null)
let partnerMatchingChat = null // XHR请求引用，用于中断

// 匹配类型中文名
const matchTypeLabel = (type) => {
  const map = { friend: '交友', study: '学习', hobby: '兴趣', sports: '运动', game: '游戏', date: '恋爱' }
  return map[type] || type || ''
}

// UUID转数字（用于对话记忆ID）
const uuidToNumber = (uuid) => {
  let number = 0
  for (let i = 0; i < uuid.length && i < 6; i++) {
    const hexValue = uuid[i]
    number = number * 16 + (parseInt(hexValue, 16) || 0)
  }
  return number % 1000000
}

// 解析标签
const parseTags = (tagsString) => {
  if (!tagsString) return []
  try {
    const parsed = JSON.parse(tagsString)
    if (Array.isArray(parsed)) return parsed
    return tagsString.split(',').map(t => t.trim()).filter(Boolean)
  } catch {
    return tagsString.split(',').map(t => t.trim()).filter(Boolean)
  }
}

// 切换匹配类型
const switchMatchType = (type) => {
  currentMatchType.value = type
  selectedIds.value = []
  allSelected.value = false
  loadRecommendations()
}

// 全选/取消全选
const toggleAll = (checked) => {
  if (checked) {
    selectedIds.value = recommendList.value.map(item => item.userProfile?.userId).filter(Boolean)
  } else {
    selectedIds.value = []
  }
}

// 批量发送邀请
const handleBatchSend = async () => {
  const ids = selectedIds.value
  if (ids.length === 0) return

  try {
    await ElMessageBox.confirm(
      `确定要向选中的 ${ids.length} 位伙伴发送匹配邀请吗？`,
      '批量邀请',
      { confirmButtonText: '全部发送', cancelButtonText: '取消', type: 'info' }
    )
  } catch {
    return
  }

  batchSending.value = true
  let success = 0
  let fail = 0
  const errors = []

  for (const toUserId of ids) {
    try {
      await sendMatchRequest(toUserId, currentMatchType.value)
      success++
    } catch (e) {
      fail++
      errors.push({ userId: toUserId, msg: e.message })
    }
  }

  batchSending.value = false
  selectedIds.value = []
  allSelected.value = false

  const msg = `批量邀请完成：成功 ${success} 人${fail > 0 ? `，失败 ${fail} 人` : ''}`
  if (fail > 0) {
    ElMessage.warning(msg)
    console.warn('批量邀请失败详情:', errors)
  } else {
    ElMessage.success(msg)
  }
}

// 标签切换
const onTabChange = (tab) => {
  if (tab === 'ai-match') {
    // AI匹配标签页，初始化记忆ID
    if (!aiMemoryId.value) {
      const stored = localStorage.getItem('partner_matching_uuid')
      if (stored) {
        aiMemoryId.value = Number(stored)
      } else {
        aiMemoryId.value = uuidToNumber(uuidv4())
        localStorage.setItem('partner_matching_uuid', aiMemoryId.value)
      }
    }
  } else if (tab === 'recommend') loadRecommendations()
  else if (tab === 'my-partners') loadMyPartners()
  else if (tab === 'received') loadReceived()
  else if (tab === 'sent') loadSent()
}

// 加载推荐
const loadRecommendations = async () => {
  recommendLoading.value = true
  try {
    const res = await getRecommendations(currentMatchType.value)
    const recommendations = res.data || []
    
    // 获取所有推荐用户的头像信息
    const userIds = recommendations.map(item => item.userProfile?.userId).filter(id => id)
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
        
        // 将头像和用户名信息合并到推荐列表中
        recommendList.value = recommendations.map(item => {
          const profile = profilesMap[item.userProfile?.userId]
          const user = usersMap[item.userProfile?.userId]
          return {
            ...item,
            userProfile: {
              ...item.userProfile,
              avatar: profile?.avatar || null,
              username: user?.username || null
            }
          }
        })
      } catch (profileErr) {
        console.error('获取推荐用户信息失败:', profileErr)
        // 如果获取信息失败，仍然显示基本信息
        recommendList.value = recommendations
      }
    } else {
      recommendList.value = recommendations
    }
  } catch (e) {
    ElMessage.error(e.message || '获取推荐失败')
  } finally {
    recommendLoading.value = false
  }
}

// 加载我的伙伴
const loadMyPartners = async () => {
  partnersLoading.value = true
  try {
    const res = await getMyPartners()
    const partners = res.data || []
    
    // 获取所有伙伴的头像和用户名信息
    const userIds = partners.map(item => item.partnerUserId).filter(id => id)
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
        
        // 将头像和用户名信息合并到伙伴列表中
        partnerList.value = partners.map(item => {
          const profile = profilesMap[item.partnerUserId]
          const user = usersMap[item.partnerUserId]
          return {
            ...item,
            avatar: profile?.avatar || null,
            username: user?.username || null
          }
        })
      } catch (profileErr) {
        console.error('获取伙伴信息失败:', profileErr)
        // 如果获取信息失败，仍然显示基本信息
        partnerList.value = partners
      }
    } else {
      partnerList.value = partners
    }
  } catch (e) {
    ElMessage.error(e.message || '获取伙伴列表失败')
  } finally {
    partnersLoading.value = false
  }
}

// 加载收到的请求
const loadReceived = async () => {
  receivedLoading.value = true
  try {
    const res = await getPendingRequests()
    const requests = res.data || []
    
    // 获取所有请求发送者的头像和用户名信息
    const userIds = requests.map(item => item.fromUserId).filter(id => id)
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
        
        // 将头像和用户名信息合并到请求列表中
        receivedList.value = requests.map(item => {
          const profile = profilesMap[item.fromUserId]
          const user = usersMap[item.fromUserId]
          return {
            ...item,
            avatar: profile?.avatar || null,
            username: user?.username || null
          }
        })
        pendingCount.value = receivedList.value.length
      } catch (profileErr) {
        console.error('获取请求发送者信息失败:', profileErr)
        // 如果获取信息失败，仍然显示基本信息
        receivedList.value = requests
        pendingCount.value = requests.length
      }
    } else {
      receivedList.value = requests
      pendingCount.value = requests.length
    }
  } catch (e) {
    ElMessage.error(e.message || '获取请求失败')
  } finally {
    receivedLoading.value = false
  }
}

// 加载发出的请求
const loadSent = async () => {
  sentLoading.value = true
  try {
    const res = await getSentRequests()
    const requests = res.data || []
    
    // 获取所有请求接收者的头像和用户名信息
    const userIds = requests.map(item => item.toUserId).filter(id => id)
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
        
        // 将头像和用户名信息合并到请求列表中
        sentList.value = requests.map(item => {
          const profile = profilesMap[item.toUserId]
          const user = usersMap[item.toUserId]
          return {
            ...item,
            avatar: profile?.avatar || null,
            username: user?.username || null
          }
        })
      } catch (profileErr) {
        console.error('获取请求接收者信息失败:', profileErr)
        // 如果获取信息失败，仍然显示基本信息
        sentList.value = requests
      }
    } else {
      sentList.value = requests
    }
  } catch (e) {
    ElMessage.error(e.message || '获取请求失败')
  } finally {
    sentLoading.value = false
  }
}

// 发送邀请
const handleSendRequest = async (toUserId) => {
  try {
    await ElMessageBox.confirm('确定要向该伙伴发送邀请吗？', '提示', {
      confirmButtonText: '发送',
      cancelButtonText: '取消',
      type: 'info',
    })
    await sendMatchRequest(toUserId, currentMatchType.value)
    ElMessage.success('邀请已发送')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '发送邀请失败')
    }
  }
}

// 接受请求
const handleAccept = async (requestId) => {
  try {
    await ElMessageBox.confirm('确定要接受该请求吗？', '提示', {
      confirmButtonText: '接受',
      cancelButtonText: '取消',
      type: 'info',
    })
    await acceptMatchRequest(requestId)
    ElMessage.success('已接受，你们现在是伙伴了')
    loadReceived()
    loadMyPartners()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

// 拒绝请求
const handleReject = async (requestId) => {
  try {
    await ElMessageBox.confirm('确定要拒绝该请求吗？', '提示', {
      confirmButtonText: '拒绝',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await rejectMatchRequest(requestId)
    ElMessage.success('已拒绝')
    loadReceived()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

// 取消请求
const handleCancel = async (requestId) => {
  try {
    await ElMessageBox.confirm('确定要取消该请求吗？', '提示', {
      confirmButtonText: '取消',
      cancelButtonText: '返回',
      type: 'warning',
    })
    await cancelMatchRequest(requestId)
    ElMessage.success('已取消')
    loadSent()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

// 解除伙伴
const handleUnmatch = async (relationId) => {
  try {
    await ElMessageBox.confirm('确定要解除伙伴关系吗？', '提示', {
      confirmButtonText: '解除',
      cancelButtonText: '返回',
      type: 'warning',
    })
    await unmatchPartner(relationId)
    ElMessage.success('已解除伙伴关系')
    loadMyPartners()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

// ========== AI智能匹配对话相关 ==========

// 自动滚动到底部
watch(aiMessages, () => {
  nextTick(() => {
    if (chatMessageRef.value) {
      chatMessageRef.value.scrollTop = chatMessageRef.value.scrollHeight
    }
  })
}, { deep: true })

// 发送消息（从输入框触发）
const sendAiMessage = () => {
  if (!aiInput.value.trim() || aiSending.value) return
  const message = aiInput.value.trim()
  aiInput.value = ''
  sendAiRequest(message)
}

// 快捷发送
const quickSend = (message) => {
  aiInput.value = message
  sendAiMessage()
}

// 新对话
const newAiChat = () => {
  if (partnerMatchingChat) {
    partnerMatchingChat.abort()
    partnerMatchingChat = null
  }
  const newUuid = uuidToNumber(uuidv4())
  localStorage.setItem('partner_matching_uuid', newUuid)
  aiMemoryId.value = newUuid
  aiMessages.value = []
  aiSending.value = false
}

// 发送AI请求
const sendAiRequest = (message) => {
  aiSending.value = true

  // 添加用户消息
  aiMessages.value.push({
    isUser: true,
    content: message,
    isTyping: false,
  })

  // 添加机器人占位消息
  const botMsg = {
    isUser: false,
    content: '',
    isTyping: true,
  }
  aiMessages.value.push(botMsg)

  // 确保记忆ID存在
  if (!aiMemoryId.value) {
    const stored = localStorage.getItem('partner_matching_uuid')
    if (stored) {
      aiMemoryId.value = Number(stored)
    } else {
      aiMemoryId.value = uuidToNumber(uuidv4())
      localStorage.setItem('partner_matching_uuid', aiMemoryId.value)
    }
  }

  const chat = chatWithPartnerAgent(aiMemoryId.value, message)
  partnerMatchingChat = chat

  chat.onProgress((newText) => {
    const msgs = aiMessages.value
    const lastMsg = msgs[msgs.length - 1]
    if (lastMsg && !lastMsg.isUser) {
      // 过滤可能的JSON响应头部
      if (newText.startsWith('{') && lastMsg.content === '') {
        try {
          const jsonData = JSON.parse(newText)
          if (jsonData.code === 200) {
            lastMsg.content = jsonData.data || jsonData.message || ''
          } else {
            lastMsg.content = jsonData.message || '服务响应异常'
          }
          lastMsg.isTyping = false
          aiSending.value = false
          return
        } catch {
          // 不是JSON，作为正常流式内容
        }
      }
      lastMsg.content += newText
    }
  })

  chat.onComplete(() => {
    const msgs = aiMessages.value
    const lastMsg = msgs[msgs.length - 1]
    if (lastMsg && !lastMsg.isUser) {
      lastMsg.isTyping = false
    }
    aiSending.value = false
    partnerMatchingChat = null
  })

  chat.onError(() => {
    const msgs = aiMessages.value
    const lastMsg = msgs[msgs.length - 1]
    if (lastMsg && !lastMsg.isUser) {
      lastMsg.content = '网络连接失败或服务异常，请稍后重试'
      lastMsg.isTyping = false
    }
    aiSending.value = false
    partnerMatchingChat = null
  })
}

// 初始化AI对话记忆ID
const initAiChat = () => {
  const stored = localStorage.getItem('partner_matching_uuid')
  if (stored) {
    aiMemoryId.value = Number(stored)
  } else {
    aiMemoryId.value = uuidToNumber(uuidv4())
    localStorage.setItem('partner_matching_uuid', aiMemoryId.value)
  }
}

onMounted(() => {
  // 如果路由中有 tab=ai-match 参数，自动切换到 AI 智能匹配标签
  if (route.query.tab === 'ai-match') {
    initAiChat()
    activeTab.value = 'ai-match'
  } else {
    loadRecommendations()
  }
})
</script>

<style scoped>
.partner-page {
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
  margin-bottom: 20px;
}

.header-icon {
  width: 48px;
  height: 48px;
  background: linear-gradient(135deg, rgba(236, 72, 153, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ec4899;
  font-size: 24px;
}

.header-content {
  flex: 1;
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

/* 匹配类型选择器 */
.match-type-bar {
  display: flex;
  gap: 10px;
  padding: 14px 20px;
  border-radius: 14px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.match-type-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #94a3b8;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s ease;
}

.match-type-chip:hover {
  background: rgba(14, 165, 233, 0.1);
  border-color: rgba(14, 165, 233, 0.3);
  color: #e2e8f0;
}

.match-type-chip.active {
  background: linear-gradient(135deg, rgba(236, 72, 153, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border-color: rgba(236, 72, 153, 0.4);
  color: #f1f5f9;
  box-shadow: 0 0 20px rgba(236, 72, 153, 0.15);
}

.match-type-chip i {
  font-size: 16px;
}

/* 批量操作栏（顶部） */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  margin-bottom: 16px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  color: #94a3b8;
  font-size: 13px;
}

.batch-bar .el-checkbox {
  color: #94a3b8;
}

:deep(.batch-bar .el-checkbox .el-checkbox__inner) {
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(255, 255, 255, 0.05);
}

.batch-info {
  flex: 1;
}

.batch-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
  border-radius: 10px;
  font-weight: 500;
  color: #ffffff;
  transition: all 0.25s ease;
}

.batch-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.4);
}

.batch-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* 底部粘性批量操作栏 */
.sticky-batch-bar {
  position: sticky;
  bottom: 0;
  margin-top: 20px;
  padding: 12px 0;
  z-index: 10;
}

.sticky-batch-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.95) 100%);
  border: 1px solid rgba(14, 165, 233, 0.3);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(12px);
}

.sticky-batch-info {
  color: #94a3b8;
  font-size: 14px;
}

.sticky-batch-info em {
  font-style: normal;
  color: #0ea5e9;
  font-weight: 700;
  font-size: 18px;
}

.sticky-batch-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 28px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  color: #ffffff;
  transition: all 0.25s ease;
  box-shadow: 0 4px 20px rgba(14, 165, 233, 0.4);
}

.sticky-batch-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(14, 165, 233, 0.5);
}

/* 标签容器 */
.tabs-container {
  border-radius: 16px;
  padding: 24px;
}

/* 标签页 */
:deep(.partner-tabs) {
  .el-tabs__header {
    margin-bottom: 24px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  }

  .el-tabs__item {
    color: #64748b;
    font-weight: 500;
    padding: 0 20px;
    margin-right: 12px;
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

/* 伙伴网格 */
.partner-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

/* 伙伴卡片 */
.partner-card {
  padding: 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  gap: 14px;
  position: relative;
}

.partner-card.is-selected {
  border-color: rgba(14, 165, 233, 0.5);
  background: rgba(14, 165, 233, 0.06);
  box-shadow: 0 0 0 1px rgba(14, 165, 233, 0.3);
}

.partner-card:hover {
  background: rgba(14, 165, 233, 0.06);
  border-color: rgba(14, 165, 233, 0.25);
  transform: translateY(-2px);
}

.request-card {
  border-left: 3px solid rgba(245, 158, 11, 0.5);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.3) 0%, rgba(139, 92, 246, 0.3) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
  font-size: 20px;
  flex-shrink: 0;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.score-badge {
  padding: 4px 12px;
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2) 0%, rgba(239, 68, 68, 0.2) 100%);
  border: 1px solid rgba(245, 158, 11, 0.3);
  border-radius: 20px;
  color: #fbbf24;
  font-size: 13px;
  font-weight: 700;
}

.status-tag {
  border-radius: 8px;
  font-size: 11px;
}

.pending-tag {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.3);
  color: #fbbf24;
}

.match-type-tag {
  background: rgba(14, 165, 233, 0.15);
  border-color: rgba(14, 165, 233, 0.3);
  color: #38bdf8;
  border-radius: 8px;
  font-size: 11px;
}

/* 用户信息 */
.user-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-id {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.user-id-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-id-row .user-id {
  flex: 1;
}

.select-checkbox {
  margin-right: 2px;
}

:deep(.select-checkbox .el-checkbox__inner) {
  width: 16px;
  height: 16px;
  border-color: rgba(148, 163, 184, 0.4);
  background: rgba(255, 255, 255, 0.05);
}

:deep(.select-checkbox .el-checkbox__inner:hover) {
  border-color: #0ea5e9;
}

:deep(.select-checkbox.is-checked .el-checkbox__inner) {
  background: #0ea5e9;
  border-color: #0ea5e9;
}

.mbti-row {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.mbti-tag {
  background: rgba(14, 165, 233, 0.15);
  border: 1px solid rgba(14, 165, 233, 0.3);
  color: #38bdf8;
  border-radius: 8px;
  font-size: 12px;
}

.tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-item {
  background: rgba(139, 92, 246, 0.15);
  border: 1px solid rgba(139, 92, 246, 0.3);
  color: #a78bfa;
  border-radius: 8px;
  font-size: 11px;
}

.info-line {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #94a3b8;
  font-size: 13px;
}

.info-line i {
  width: 16px;
  text-align: center;
}

/* 维度评分 */
.dimension-scores {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
  padding: 10px 12px;
  background: rgba(0, 0, 0, 0.15);
  border-radius: 10px;
}

.dimension-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dimension-label {
  width: 56px;
  font-size: 11px;
  color: #64748b;
  flex-shrink: 0;
  text-align: right;
}

.dimension-bar-bg {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 3px;
  overflow: hidden;
}

.dimension-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #0ea5e9, #8b5cf6);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.dimension-value {
  width: 24px;
  font-size: 11px;
  font-weight: 600;
  color: #94a3b8;
  text-align: right;
}

/* 操作按钮 */
.card-actions {
  display: flex;
  gap: 10px;
  margin-top: auto;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: none;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  color: #ffffff;
  transition: all 0.25s ease;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.send-btn {
  flex: 1;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
}

.send-btn:hover {
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.4);
}

.accept-btn {
  flex: 1;
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
}

.accept-btn:hover {
  box-shadow: 0 4px 14px rgba(34, 197, 94, 0.4);
}

.reject-btn {
  flex: 1;
  background: linear-gradient(135deg, #ef4444 0%, #dc2626 100%);
}

.reject-btn:hover {
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.4);
}

.cancel-btn {
  flex: 1;
  background: rgba(239, 68, 68, 0.15);
  border: 1px solid rgba(239, 68, 68, 0.3) !important;
  color: #f87171;
}

.cancel-btn:hover {
  background: rgba(239, 68, 68, 0.25);
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.3);
}

.unmatch-btn {
  flex: 1;
  background: rgba(100, 116, 139, 0.15);
  border: 1px solid rgba(100, 116, 139, 0.3) !important;
  color: #94a3b8;
}

.unmatch-btn:hover {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.3) !important;
  color: #f87171;
  box-shadow: 0 4px 14px rgba(239, 68, 68, 0.2);
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

/* 加载状态 */
.loading-state {
  text-align: center;
  padding: 48px 20px;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  margin: 0 auto 16px;
  background: rgba(14, 165, 233, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
  font-size: 24px;
}

.loading-state p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

/* ========== AI智能匹配对话样式 ========== */
.ai-chat-container {
  display: flex;
  flex-direction: column;
  height: 560px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 14px;
  overflow: hidden;
}

.ai-chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.04);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.ai-chat-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-chat-avatar {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.25) 0%, rgba(139, 92, 246, 0.25) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
  font-size: 20px;
  flex-shrink: 0;
}

.ai-chat-info {
  display: flex;
  flex-direction: column;
}

.ai-chat-name {
  font-size: 15px;
  font-weight: 600;
  color: #f1f5f9;
}

.ai-chat-desc {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.ai-chat-new-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #94a3b8;
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
}

.ai-chat-new-btn:hover {
  background: rgba(14, 165, 233, 0.1);
  border-color: rgba(14, 165, 233, 0.3);
  color: #e2e8f0;
}

/* 消息区域 */
.ai-chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.ai-chat-messages::-webkit-scrollbar {
  width: 4px;
}

.ai-chat-messages::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.2);
  border-radius: 2px;
}

/* 空状态 */
.ai-chat-empty {
  text-align: center;
  padding: 40px 20px;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ai-chat-empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.15) 0%, rgba(139, 92, 246, 0.15) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
  font-size: 28px;
  margin-bottom: 16px;
}

.ai-chat-empty-title {
  margin: 0 0 6px 0;
  color: #e2e8f0;
  font-size: 16px;
  font-weight: 600;
}

.ai-chat-empty-desc {
  margin: 0 0 24px 0;
  color: #64748b;
  font-size: 13px;
}

/* 快捷推荐标签 */
.ai-chat-suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: center;
  max-width: 480px;
}

.suggestion-chip {
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 10px 18px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #94a3b8;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.25s ease;
  user-select: none;
}

.suggestion-chip:hover {
  background: rgba(14, 165, 233, 0.1);
  border-color: rgba(14, 165, 233, 0.3);
  color: #e2e8f0;
  transform: translateY(-1px);
}

.suggestion-chip i {
  font-size: 15px;
  color: #0ea5e9;
}

/* 消息 */
.ai-message {
  display: flex;
  gap: 10px;
  animation: msgFadeIn 0.3s ease;
}

@keyframes msgFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.ai-message-avatar {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.06);
  color: #64748b;
  font-size: 13px;
  margin-top: 2px;
}

.user-msg .ai-message-avatar {
  background: rgba(14, 165, 233, 0.15);
  color: #38bdf8;
}

.bot-msg .ai-message-avatar {
  background: rgba(139, 92, 246, 0.15);
  color: #a78bfa;
}

.ai-message-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 80%;
}

.user-msg .ai-message-content {
  align-items: flex-end;
}

.ai-message-bubble {
  padding: 10px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.bot-msg .ai-message-bubble {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  color: #e2e8f0;
  border-top-left-radius: 4px;
}

.user-msg .ai-message-bubble {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border: 1px solid rgba(14, 165, 233, 0.25);
  color: #f1f5f9;
  border-top-right-radius: 4px;
}

/* 打字动画 */
.ai-typing-dots {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
}

.ai-typing-dots .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #64748b;
  animation: aiPulse 1.4s infinite ease-in-out both;
}

.ai-typing-dots .dot:nth-child(1) { animation-delay: -0.32s; }
.ai-typing-dots .dot:nth-child(2) { animation-delay: -0.16s; }

@keyframes aiPulse {
  0%, 80%, 100% { transform: scale(0.75); opacity: 0.5; }
  40% { transform: scale(1); opacity: 1; }
}

/* 输入区域 */
.ai-chat-input-area {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.03);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.ai-chat-input {
  flex: 1;
}

.ai-chat-input :deep(.el-textarea__inner) {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  color: #e2e8f0;
  font-size: 13px;
  padding: 10px 14px;
  min-height: 40px;
  transition: border-color 0.2s;
}

.ai-chat-input :deep(.el-textarea__inner:focus) {
  border-color: rgba(14, 165, 233, 0.5);
  background: rgba(255, 255, 255, 0.07);
}

.ai-chat-input :deep(.el-textarea__inner::placeholder) {
  color: #475569;
}

.ai-chat-send-btn {
  width: 44px;
  height: 44px;
  padding: 0;
  border-radius: 10px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
  color: #ffffff;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
  flex-shrink: 0;
}

.ai-chat-send-btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(14, 165, 233, 0.4);
}

.ai-chat-send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

/* UI polish overrides: reduce visual noise and make AI matching feel like a focused tool. */
.page-header {
  padding: 18px 20px;
  border-radius: 12px;
  margin-bottom: 16px;
  background: rgba(15, 23, 42, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.header-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(14, 165, 233, 0.14);
  border: 1px solid rgba(14, 165, 233, 0.24);
  color: #38bdf8;
  font-size: 20px;
}

.header-content {
  min-width: 0;
}

.header-content h2 {
  font-size: 22px;
}

.header-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(96px, 1fr));
  gap: 10px;
}

.header-stat {
  padding: 9px 12px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.035);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.header-stat span {
  display: block;
  margin-bottom: 3px;
  color: #64748b;
  font-size: 11px;
}

.header-stat strong {
  color: #dbeafe;
  font-size: 13px;
  font-weight: 600;
}

.tabs-container {
  border-radius: 12px;
  padding: 18px;
  background: rgba(15, 23, 42, 0.7);
  border: 1px solid rgba(148, 163, 184, 0.12);
}

.ai-chat-container {
  height: min(640px, calc(100vh - 230px));
  min-height: 520px;
  background: rgba(2, 6, 23, 0.35);
  border: 1px solid rgba(148, 163, 184, 0.12);
  border-radius: 12px;
}

.ai-chat-header {
  gap: 16px;
  padding: 14px 16px;
  background: rgba(15, 23, 42, 0.86);
  border-bottom: 1px solid rgba(148, 163, 184, 0.12);
}

.ai-chat-avatar {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(14, 165, 233, 0.14);
  border: 1px solid rgba(14, 165, 233, 0.28);
  color: #38bdf8;
  font-size: 18px;
}

.ai-chat-desc {
  color: #94a3b8;
}

.ai-chat-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ai-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.22);
  color: #86efac;
  font-size: 12px;
  white-space: nowrap;
}

.ai-status-pill i {
  color: #22c55e;
  font-size: 7px;
}

.ai-chat-empty {
  text-align: left;
  align-items: flex-start;
  max-width: 620px;
  margin: 0 auto;
}

.ai-chat-empty-icon {
  width: 54px;
  height: 54px;
  border-radius: 12px;
  background: rgba(14, 165, 233, 0.12);
  border: 1px solid rgba(14, 165, 233, 0.24);
  color: #38bdf8;
  font-size: 24px;
}

.ai-chat-empty-title {
  font-size: 20px;
}

.ai-chat-empty-desc {
  color: #94a3b8;
  font-size: 14px;
}

.ai-chat-suggestions {
  display: grid;
  grid-template-columns: repeat(2, minmax(180px, 1fr));
  gap: 10px;
  width: 100%;
  max-width: none;
}

.suggestion-chip {
  justify-content: flex-start;
  gap: 10px;
  padding: 12px 14px;
  background: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.16);
  color: #cbd5e1;
}

.suggestion-chip i {
  width: 16px;
  color: #38bdf8;
  text-align: center;
}

.ai-message-content {
  max-width: min(80%, 760px);
  min-width: 0;
}

.ai-message-bubble {
  border-radius: 12px;
}

.ai-chat-input-area {
  padding: 14px 16px;
  background: rgba(15, 23, 42, 0.9);
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.ai-chat-input :deep(.el-textarea__inner) {
  background: rgba(2, 6, 23, 0.46);
  border: 1px solid rgba(148, 163, 184, 0.16);
  font-size: 14px;
  min-height: 42px;
}

.ai-chat-send-btn {
  width: 42px;
  height: 42px;
  background: #0ea5e9;
}

@media (max-width: 900px) {
  .page-header {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .header-stats {
    width: 100%;
    grid-template-columns: repeat(3, 1fr);
  }

  .ai-chat-container {
    height: calc(100vh - 250px);
    min-height: 500px;
  }
}

@media (max-width: 640px) {
  .partner-page {
    padding: 14px;
  }

  .tabs-container {
    padding: 12px;
  }

  .header-stats,
  .ai-chat-suggestions {
    grid-template-columns: 1fr;
  }

  .ai-chat-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .ai-chat-actions {
    width: 100%;
    justify-content: space-between;
  }

  .ai-message-content {
    max-width: 88%;
  }
}
</style>
