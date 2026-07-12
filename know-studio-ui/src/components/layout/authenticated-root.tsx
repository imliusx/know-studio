import { Outlet, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { getCurrentUser } from '@/api/auth'
import { listKnowledgeBases } from '@/api/knowledge-bases'
import { isUnauthorizedError } from '@/api/http'
import { useAuthStore } from '@/stores/auth-store'
import { useKnowledgeBaseStore } from '@/stores/knowledge-base-store'

export function AuthenticatedRoot() {
  const navigate = useNavigate()
  const accessToken = useAuthStore((state) => state.auth.accessToken)
  const setUser = useAuthStore((state) => state.auth.setUser)
  const resetAuth = useAuthStore((state) => state.auth.reset)
  const setKnowledgeBases = useKnowledgeBaseStore((state) => state.setKnowledgeBases)
  const currentUserQuery = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: getCurrentUser,
    enabled: Boolean(accessToken),
  })
  const knowledgeBasesQuery = useQuery({
    queryKey: ['knowledge-bases'],
    queryFn: listKnowledgeBases,
    enabled: Boolean(accessToken) && currentUserQuery.isSuccess,
  })

  useEffect(() => {
    if (currentUserQuery.data) {
      setUser(currentUserQuery.data)
    }
  }, [currentUserQuery.data, setUser])

  useEffect(() => {
    if (knowledgeBasesQuery.data) {
      setKnowledgeBases(knowledgeBasesQuery.data)
    }
  }, [setKnowledgeBases, knowledgeBasesQuery.data])

  useEffect(() => {
    if (!currentUserQuery.error || !isUnauthorizedError(currentUserQuery.error)) {
      return
    }
    resetAuth()
    navigate({ to: '/sign-in', replace: true })
  }, [currentUserQuery.error, navigate, resetAuth])

  if (currentUserQuery.isPending || knowledgeBasesQuery.isPending) {
    return null
  }

  return <Outlet />
}
