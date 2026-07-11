import http, { unwrapApiResponse } from './http'

export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER'

export interface WorkspaceInfo {
  workspaceId: number
  name: string
  description: string | null
  ownerId: number
  role: WorkspaceRole
}

export interface WorkspaceMemberInfo {
  userId: number
  email: string
  displayName: string
  role: WorkspaceRole
}

export async function listWorkspaces() {
  const response = await http.get('/workspaces')
  return unwrapApiResponse<WorkspaceInfo[]>(response.data, '获取工作空间失败')
}

export async function createWorkspace(request: {
  name: string
  description?: string
}) {
  const response = await http.post('/workspaces', request)
  return unwrapApiResponse<{ workspaceId: number }>(
    response.data,
    '创建工作空间失败'
  )
}

export async function listWorkspaceMembers(workspaceId: number) {
  const response = await http.get(`/workspaces/${workspaceId}/members`)
  return unwrapApiResponse<WorkspaceMemberInfo[]>(
    response.data,
    '获取工作空间成员失败'
  )
}

export async function addWorkspaceMember(
  workspaceId: number,
  request: { email: string; role: Exclude<WorkspaceRole, 'OWNER'> }
) {
  const response = await http.post(`/workspaces/${workspaceId}/members`, request)
  return unwrapApiResponse<void>(response.data, '添加工作空间成员失败')
}
