<!-- src/views/UserProfileDashboard.vue -->
<template>
  <AppLayout>
    <div class="user-profile-dashboard">
      <!-- 页面头部 -->
      <div class="dashboard-header">
        <div class="header-content">
          <h2>伙伴画像中心</h2>
          <p>完善您的个人信息，获得更好的匹配和推荐</p>
        </div>
        <el-button type="primary" class="edit-btn" @click="showEditModal = true">
          <i class="fa-solid fa-pencil"></i>
          <span>编辑画像</span>
        </el-button>
      </div>

      <el-row :gutter="24">
        <!-- 画像概览卡片 -->
        <el-col :span="8">
          <div class="profile-summary-card glass-effect">
            <div class="card-header">
              <div class="header-icon">
                <i class="fa-solid fa-user-circle"></i>
              </div>
              <span>画像概览</span>
            </div>

            <div v-if="userProfile" class="profile-overview">
              <!-- 用户头像 -->
              <div class="user-avatar-section">
                <div class="avatar-wrapper">
                  <img v-if="userProfile.avatar" :src="userProfile.avatar" alt="avatar" class="user-avatar" />
                  <div v-else class="user-avatar-placeholder">
                    <i class="fa-solid fa-user"></i>
                  </div>
                </div>
                <div class="user-info">
                  <h3>{{ currentUser?.username || '未知用户' }}</h3>
                  <p>{{ currentUser?.email || '未提供邮箱' }}</p>
                </div>
              </div>
              
              <!-- MBTI类型 -->
              <div class="overview-item mbti-item">
                <label>MBTI类型</label>
                <div class="mbti-badge">
                  <span class="mbti-value">{{ userProfile.mbtiType || '未设置' }}</span>
                </div>
              </div>

              <!-- 个人标签 -->
              <div class="overview-item">
                <label>个人标签</label>
                <div class="tags-container">
                  <el-tag
                      v-for="(tag, index) in parseTags(userProfile.tags)"
                      :key="index"
                      size="small"
                      class="tag-item"
                  >
                    {{ tag }}
                  </el-tag>
                </div>
              </div>

              <!-- 兴趣爱好 -->
              <div class="overview-item">
                <label>兴趣爱好</label>
                <span class="item-value">{{ userProfile.interests || '未填写' }}</span>
              </div>

              <!-- 性格特征 -->
              <div class="overview-item">
                <label>性格特征</label>
                <span class="item-value">{{ userProfile.personalityTraits || '未填写' }}</span>
              </div>

              <!-- 沟通风格 -->
              <div class="overview-item">
                <label>沟通风格</label>
                <span class="item-value">{{ userProfile.communicationStyle || '未填写' }}</span>
              </div>

              <!-- 偏好设置 -->
              <div class="overview-item">
                <label>偏好设置</label>
                <span class="item-value">{{ userProfile.preferences || '未填写' }}</span>
              </div>
            </div>
            <div v-else class="empty-overview">
              <div class="empty-icon">
                <i class="fa-solid fa-user-circle"></i>
              </div>
              <p>请先完善用户画像</p>
              <el-button type="primary" size="small" @click="showEditModal = true">
                立即完善
              </el-button>
            </div>
          </div>
        </el-col>

        <!-- 用户匹配器 -->
        <el-col :span="16">
          <div class="matcher-card glass-effect">
            <UserProfileMatcher />
          </div>
        </el-col>
      </el-row>

      <!-- 编辑弹窗 -->
      <UserProfileEditor
          :is-modal="true"
          v-model:visible="showEditModal"
          @saved="handleProfileSaved"
      />
    </div>
  </AppLayout>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserProfile } from '@/api/userProfileService'
import { getUser } from '@/api/authService'
import AppLayout from '@/layouts/AppLayout.vue'
import UserProfileEditor from '@/components/UserProfileEditor.vue'
import UserProfileMatcher from '@/components/UserProfileMatcher.vue'
import { getUsersBatch } from '@/api/userService'

// 用户画像数据
const userProfile = ref(null)

// 编辑弹窗显示状态
const showEditModal = ref(false)

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

