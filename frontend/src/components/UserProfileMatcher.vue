<!-- src/components/UserProfileMatcher.vue -->
<template>
  <div class="user-profile-matcher">
    <el-tabs v-model="activeTab" class="matcher-tabs">
      <!-- 相似伙伴 -->
      <el-tab-pane label="相似伙伴" name="similar">
        <div class="matcher-section">
          <div class="section-header">
            <h3>相似伙伴匹配</h3>
            <el-button type="primary" @click="findSimilarUsers" :loading="findingSimilar" class="action-btn">
              <i class="fa-solid fa-search"></i>
              <span>查找相似伙伴</span>
            </el-button>
          </div>

          <div v-if="similarUsers.length > 0" class="users-grid">
            <div
                v-for="user in similarUsers"
                :key="user.userId"
                class="user-card"
            >
              <div class="user-avatar">
                <img v-if="user.avatar" :src="user.avatar" alt="avatar" />
                <i v-else class="fa-solid fa-user"></i>
              </div>
              <div class="user-details">
                <div class="user-id">{{ user.username || `伙伴${user.userId}` }}</div>
                <div class="mbti-tag" v-if="user.mbtiType">
                  <el-tag size="small" class="mbti-tag-item">{{ user.mbtiType }}</el-tag>
                </div>
                <div class="user-tags" v-if="user.tags">
                  <el-tag
                      v-for="(tag, index) in parseTags(user.tags)"
                      :key="index"
                      size="small"
                      class="user-tag-item"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="!findingSimilar" class="empty-state">
            <div class="empty-icon">
              <i class="fa-solid fa-users"></i>
            </div>
            <p>暂无相似伙伴</p>
            <p class="empty-hint">点击上方按钮查找相似伙伴</p>
          </div>
        </div>
      </el-tab-pane>

      <!-- 推荐队伍 -->
      <el-tab-pane label="推荐队伍" name="teams">
        <div class="matcher-section">
          <div class="section-header">
            <h3>队伍推荐</h3>
            <el-button type="primary" @click="findSuitableTeams" :loading="findingTeams" class="action-btn">
              <i class="fa-solid fa-search"></i>
              <span>查找适合的队伍</span>
            </el-button>
          </div>

          <div v-if="suitableTeams.length > 0" class="teams-grid">
            <div
                v-for="team in suitableTeams"
                :key="team.id || team.teamId"
                class="team-card"
                @click="goToTeamDetail(team.id || team.teamId)"
            >
              <div class="team-icon">
                <i class="fa-solid fa-users"></i>
              </div>
              <div class="team-details">
                <div class="team-name">{{ team.name || `队伍 ${team.id || team.teamId}` }}</div>
                <div class="team-id">ID: {{ team.id || team.teamId }}</div>
                <div v-if="team.personalityConstraint" class="team-constraint">
                  <i class="fa-solid fa-heart"></i>
                  <span>{{ team.personalityConstraint }}</span>
                </div>
                <div class="team-access">
                  <el-tag size="small" :type="accessLevelTag(team.accessLevel)">
                    {{ accessLevelText(team.accessLevel) }}
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
          <div v-else-if="!findingTeams" class="empty-state">
            <div class="empty-icon">
              <i class="fa-solid fa-users"></i>
            </div>
            <p>暂无推荐队伍</p>
            <p class="empty-hint">点击上方按钮查找适合的队伍</p>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { findSimilarUsers as apiFindSimilarUsers, findSuitableTeams as apiFindSuitableTeams, getUserProfilesBatch } from '@/api/userProfileService'
import { getUser } from '@/api/authService'
import { getUsersBatch } from '@/api/userService'
import { useRouter } from 'vue-router'

const router = useRouter()

// 激活的标签页
const activeTab = ref('similar')

// 相似用户数据
const similarUsers = ref([])
const findingSimilar = ref(false)

// 推荐队伍数据
const suitableTeams = ref([])
const findingTeams = ref(false)

// 当前用户
const currentUser = getUser()

// 解析标签字符串为数组
const parseTags = (tagsString) => {
  if (!tagsString) return []
  try {
    // 尝试解析为JSON数组
    const parsed = JSON.parse(tagsString)
    if (Array.isArray(parsed)) {
      return parsed
    }
    // 如果不是数组，按逗号分割
    return tagsString.split(',').map(tag => tag.trim()).filter(tag => tag)
  } catch {
    // 如果不是JSON格式，则按逗号分割
    return tagsString.split(',').map(tag => tag.trim()).filter(tag => tag)
  }
}

