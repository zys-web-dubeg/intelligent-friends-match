<template>
  <div class="team-list-page">
    <!-- 页面头部 -->
    <div class="page-header glass-effect">
      <div class="header-icon">
        <i class="fa-solid fa-users"></i>
      </div>
      <div class="header-content">
        <h2>队伍管理</h2>
        <p>管理您的队伍，发现更多志同道合的伙伴</p>
      </div>
      <el-button class="create-btn" @click="$router.push('/teams/create')">
        <i class="fa-solid fa-plus"></i>
        <span>创建队伍</span>
      </el-button>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <div class="search-wrapper">
        <i class="fa-solid fa-search search-icon"></i>
        <el-input
          v-model="searchKeyword"
          placeholder="搜索队伍名称..."
          clearable
          class="search-input"
        />
      </div>
    </div>

    <!-- 标签页 -->
    <div class="tabs-container glass-effect">
      <el-tabs v-model="activeTab" class="team-tabs">
        <el-tab-pane label="我的队伍" name="my">
          <div v-if="filteredMyTeams.length === 0" class="empty-state">
            <div class="empty-icon">
              <i class="fa-solid fa-users"></i>
            </div>
            <p>暂无队伍，快去创建一个吧</p>
            <el-button type="primary" size="small" @click="$router.push('/teams/create')">
              创建队伍
            </el-button>
          </div>
          <div v-else class="team-grid">
            <div v-for="team in filteredMyTeams" :key="team.id" class="team-card" @click="$router.push(`/teams/${team.id}`)">
              <div class="team-icon">
                <i class="fa-solid fa-users"></i>
              </div>
              <div class="team-info">
                <h3>{{ team.name }}</h3>
                <el-tag :type="accessLevelTag(team.accessLevel)" class="team-tag">
                  {{ accessLevelText(team.accessLevel) }}
                </el-tag>
                <p v-if="team.personalityConstraint" class="constraint">
                  {{ team.personalityConstraint }}
                </p>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="推荐队伍" name="match">
          <div v-if="matchTeamsLoading" class="loading-state">
            <div class="loading-spinner">
              <i class="fa-solid fa-spinner fa-spin"></i>
            </div>
            <p>您感兴趣的队伍就在路上~</p>
          </div>
          <div v-else-if="filteredMatchTeams.length === 0" class="empty-state">
            <div class="empty-icon">
              <i class="fa-solid fa-compass"></i>
            </div>
            <p>暂无推荐队伍</p>
            <p class="empty-hint">完善伙伴画像可获得更好的推荐</p>
          </div>
          <div v-else class="team-grid">
            <div v-for="team in filteredMatchTeams" :key="team.id" class="team-card" @click="$router.push(`/teams/${team.id}`)">
              <div class="team-icon recommended">
                <i class="fa-solid fa-users"></i>
              </div>
              <div class="team-info">
                <h3>{{ team.name }}</h3>
                <div class="team-tags-row">
                  <el-tag :type="accessLevelTag(team.accessLevel)" class="team-tag">
                    {{ accessLevelText(team.accessLevel) }}
                  </el-tag>
                  <el-tag size="small" class="recommend-tag">推荐</el-tag>
                </div>
                <p v-if="team.personalityConstraint" class="constraint">
                  {{ team.personalityConstraint }}
                </p>
                <el-button size="small" class="join-btn" @click.stop="handleJoin(team.id)">
                  <i class="fa-solid fa-user-plus"></i>
                  <span>加入</span>
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getUserTeams, joinTeam, getMatchTeams } from '@/api/teamService'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'

const activeTab = ref('my')
const myTeams = ref([])
const matchTeams = ref([])
const searchKeyword = ref('')
const matchTeamsLoading = ref(false)

const filteredMyTeams = computed(() => {
  if (!searchKeyword.value) return myTeams.value
  return myTeams.value.filter(t => t.name.includes(searchKeyword.value))
})

const filteredMatchTeams = computed(() => {
  if (!searchKeyword.value) return matchTeams.value
  return matchTeams.value.filter(t => t.name.includes(searchKeyword.value))
})

const accessLevelText = (level) => {
  const map = { 0: '公开', 1: '审核', 2: '私有' }
  return map[level] || '未知'
}

const accessLevelTag = (level) => {
  const map = { 0: 'success', 1: 'warning', 2: 'danger' }
  return map[level] || 'info'
}

const loadMyTeams = async () => {
  try {
    const res = await getUserTeams()
    myTeams.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '获取队伍列表失败')
  }
}

