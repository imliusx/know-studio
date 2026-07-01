import { createFileRoute, redirect } from '@tanstack/react-router'
import { AuthenticatedRoot } from '@/components/layout/authenticated-root'
import { useAuthStore } from '@/stores/auth-store'

export const Route = createFileRoute('/_authenticated')({
  beforeLoad: ({ location }) => {
    if (import.meta.env.DEV) return

    const { accessToken } = useAuthStore.getState().auth

    if (!accessToken) {
      throw redirect({
        to: '/sign-in',
        search: { redirect: location.href },
      })
    }
  },
  component: AuthenticatedRoot,
})
