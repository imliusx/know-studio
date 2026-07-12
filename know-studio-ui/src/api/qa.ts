import http, { unwrapBareResponse } from './http'
import type { EntityId } from './id'

export interface Citation {
  knowledgeBaseId: EntityId
  documentId: EntityId
  chunkId: EntityId
  chunkIndex: number
  fileName: string
  score: number
  snippet: string
}

export interface AskQuestionResponse {
  answered: boolean
  answer?: string
  reasonCode?: string
  reasonMessage?: string
  citations?: Citation[]
}

export async function askQuestion(request: {
  groupId: number
  question: string
}) {
  const response = await http.post<AskQuestionResponse>('/qa/ask', request)
  return unwrapBareResponse<AskQuestionResponse>(response.data, '提问失败')
}
