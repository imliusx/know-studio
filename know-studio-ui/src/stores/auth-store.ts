import { create } from 'zustand'
import { getCookie, setCookie, removeCookie } from '@/lib/cookies'
import { setAuthToken } from '@/api/http'
import { getCurrentUser, type CurrentIdentity } from '@/api/auth'
import { useKnowledgeBaseStore } from '@/stores/knowledge-base-store'

const ACCESS_TOKEN = 'ddrag_access_token'
const CURRENT_USER = 'ddrag_current_user'

interface AuthState {
  auth: {
    user: CurrentIdentity | null
    setUser: (user: CurrentIdentity | null) => void
    accessToken: string
    setAccessToken: (accessToken: string) => void
    refreshSession: () => Promise<string>
    resetAccessToken: () => void
    reset: () => void
  }
}

export const useAuthStore = create<AuthState>()((set) => {
  const cookieState = getCookie(ACCESS_TOKEN)
  const initToken = cookieState ? decodeURIComponent(cookieState) : ''
  let currentAccessToken = initToken
  const initUser = initToken ? readStoredUser() : null
  if (!initToken) writeStoredUser(null)
  setAuthToken(initToken || null)

  return {
    auth: {
      user: initUser,
      setUser: (user) =>
        set((state) => {
          writeStoredUser(user)
          return { ...state, auth: { ...state.auth, user } }
        }),
      accessToken: initToken,
      setAccessToken: (accessToken) =>
        set((state) => {
          currentAccessToken = accessToken
          setCookie(ACCESS_TOKEN, encodeURIComponent(accessToken))
          setAuthToken(accessToken)
          return { ...state, auth: { ...state.auth, accessToken } }
        }),
      refreshSession: async (): Promise<string> => {
        try {
          const currentUser = await getCurrentUser()
          const accessToken = currentAccessToken
          if (!accessToken) throw new Error('登录状态已失效')
          set((state) => {
            writeStoredUser(currentUser)
            return {
              ...state,
              auth: {
                ...state.auth,
                user: currentUser,
              },
            }
          })
          return accessToken
        } catch (error) {
          currentAccessToken = ''
          removeCookie(ACCESS_TOKEN)
          writeStoredUser(null)
          setAuthToken(null)
          set((state) => ({
            ...state,
            auth: { ...state.auth, user: null, accessToken: '' },
          }))
          useKnowledgeBaseStore.getState().reset()
          throw error
        }
      },
      resetAccessToken: () =>
        set((state) => {
          currentAccessToken = ''
          removeCookie(ACCESS_TOKEN)
          writeStoredUser(null)
          setAuthToken(null)
          useKnowledgeBaseStore.getState().reset()
          return {
            ...state,
            auth: { ...state.auth, user: null, accessToken: '' },
          }
        }),
      reset: () =>
        set((state) => {
          currentAccessToken = ''
          removeCookie(ACCESS_TOKEN)
          writeStoredUser(null)
          setAuthToken(null)
          useKnowledgeBaseStore.getState().reset()
          return {
            ...state,
            auth: { ...state.auth, user: null, accessToken: '' },
          }
        }),
    },
  }
})

function readStoredUser() {
  try {
    const value = window.localStorage.getItem(CURRENT_USER)
    return value ? (JSON.parse(value) as CurrentIdentity) : null
  } catch {
    return null
  }
}

function writeStoredUser(user: CurrentIdentity | null) {
  if (!user) {
    window.localStorage.removeItem(CURRENT_USER)
    return
  }

  window.localStorage.setItem(CURRENT_USER, JSON.stringify(user))
}
