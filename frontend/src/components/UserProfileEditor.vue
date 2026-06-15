<!-- src/components/UserProfileEditor.vue -->
<template>
  <div class="user-profile-editor">
    <!-- 弹窗模式 -->
    <el-dialog
        v-if="isModal"
        :title="'编辑伙伴画像'"
        :model-value="visible"
        @update:model-value="handleUpdateVisible"
        :width="'600px'"
        :close-on-click-modal="false"
        class="profile-modal"
    >
      <el-form
          :model="profileForm"
          :rules="rules"
          ref="formRef"
          label-width="120px"
      >


        <!-- 头像上传 -->
        <el-form-item label="用户头像" prop="avatar">
          <div class="avatar-upload">
            <el-upload
                class="avatar-uploader"
                action="#"
                :http-request="customUploadAvatar"
                :show-file-list="false"
                accept="image/*"
            >
              <div v-if="profileForm.avatar" class="avatar-preview">
                <img :src="profileForm.avatar" class="avatar" alt="avatar" />
              </div>
              <div v-else class="avatar-placeholder">
                <i class="el-icon-plus avatar-uploader-icon"></i>
                <span class="upload-text">点击上传头像</span>
              </div>
            </el-upload>
            <p class="avatar-tip">支持 JPG、PNG 格式，大小不超过 2MB</p>
          </div>
        </el-form-item>

        <!-- MBTI类型选择 -->
        <el-form-item label="MBTI类型" prop="mbtiType">
          <el-select
              v-model="profileForm.mbtiType"
              placeholder="请选择您的MBTI类型"
              style="width: 100%;"
          >
            <el-option-group label="外向(E) / 内向(I)">
              <el-option label="ESTJ" value="ESTJ" />
              <el-option label="ESTP" value="ESTP" />
              <el-option label="ESFJ" value="ESFJ" />
              <el-option label="ESFP" value="ESFP" />
              <el-option label="ENTJ" value="ENTJ" />
              <el-option label="ENTP" value="ENTP" />
              <el-option label="ENFJ" value="ENFJ" />
              <el-option label="ENFP" value="ENFP" />
              <el-option label="ISTJ" value="ISTJ" />
              <el-option label="ISTP" value="ISTP" />
              <el-option label="ISFJ" value="ISFJ" />
              <el-option label="ISFP" value="ISFP" />
              <el-option label="INTJ" value="INTJ" />
              <el-option label="INTP" value="INTP" />
              <el-option label="INFJ" value="INFJ" />
              <el-option label="INFP" value="INFP" />
            </el-option-group>
          </el-select>
        </el-form-item>

        <!-- 标签编辑 -->
        <el-form-item label="个人标签" prop="tags">
          <el-input
              v-model="profileForm.tags"
              type="textarea"
              :rows="3"
              placeholder="请输入个人标签，用逗号分隔，例如：程序员,前端开发,React"
          />
        </el-form-item>

        <!-- 兴趣爱好 -->
        <el-form-item label="兴趣爱好" prop="interests">
          <el-input
              v-model="profileForm.interests"
              type="textarea"
              :rows="2"
              placeholder="请输入您的兴趣爱好，例如：编程、阅读、音乐、旅行"
          />
        </el-form-item>

        <!-- 偏好设置 -->
        <el-form-item label="偏好设置" prop="preferences">
          <el-input
              v-model="profileForm.preferences"
              type="textarea"
              :rows="2"
              placeholder="请输入您的偏好设置，例如：喜欢安静的工作环境，偏爱敏捷开发"
          />
        </el-form-item>

        <!-- 性格特征 -->
        <el-form-item label="性格特征" prop="personalityTraits">
          <el-input
              v-model="profileForm.personalityTraits"
              type="textarea"
              :rows="2"
              placeholder="请输入您的性格特征，例如：外向、细心、有责任心"
          />
        </el-form-item>

        <!-- 沟通风格 -->
        <el-form-item label="沟通风格" prop="communicationStyle">
          <el-input
              v-model="profileForm.communicationStyle"
              type="textarea"
              :rows="2"
              placeholder="请输入您的沟通风格，例如：直接、友好、注重细节"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="submitProfile" :loading="saving">
          <span v-if="!saving">保存画像</span>
          <span v-else>保存中...</span>
        </el-button>
      </template>
    </el-dialog>

    <!-- 内嵌模式（用于兼容旧代码） -->
    <div v-else class="editor-content">
      <el-form
          :model="profileForm"
          :rules="rules"
          ref="formRef"
          label-width="120px"
          style="max-width: 600px;"
      >
        <!-- 头像上传 -->
        <el-form-item label="用户头像" prop="avatar">
          <div class="avatar-upload">
            <el-upload
                class="avatar-uploader"
                action="#"
                :http-request="customUploadAvatar"
                :show-file-list="false"
                accept="image/*"
            >
              <div v-if="profileForm.avatar" class="avatar-preview">
                <img :src="profileForm.avatar" class="avatar" alt="avatar" />
              </div>
              <div v-else class="avatar-placeholder">
                <i class="el-icon-plus avatar-uploader-icon"></i>
                <span class="upload-text">点击上传头像</span>
              </div>
            </el-upload>
            <p class="avatar-tip">支持 JPG、PNG 格式，大小不超过 2MB</p>
          </div>
        </el-form-item>

        <!-- MBTI类型选择 -->
        <el-form-item label="MBTI类型" prop="mbtiType">
          <el-select
              v-model="profileForm.mbtiType"
              placeholder="请选择您的MBTI类型"
              style="width: 100%;"
          >
            <el-option-group label="外向(E) / 内向(I)">
              <el-option label="ESTJ" value="ESTJ" />
              <el-option label="ESTP" value="ESTP" />
              <el-option label="ESFJ" value="ESFJ" />
              <el-option label="ESFP" value="ESFP" />
              <el-option label="ENTJ" value="ENTJ" />
              <el-option label="ENTP" value="ENTP" />
              <el-option label="ENFJ" value="ENFJ" />
              <el-option label="ENFP" value="ENFP" />
              <el-option label="ISTJ" value="ISTJ" />
              <el-option label="ISTP" value="ISTP" />
              <el-option label="ISFJ" value="ISFJ" />
              <el-option label="ISFP" value="ISFP" />
              <el-option label="INTJ" value="INTJ" />
              <el-option label="INTP" value="INTP" />
              <el-option label="INFJ" value="INFJ" />
              <el-option label="INFP" value="INFP" />
            </el-option-group>
          </el-select>
        </el-form-item>

        <!-- 标签编辑 -->
        <el-form-item label="个人标签" prop="tags">
          <el-input
              v-model="profileForm.tags"
              type="textarea"
              :rows="3"
              placeholder="请输入个人标签，用逗号分隔"
          />
        </el-form-item>

        <!-- 兴趣爱好 -->
        <el-form-item label="兴趣爱好" prop="interests">
          <el-input
              v-model="profileForm.interests"
              type="textarea"
              :rows="2"
              placeholder="请输入您的兴趣爱好"
          />
        </el-form-item>

        <!-- 偏好设置 -->
        <el-form-item label="偏好设置" prop="preferences">
          <el-input
              v-model="profileForm.preferences"
              type="textarea"
              :rows="2"
              placeholder="请输入您的偏好设置"
          />
        </el-form-item>

        <!-- 性格特征 -->
        <el-form-item label="性格特征" prop="personalityTraits">
          <el-input
              v-model="profileForm.personalityTraits"
              type="textarea"
              :rows="2"
              placeholder="请输入您的性格特征"
          />
        </el-form-item>

        <!-- 沟通风格 -->
        <el-form-item label="沟通风格" prop="communicationStyle">
          <el-input
              v-model="profileForm.communicationStyle"
              type="textarea"
              :rows="2"
              placeholder="请输入您的沟通风格"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitProfile" :loading="saving">
            保存画像
          </el-button>
          <el-button @click="loadProfile">刷新</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserProfile, saveUserProfile, uploadAvatar, updateUserAvatar } from '@/api/userProfileService'