const loadMatchTeams = async () => {
  matchTeamsLoading.value = true  // 开始加载
  try {
    // 使用teamService中的getMatchTeams函数获取匹配的队伍
    const response = await getMatchTeams()
    matchTeams.value = response.data || []
    
    if (matchTeams.value.length > 0) {
      ElMessage.success(`找到 ${matchTeams.value.length} 个推荐队伍`)
    } else {
      ElMessage.info('暂无推荐队伍')
    }
    
    console.log('推荐队伍:', matchTeams.value)
  } catch (e) {
    console.error('获取推荐队伍失败:', e)
    
    // 更友好的错误提示
    if (e.code === 'ECONNABORTED' || e.message?.includes('timeout')) {
      ElMessage.error('获取推荐队伍超时，请稍后再试')
    } else {
      ElMessage.error(e.message || '获取推荐队伍失败')
    }
  } finally {
    matchTeamsLoading.value = false  // 结束加载
  }
}

const handleJoin = async (teamId) => {
  try {
    await ElMessageBox.confirm('确定要加入该队伍吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    await joinTeam(teamId)
    ElMessage.success('加入队伍成功')
    loadMyTeams()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '加入队伍失败')
    }
  }
}

onMounted(() => {
  loadMyTeams()
  loadMatchTeams()
})
</script>

<style scoped>
.team-list-page {
  padding: 24px;
  min-height: 100vh;
  background: #0f172a;
}

.team-list-content {
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

.create-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
  border-radius: 12px;
  font-weight: 600;
  color: #ffffff;
  transition: all 0.3s ease;
}

.create-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(14, 165, 233, 0.4);
}

/* 搜索栏 */
.search-bar {
  margin-bottom: 20px;
  max-width: 400px;
}

.search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 16px;
  color: #64748b;
  font-size: 16px;
}

:deep(.search-input .el-input__wrapper) {
  padding-left: 48px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
}

:deep(.search-input .el-input__wrapper:focus) {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
}

:deep(.search-input .el-input__inner) {
  color: #f1f5f9;
  background: transparent;
}

:deep(.search-input .el-input__inner::placeholder) {
  color: #64748b;
}

/* 标签容器 */
.tabs-container {
  border-radius: 16px;
  padding: 24px;
}

/* 标签页样式 */
:deep(.team-tabs) {
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

/* 队伍网格 */
.team-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

/* 队伍卡片 */
.team-card {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 20px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.team-card:hover {
  background: rgba(14, 165, 233, 0.08);
  border-color: rgba(14, 165, 233, 0.3);
  transform: translateY(-2px);
}

.team-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.3) 0%, rgba(139, 92, 246, 0.3) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
  font-size: 20px;
  flex-shrink: 0;
}

.team-icon.recommended {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.3) 0%, rgba(16, 185, 129, 0.3) 100%);
  color: #22c55e;
}

.team-info {
  flex: 1;
  min-width: 0;
}

.team-info h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.team-tags-row {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.team-tag {
  background: rgba(14, 165, 233, 0.15);
  border: 1px solid rgba(14, 165, 233, 0.3);
  color: #38bdf8;
  border-radius: 8px;
  font-size: 12px;
}

:deep(.team-tag.el-tag--success) {
  background: rgba(34, 197, 94, 0.15);
  border-color: rgba(34, 197, 94, 0.3);
  color: #4ade80;
}

:deep(.team-tag.el-tag--warning) {
  background: rgba(245, 158, 11, 0.15);
  border-color: rgba(245, 158, 11, 0.3);
  color: #fbbf24;
}

:deep(.team-tag.el-tag--danger) {
  background: rgba(239, 68, 68, 0.15);
  border-color: rgba(239, 68, 68, 0.3);
  color: #f87171;
}

.recommend-tag {
  background: rgba(245, 158, 11, 0.15);
  border: 1px solid rgba(245, 158, 11, 0.3);
  color: #fbbf24;
  border-radius: 8px;
}

.constraint {
  color: #94a3b8;
  font-size: 13px;
  margin: 0 0 12px 0;
  line-height: 1.5;
}

.join-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: linear-gradient(135deg, #22c55e 0%, #16a34a 100%);
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #ffffff;
  transition: all 0.3s ease;
}

.join-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(34, 197, 94, 0.3);
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
  margin: 0 0 12px 0;
  color: #64748b;
  font-size: 14px;
}

.empty-hint {
  font-size: 12px !important;
  color: #475569 !important;
}

.empty-state .el-button--primary {
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
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
</style>