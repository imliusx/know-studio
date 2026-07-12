import http, { unwrapApiResponse } from './http'
import type { EntityId } from './id'

export type RetrievalMode = 'VECTOR_ONLY' | 'HYBRID' | 'HYBRID_RERANK'

export interface EvaluationDataset {
  id: EntityId
  knowledgeBaseId: EntityId
  userId: EntityId
  name: string
  description: string | null
  createdAt: string
}

export interface EvaluationSample {
  id: EntityId
  datasetId: EntityId
  question: string
  relevantChunkIds: EntityId[]
  expectedAnswer: string | null
  expectRefusal: boolean
  createdAt: string
}

export interface EvaluationRun {
  id: EntityId
  datasetId: EntityId
  userId: EntityId
  mode: RetrievalMode
  recallAtK: number
  refusalAccuracy: number
  sampleCount: number
  positiveSampleCount: number
  refusalSampleCount: number
  averageLatencyMillis: number
  topK: number
  createdAt: string
}

export interface EvaluationMetric {
  mode: RetrievalMode
  recallAtK: number
  refusalAccuracy: number
  sampleCount: number
  positiveSampleCount: number
  refusalSampleCount: number
  averageLatencyMillis: number
}

export interface EvaluationReport {
  datasetId: EntityId
  topK: number
  metrics: EvaluationMetric[]
  completedAt: string
}

const evaluationBase = (knowledgeBaseId: EntityId) =>
  `/knowledge-bases/${knowledgeBaseId}/evaluations`

export async function listEvaluationDatasets(knowledgeBaseId: EntityId) {
  const response = await http.get(`${evaluationBase(knowledgeBaseId)}/datasets`)
  return unwrapApiResponse<EvaluationDataset[]>(response.data, '获取评测数据集失败')
}

export async function createEvaluationDataset(
  knowledgeBaseId: EntityId,
  request: { name: string; description?: string }
) {
  const response = await http.post(
    `${evaluationBase(knowledgeBaseId)}/datasets`,
    request
  )
  return unwrapApiResponse<EvaluationDataset>(response.data, '创建评测数据集失败')
}

export async function listEvaluationSamples(
  knowledgeBaseId: EntityId,
  datasetId: EntityId
) {
  const response = await http.get(
    `${evaluationBase(knowledgeBaseId)}/datasets/${datasetId}/samples`
  )
  return unwrapApiResponse<EvaluationSample[]>(response.data, '获取评测样本失败')
}

export async function addEvaluationSample(
  knowledgeBaseId: EntityId,
  datasetId: EntityId,
  request: {
    question: string
    relevantChunkIds: EntityId[]
    expectedAnswer?: string
    expectRefusal: boolean
  }
) {
  const response = await http.post(
    `${evaluationBase(knowledgeBaseId)}/datasets/${datasetId}/samples`,
    request
  )
  return unwrapApiResponse<EvaluationSample>(response.data, '添加评测样本失败')
}

export async function listEvaluationRuns(
  knowledgeBaseId: EntityId,
  datasetId: EntityId
) {
  const response = await http.get(
    `${evaluationBase(knowledgeBaseId)}/datasets/${datasetId}/runs`
  )
  return unwrapApiResponse<EvaluationRun[]>(response.data, '获取评测记录失败')
}

export async function runEvaluationAblation(
  knowledgeBaseId: EntityId,
  datasetId: EntityId,
  topK: number
) {
  const response = await http.post(
    `${evaluationBase(knowledgeBaseId)}/datasets/${datasetId}/runs/ablation`,
    undefined,
    { params: { topK } }
  )
  return unwrapApiResponse<EvaluationReport>(response.data, '运行消融评测失败')
}
