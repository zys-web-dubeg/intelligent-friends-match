// src/api/userService.js
import apiClient from './httpClient'

/**
 * 批量获取用户信息
 */
export const getUsersBatch = async (userIds) => {
    const response = await apiClient.post('/api/user/batch', userIds);
    return response;
};

/**
 * 根据ID获取单个用户信息
 */
export const getUserById = async (userId) => {
    const response = await apiClient.get(`/api/user/${userId}`);
    return response;
};

/**
 * 获取所有用户列表
 */
export const getAllUsers = async () => {
    const response = await apiClient.get('/api/user/list');
    return response;
};