// 查找相似伙伴
const findSimilarUsers = async () => {
  if (!currentUser?.id) {
    ElMessage.error('请先登录')
    return
  }

  findingSimilar.value = true
  try {
    const response = await apiFindSimilarUsers(currentUser.id, 10)
    if (response.code === 200) {
      const similarUsersData = response.data || []
      
      // 获取相似用户的头像信息
      const userIds = similarUsersData.map(user => user.userId).filter(id => id)
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
          
          // 将头像和用户名信息合并到相似用户列表中
          similarUsers.value = similarUsersData.map(user => {
            const profile = profilesMap[user.userId]
            const userBasicInfo = usersMap[user.userId]
            return {
              ...user,
              avatar: profile?.avatar || null,
              username: userBasicInfo?.username || null
            }
          })
        } catch (profileErr) {
          console.error('获取相似用户信息失败:', profileErr)
          // 如果获取信息失败，仍然显示基本信息
          similarUsers.value = similarUsersData
        }
      } else {
        similarUsers.value = similarUsersData
      }
      
      ElMessage.success(`找到 ${similarUsers.value.length} 个相似伙伴`)
    } else {
      ElMessage.error(response.message || '查找相似伙伴失败')
    }
  } catch (error) {
    console.error('查找相似伙伴失败:', error)
    ElMessage.error('查找相似伙伴失败')
  } finally {
    findingSimilar.value = false
  }
}

// 查找适合的队伍
const findSuitableTeams = async () => {
  if (!currentUser?.id) {
    ElMessage.error('请先登录')
    return
  }

  findingTeams.value = true
  try {
    const response = await apiFindSuitableTeams(currentUser.id)
    if (response.code === 200) {
      suitableTeams.value = response.data || []
      ElMessage.success(`找到 ${suitableTeams.value.length} 个推荐队伍`)
    } else {
      ElMessage.error(response.message || '查找推荐队伍失败')
    }
  } catch (error) {
    console.error('查找推荐队伍失败:', error)
    ElMessage.error('查找推荐队伍失败')
  } finally {
    findingTeams.value = false
  }
}

// 访问级别文本
const accessLevelText = (level) => {
  const map = { 0: '公开', 1: '审核', 2: '私有' }
  return map[level] || '未知'
}

// 访问级别标签类型
const accessLevelTag = (level) => {
  const map = { 0: 'success', 1: 'warning', 2: 'danger' }
  return map[level] || 'info'
}

// 跳转到队伍详情页
const goToTeamDetail = (teamId) => {
  router.push(`/teams/${teamId}`)
}

// 初始化
onMounted(() => {
  // 默认加载相似用户
  findSimilarUsers()
})
</script>

<style scoped>
.user-profile-matcher {
  width: 100%;
}

/* 标签页样式 */
:deep(.matcher-tabs) {
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

/* 区域头部 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
  border-radius: 10px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 15px rgba(14, 165, 233, 0.3);
}

/* 用户网格 */
.users-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.user-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.user-card:hover {
  background: rgba(14, 165, 233, 0.08);
  border-color: rgba(14, 165, 233, 0.3);
  transform: translateY(-2px);
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.3) 0%, rgba(139, 92, 246, 0.3) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #0ea5e9;
  flex-shrink: 0;
  overflow: hidden;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-id {
  font-weight: 600;
  color: #f1f5f9;
  margin-bottom: 6px;
  font-size: 14px;
}

.mbti-tag-item {
  background: rgba(14, 165, 233, 0.15);
  border: 1px solid rgba(14, 165, 233, 0.3);
  color: #38bdf8;
  margin-bottom: 6px;
}

.user-tag-item {
  background: rgba(139, 92, 246, 0.15);
  border: 1px solid rgba(139, 92, 246, 0.3);
  color: #a78bfa;
  margin-right: 6px;
  margin-bottom: 4px;
}

/* 队伍网格 */
.teams-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 14px;
}

.team-card {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.team-card:hover {
  background: rgba(34, 197, 94, 0.08);
  border-color: rgba(34, 197, 94, 0.3);
  transform: translateY(-2px);
}

.team-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.3) 0%, rgba(16, 185, 129, 0.3) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #22c55e;
  flex-shrink: 0;
}

.team-details {
  flex: 1;
  min-width: 0;
}

.team-name {
  font-weight: 600;
  color: #f1f5f9;
  margin-bottom: 4px;
  font-size: 14px;
}

.team-id {
  color: #64748b;
  font-size: 12px;
  margin-bottom: 6px;
}

.team-constraint {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #94a3b8;
  font-size: 12px;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-access {
  display: flex;
  gap: 4px;
}

:deep(.team-access .el-tag--success) {
  background: rgba(34, 197, 94, 0.15);
  border-color: rgba(34, 197, 94, 0.3);
  color: #4ade80;
}

:deep(.team-access .el-tag--warning) {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.3);
  color: #fbbf24;
}

:deep(.team-access .el-tag--danger) {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.3);
  color: #f87171;
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
</style>