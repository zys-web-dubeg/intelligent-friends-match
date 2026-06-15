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
          <i class="fa-solid fa-robot logo-icon"></i>
        </div>
        <h2 class="auth-title">智能伙伴匹配平台</h2>
        <p class="auth-subtitle">登录您的账户开始使用</p>
      </div>
      <el-form @submit.prevent="handleLogin" :model="form" :rules="rules" ref="formRef" class="auth-form">
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
        <el-form-item prop="captcha">
          <div class="captcha-wrapper">
            <div class="input-wrapper captcha-input">
              <i class="fa-solid fa-key input-icon"></i>
              <el-input v-model="form.captcha" placeholder="验证码" size="large" maxlength="4" />
            </div>
            <img :src="captchaUrl" @click="refreshCaptcha" class="captcha-img" title="点击刷新验证码" />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="submit-btn" @click="handleLogin" :loading="loading">
            <span v-if="!loading">登录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form-item>
      </el-form>
      <div class="auth-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, setToken, setUser } from '@/api/authService'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const captchaUrl = ref('')

const form = reactive({
  username: '',
  password: '',
  captcha: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
  ],
  captcha: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 4, message: '验证码为4位字符', trigger: 'blur' },
  ],
}

const refreshCaptcha = () => {
  // 添加时间戳避免浏览器缓存
  captchaUrl.value = '/api/user/captcha?t=' + Date.now()
}

onMounted(() => {
  refreshCaptcha()
})

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const response = await login(form.username, form.password, form.captcha)
      console.log('=== Login Response ===', response)

      // 尝试多种可能的数据结构
      let token = response?.token || response?.data?.token || response?.accessToken || response?.data?.accessToken
      let userInfo = response?.user || response?.data?.user || response?.userInfo || response?.data?.userInfo

      console.log('Extracted token:', token)
      console.log('Extracted userInfo:', userInfo)

      if (token) {
        setToken(token)
        console.log('✓ Token saved to localStorage')
      } else {
        console.warn('⚠ No token found in response')
      }

      if (userInfo) {
        setUser(userInfo)
        console.log('✓ User info saved to localStorage')
      }

      ElMessage.success('登录成功')
      console.log('准备跳转到 /home')

      router.push('/')

    } catch (error) {
      console.error('=== Login Error ===', error)
      console.error('Error response:', error.response)
      console.error('Error data:', error.response?.data)
      ElMessage.error(error.response?.data?.message || error.message || '登录失败，请重试')
      // 登录失败，刷新验证码
      refreshCaptcha()
      form.captcha = ''
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
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  top: -100px;
  right: -100px;
  animation: float 6s ease-in-out infinite;
}

.deco-circle-2 {
  width: 300px;
  height: 300px;
  background: linear-gradient(135deg, #22c55e 0%, #10b981 100%);
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
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border-radius: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 40px rgba(14, 165, 233, 0.4);
  transition: all 0.3s ease;
}

.logo-wrapper:hover {
  transform: scale(1.05);
  box-shadow: 0 12px 45px rgba(14, 165, 233, 0.5);
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
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
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

.captcha-wrapper {
  display: flex;
  gap: 12px;
  align-items: center;
}

.captcha-input {
  flex: 1;
  margin-bottom: 0;
}

.captcha-img {
  height: 48px;
  width: 100px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid rgba(255, 255, 255, 0.1);
  object-fit: cover;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.05);
}

.captcha-img:hover {
  border-color: #0ea5e9;
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