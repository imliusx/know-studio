import http, { unwrapApiResponse } from './http'

export type TeamRole = 'TEAM_ADMIN' | 'MEMBER'

export interface TeamInfo {
  teamId: number
  name: string
  description: string | null
  role: TeamRole
}

export interface TeamMemberInfo {
  userId: number
  email: string
  displayName: string
  role: TeamRole
}

export async function listTeams() {
  const response = await http.get('/teams')
  return unwrapApiResponse<TeamInfo[]>(response.data, '获取团队失败')
}

export async function createTeam(request: { name: string; description?: string }) {
  const response = await http.post('/teams', request)
  return unwrapApiResponse<{ teamId: number }>(response.data, '创建团队失败')
}

export async function listTeamMembers(teamId: number) {
  const response = await http.get(`/teams/${teamId}/members`)
  return unwrapApiResponse<TeamMemberInfo[]>(response.data, '获取团队成员失败')
}

export async function addTeamMember(
  teamId: number,
  request: { email: string; role: TeamRole }
) {
  const response = await http.post(`/teams/${teamId}/members`, request)
  return unwrapApiResponse<void>(response.data, '添加团队成员失败')
}

export async function updateTeamMember(teamId: number, userId: number, role: TeamRole) {
  const response = await http.put(`/teams/${teamId}/members/${userId}`, { role })
  return unwrapApiResponse<void>(response.data, '更新成员角色失败')
}

export async function removeTeamMember(teamId: number, userId: number) {
  const response = await http.delete(`/teams/${teamId}/members/${userId}`)
  return unwrapApiResponse<void>(response.data, '移除团队成员失败')
}
