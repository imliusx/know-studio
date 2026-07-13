import {
  completeDocumentUpload,
  getUploadStatus,
  initDocumentUpload,
  uploadDocumentChunk,
} from '@/api/documents'
import type { EntityId } from '@/api/id'

const DEFAULT_CHUNK_SIZE = 5 * 1024 * 1024
const DEFAULT_CONCURRENCY = 3

export type UploadStage = 'hashing' | 'checking' | 'uploading' | 'completing'
export type UploadMode = 'resumable' | 'basic'

export interface UploadProgressPayload {
  percent: number
  stage: UploadStage
  mode: UploadMode
}

export async function uploadDocumentWithResume(
  groupId: EntityId,
  file: File,
  onProgress: (payload: UploadProgressPayload) => void
) {
  onProgress({ percent: 0, stage: 'hashing', mode: 'resumable' })
  const fileHash = await computeFileHash(file)
  const chunkSize = resolveChunkSize(file.size)
  const chunkCount = Math.ceil(file.size / chunkSize)

  onProgress({ percent: 0, stage: 'checking', mode: 'resumable' })
  const initResponse = await initDocumentUpload(groupId, {
    fileName: file.name,
    fileSize: file.size,
    contentType: file.type || 'application/octet-stream',
    contentHash: fileHash,
    totalChunks: chunkCount,
  })

  if (initResponse.instantUpload && initResponse.documentId) {
    onProgress({ percent: 100, stage: 'completing', mode: 'resumable' })
    return initResponse.documentId
  }

  if (!initResponse.uploadSessionId) {
    throw new Error('上传会话创建失败')
  }

  const status = await getUploadStatus(groupId, initResponse.uploadSessionId)
  const uploadedChunks = new Set([
    ...(initResponse.uploadedChunks ?? []),
    ...(status.uploadedChunks ?? []),
  ])
  const chunkIndexes = Array.from({ length: chunkCount }, (_, index) => index)
  const missingChunkIndexes = chunkIndexes.filter(
    (index) => !uploadedChunks.has(index)
  )
  let uploadedBytes = calculateUploadedBytes(file.size, chunkSize, uploadedChunks)

  const uploadChunkTask = async (chunkIndex: number) => {
    const start = chunkIndex * chunkSize
    const end = Math.min(file.size, start + chunkSize)
    const chunk = file.slice(start, end)
    const chunkHash = await computeChunkHash(chunk)

    await uploadDocumentChunk(
      groupId,
      {
        uploadSessionId: initResponse.uploadSessionId!,
        chunkIndex,
        chunkHash,
        chunk,
      },
      (loadedBytes) => {
        const completedBytes = calculateUploadedBytes(
          file.size,
          chunkSize,
          uploadedChunks
        )
        const percent = Math.min(
          99,
          Math.floor(((completedBytes + loadedBytes) / file.size) * 100)
        )
        onProgress({ percent, stage: 'uploading', mode: 'resumable' })
      }
    )

    uploadedChunks.add(chunkIndex)
    uploadedBytes = calculateUploadedBytes(file.size, chunkSize, uploadedChunks)
    onProgress({
      percent: Math.min(99, Math.floor((uploadedBytes / file.size) * 100)),
      stage: 'uploading',
      mode: 'resumable',
    })
  }

  await runWithConcurrency(
    missingChunkIndexes,
    DEFAULT_CONCURRENCY,
    uploadChunkTask
  )

  onProgress({ percent: 99, stage: 'completing', mode: 'resumable' })
  const documentId = await completeDocumentUpload(
    groupId,
    initResponse.uploadSessionId
  )
  onProgress({ percent: 100, stage: 'completing', mode: 'resumable' })
  return documentId
}

function resolveChunkSize(fileSize: number) {
  return fileSize <= DEFAULT_CHUNK_SIZE ? fileSize : DEFAULT_CHUNK_SIZE
}

function calculateUploadedBytes(
  fileSize: number,
  chunkSize: number,
  uploadedChunks: Set<number>
) {
  let total = 0
  for (const chunkIndex of uploadedChunks) {
    const start = chunkIndex * chunkSize
    const end = Math.min(fileSize, start + chunkSize)
    total += end - start
  }
  return total
}

async function runWithConcurrency(
  chunkIndexes: number[],
  concurrency: number,
  handler: (chunkIndex: number) => Promise<void>
) {
  const queue = [...chunkIndexes]
  const workers = Array.from(
    { length: Math.min(concurrency, queue.length || 1) },
    async () => {
      while (queue.length > 0) {
        const chunkIndex = queue.shift()
        if (typeof chunkIndex === 'number') {
          await handler(chunkIndex)
        }
      }
    }
  )
  await Promise.all(workers)
}

async function computeFileHash(file: File) {
  const buffer = await file.arrayBuffer()
  return computeHashHex(buffer)
}

async function computeChunkHash(chunk: Blob) {
  const buffer = await chunk.arrayBuffer()
  return computeHashHex(buffer)
}

async function computeHashHex(buffer: ArrayBuffer) {
  const hashBuffer = await crypto.subtle.digest('SHA-256', buffer)
  return Array.from(new Uint8Array(hashBuffer))
    .map((value) => value.toString(16).padStart(2, '0'))
    .join('')
}
