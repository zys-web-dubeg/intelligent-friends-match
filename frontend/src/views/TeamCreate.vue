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
          <el-radio-group v-model="form.accessLevel" class="access-options">
            <el-radio-button
              v-for="option in accessOptions"
              :key="option.value"
              :value="option.value"
              class="access-option"
            >
              <span class="access-option-inner">
                <span :class="['access-icon', option.className]">
                  <i :class="option.icon"></i>
                </span>
                <span class="access-copy">
                  <span class="access-title">{{ option.label }}</span>
                  <span class="access-desc">{{ option.desc }}</span>
                </span>
              </span>
            </el-radio-button>
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

const accessOptions = [
  {
    value: 0,
    label: '公开',
    desc: '任何人可加入',
    icon: 'fa-solid fa-globe',
    className: 'public'
  },
  {
    value: 1,
    label: '审核',
    desc: '需审核加入',
    icon: 'fa-solid fa-shield-halved',
    className: 'review'
  },
  {
    value: 2,
    label: '私有',
    desc: '仅邀请可加入',
    icon: 'fa-solid fa-lock',
    className: 'private'
  }
]

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
  min-height: 100vh;
  background: #0f172a;
  color: #f1f5f9;
}

.page-header,
.form-card {
  max-width: 860px;
  margin-left: auto;
  margin-right: auto;
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
:deep(.team-form .el-form-item) {
  margin-bottom: 24px;
}

:deep(.team-form .el-form-item__label) {
  color: #94a3b8;
  font-weight: 600;
}

/* 输入框包装器 */
.input-wrapper {
  position: relative;
  width: 100%;
}

.input-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: #64748b;
  font-size: 15px;
  z-index: 1;
  pointer-events: none;
}

:deep(.form-input .el-input__wrapper) {
  padding-left: 44px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  transition: all 0.3s ease;
}

:deep(.form-input .el-input__wrapper.is-focus) {
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

:deep(.form-input .el-textarea__inner) {
  min-height: 96px !important;
  padding: 12px 14px 12px 44px;
  border-radius: 12px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.05);
  box-shadow: none;
  color: #f1f5f9;
  resize: vertical;
  transition: all 0.3s ease;
}

:deep(.form-input .el-textarea__inner:focus) {
  border-color: #0ea5e9;
  box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
}

:deep(.form-input .el-textarea__inner::placeholder) {
  color: #64748b;
}

/* 单选框组 */
.access-options {
  width: 100%;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

:deep(.access-option) {
  height: auto;
}

:deep(.access-option .el-radio-button__inner) {
  width: 100%;
  height: 100%;
  padding: 0;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 12px;
  box-shadow: none;
  color: inherit;
  overflow: hidden;
  transition: all 0.3s ease;
}

:deep(.access-option:first-child .el-radio-button__inner),
:deep(.access-option:last-child .el-radio-button__inner) {
  border-radius: 12px;
}

:deep(.access-option .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: rgba(14, 165, 233, 0.12);
  border-color: rgba(14, 165, 233, 0.45);
  box-shadow: 0 0 0 1px rgba(14, 165, 233, 0.25);
}

:deep(.access-option .el-radio-button__inner:hover) {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(14, 165, 233, 0.3);
}

.access-option-inner {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  min-height: 82px;
  text-align: left;
}

.access-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.access-icon.public {
  background: rgba(34, 197, 94, 0.15);
  color: #22c55e;
}

.access-icon.review {
  background: rgba(245, 158, 11, 0.15);
  color: #f59e0b;
}

.access-icon.private {
  background: rgba(239, 68, 68, 0.15);
  color: #ef4444;
}

.access-copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
}

.access-title {
  font-size: 15px;
  font-weight: 600;
  color: #f1f5f9;
  line-height: 1.2;
}

.access-desc {
  font-size: 12px;
  color: #64748b;
  line-height: 1.4;
}

/* 表单操作按钮 */
.form-actions {
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.form-actions .el-form-item__content) {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
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

@media (max-width: 900px) {
  .access-options {
    grid-template-columns: 1fr;
  }

  .team-create-page {
    padding: 16px;
  }

  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .form-card {
    padding: 24px 18px;
  }

  :deep(.team-form .el-form-item) {
    display: block;
  }

  :deep(.team-form .el-form-item__label) {
    justify-content: flex-start;
    width: auto !important;
    margin-bottom: 8px;
  }

  :deep(.team-form .el-form-item__content) {
    margin-left: 0 !important;
  }

  :deep(.form-actions .el-form-item__content) {
    flex-direction: column;
  }

  .submit-btn,
  .cancel-btn {
    width: 100%;
    justify-content: center;
  }
}
</style>
