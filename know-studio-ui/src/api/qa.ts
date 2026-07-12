import http, { unwrapBareResponse } from './http'

export interface Citation {
  knowledgeBaseId: number
  documentId: number
  chunkId: number
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
