import { createFileRoute, redirect } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/groups/')({
  beforeLoad: () => {
    throw redirect({ to: '/admin/groups' })
  },
})
