import { create } from 'zustand'
import { getCookie, setCookie, removeCookie } from '@/lib/cookies'
import { setAuthToken } from '@/api/http'
import type { CurrentUserProfile } from '@/api/auth'

const ACCESS_TOKEN = 'ddrag_access_token'
const CURRENT_USER = 'ddrag_current_user'

interface AuthState {
  auth: {
    user: CurrentUserProfile | null
    setUser: (user: CurrentUserProfile | null) => void
    accessToken: string
    setAccessToken: (accessToken: string) => void
    resetAccessToken: () => void
    reset: () => void
  }
}

export const useAuthStore = create<AuthState>()((set) => {
  const cookieState = getCookie(ACCESS_TOKEN)
  const initToken = cookieState ? decodeURIComponent(cookieState) : ''
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
          setCookie(ACCESS_TOKEN, encodeURIComponent(accessToken))
          setAuthToken(accessToken)
          return { ...state, auth: { ...state.auth, accessToken } }
        }),
      resetAccessToken: () =>
        set((state) => {
          removeCookie(ACCESS_TOKEN)
          writeStoredUser(null)
          setAuthToken(null)
          return {
            ...state,
            auth: { ...state.auth, user: null, accessToken: '' },
          }
        }),
      reset: () =>
        set((state) => {
          removeCookie(ACCESS_TOKEN)
          writeStoredUser(null)
          setAuthToken(null)
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
    return value ? (JSON.parse(value) as CurrentUserProfile) : null
  } catch {
    return null
  }
}

function writeStoredUser(user: CurrentUserProfile | null) {
  if (!user) {
    window.localStorage.removeItem(CURRENT_USER)
    return
  }

  window.localStorage.setItem(CURRENT_USER, JSON.stringify(user))
}
