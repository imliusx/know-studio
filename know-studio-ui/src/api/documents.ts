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

export async function listDocuments(query: DocumentQuery = {}) {
  const response = await http.get<DocumentListItem[]>('/documents', {
    params: query,
  })
  return unwrapBareResponse<DocumentListItem[]>(response.data, '获取文档失败')
}

export async function uploadDocument(groupId: number, file: File) {
  const formData = new FormData()
  formData.append('groupId', String(groupId))
  formData.append('file', file)

  const response = await http.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120_000,
  })
  return unwrapApiResponse<number>(response.data, '上传文档失败')
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
