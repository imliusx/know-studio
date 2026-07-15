import http, { unwrapApiResponse } from './http'

export interface CurrentUserProfile {
  userId: number
  userCode: string
  displayName: string
  systemRole: 'ADMIN' | 'USER'
  mustChangePassword: boolean
}

export interface AuthTokensResponse {
  accessToken: string
  currentUser: CurrentUserProfile
}

export interface LoginRequest {
  loginId: string
  password: string
}

export interface RegisterRequest {
  username: string
  email: string
  displayName: string
  password: string
}

export interface ResetPasswordRequest {
  username: string
  email: string
  newPassword: string
}

export async function login(request: LoginRequest) {
  const response = await http.post('/auth/login', request)
  return unwrapApiResponse<AuthTokensResponse>(response.data, '登录失败')
}

export async function register(request: RegisterRequest) {
  const response = await http.post('/auth/register', request)
  return unwrapApiResponse<void>(response.data, '注册失败')
}

export async function getCurrentUser() {
  const response = await http.get('/auth/me')
  return unwrapApiResponse<CurrentUserProfile>(response.data, '获取当前用户失败')
}

export async function logout() {
  const response = await http.post('/auth/logout')
  return unwrapApiResponse<void>(response.data, '登出失败')
}

export async function changePassword(request: {
  currentPassword: string
  newPassword: string
}) {
  const response = await http.post('/account/change-password', request)
  return unwrapApiResponse<void>(response.data, '修改密码失败')
}

export async function resetPasswordByIdentity(request: ResetPasswordRequest) {
  const response = await http.post('/auth/reset-password', request)
  return unwrapApiResponse<void>(response.data, '重置密码失败')
}