// 加载用户画像
const loadUserProfile = async () => {
  if (!currentUser?.id) {
    ElMessage.error('请先登录')
    return
  }

  try {
    const response = await getUserProfile(currentUser.id)
    if (response.code === 200 && response.data) {
      userProfile.value = response.data
    } else {
      userProfile.value = null
    }
  } catch (error) {
    console.error('加载伙伴画像失败:', error)
    ElMessage.error('加载伙伴画像失败')
  }
}

// 处理画像保存成功
const handleProfileSaved = (savedProfile) => {
  userProfile.value = savedProfile
}

// 检查用户画像是否存在
const checkAndPromptProfile = async () => {
  if (!currentUser?.id) {
    ElMessage.error('请先登录')
    return false
  }

  try {
    const response = await getUserProfile(currentUser.id)
    if (response.code === 200 && response.data) {
      userProfile.value = response.data
      return true
    } else {
      // 用户画像不存在，提示用户先编辑画像
      ElMessageBox.confirm(
        '您还没有完善伙伴画像，是否现在去编辑？',
        '提示',
        {
          confirmButtonText: '去编辑',
          cancelButtonText: '稍后再说',
          type: 'warning',
          distinguishCancelAndClose: true
        }
      ).then(() => {
        showEditModal.value = true
      }).catch((action) => {
        if (action === 'cancel') {
          ElMessage.info('您可以在任何时候点击右上角“编辑画像”按钮进行编辑')
        }
      })
      userProfile.value = null
      return false
    }
  } catch (error) {
    console.error('检查伙伴画像失败:', error)
    ElMessage.error('检查伙伴画像失败')
    return false
  }
}

// 初始化加载
onMounted(async () => {
  await loadUserProfile()
  // 检查用户画像是否存在，如果不存在给出提示
  await checkAndPromptProfile()
})
</script>

<style scoped>
.user-profile-dashboard {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 页面头部 */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
  padding: 20px 24px;
  background: rgba(30, 41, 59, 0.6);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.header-content h2 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: 0.5px;
}

.header-content p {
  margin: 0;
  color: #94a3b8;
  font-size: 14px;
}

.edit-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.edit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(14, 165, 233, 0.4);
}

/* 卡片基础样式 */
.profile-summary-card,
.matcher-card {
  border-radius: 16px;
  padding: 24px;
  transition: all 0.3s ease;
}

.profile-summary-card {
  height: fit-content;
}

/* 卡片头部 */
.card-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
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
  font-size: 18px;
}

.card-header span {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

/* 概览内容 */
.profile-overview {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.overview-item label {
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.item-value {
  color: #e2e8f0;
  font-size: 14px;
  line-height: 1.5;
}

/* MBTI特殊样式 */
.mbti-item {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
  padding: 12px 16px;
  border-radius: 12px;
  border: 1px solid rgba(14, 165, 233, 0.2);
}

.mbti-badge {
  display: inline-block;
}

.mbti-value {
  font-size: 20px;
  font-weight: 700;
  color: #0ea5e9;
  letter-spacing: 2px;
}

/* 标签容器 */
.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  background: rgba(14, 165, 233, 0.15);
  border: 1px solid rgba(14, 165, 233, 0.3);
  color: #38bdf8;
  border-radius: 8px;
  font-size: 12px;
}

/* 用户头像区域 */
.user-avatar-section {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.avatar-wrapper {
  flex-shrink: 0;
}

.user-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid rgba(14, 165, 233, 0.3);
}

.user-avatar-placeholder {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  font-size: 24px;
}

.user-info h3 {
  margin: 0 0 4px 0;
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
}

.user-info p {
  margin: 0;
  font-size: 14px;
  color: #94a3b8;
}

/* 空状态 */
.empty-overview {
  text-align: center;
  padding: 40px 20px;
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
  font-size: 32px;
}

.empty-overview p {
  margin: 0 0 16px 0;
  color: #64748b;
  font-size: 14px;
}

.empty-overview .el-button--primary {
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
}

/* 匹配器卡片 */
.matcher-card {
  height: 100%;
}

/* 响应式 */
@media (max-width: 1200px) {
  .dashboard-header {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }
}
</style>