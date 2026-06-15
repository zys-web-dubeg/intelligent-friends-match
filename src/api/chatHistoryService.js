
import apiClient from './httpClient'

/**
 * 获取队伍聊天记录
 * @param {string} teamId - 队伍ID
 * @param {number} page - 页码，默认为0
 * @param {number} size - 每页大小，默认为20
 * @param {string} beforeTimestamp - 时间戳，获取此时间之前的消息
 * @returns {Promise<any>} 返回聊天记录列表
 */
export const getTeamChatHistory = async (teamId, page = 0, size = 20, beforeTimestamp = null) => {
    let url = `/api/v1/chat-history/team/${teamId}?page=${page}&size=${size}`
    if (beforeTimestamp) {
        url += `&beforeTimestamp=${beforeTimestamp}`
    }
    const response = await apiClient.get(url)
    return response
}

/**
 * 获取用户与AI的私聊记录
 * @param {string} userId - 用户ID
 * @param {string} aiId - AI助手ID
 * @param {number} page - 页码，默认为0
 * @param {number} size - 每页大小，默认为20
 * @returns {Promise<any>} 返回聊天记录列表
 */
export const getUserAiChatHistory = async (userId, aiId, page = 0, size = 20) => {
    const response = await apiClient.get(`/api/v1/chat-history/user/${userId}/ai/${aiId}?page=${page}&size=${size}`)
    return response
}

/**
 * 获取用户聊天统计信息
 * @param {number} userId - 用户ID
 * @returns {Promise<any>} 返回统计信息
 */
export const getUserChatStats = async (userId) => {
    const response = await apiClient.get(`/api/v1/chat-history/user/${userId}/stats`)
    return response
}

/**
 * 清理过期聊天记录
 * @param {string} retentionDays - 保留天数
 * @returns {Promise<any>} 返回清理结果
 */
export const cleanupChatHistory = async (retentionDays) => {
    const response = await apiClient.delete(`/api/v1/chat-history/cleanup?retentionDays=${retentionDays}`)
    return response
}