import { getUser } from '@/api/authService'

// 弹窗模式属性
const props = defineProps({
  isModal: {
    type: Boolean,
    default: false
  },
  visible: {
    type: Boolean,
    default: false
  }
})

// 弹窗模式事件
const emit = defineEmits(['update:visible', 'saved'])

// 表单引用
const formRef = ref(null)

// 表单数据
const profileForm = reactive({
  id: null,
  userId: null,
  avatar: '',
  mbtiType: '',
  tags: '',
  interests: '',
  preferences: '',
  personalityTraits: '',
  communicationStyle: ''
})

// 验证规则
const rules = {
  mbtiType: [
    { required: true, message: '请选择MBTI类型', trigger: 'change' }
  ],
  tags: [
    { required: true, message: '请输入个人标签', trigger: 'blur' }
  ]
}

// 加载状态
const saving = ref(false)

// 获取当前用户信息
const currentUser = getUser()

// 加载用户画像
const loadProfile = async () => {
  if (!currentUser?.id) {
    ElMessage.error('请先登录')
    return
  }

  try {
    const response = await getUserProfile(currentUser.id)
    if (response.code === 200 && response.data) {
      Object.assign(profileForm, response.data)
    } else {
      // 如果用户画像不存在，初始化默认值
      Object.assign(profileForm, {
        userId: currentUser.id,
        mbtiType: '',
        tags: '',
        interests: '',
        preferences: '',
        personalityTraits: '',
        communicationStyle: ''
      })
    }
  } catch (error) {
    console.error('加载伙伴画像失败:', error)
    ElMessage.error('加载伙伴画像失败')
  }
}

