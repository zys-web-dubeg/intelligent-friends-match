
<template>
  <div :class="['chat-history-item', { 'user-message': isUser, 'bot-message': !isUser }]">
    <div class="avatar">
      <i :class="isUser ? 'fa-solid fa-user' : 'fa-solid fa-robot'"></i>
    </div>
    <div class="content-wrapper">
      <div class="header">
        <span class="sender">
          {{ senderName || senderId }}
          <span v-if="senderId && senderName !== senderId" class="sender-id">#{{ senderId }}</span>
        </span>
        <span class="timestamp">{{ formatDate(timestamp) }}</span>
      </div>
      <div class="content" v-html="content"></div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  senderName: {
    type: String,
    default: ''
  },
  senderId: {
    type: String,
    required: true
  },
  content: {
    type: String,
    required: true
  },
  timestamp: {
    type: [String, Number, Date],
    required: true
  },
  isUser: {
    type: Boolean,
    default: false
  }
})

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
</script>

<style scoped>
.chat-history-item {
  display: flex;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 8px;
  transition: background-color 0.3s;
}

.chat-history-item:hover {
  background-color: #f5f7fa;
}

.user-message {
  background-color: #f0f9ff;
  border-left: 4px solid #409eff;
}

.bot-message {
  background-color: #f9fafb;
  border-left: 4px solid #67c23a;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  margin-right: 12px;
  flex-shrink: 0;
}

.content-wrapper {
  flex: 1;
}

.header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
  font-size: 12px;
  color: #909399;
}

.sender {
  font-weight: 500;
  color: #606266;
}

.sender-id {
  margin-left: 6px;
  color: #909399;
  font-size: 11px;
  font-weight: 400;
}

.timestamp {
  color: #c0c4cc;
}

.content {
  color: #303133;
  line-height: 1.5;
  word-wrap: break-word;
}
</style>
