<template>
  <div class="team-create-page">
    <!-- 页面头部 -->
    <div class="page-header glass-effect">
      <el-button class="back-btn" @click="$router.back()">
        <i class="fa-solid fa-arrow-left"></i>
        <span>返回</span>
      </el-button>
      <div class="header-content">
        <h2>创建队伍</h2>
        <p>创建一个新的队伍，邀请志同道合的伙伴</p>
      </div>
    </div>

    <!-- 表单卡片 -->
    <div class="form-card glass-effect">
      <div class="form-header">
        <div class="form-icon">
          <i class="fa-solid fa-users"></i>
        </div>
        <h3>队伍信息</h3>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        @submit.prevent="handleSubmit"
        class="team-form"
      >
        <el-form-item label="队伍名称" prop="name">
          <div class="input-wrapper">
            <i class="fa-solid fa-hashtag input-icon"></i>
            <el-input v-model="form.name" placeholder="请输入队伍名称" maxlength="50" show-word-limit class="form-input" />
          </div>
        </el-form-item>

        <el-form-item label="访问级别" prop="accessLevel">
          <el-radio-group v-model="form.accessLevel" class="radio-group">
            <el-radio :value="0" class="radio-item">
              <div class="radio-icon public"><i class="fa-solid fa-globe"></i></div>
              <div class="radio-text">
                <div class="radio-label">公开</div>
                <div class="radio-desc">任何人可加入</div>
              </div>
            </el-radio>
            <el-radio :value="1" class="radio-item">
              <div class="radio-icon review"><i class="fa-solid fa-shield-check"></i></div>
              <div class="radio-text">
                <div class="radio-label">审核</div>
                <div class="radio-desc">需审核加入</div>
              </div>
            </el-radio>
            <el-radio :value="2" class="radio-item">
              <div class="radio-icon private"><i class="fa-solid fa-lock"></i></div>
              <div class="radio-text">
                <div class="radio-label">私有</div>
                <div class="radio-desc">仅邀请可加入</div>
              </div>
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="个性约束" prop="personalityConstraint">
          <div class="input-wrapper">
            <i class="fa-solid fa-heart input-icon"></i>
            <el-input
              v-model="form.personalityConstraint"
              type="textarea"
              :rows="3"
              placeholder="可选，描述队伍成员个性要求"
              class="form-input"
            />
          </div>
        </el-form-item>

        <el-form-item label="向量ID" prop="pineconeVectorId">
          <div class="input-wrapper">
            <i class="fa-solid fa-database input-icon"></i>
            <el-input v-model="form.pineconeVectorId" placeholder="可选，Pinecone向量ID" class="form-input" />
          </div>
        </el-form-item>

        <el-form-item class="form-actions">
          <el-button type="primary" class="submit-btn" @click="handleSubmit" :loading="loading">
            <i class="fa-solid fa-plus"></i>
            <span>创建队伍</span>
          </el-button>
          <el-button class="cancel-btn" @click="$router.back()">
            <i class="fa-solid fa-x"></i>
            <span>取消</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { createTeam } from '@/api/teamService'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  name: '',
  accessLevel: 0,
  personalityConstraint: '',
  pineconeVectorId: ''
})

const rules = {
  name: [
    { required: true, message: '请输入队伍名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const payload = {
      name: form.name,
      accessLevel: form.accessLevel,
    }
    if (form.personalityConstraint) payload.personalityConstraint = form.personalityConstraint
    if (form.pineconeVectorId) payload.pineconeVectorId = form.pineconeVectorId

    await createTeam(payload)
    ElMessage.success('队伍创建成功')
    router.push('/teams')
  } catch (e) {
    ElMessage.error(e.message || '创建队伍失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.team-create-page {
  padding: 24px;
  max-width: 600px;
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

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 10px;
  color: #94a3b8;
  transition: all 0.3s ease;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #f1f5f9;
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

/* 表单卡片 */
.form-card {
  padding: 32px;
  border-radius: 16px;
}

.form-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.form-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, rgba(14, 165, 233, 0.2) 0%, rgba(139, 92, 246, 0.2) 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #0ea5e9;
  font-size: 20px;
}

.form-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
}

/* 表单样式 */
.team-form {
  .el-form-item {
    margin-bottom: 24px;
  }

  .el-form-item__label {
    color: #94a3b8;
    font-weight: 500;
  }
}

/* 输入框包装器 */
.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #64748b;
  font-size: 15px;
  z-index: 1;
}

:deep(.form-input .el-input__wrapper) {
  padding-left: 44px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
}

:deep(.form-input .el-input__wrapper:focus) {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
}

:deep(.form-input .el-input__inner) {
  color: #f1f5f9;
  background: transparent;
}

:deep(.form-input .el-input__inner::placeholder) {
  color: #64748b;
}

/* 单选框组 */
.radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.radio-item) {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.radio-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(14, 165, 233, 0.2);
}

:deep(.radio-item.is-checked) {
  background: rgba(14, 165, 233, 0.1);
  border-color: rgba(14, 165, 233, 0.4);
}

:deep(.radio-item .el-radio__input) {
  display: none;
}

.radio-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}

.radio-icon.public {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.radio-icon.review {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.radio-icon.private {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.radio-text {
  flex: 1;
}

.radio-label {
  font-size: 15px;
  font-weight: 600;
  color: #f1f5f9;
}

.radio-desc {
  font-size: 12px;
  color: #64748b;
}

/* 表单操作按钮 */
.form-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.submit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 24px;
  background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
  border: none;
  border-radius: 12px;
  font-weight: 600;
  transition: all 0.3s ease;
}

.submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(14, 165, 233, 0.4);
}

.cancel-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 24px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  color: #94a3b8;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #f1f5f9;
}
</style>