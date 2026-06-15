import apiClient from './httpClient'
import { getUser, getToken } from './authService'

const getUserId = () => {
  const user = getUser()
  return user?.id
}

/**
 * 获取伙伴推荐
 */
export const getRecommendations = async (matchType = 'friend', limit = 5) => {
  const userId = getUserId()
  const response = await apiClient.get(`/api/v1/partners/recommend/${userId}?matchType=${matchType}&limit=${limit}`)
  return response
}

/**
 * 发送匹配请求
 * body: { fromUserId, toUserId, matchType, message }
 */
export const sendMatchRequest = async (toUserId, matchType = 'friend', message = '') => {
  const fromUserId = getUserId()
  const response = await apiClient.post('/api/v1/partners/request', { fromUserId, toUserId, matchType, message })
  return response
}

/**
 * 接受匹配请求
 */
export const acceptMatchRequest = async (requestId) => {
  const response = await apiClient.post(`/api/v1/partners/accept/${requestId}`)
  return response
}

/**
 * 拒绝匹配请求
 */
export const rejectMatchRequest = async (requestId) => {
  const response = await apiClient.post(`/api/v1/partners/reject/${requestId}`)
  return response
}

/**
 * 获取我的伙伴列表
 * 返回: [{ relationId, partnerUserId, matchType, message }]
 */
export const getMyPartners = async () => {
  const userId = getUserId()
  const response = await apiClient.get(`/api/v1/partners/matches/${userId}`)
  return response
}

/**
 * 获取收到的待处理请求
 * 返回: [{ id, fromUserId, toUserId, status, matchType, message }]
 */
export const getPendingRequests = async () => {
  const userId = getUserId()
  const response = await apiClient.get(`/api/v1/partners/pending/${userId}`)
  return response
}

/**
 * 获取发出的待处理请求
 */
export const getSentRequests = async () => {
  const userId = getUserId()
  const response = await apiClient.get(`/api/v1/partners/sent/${userId}`)
  return response
}

/**
 * 取消发出的请求
 * POST /api/v1/partners/cancel/{requestId}?userId=xxx
 */
export const cancelMatchRequest = async (requestId) => {
  const userId = getUserId()
  const response = await apiClient.post(`/api/v1/partners/cancel/${requestId}?userId=${userId}`)
  return response
}

/**
 * 解除匹配关系
 * POST /api/v1/partners/unmatch/{relationId}?userId=xxx
 */
export const unmatchPartner = async (relationId) => {
  const userId = getUserId()
  const response = await apiClient.post(`/api/v1/partners/unmatch/${relationId}?userId=${userId}`)
  return response
}

/**
 * 与智能伙伴匹配助手对话（流式响应）
 * 使用 axios 原生 XHR 获取流式数据
 * 返回一个对象，包含 { request, onProgress } 用于消费流式响应
 */
export const chatWithPartnerAgent = (memoryId, message) => {
  const userId = getUserId()
  const token = getToken()

  // 使用原生 XMLHttpRequest 处理流式响应
  const xhr = new XMLHttpRequest()
  let lastIndex = 0
  const callbacks = { onProgress: null, onComplete: null, onError: null }

  xhr.open('POST', '/api/v1/partners/chat')
  xhr.setRequestHeader('Content-Type', 'application/json')
  if (token) {
    xhr.setRequestHeader('Authorization', `Bearer ${token}`)
  }
  xhr.responseType = ''

  xhr.onprogress = () => {
    const newText = xhr.responseText.substring(lastIndex)
    lastIndex = xhr.responseText.length
    if (callbacks.onProgress) {
      callbacks.onProgress(newText)
    }
  }

  xhr.onload = () => {
    // 补全可能遗漏的最后一段流式数据
    if (lastIndex < xhr.responseText.length && callbacks.onProgress) {
      const remainingText = xhr.responseText.substring(lastIndex)
      callbacks.onProgress(remainingText)
    }
    if (callbacks.onComplete) callbacks.onComplete()
  }

  xhr.onerror = (err) => {
    if (callbacks.onError) callbacks.onError(err)
  }

  xhr.send(JSON.stringify({ memoryId, message, userId }))

  return {
    onProgress: (fn) => { callbacks.onProgress = fn },
    onComplete: (fn) => { callbacks.onComplete = fn },
    onError: (fn) => { callbacks.onError = fn },
    abort: () => { xhr.abort() },
  }
}
