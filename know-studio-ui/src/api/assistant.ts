import http, { unwrapApiResponse, unwrapBareResponse } from './http'
import type { Citation } from './qa'

export type AssistantToolMode = 'CHAT' | 'KB_SEARCH'

export interface AssistantSessionListItem {
  sessionId: number
  title: string
  lastMessageAt: string | null
}

export interface AssistantSessionDetail {
  sessionId: number
  title: string
  status: string
  lastMessageAt: string | null
  createdAt: string
}

export interface AssistantMessage {
  messageId: number
  sessionId: number
  role: string
  toolMode: AssistantToolMode | null
  groupId: number | null
  content: string
  structuredPayload: string | null
  createdAt: string
}

export interface AssistantConversationContext {
  summaryText: string | null
  recentMessages: AssistantMessage[]
}

export interface AssistantChatRequest {
  sessionId: number
  message: string
  toolMode: AssistantToolMode
  groupId?: number
}

export interface AssistantChatResponse {
  sessionId: number
  messageId: number
  reply: string
  toolMode: AssistantToolMode
  groupId: number | null
  citations?: Citation[]
}

export interface AssistantStreamEvent {
  event: 'start' | 'delta' | 'done' | 'error'
  sessionId?: number
  toolMode?: AssistantToolMode
  groupId?: number
  delta?: string
  messageId?: number
  reply?: string
  citations?: Citation[]
  error?: string
}

export async function createAssistantSession(initialMessage?: string) {
  const response = await http.post('/assistant/sessions', { initialMessage })
  return unwrapApiResponse<AssistantSessionDetail>(
    response.data,
    '创建会话失败'
  )
}

export async function listAssistantSessions() {
  const response = await http.get<AssistantSessionListItem[]>(
    '/assistant/sessions'
  )
  return unwrapBareResponse<AssistantSessionListItem[]>(
    response.data,
    '获取助手会话失败'
  )
}

export async function getAssistantContext(sessionId: number, recentLimit = 20) {
  const response = await http.get<AssistantConversationContext>(
    `/assistant/sessions/${sessionId}/context`,
    { params: { recentLimit } }
  )
  return unwrapBareResponse<AssistantConversationContext>(
    response.data,
    '获取助手上下文失败'
  )
}

export async function renameAssistantSession(sessionId: number, title: string) {
  const response = await http.patch(`/assistant/sessions/${sessionId}`, {
    title,
  })
  return unwrapApiResponse<AssistantSessionDetail>(
    response.data,
    '重命名会话失败'
  )
}

export async function deleteAssistantSession(sessionId: number) {
  const response = await http.delete(`/assistant/sessions/${sessionId}`)
  return unwrapApiResponse<void>(response.data, '删除会话失败')
}

export async function chatWithAssistant(request: AssistantChatRequest) {
  const response = await http.post('/assistant/chat', request)
  return unwrapApiResponse<AssistantChatResponse>(response.data, '发送消息失败')
}

export async function streamAssistantChat(
  request: AssistantChatRequest,
  accessToken: string,
  onEvent: (event: AssistantStreamEvent) => void,
  signal?: AbortSignal
) {
  const response = await fetch('/api/assistant/chat/stream', {
    method: 'POST',
    credentials: 'include',
    signal,
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify(request),
  })

  if (!response.ok || !response.body) {
    throw new Error(`助手流式响应失败：${response.status}`)
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''

  while (true) {
    const { done, value } = await reader.read()
    if (done) break

    buffer += decoder.decode(value, { stream: true })
    const events = buffer.split('\n\n')
    buffer = events.pop() ?? ''

    for (const rawEvent of events) {
      const parsed = parseSseEvent(rawEvent)
      if (parsed) onEvent(parsed)
    }
  }

  const tail = parseSseEvent(buffer)
  if (tail) onEvent(tail)
}

function parseSseEvent(rawEvent: string): AssistantStreamEvent | null {
  const lines = rawEvent.split('\n')
  const eventName = lines
    .find((line) => line.startsWith('event:'))
    ?.slice('event:'.length)
    .trim()
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice('data:'.length).trim())
    .join('\n')

  if (!data) return null

  try {
    const parsed = JSON.parse(data) as AssistantStreamEvent
    if (eventName && !parsed.event) {
      return { ...parsed, event: eventName as AssistantStreamEvent['event'] }
    }
    return parsed
  } catch {
    return null
  }
}
