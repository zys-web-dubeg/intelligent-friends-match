<template>
  <AppLayout @switch-agent="onSwitchAgent">
    <div class="home-container">
      <!-- 背景装饰 -->
      <div class="bg-decoration">
        <div class="deco-circle deco-circle-1"></div>
        <div class="deco-circle deco-circle-2"></div>
        <div class="deco-circle deco-circle-3"></div>
      </div>

      <!-- 主要内容 -->
      <div class="home-content">
        <!-- Logo区域 -->
        <div class="logo-section">
          <div class="logo-wrapper">
            <i class="fa-solid fa-robot logo-icon"></i>
          </div>
          <h1 class="app-title">智能伙伴匹配平台</h1>
          <p class="app-subtitle">开启您的智能对话之旅</p>
        </div>

        <!-- 功能卡片 -->
        <div class="feature-cards">
          <div class="feature-card" @click="goTo('/chat')">
            <div class="card-icon chat">
              <i class="fa-solid fa-comments"></i>
            </div>
            <h3>智能聊天</h3>
            <p>与AI伙伴进行自然对话，获取专业解答</p>
          </div>
          <div class="feature-card" @click="goTo('/teams')">
            <div class="card-icon team">
              <i class="fa-solid fa-users"></i>
            </div>
            <h3>队伍协作</h3>
            <p>创建队伍，与团队成员协同工作</p>
          </div>
          <div class="feature-card" @click="goTo('/user-profile')">
            <div class="card-icon profile">
              <i class="fa-solid fa-user"></i>
            </div>
            <h3>伙伴画像</h3>
            <p>完善个人信息，获得精准推荐</p>
          </div>
          <div class="feature-card" @click="goTo('/statistics')">
            <div class="card-icon stats">
              <i class="fa-solid fa-chart-line"></i>
            </div>
            <h3>数据分析</h3>
            <p>查看使用统计和数据洞察</p>
          </div>
        </div>

        <!-- 快捷操作 -->
        <div class="quick-actions">
          <el-button type="primary" class="quick-btn" @click="goTo('/chat')">
            <i class="fa-solid fa-comments"></i>
            <span>开始聊天</span>
          </el-button>
        </div>
      </div>
    </div>
  </AppLayout>
</template>

<script setup>
import { useRouter } from 'vue-router'
import AppLayout from '@/layouts/AppLayout.vue'

const router = useRouter()

const onSwitchAgent = (agentId) => {
  router.push({ path: '/chat', query: { agent: agentId } })
}

const goTo = (path) => {
  router.push(path)
}
</script>

<style scoped>
.home-container {
  width: 100%;
  min-height: calc(100vh - 48px);
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
}

.deco-circle-1 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  top: -150px;
  right: -150px;
  animation: float 8s ease-in-out infinite;
}

.deco-circle-2 {
  width: 350px;
  height: 350px;
  background: linear-gradient(135deg, #ec4899 0%, #8b5cf6 100%);
  bottom: -100px;
  left: -100px;
  animation: float 10s ease-in-out infinite reverse;
}

.deco-circle-3 {
  width: 250px;
  height: 250px;
  background: linear-gradient(135deg, #22c55e 0%, #10b981 100%);
  top: 50%;
  left: 20%;
  transform: translateY(-50%);
  animation: float 12s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-30px) scale(1.05);
  }
}

/* 主要内容 */
.home-content {
  position: relative;
  z-index: 1;
  text-align: center;
  padding: 40px 20px;
  max-width: 1000px;
  width: 100%;
}

/* Logo区域 */
.logo-section {
  margin-bottom: 48px;
}

.logo-wrapper {
  width: 100px;
  height: 100px;
  margin: 0 auto 24px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border-radius: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 15px 50px rgba(14, 165, 233, 0.4);
  transition: all 0.3s ease;
}

.logo-wrapper:hover {
  transform: scale(1.05);
  box-shadow: 0 20px 60px rgba(14, 165, 233, 0.5);
}

.logo-icon {
  font-size: 48px;
  color: #ffffff;
}

.app-title {
  margin: 0 0 12px 0;
  font-size: 36px;
  font-weight: 700;
  color: #f1f5f9;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.app-subtitle {
  margin: 0;
  font-size: 16px;
  color: #64748b;
}

/* 功能卡片 */
.feature-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 20px;
  margin-bottom: 48px;
}

.feature-card {
  padding: 28px 24px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
}

.feature-card:hover {
  background: rgba(14, 165, 233, 0.1);
  border-color: rgba(14, 165, 233, 0.3);
  transform: translateY(-4px);
}

.card-icon {
  width: 50px;
  height: 50px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-bottom: 16px;
}

.card-icon.chat {
  background: linear-gradient(135deg, rgba(236, 72, 153, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  color: #ec4899;
}

.card-icon.team {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  color: #0ea5e9;
}

.card-icon.profile {
  background: linear-gradient(135deg, rgba(34, 197, 94, 0.2) 0%, rgba(16, 185, 129, 0.2) 100%);
  color: #22c55e;
}

.card-icon.stats {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.2) 0%, rgba(239, 68, 68, 0.2) 100%);
  color: #f59e0b;
}

.feature-card h3 {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.feature-card p {
  margin: 0;
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

/* 快捷操作 */
.quick-actions {
  display: flex;
  justify-content: center;
}

.quick-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
  border-radius: 14px;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 6px 24px rgba(14, 165, 233, 0.4);
}

.quick-btn:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 32px rgba(14, 165, 233, 0.5);
}
</style>
