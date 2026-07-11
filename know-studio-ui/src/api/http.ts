import axios from 'axios'

export interface ApiResponse<T> {
  success: boolean
  code: string
  data: T
  message: string | null
}

interface ApiErrorPayload {
  success?: boolean
  code?: string
  message?: string | null
  title?: string
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api',
  timeout: 30_000,
  withCredentials: true,
})

export class HttpStatusError extends Error {
  status: number
  payload?: unknown

  constructor(status: number, message: string, payload?: unknown) {
    super(message)
    this.name = 'HttpStatusError'
    this.status = status
    this.payload = payload
  }
}

export function setAuthToken(accessToken: string | null) {
  if (!accessToken) {
    delete http.defaults.headers.common.Authorization
    return
  }

  http.defaults.headers.common.Authorization = `Bearer ${accessToken}`
}

export function unwrapApiResponse<T>(
  payload: ApiResponse<T>,
  fallbackMessage: string
): T {
  if (!payload.success) {
    throw new Error(payload.message ?? fallbackMessage)
  }

  return payload.data
}

export function unwrapBareResponse<T>(payload: T | ApiResponse<T>, fallbackMessage: string): T {
  if (
    payload &&
    typeof payload === 'object' &&
    'success' in payload &&
    'data' in payload
  ) {
    return unwrapApiResponse(payload as ApiResponse<T>, fallbackMessage)
  }

  return payload as T
}

export function extractApiError(error: unknown, fallbackMessage = '请求失败') {
  if (error instanceof HttpStatusError && error.message.trim()) {
    return error.message
  }

  if (axios.isAxiosError<ApiErrorPayload>(error)) {
    const responseMessage = error.response?.data?.message
    if (typeof responseMessage === 'string' && responseMessage.trim()) {
      return responseMessage
    }

    const title = error.response?.data?.title
    if (typeof title === 'string' && title.trim()) {
      return title
    }

    if (typeof error.message === 'string' && error.message.trim()) {
      return error.message
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return fallbackMessage
}

export function isUnauthorizedError(error: unknown) {
  if (error instanceof HttpStatusError) {
    return error.status === 401
  }

  return axios.isAxiosError(error) && error.response?.status === 401
}

export default http
