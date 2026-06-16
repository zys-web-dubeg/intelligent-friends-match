import apiClient from './httpClient'

export const login = async (username, password, captcha) => {
  const response = await apiClient.post('/api/user/login', {
    username,
    password,
    captcha,
  })
  return response
}

export const register = async (username, password, phone, email) => {
  const response = await apiClient.post('/api/user/register', {
    username,
    password,
    phone,
    email,
  })
  return response
}

export const setToken = (token) => {
  localStorage.setItem('auth_token', token)
}

export const getToken = () => {
  return localStorage.getItem('auth_token')
}

export const removeToken = () => {
  localStorage.removeItem('auth_token')
}

export const setUser = (userInfo) => {
  localStorage.setItem('user_info', JSON.stringify(userInfo))
}

export const getUser = () => {
  const user = localStorage.getItem('user_info')
  return user ? JSON.parse(user) : null
}

export const removeUser = () => {
  localStorage.removeItem('user_info')
}

export const logout = () => {
  removeToken()
  removeUser()
}
