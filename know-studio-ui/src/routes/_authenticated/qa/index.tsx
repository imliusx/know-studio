import { createFileRoute, redirect } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/qa/')({
  beforeLoad: () => {
    throw redirect({ to: '/admin/qa' })
  },
})
