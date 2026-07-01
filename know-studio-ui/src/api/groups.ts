import http, { unwrapApiResponse, unwrapBareResponse } from './http'

export interface VisibleGroup {
  groupId: number
  groupCode: string
  groupName: string
}

export interface PendingInvitation {
  invitationId: number
  groupId: number
  groupName: string
  inviterUserId: number
  inviterDisplayName: string
  status: string
}

export interface GroupQueryResult {
  ownedGroups: VisibleGroup[]
  joinedGroups: VisibleGroup[]
  pendingInvitations: PendingInvitation[]
}

export interface GroupMember {
  userId: number
  userCode: string
  displayName: string
  role: 'OWNER' | 'MEMBER' | string
}

export interface MyJoinRequest {
  requestId: number
  groupId: number
  groupCode: string
  groupName: string
  status: string
  createdAt: string
  decidedAt: string | null
}

export interface OwnerJoinRequest {
  requestId: number
  groupId: number
  applicantUserId: number
  applicantUserCode: string
  applicantDisplayName: string
  status: string
  createdAt: string
}

export async function getMyGroups() {
  const response = await http.get<GroupQueryResult>('/groups/my')
  return unwrapBareResponse<GroupQueryResult>(response.data, '获取小组失败')
}

export async function createGroup(request: {
  name: string
  description?: string
}) {
  const response = await http.post('/groups', request)
  return unwrapApiResponse<number>(response.data, '创建小组失败')
}

export async function listGroupMembers(groupId: number) {
  const response = await http.get<GroupMember[]>(`/groups/${groupId}/members`)
  return unwrapBareResponse<GroupMember[]>(response.data, '获取成员失败')
}

export async function removeGroupMember(groupId: number, userId: number) {
  const response = await http.delete(`/groups/${groupId}/members/${userId}`)
  return unwrapApiResponse<void>(response.data, '移除成员失败')
}

export async function leaveGroup(groupId: number) {
  const response = await http.post(`/groups/${groupId}/leave`)
  return unwrapApiResponse<void>(response.data, '退出小组失败')
}

export async function createInvitation(groupId: number, inviteeUserId: number) {
  const response = await http.post(`/groups/${groupId}/invitations`, {
    inviteeUserId,
  })
  return unwrapApiResponse<number>(response.data, '创建邀请失败')
}

export async function acceptInvitation(invitationId: number) {
  const response = await http.post(`/invitations/${invitationId}/accept`)
  return unwrapApiResponse<void>(response.data, '接受邀请失败')
}

export async function rejectInvitation(invitationId: number) {
  const response = await http.post(`/invitations/${invitationId}/reject`)
  return unwrapApiResponse<void>(response.data, '拒绝邀请失败')
}

export async function submitJoinRequest(groupCode: string) {
  const response = await http.post('/groups/join-requests', { groupCode })
  return unwrapApiResponse<number>(response.data, '提交加入申请失败')
}

export async function listMyJoinRequests() {
  const response = await http.get('/groups/join-requests/my')
  return unwrapApiResponse<MyJoinRequest[]>(response.data, '获取加入申请失败')
}

export async function listOwnerJoinRequests(groupId: number) {
  const response = await http.get(`/groups/${groupId}/join-requests`)
  return unwrapApiResponse<OwnerJoinRequest[]>(
    response.data,
    '获取组内申请失败'
  )
}

export async function approveJoinRequest(groupId: number, requestId: number) {
  const response = await http.post(
    `/groups/${groupId}/join-requests/${requestId}/approve`
  )
  return unwrapApiResponse<void>(response.data, '批准加入申请失败')
}

export async function rejectJoinRequest(groupId: number, requestId: number) {
  const response = await http.post(
    `/groups/${groupId}/join-requests/${requestId}/reject`
  )
  return unwrapApiResponse<void>(response.data, '拒绝加入申请失败')
}
