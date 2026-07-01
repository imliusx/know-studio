import { createFileRoute, redirect } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/assistant/')({
  beforeLoad: () => {
    throw redirect({ to: '/admin/assistant' })
  },
})