// 保存伙伴画像
const submitProfile = async () => {
  if (!formRef.value) return

  // 验证表单
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) {
    return
  }

  if (!currentUser?.id) {
    ElMessage.error('请先登录')
    return
  }

  // 设置用户ID
  profileForm.userId = currentUser.id

  saving.value = true
  try {
    // 先保存用户资料
    const response = await saveUserProfile(profileForm)
    if (response.code === 200) {
      ElMessage.success('伙伴画像保存成功')
      // 更新本地用户信息
      profileForm.id = response.data.id
      
      // 如果头像已更改，则更新头像
      if (profileForm.avatar) {
        await updateAvatarInDatabase(currentUser.id, profileForm.avatar)
      }
      
      // 如果是弹窗模式，关闭弹窗并触发事件
      if (props.isModal && emit) {
        emit('update:visible', false)
        emit('saved', { ...profileForm })
      }
    } else {
      ElMessage.error(response.message || '保存失败')
    }
  } catch (error) {
    console.error('保存伙伴画像失败:', error)
    ElMessage.error('保存伙伴画像失败')
  } finally {
    saving.value = false
  }
}

// 处理弹窗状态更新
const handleUpdateVisible = (val) => {
  emit('update:visible', val)
}

// 自定义上传头像方法
const customUploadAvatar = async (options) => {
  const { file } = options
  
  // 验证文件类型
  const validTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!validTypes.includes(file.type)) {
    ElMessage.error('只能上传 JPG、PNG、GIF、WEBP 格式的图片')
    return
  }
  
  // 验证文件大小（不超过2MB）
  const maxSize = 2 * 1024 * 1024 // 2MB
  if (file.size > maxSize) {
    ElMessage.error('图片大小不能超过 2MB')
    return
  }
  
  try {
    // 显示上传提示
    const loading = ElMessage({
      message: '正在上传头像...',
      type: 'info',
      duration: 0
    })
    
    // 上传文件到服务器
    const uploadResponse = await uploadAvatar(file)
    
    // 关闭加载提示
    loading.close()
    
    if (uploadResponse.code === 200 && uploadResponse.data) {
      // 获取上传后的URL
      const avatarUrl = uploadResponse.data.url || uploadResponse.data
      
      // 更新表单中的头像URL
      profileForm.avatar = avatarUrl
      
      ElMessage.success('头像上传成功')
    } else {
      ElMessage.error(uploadResponse.message || '头像上传失败')
    }
  } catch (error) {
    console.error('上传头像失败:', error)
    ElMessage.error('头像上传失败')
  }
}

// 更新用户头像到数据库
const updateAvatarInDatabase = async (userId, avatarUrl) => {
  try {
    const response = await updateUserAvatar(userId, avatarUrl)
    if (response.code === 200) {
      ElMessage.success('头像更新成功')
      return true
    } else {
      ElMessage.error(response.message || '头像更新失败')
      return false
    }
  } catch (error) {
    console.error('更新头像失败:', error)
    ElMessage.error('头像更新失败')
    return false
  }
}

// 取消操作
const handleCancel = () => {
  if (emit) {
    emit('update:visible', false)
  }
}

// 监听visible变化，当弹窗打开时重新加载数据
watch(() => props.visible, (newVal) => {
  if (newVal) {
    loadProfile()
  }
})

// 初始化加载
onMounted(() => {
  if (!props.isModal) {
    loadProfile()
  }
})
</script>

<style scoped>
.user-profile-editor {
  width: 100%;
}

