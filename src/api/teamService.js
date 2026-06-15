import apiClient from './httpClient'
import { getUser } from './authService'

export const createTeam = async (teamData) => {
  const response = await apiClient.post('/api/v1/teams', teamData)
  return response
}

export const joinTeam = async (teamId) => {
  const user = getUser()
  const userId = user?.id
  const response = await apiClient.post(`/api/v1/teams/${teamId}/join?userId=${userId}`)
  return response
}

export const getUserTeams = async () => {
  const user = getUser()
  const userId = user?.id
  const response = await apiClient.get(`/api/v1/teams/user/${userId}`)
  return response
}

export const getMatchTeams = async () => {
  const user = getUser()
  const userId = user?.id
  const response = await apiClient.get(`/api/v1/teams/match/${userId}`)
  return response
}

export const getTeamMembers = async (teamId) => {
  const response = await apiClient.get(`/api/v1/teams/${teamId}/members`)
  return response
}
