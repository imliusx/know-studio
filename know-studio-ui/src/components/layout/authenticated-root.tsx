import { Outlet, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { getCurrentUser } from '@/api/auth'
import { listWorkspaces } from '@/api/workspaces'
import { isUnauthorizedError } from '@/api/http'
import { useAuthStore } from '@/stores/auth-store'
import { useWorkspaceStore } from '@/stores/workspace-store'
import { WorkspaceEmptyState } from '@/components/workspace/workspace-empty-state'

export function AuthenticatedRoot() {
  const navigate = useNavigate()
  const accessToken = useAuthStore((state) => state.auth.accessToken)
  const setUser = useAuthStore((state) => state.auth.setUser)
  const resetAuth = useAuthStore((state) => state.auth.reset)
  const setWorkspaces = useWorkspaceStore((state) => state.setWorkspaces)
  const currentUserQuery = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: getCurrentUser,
    enabled: Boolean(accessToken),
  })
  const workspacesQuery = useQuery({
    queryKey: ['workspaces'],
    queryFn: listWorkspaces,
    enabled: Boolean(accessToken) && currentUserQuery.isSuccess,
  })

  useEffect(() => {
    if (currentUserQuery.data) {
      setUser(currentUserQuery.data)
    }
  }, [currentUserQuery.data, setUser])

  useEffect(() => {
    if (workspacesQuery.data) {
      setWorkspaces(workspacesQuery.data)
    }
  }, [setWorkspaces, workspacesQuery.data])

  useEffect(() => {
    if (!currentUserQuery.error || !isUnauthorizedError(currentUserQuery.error)) {
      return
    }
    resetAuth()
    navigate({ to: '/sign-in', replace: true })
  }, [currentUserQuery.error, navigate, resetAuth])

  if (currentUserQuery.isPending || workspacesQuery.isPending) {
    return null
  }

  if (workspacesQuery.isSuccess && workspacesQuery.data.length === 0) {
    return <WorkspaceEmptyState />
  }

  return <Outlet />
}