/* 弹窗样式 */
:deep(.profile-modal) {
  .el-overlay-dialog {
    background: rgba(0, 0, 0, 0.5);
  }

  .el-dialog {
    background: linear-gradient(180deg, #0f172a 0%, #1e293b 100%) !important;
    backdrop-filter: blur(10px);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 16px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
  }

  .el-dialog__header {
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    padding: 24px 24px 16px 24px;
    background: linear-gradient(135deg, rgba(14, 165, 233, 0.1) 0%, rgba(139, 92, 246, 0.1) 100%);
    border-radius: 15px 15px 0 0;
  }

  .el-dialog__close {
    color: #94a3b8;
  }

  .el-dialog__close:hover {
    color: #f1f5f9;
  }

  .el-dialog__title {
    color: #f1f5f9;
    font-size: 18px;
    font-weight: 700;
    background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .el-dialog__body {
    padding: 24px;
    background: rgba(15, 23, 42, 0.6);
  }

  .el-dialog__footer {
    border-top: 1px solid rgba(255, 255, 255, 0.1);
    padding: 16px 24px;
    background: rgba(15, 23, 42, 0.8);
    border-radius: 0 0 15px 15px;
  }

  .el-form-item {
    margin-bottom: 20px;
  }

  .el-form-item__label {
    color: #94a3b8;
    font-weight: 600;
    padding-bottom: 8px;
    font-size: 14px;
  }

  .el-select, .el-input {
    width: 100%;
    
    .el-input__wrapper {
      background: rgba(15, 23, 42, 0.6) !important;
      border: 1px solid rgba(255, 255, 255, 0.15);
      border-radius: 12px;
      padding: 8px 12px;
      transition: all 0.3s ease;
    }

    .el-input__wrapper.is-focus {
      border-color: #0ea5e9;
      box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
    }

    .el-input__inner {
      color: #f1f5f9;
      background: rgba(15, 23, 42, 0.6) !important;
    }

    .el-textarea__inner {
      background: rgba(15, 23, 42, 0.6) !important;
      border: 1px solid rgba(255, 255, 255, 0.15);
      border-radius: 12px;
      color: #f1f5f9;
      padding: 12px;
      transition: all 0.3s ease;
      min-height: 80px;
    }

    .el-textarea__inner:focus {
      border-color: #0ea5e9;
      box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
    }
    
    /* 确保下拉选择器也是暗色主题 */
    .el-select__popper {
      background: #0f172a;
      border: 1px solid rgba(255, 255, 255, 0.15);
      border-radius: 12px;
    }
    
    .el-popper__arrow {
      background: #0f172a;
    }
  }

  .el-button {
    border-radius: 12px;
    padding: 10px 20px;
    font-weight: 600;
    transition: all 0.3s ease;
    border: 1px solid transparent;
  }

  .el-button--primary {
    background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%);
    border: none;
    color: white;
  }

  .el-button--primary:hover {
    background: linear-gradient(135deg, #0284c7 0%, #7c3aed 100%);
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(14, 165, 233, 0.4);
  }

  .el-button--default {
    background: linear-gradient(135deg, #0ea5e9 0%, #8b5cf6 100%) !important;
    border: none !important;
    color: white !important;
  }

  .el-button--default:hover {
    background: linear-gradient(135deg, #0284c7 0%, #7c3aed 100%) !important;
    transform: translateY(-2px);
    box-shadow: 0 6px 20px rgba(14, 165, 233, 0.4);
  }
  
  /* 修复select下拉选项背景 */
  .el-select .el-input__wrapper {
    background: rgba(15, 23, 42, 0.6) !important;
  }
  
  /* 修复选项列表背景 */
  .el-select-dropdown {
    background: #0f172a;
    border: 1px solid rgba(255, 255, 255, 0.15);
  }
  
  .el-select-dropdown__item {
    background: #0f172a;
    color: #cbd5e1;
  }
  
  .el-select-dropdown__item.hover, .el-select-dropdown__item:hover {
    background: rgba(14, 165, 233, 0.2);
  }
  
  .el-select-dropdown__item.selected {
    background: rgba(14, 165, 233, 0.3);
    color: #f1f5f9;
  }
}

/* 内嵌模式样式 */
.editor-content {
  padding: 20px;
  background: rgba(15, 23, 42, 0.6);
  border-radius: 16px;
}

/* 玻璃态效果类 */
.glass-effect {
  background: rgba(15, 23, 42, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
}
  .avatar-upload {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .avatar-uploader {
    display: inline-block;
    text-align: center;
  }

  .avatar {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid #dcdfe6;
  }

  .avatar-preview {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    overflow: hidden;
    cursor: pointer;
    transition: all 0.3s;
  }

  .avatar-preview:hover {
    transform: scale(1.05);
    border-color: #0ea5e9;
  }

  .avatar-placeholder {
    width: 80px;
    height: 80px;
    border-radius: 50%;
    border: 2px dashed #dcdfe6;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    transition: all 0.3s;
  }

  .avatar-placeholder:hover {
    border-color: #0ea5e9;
  }

  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 32px;
    height: 32px;
    text-align: center;
  }

  .upload-text {
    font-size: 12px;
    color: #8c939d;
    margin-top: 4px;
  }

  .avatar-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
    text-align: center;
  }
</style>