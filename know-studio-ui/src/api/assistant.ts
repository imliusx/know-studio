import http, { HttpStatusError, unwrapApiResponse } from './http'
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

export type AssistantStreamEvent =
  | { event: 'token'; content: string }
  | { event: 'thinking'; content: string }
  | {
      event: 'tool_call'
      tool: { name: string; input: Record<string, unknown> }
    }
  | {
      event: 'tool_result'
      result: {
        name: string
        content: string
        metadata: Record<string, unknown>
      }
    }
  | { event: 'citation'; citation: Citation }
  | { event: 'done'; payload: unknown }
  | { event: 'error'; error: string }

interface SessionInfoWire {
  id: number
  title: string
  status: string
  createdAt: string
  updatedAt: string
}

interface ConversationContextWire {
  compactSummary: string
  sessionSummary: string
  recentMessages: Array<{
    id: number
    role: string
    content: string
    metadata: Record<string, unknown>
    createdAt: string
  }>
}

export async function createAssistantSession(
  workspaceId: number,
  title?: string,
  toolMode: AssistantToolMode = 'CHAT',
  deepThinking = false
) {
  const response = await http.post(`/workspaces/${workspaceId}/sessions`, {
    title,
    toolMode: toolMode === 'KB_SEARCH',
    deepThinking,
  })
  return toSessionDetail(
    unwrapApiResponse<SessionInfoWire>(response.data, '创建会话失败')
  )
}

export async function listAssistantSessions(workspaceId: number) {
  const response = await http.get(`/workspaces/${workspaceId}/sessions`)
  return unwrapApiResponse<SessionInfoWire[]>(
    response.data,
    '获取助手会话失败'
  ).map((session) => ({
    sessionId: session.id,
    title: session.title,
    lastMessageAt: session.updatedAt,
  }))
}

export async function getAssistantContext(
  workspaceId: number,
  sessionId: number,
  recentLimit = 20
) {
  const response = await http.get(
    `/workspaces/${workspaceId}/sessions/${sessionId}/context`,
    { params: { question: '', recentLimit } }
  )
  const context = unwrapApiResponse<ConversationContextWire>(
    response.data,
    '获取助手上下文失败'
  )
  return {
    summaryText: context.sessionSummary || context.compactSummary || null,
    recentMessages: context.recentMessages.map((message) => ({
      messageId: message.id,
      sessionId,
      role: message.role,
      toolMode: null,
      groupId: workspaceId,
      content: message.content,
      structuredPayload: JSON.stringify(message.metadata ?? {}),
      createdAt: message.createdAt,
    })),
  } satisfies AssistantConversationContext
}

export async function renameAssistantSession(
  workspaceId: number,
  sessionId: number,
  title: string
) {
  const response = await http.patch(
    `/workspaces/${workspaceId}/sessions/${sessionId}`,
    { title }
  )
  return toSessionDetail(
    unwrapApiResponse<SessionInfoWire>(response.data, '重命名会话失败')
  )
}

export async function deleteAssistantSession(
  workspaceId: number,
  sessionId: number
) {
  const response = await http.delete(
    `/workspaces/${workspaceId}/sessions/${sessionId}`
  )
  return unwrapApiResponse<void>(response.data, '删除会话失败')
}

export async function streamAssistantChat(
  workspaceId: number,
  request: {
    sessionId: number
    message: string
    toolMode: AssistantToolMode
    deepThinking: boolean
  },
  accessToken: string,
  onEvent: (event: AssistantStreamEvent) => void,
  signal?: AbortSignal
) {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? '/api'
  const response = await fetch(
    `${apiBaseUrl.replace(/\/$/, '')}/workspaces/${workspaceId}/agent/chat/stream`,
    {
      method: 'POST',
      credentials: 'include',
      signal,
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({
        sessionId: request.sessionId,
        message: request.message,
        toolMode: request.toolMode === 'KB_SEARCH',
        deepThinking: request.deepThinking,
      }),
    }
  )

  if (!response.ok || !response.body) {
    throw new HttpStatusError(
      response.status,
      (await readStreamErrorMessage(response)) ||
        `助手流式响应失败：${response.status}`
    )
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const frames = buffer.split(/\r?\n\r?\n/)
    buffer = frames.pop() ?? ''
    frames.forEach((frame) => {
      const event = parseSseEvent(frame)
      if (event) onEvent(event)
    })
  }
  buffer += decoder.decode()
  const tail = parseSseEvent(buffer)
  if (tail) onEvent(tail)
}

function parseSseEvent(frame: string): AssistantStreamEvent | null {
  const lines = frame.split(/\r?\n/)
  const event = lines
    .find((line) => line.startsWith('event:'))
    ?.slice(6)
    .trim()
  const data = lines
    .filter((line) => line.startsWith('data:'))
    .map((line) => line.slice(5).replace(/^ /, ''))
    .join('\n')
  if (!event || !data) return null
  let payload: unknown
  try {
    payload = JSON.parse(data)
  } catch {
    payload = data
  }
  if (event === 'token' || event === 'thinking') {
    return { event, content: String(payload ?? '') }
  }
  if (event === 'tool_call') {
    const record = asRecord(payload)
    const input = { ...record }
    delete input.name
    return {
      event,
      tool: {
        name: String(record.name ?? 'tool'),
        input,
      },
    }
  }
  if (event === 'tool_result') {
    const record = asRecord(payload)
    return {
      event,
      result: {
        name: String(record.toolName ?? record.name ?? 'tool'),
        content: String(record.content ?? ''),
        metadata: asRecord(record.metadata),
      },
    }
  }
  if (event === 'citation') {
    const record = asRecord(payload)
    return {
      event,
      citation: {
        documentId: Number(record.documentId),
        chunkId: Number(record.chunkId),
        chunkIndex: Number(record.chunkIndex),
        fileName: String(record.fileName ?? ''),
        score: Number(record.score ?? 0),
        snippet: String(record.text ?? ''),
      },
    }
  }
  if (event === 'done') return { event, payload }
  if (event === 'error') {
    const record = asRecord(payload)
    return {
      event,
      error: String(record.message ?? payload ?? '助手流式响应失败'),
    }
  }
  return null
}

function asRecord(value: unknown): Record<string, unknown> {
  return value && typeof value === 'object'
    ? (value as Record<string, unknown>)
    : {}
}

async function readStreamErrorMessage(response: Response) {
  try {
    const payload = asRecord(await response.clone().json())
    return typeof payload.message === 'string' ? payload.message : ''
  } catch {
    return (await response.text()).trim()
  }
}

function toSessionDetail(session: SessionInfoWire): AssistantSessionDetail {
  return {
    sessionId: session.id,
    title: session.title,
    status: session.status,
    lastMessageAt: session.updatedAt,
    createdAt: session.createdAt,
  }
}
