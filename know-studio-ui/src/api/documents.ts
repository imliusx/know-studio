import http, { unwrapApiResponse } from './http'

export type DocumentStatus =
  | 'PENDING'
  | 'PROCESSING'
  | 'READY'
  | 'FAILED'

interface DocumentView {
  id: number
  knowledgeBaseId: number
  fileName: string
  contentType: string | null
  fileSize: number
  contentHash: string
  status: DocumentStatus
  chunkCount: number
  failureReason: string | null
  previewText: string | null
}

export interface DocumentListItem {
  documentId: number
  groupId: number
  fileName: string
  fileExt: string | null
  contentType: string | null
  fileSize: number
  status: DocumentStatus
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
  fileName?: string
  status?: DocumentStatus
}

export interface UploadInitResult {
  instantUpload: boolean
  documentId: number | null
  uploadSessionId: number | null
  uploadedChunks: number[]
}

export interface UploadStatusResult {
  uploadSessionId: number
  totalChunks: number
  uploadedChunks: number[]
  status: 'UPLOADING' | 'COMPLETED' | 'EXPIRED'
  documentId: number | null
}

export async function listDocuments(
  knowledgeBaseId: number,
  query: DocumentQuery = {}
) {
  const response = await http.get(`/knowledge-bases/${knowledgeBaseId}/documents`, {
    params: query,
  })
  const documents = unwrapApiResponse<DocumentView[]>(response.data, '获取文档失败')
  return documents.map(toListItem)
}

export async function initDocumentUpload(
  knowledgeBaseId: number,
  payload: {
    fileName: string
    fileSize: number
    contentType: string
    contentHash: string
    totalChunks: number
  }
) {
  const response = await http.post(
    `/knowledge-bases/${knowledgeBaseId}/documents/uploads`,
    payload
  )
  return unwrapApiResponse<UploadInitResult>(response.data, '初始化上传失败')
}

export async function uploadDocumentChunk(
  knowledgeBaseId: number,
  payload: {
    uploadSessionId: number
    chunkIndex: number
    chunkHash: string
    chunk: Blob
  },
  onProgress?: (loadedBytes: number) => void
) {
  const formData = new FormData()
  formData.append('file', payload.chunk)
  const response = await http.put(
    `/knowledge-bases/${knowledgeBaseId}/documents/uploads/${payload.uploadSessionId}/chunks/${payload.chunkIndex}`,
    formData,
    {
      headers: { 'X-Chunk-SHA256': payload.chunkHash },
      onUploadProgress: (event) => onProgress?.(event.loaded),
    }
  )
  return unwrapApiResponse<UploadStatusResult>(response.data, '上传分片失败')
}

export async function getUploadStatus(
  knowledgeBaseId: number,
  uploadSessionId: number
) {
  const response = await http.get(
    `/knowledge-bases/${knowledgeBaseId}/documents/uploads/${uploadSessionId}`
  )
  return unwrapApiResponse<UploadStatusResult>(response.data, '获取上传状态失败')
}

export async function completeDocumentUpload(
  knowledgeBaseId: number,
  uploadSessionId: number
) {
  const response = await http.post(
    `/knowledge-bases/${knowledgeBaseId}/documents/uploads/${uploadSessionId}/complete`
  )
  const result = unwrapApiResponse<{ documentId: number }>(
    response.data,
    '完成上传失败'
  )
  return result.documentId
}

export async function deleteDocument(documentId: number, knowledgeBaseId: number) {
  const response = await http.delete(
    `/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`
  )
  return unwrapApiResponse<void>(response.data, '删除文档失败')
}

export async function retryDocumentIngestion(
  documentId: number,
  knowledgeBaseId: number
) {
  const response = await http.post(
    `/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/retry-ingestion`
  )
  return unwrapApiResponse<void>(response.data, '重试入库失败')
}

export async function previewDocument(documentId: number, knowledgeBaseId: number) {
  const response = await http.get(
    `/knowledge-bases/${knowledgeBaseId}/documents/${documentId}`
  )
  const document = unwrapApiResponse<DocumentView>(response.data, '获取文档详情失败')
  return {
    documentId: document.id,
    fileName: document.fileName,
    previewText: document.previewText ?? '',
  } satisfies DocumentPreview
}

export async function downloadDocumentContent(
  documentId: number,
  knowledgeBaseId: number
) {
  const response = await http.get(
    `/knowledge-bases/${knowledgeBaseId}/documents/${documentId}/content`,
    { responseType: 'blob' }
  )
  return response.data as Blob
}

function toListItem(document: DocumentView): DocumentListItem {
  const extensionIndex = document.fileName.lastIndexOf('.')
  return {
    documentId: document.id,
    groupId: document.knowledgeBaseId,
    fileName: document.fileName,
    fileExt:
      extensionIndex >= 0 ? document.fileName.slice(extensionIndex + 1) : null,
    contentType: document.contentType,
    fileSize: document.fileSize,
    status: document.status,
    failureReason: document.failureReason,
    uploadedAt: new Date(0).toISOString(),
    uploaderUserId: 0,
    uploaderUserCode: '-',
    uploaderDisplayName: '-',
    previewText: document.previewText,
  }
}
