import http, { unwrapApiResponse } from './http'

export type RetrievalMode = 'VECTOR_ONLY' | 'HYBRID' | 'HYBRID_RERANK'

export interface EvaluationDataset {
  id: number
  knowledgeBaseId: number
  userId: number
  name: string
  description: string | null
  createdAt: string
}

export interface EvaluationSample {
  id: number
  datasetId: number
  question: string
  relevantChunkIds: number[]
  expectedAnswer: string | null
  createdAt: string
}

export interface EvaluationRun {
  id: number
  datasetId: number
  userId: number
  mode: RetrievalMode
  recallAtK: number
  sampleCount: number
  averageLatencyMillis: number
  topK: number
  createdAt: string
}

export interface EvaluationMetric {
  mode: RetrievalMode
  recallAtK: number
  sampleCount: number
  averageLatencyMillis: number
}

export interface EvaluationReport {
  datasetId: number
  topK: number
  metrics: EvaluationMetric[]
  completedAt: string
}

const evaluationBase = (knowledgeBaseId: number) =>
  `/knowledge-bases/${knowledgeBaseId}/evaluations`

export async function listEvaluationDatasets(knowledgeBaseId: number) {
  const response = await http.get(`${evaluationBase(knowledgeBaseId)}/datasets`)
  return unwrapApiResponse<EvaluationDataset[]>(response.data, '获取评测数据集失败')
}

export async function createEvaluationDataset(
  knowledgeBaseId: number,
  request: { name: string; description?: string }
) {
  const response = await http.post(
    `${evaluationBase(knowledgeBaseId)}/datasets`,
    request
  )
  return unwrapApiResponse<EvaluationDataset>(response.data, '创建评测数据集失败')
}

export async function listEvaluationSamples(
  knowledgeBaseId: number,
  datasetId: number
) {
  const response = await http.get(
    `${evaluationBase(knowledgeBaseId)}/datasets/${datasetId}/samples`
  )
  return unwrapApiResponse<EvaluationSample[]>(response.data, '获取评测样本失败')
}

export async function addEvaluationSample(
  knowledgeBaseId: number,
  datasetId: number,
  request: {
    question: string
    relevantChunkIds: number[]
    expectedAnswer?: string
  }
) {
  const response = await http.post(
    `${evaluationBase(knowledgeBaseId)}/datasets/${datasetId}/samples`,
    request
  )
  return unwrapApiResponse<EvaluationSample>(response.data, '添加评测样本失败')
}

export async function listEvaluationRuns(
  knowledgeBaseId: number,
  datasetId: number
) {
  const response = await http.get(
    `${evaluationBase(knowledgeBaseId)}/datasets/${datasetId}/runs`
  )
  return unwrapApiResponse<EvaluationRun[]>(response.data, '获取评测记录失败')
}

export async function runEvaluationAblation(
  knowledgeBaseId: number,
  datasetId: number,
  topK: number
) {
  const response = await http.post(
    `${evaluationBase(knowledgeBaseId)}/datasets/${datasetId}/runs/ablation`,
    undefined,
    { params: { topK } }
  )
  return unwrapApiResponse<EvaluationReport>(response.data, '运行消融评测失败')
}
