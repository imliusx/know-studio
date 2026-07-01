import { Outlet } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { useEffect } from 'react'
import { getCurrentUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth-store'

export function AuthenticatedRoot() {
  const accessToken = useAuthStore((state) => state.auth.accessToken)
  const setUser = useAuthStore((state) => state.auth.setUser)
  const currentUserQuery = useQuery({
    queryKey: ['auth', 'me'],
    queryFn: getCurrentUser,
    enabled: Boolean(accessToken),
  })

  useEffect(() => {
    if (currentUserQuery.data) {
      setUser(currentUserQuery.data)
    }
  }, [currentUserQuery.data, setUser])

  return <Outlet />
}
