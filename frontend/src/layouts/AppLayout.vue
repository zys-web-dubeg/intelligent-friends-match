<template>
  <div class="app-layout">
    <div class="sidebar">
      <div class="sidebar-header">
        <div class="sidebar-logo">
          <i class="fa-solid fa-robot"></i>
        </div>
        <span class="sidebar-title">智能伙伴匹配平台</span>
      </div>
      <div class="sidebar-menu">
        <div class="menu-group">
          <el-button class="menu-button home-button" @click="goToHome">
            <i class="fa-solid fa-home"></i>
            <span>主页</span>
          </el-button>
          <el-dropdown class="agent-dropdown" @command="onAgentCommand" placement="right-start">
            <el-button class="menu-button agent-switch-btn">
              <i class="fa-solid fa-robot"></i>
              <span>智能助手</span>
              <i class="fa-solid fa-chevron-down dropdown-icon"></i>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu class="custom-dropdown">
                <el-dropdown-item
                    v-for="agent in agents"
                    :key="agent.id"
                    :command="agent.id"
                    :class="{ active: currentAgentId === agent.id }"
                >
                  <div class="dropdown-item-content">
                    <img :src="agent.image" class="dropdown-agent-img" />
                    <div class="dropdown-agent-info">
                      <span class="dropdown-agent-name">{{ agent.name }}</span>
                      <span class="dropdown-agent-title">{{ agent.title }}</span>
                    </div>
                  </div>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button class="menu-button stats-button" @click="goToStats">
            <i class="fa-solid fa-chart-bar"></i>
            <span>数据统计</span>
          </el-button>
          <el-button class="menu-button team-button" @click="goToTeams">
            <i class="fa-solid fa-users"></i>
            <span>队伍管理</span>
          </el-button>
          <el-button class="menu-button profile-button" @click="goToUserProfile">
            <i class="fa-solid fa-user-circle"></i>
            <span>伙伴画像</span>
          </el-button>
          <el-button class="menu-button partner-button" @click="goToPartners">
            <i class="fa-solid fa-handshake"></i>
            <span>伙伴匹配</span>
          </el-button>
          <el-button class="menu-button history-button" @click="goToChatHistory">
            <i class="fa-solid fa-history"></i>
            <span>聊天历史</span>
          </el-button>
        </div>

        <div class="menu-spacer"></div>

        <div class="menu-footer">
          <el-button class="menu-button logout-button" @click="handleLogout">
            <i class="fa-solid fa-sign-out-alt"></i>
            <span>退出登录</span>
          </el-button>
        </div>
      </div>
    </div>
    <div class="main-content">
      <slot />
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { logout } from '@/api/authService'
import { agents as defaultAgents, defaultAgentId } from '@/config/agents'

const props = defineProps({
  agents: { type: Array, default: () => defaultAgents },
  currentAgentId: { type: String, default: defaultAgentId },
})

const emit = defineEmits(['switch-agent', 'new-chat'])

const router = useRouter()

const onAgentCommand = (agentId) => {
  emit('switch-agent', agentId)
}

const goToStats = () => {
  router.push('/statistics')
}

const goToTeams = () => {
  router.push('/teams')
}

const goToHome = () => {
  router.push('/home')
}

const goToUserProfile = () => {
  router.push('/user-profile')
}

const goToPartners = () => {
  router.push('/partners')
}

const goToChatHistory = () => {
  router.push('/chat-history')
}

const handleLogout = () => {
  logout()
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  background: linear-gradient(180deg, #0f172a 0%, #1e1e2e 50%, #0f172a 100%);
}

.sidebar {
  width: 260px;
  box-sizing: border-box;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 10;
  box-shadow: 4px 0 30px rgba(0, 0, 0, 0.3);
  border-right: 1px solid rgba(255, 255, 255, 0.05);
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(14, 165, 233, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(14, 165, 233, 0.03) 1px, transparent 1px);
  background-size: 30px 30px;
  pointer-events: none;
}

.sidebar-header {
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  position: relative;
  z-index: 1;
}

.sidebar-logo {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #ffffff;
  box-shadow: 0 4px 20px rgba(14, 165, 233, 0.4);
  transition: all 0.3s ease;
}

.sidebar-logo:hover {
  transform: scale(1.05);
  box-shadow: 0 6px 25px rgba(14, 165, 233, 0.5);
}

.sidebar-title {
  font-size: 18px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: 0.5px;
}

.sidebar-menu {
  padding: 16px;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 1;
  flex: 1;
  box-sizing: border-box;
}

.menu-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.menu-spacer {
  flex: 1;
}

.menu-footer {
  margin-top: 12px;
}

.menu-button {
  width: 100%;
  height: 48px;
  margin-left: 0 !important;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 12px;
  padding: 0 16px !important;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid transparent;
  color: #94a3b8;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.25s ease;
}

.menu-button:hover {
  background: rgba(14, 165, 233, 0.15);
  color: #f1f5f9;
  border-color: rgba(14, 165, 233, 0.3);
  box-shadow: 0 4px 15px rgba(14, 165, 233, 0.2);
}

.menu-button.active {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  color: #f1f5f9;
  border-color: rgba(14, 165, 233, 0.4);
}

.menu-button i {
  font-size: 16px;
  width: 20px;
  text-align: center;
}

.menu-button span {
  flex: 1;
  text-align: left;
}

.agent-dropdown {
  width: 100%;
  display: block;
}

.agent-dropdown :deep(.el-dropdown) {
  width: 100%;
}

.agent-switch-btn {
  width: 100%;
  margin-left: 0 !important;
}

.dropdown-icon {
  margin-left: auto;
  font-size: 12px;
}

.menu-divider {
  height: 1px;
  background: rgba(255, 255, 255, 0.08);
  margin: 16px 0;
}

.logout-button:hover {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
  border-color: rgba(239, 68, 68, 0.3);
}

.dropdown-agent-img {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  object-fit: cover;
}

.dropdown-item-content {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.dropdown-agent-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dropdown-agent-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dropdown-agent-title {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

:deep(.custom-dropdown) {
  padding: 8px;
  border-radius: 16px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
  min-width: 240px;
  background: #1e293b;
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.el-dropdown-menu__item) {
  padding: 10px 12px;
  margin-bottom: 4px;
  border-radius: 10px;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  color: #cbd5e1;
}

:deep(.el-dropdown-menu__item.active) {
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  color: #f1f5f9;
}

:deep(.el-dropdown-menu__item:not(.active):hover) {
  background: rgba(255, 255, 255, 0.1);
}

.main-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
  background: transparent;
  position: relative;
}

.main-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: 
    linear-gradient(rgba(148, 163, 184, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(148, 163, 184, 0.02) 1px, transparent 1px);
  background-size: 40px 40px;
  pointer-events: none;
}

@media (max-width: 768px) {
  .app-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
    padding: 12px 20px;
  }

  .sidebar-header {
    border-bottom: none;
    padding: 0 10px;
  }

  .sidebar-title {
    display: none;
  }

  .sidebar-menu {
    flex-direction: row;
    padding: 0;
    gap: 8px;
  }

  .menu-button span {
    display: none;
  }

  .menu-button {
    width: 44px;
    height: 44px;
    justify-content: center;
  }

  .menu-divider {
    display: none;
  }

  .agent-dropdown {
    position: relative;
  }

  .main-content {
    padding: 16px;
  }
}

@media (min-width: 769px) {
  .main-content {
    padding: 24px;
  }
}
</style>
