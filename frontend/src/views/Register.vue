<template>
  <div class="auth-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="deco-circle deco-circle-1"></div>
      <div class="deco-circle deco-circle-2"></div>
      <div class="deco-circle deco-circle-3"></div>
    </div>
    
    <div class="auth-card glass-effect">
      <div class="auth-header">
        <div class="logo-wrapper">
          <i class="fa-solid fa-user-plus logo-icon"></i>
        </div>
        <h2 class="auth-title">创建账户</h2>
        <p class="auth-subtitle">注册成为智能伙伴匹配平台用户</p>
      </div>
      <el-form @submit.prevent="handleRegister" :model="form" :rules="rules" ref="formRef" class="auth-form">
        <el-form-item prop="username">
          <div class="input-wrapper">
            <i class="fa-solid fa-user input-icon"></i>
            <el-input v-model="form.username" placeholder="用户名" size="large" />
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <div class="input-wrapper">
            <i class="fa-solid fa-lock input-icon"></i>
            <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password />
          </div>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <div class="input-wrapper">
            <i class="fa-solid fa-lock input-icon"></i>
            <el-input v-model="form.confirmPassword" type="password" placeholder="确认密码" size="large" show-password />
          </div>
        </el-form-item>
        <el-form-item prop="phone">
          <div class="input-wrapper">
            <i class="fa-solid fa-phone input-icon"></i>
            <el-input v-model="form.phone" placeholder="电话号码" size="large" />
          </div>
        </el-form-item>
        <el-form-item prop="email">
          <div class="input-wrapper">
            <i class="fa-solid fa-envelope input-icon"></i>
            <el-input v-model="form.email" placeholder="邮箱" size="large" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="submit-btn" @click="handleRegister" :loading="loading">
            <span v-if="!loading">注册</span>
            <span v-else>注册中...</span>
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-link">
        已有账号？<router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register } from '@/api/authService'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  phone: '',
  email: '',
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入电话号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
}

const handleRegister = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await register(form.username, form.password, form.phone, form.email)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error) {
      ElMessage.error(error.response?.data?.message || '注册失败，请重试')
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.auth-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 20px;
  background: linear-gradient(180deg, #0f172a 0%, #1e1e2e 50%, #0f172a 100%);
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
  opacity: 0.15;
}

.deco-circle-1 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #22c55e 0%, #10b981 100%);
  top: -100px;
  right: -100px;
  animation: float 6s ease-in-out infinite;
}

.deco-circle-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  bottom: -50px;
  left: -50px;
  animation: float 8s ease-in-out infinite reverse;
}

.deco-circle-3 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, #f59e0b 0%, #ef4444 100%);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation: float 10s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-20px) scale(1.05);
  }
}

.auth-card {
  position: relative;
  z-index: 1;
  padding: 48px;
  border-radius: 24px;
  width: 100%;
  max-width: 420px;
}

.auth-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-wrapper {
  width: 80px;
  height: 80px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #22c55e 0%, #10b981 100%);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 40px rgba(34, 197, 94, 0.4);
  transition: all 0.3s ease;
}

.logo-wrapper:hover {
  transform: scale(1.05);
  box-shadow: 0 12px 45px rgba(34, 197, 94, 0.5);
}

.logo-icon {
  font-size: 36px;
  color: #ffffff;
}

.auth-title {
  font-size: 28px;
  font-weight: 700;
  color: #f1f5f9;
  margin-bottom: 8px;
}

.auth-subtitle {
  font-size: 14px;
  color: #94a3b8;
}

.auth-form {
  margin-bottom: 24px;
}

.input-wrapper {
  position: relative;
  margin-bottom: 16px;
}

.input-icon {
  position: absolute;
  left: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #64748b;
  font-size: 16px;
  z-index: 1;
}

:deep(.input-wrapper .el-input__wrapper) {
  padding-left: 48px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  box-shadow: none;
  background: rgba(255, 255, 255, 0.05);
  transition: all 0.2s ease;
}

:deep(.input-wrapper .el-input__wrapper:focus) {
  border-color: #22c55e;
  box-shadow: 0 0 0 3px rgba(34, 197, 94, 0.2);
  background: rgba(255, 255, 255, 0.08);
}

:deep(.input-wrapper .el-input__inner) {
  font-size: 15px;
  color: #f1f5f9;
  background: transparent;
}

:deep(.input-wrapper .el-input__inner::placeholder) {
  color: #64748b;
}

.submit-btn {
  width: 100%;
  height: 52px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
}

:deep(.submit-btn.el-button--primary) {
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%) !important;
  border: none !important;
  color: #ffffff !important;
  box-shadow: 0 4px 14px 0 rgba(14, 165, 233, 0.4);
}

:deep(.submit-btn.el-button--primary:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px 0 rgba(14, 165, 233, 0.5);
}

:deep(.submit-btn.el-button--primary:active) {
  transform: translateY(0);
}

.auth-link {
  text-align: center;
  color: #64748b;
  font-size: 14px;
}

.auth-link a {
  color: #0ea5e9;
  font-weight: 600;
}

.auth-link a:hover {
  text-decoration: underline;
}
</style>