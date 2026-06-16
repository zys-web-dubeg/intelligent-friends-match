// src/api/userProfileService.js
import apiClient from './httpClient'

/**
 * 获取伙伴画像
 */
export const getUserProfile = async (userId) => {
    const response = await apiClient.get(`/api/v1/user-profile/${userId}`)
    return response
}

/**
 * 保存或更新伙伴画像
 */
export const saveUserProfile = async (userProfile) => {
    const response = await apiClient.post('/api/v1/user-profile', userProfile)
    return response
}

/**
 * 批量获取伙伴画像
 */
export const getUserProfilesBatch = async (userIds) => {
    const response = await apiClient.post('/api/v1/user-profile/batch', userIds)
    return response
}

/**
 * 更新用户标签
 */
export const updateUserTags = async (userId, tags) => {
    const response = await apiClient.put(`/api/v1/user-profile/${userId}/tags`, tags)
    return response
}

/**
 * 更新用户MBTI类型
 */
export const updateMbtiType = async (userId, mbtiType) => {
    const response = await apiClient.put(`/api/v1/user-profile/${userId}/mbti?mbtiType=${mbtiType}`)
    return response
}

/**
 * 查找相似用户
 */
export const findSimilarUsers = async (userId, limit = 5) => {
    const response = await apiClient.get(`/api/v1/user-profile/${userId}/similar-users?limit=${limit}`)
    return response
}

/**
 * 根据画像查找适合的队伍
 */
export const findSuitableTeams = async (userId) => {
    const response = await apiClient.get(`/api/v1/user-profile/${userId}/suitable-teams`)
    return response
}


/**
 * 上传头像
 */
export const uploadAvatar = async (file) => {
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await apiClient.post('/admin/common/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
    
    return response
}

/**
 * 更新用户头像
 */
export const updateUserAvatar = async (userId, avatarUrl) => {
    const response = await apiClient.put(`/api/v1/user-profile/${userId}/avatar`, { avatarUrl })
    return response
}