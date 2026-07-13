import http, { unwrapApiResponse } from './http'
import type { EntityId } from './id'

export interface AdminUserItem {
  userId: EntityId
  userCode: string
  username: string
  email: string
  displayName: string
  systemRole: 'ADMIN' | 'USER'
  status: string
  mustChangePassword: boolean
  lastLoginAt: string | null
}

export async function listAdminUsers() {
  const response = await http.get('/admin/users')
  return unwrapApiResponse<AdminUserItem[]>(response.data, '获取用户列表失败')
}

export async function getAdminUserDetail(userId: EntityId) {
  const response = await http.get(`/admin/users/${userId}`)
  return unwrapApiResponse<AdminUserItem>(response.data, '获取用户详情失败')
}

export async function updateUserStatus(userId: EntityId, status: string) {
  const response = await http.patch(`/admin/users/${userId}/status`, { status })
  return unwrapApiResponse<void>(response.data, '更新用户状态失败')
}
