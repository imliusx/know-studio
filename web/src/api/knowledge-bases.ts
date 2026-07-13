import http, { unwrapApiResponse } from './http'
import type { EntityId } from './id'

export type KnowledgeBasePermission = 'READ' | 'MANAGE'
export type KnowledgeBaseVisibility = 'COMPANY' | 'TEAM' | 'PRIVATE'

export interface KnowledgeBaseInfo {
  knowledgeBaseId: EntityId
  name: string
  description: string | null
  visibility: KnowledgeBaseVisibility
  ownerTeamId: EntityId | null
  permission: KnowledgeBasePermission
}

export interface KnowledgeBaseTeamGrantInfo {
  teamId: EntityId
  permission: KnowledgeBasePermission
}

export async function listKnowledgeBases() {
  const response = await http.get('/knowledge-bases')
  return unwrapApiResponse<KnowledgeBaseInfo[]>(response.data, '获取知识库失败')
}

export async function createKnowledgeBase(request: {
  name: string
  description?: string
  visibility: KnowledgeBaseVisibility
  ownerTeamId?: EntityId
}) {
  const response = await http.post('/knowledge-bases', request)
  return unwrapApiResponse<{ knowledgeBaseId: EntityId }>(
    response.data,
    '创建知识库失败'
  )
}

export async function listKnowledgeBaseTeamGrants(knowledgeBaseId: EntityId) {
  const response = await http.get(`/knowledge-bases/${knowledgeBaseId}/teams`)
  return unwrapApiResponse<KnowledgeBaseTeamGrantInfo[]>(
    response.data,
    '获取知识库授权失败'
  )
}

export async function saveKnowledgeBaseTeamGrant(
  knowledgeBaseId: EntityId,
  teamId: EntityId,
  permission: KnowledgeBasePermission
) {
  const response = await http.put(
    `/knowledge-bases/${knowledgeBaseId}/teams/${teamId}`,
    { permission }
  )
  return unwrapApiResponse<void>(response.data, '更新知识库授权失败')
}
