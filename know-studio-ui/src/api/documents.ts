import http, { unwrapApiResponse, unwrapBareResponse } from './http'

export interface DocumentListItem {
  documentId: number
  groupId: number
  fileName: string
  fileExt: string | null
  contentType: string | null
  fileSize: number
  status: string
  failureReason: string | null
  uploadedAt: string
  uploaderUserId: number
  uploaderUserCode: string
  uploaderDisplayName: string
  previewText: string | null
}

export interface DocumentPreview {
  documentId: number
  fileName: string
  previewText: string
}

export interface DocumentQuery {
  groupId?: number
  fileName?: string
  status?: string
}

export interface InitDocumentUploadPayload {
  groupId: number
  fileName: string
  fileSize: number
  contentType: string
  fileHash: string
  chunkSize: number
  chunkCount: number
}

export interface UploadChunkPayload {
  uploadId: string
  chunkIndex: number
  chunkHash: string
  chunk: Blob
}

export interface UploadInitResult {
  instantUpload: boolean
  documentId: number | null
  uploadId: string | null
  uploadedChunks: number[]
  chunkSize: number | null
  chunkCount: number | null
}

export interface UploadStatusResult {
  status: string
  uploadedChunks: number[]
  uploadedChunkCount: number
  chunkCount: number | null
}

export async function listDocuments(query: DocumentQuery = {}) {
  const response = await http.get<DocumentListItem[]>('/documents', {
    params: query,
  })
  return unwrapBareResponse<DocumentListItem[]>(response.data, '获取文档失败')
}

export async function uploadDocument(
  groupId: number,
  file: File,
  onProgress?: (loadedBytes: number, totalBytes?: number) => void
) {
  const formData = new FormData()
  formData.append('groupId', String(groupId))
  formData.append('file', file)

  const response = await http.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (event) => onProgress?.(event.loaded, event.total),
    timeout: 120_000,
  })
  return unwrapApiResponse<number>(response.data, '上传文档失败')
}

export async function initDocumentUpload(payload: InitDocumentUploadPayload) {
  const response = await http.post('/documents/upload/init', payload)
  return unwrapApiResponse<UploadInitResult>(response.data, '初始化上传失败')
}

export async function uploadDocumentChunk(
  payload: UploadChunkPayload,
  onProgress?: (loadedBytes: number) => void
) {
  const formData = new FormData()
  formData.append('uploadId', payload.uploadId)
  formData.append('chunkIndex', String(payload.chunkIndex))
  formData.append('chunkHash', payload.chunkHash)
  formData.append('chunk', payload.chunk)

  const response = await http.post('/documents/upload/chunks', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: (event) => onProgress?.(event.loaded),
  })
  return unwrapApiResponse<UploadStatusResult>(response.data, '上传分片失败')
}

export async function getUploadStatus(uploadId: string) {
  const response = await http.get(`/documents/upload/${uploadId}`)
  return unwrapApiResponse<UploadStatusResult>(response.data, '获取上传状态失败')
}

export async function completeDocumentUpload(uploadId: string) {
  const response = await http.post(`/documents/upload/${uploadId}/complete`)
  return unwrapApiResponse<number>(response.data, '完成上传失败')
}

export async function deleteDocument(documentId: number, groupId: number) {
  const response = await http.delete(`/documents/${documentId}`, {
    params: { groupId },
  })
  return unwrapApiResponse<void>(response.data, '删除文档失败')
}

export async function retryDocumentIngestion(documentId: number, groupId: number) {
  const response = await http.post(
    `/documents/${documentId}/retry-ingestion`,
    null,
    { params: { groupId } }
  )
  return unwrapApiResponse<void>(response.data, '重试入库失败')
}

export async function previewDocument(documentId: number, groupId: number) {
  const response = await http.get<DocumentPreview>(
    `/documents/${documentId}/preview`,
    { params: { groupId } }
  )
  return unwrapBareResponse<DocumentPreview>(response.data, '获取文档预览失败')
}
