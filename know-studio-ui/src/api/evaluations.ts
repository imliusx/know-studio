import http, { unwrapApiResponse } from './http'

export type RetrievalMode = 'VECTOR_ONLY' | 'HYBRID' | 'HYBRID_RERANK'

export interface EvaluationDataset {
  id: number
  workspaceId: number
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

const evaluationBase = (workspaceId: number) =>
  `/workspaces/${workspaceId}/evaluations`

export async function listEvaluationDatasets(workspaceId: number) {
  const response = await http.get(`${evaluationBase(workspaceId)}/datasets`)
  return unwrapApiResponse<EvaluationDataset[]>(response.data, '获取评测数据集失败')
}

export async function createEvaluationDataset(
  workspaceId: number,
  request: { name: string; description?: string }
) {
  const response = await http.post(
    `${evaluationBase(workspaceId)}/datasets`,
    request
  )
  return unwrapApiResponse<EvaluationDataset>(response.data, '创建评测数据集失败')
}

export async function listEvaluationSamples(
  workspaceId: number,
  datasetId: number
) {
  const response = await http.get(
    `${evaluationBase(workspaceId)}/datasets/${datasetId}/samples`
  )
  return unwrapApiResponse<EvaluationSample[]>(response.data, '获取评测样本失败')
}

export async function addEvaluationSample(
  workspaceId: number,
  datasetId: number,
  request: {
    question: string
    relevantChunkIds: number[]
    expectedAnswer?: string
  }
) {
  const response = await http.post(
    `${evaluationBase(workspaceId)}/datasets/${datasetId}/samples`,
    request
  )
  return unwrapApiResponse<EvaluationSample>(response.data, '添加评测样本失败')
}

export async function listEvaluationRuns(
  workspaceId: number,
  datasetId: number
) {
  const response = await http.get(
    `${evaluationBase(workspaceId)}/datasets/${datasetId}/runs`
  )
  return unwrapApiResponse<EvaluationRun[]>(response.data, '获取评测记录失败')
}

export async function runEvaluationAblation(
  workspaceId: number,
  datasetId: number,
  topK: number
) {
  const response = await http.post(
    `${evaluationBase(workspaceId)}/datasets/${datasetId}/runs/ablation`,
    undefined,
    { params: { topK } }
  )
  return unwrapApiResponse<EvaluationReport>(response.data, '运行消融评测失败')
}
