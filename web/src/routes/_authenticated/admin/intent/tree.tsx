import { createFileRoute } from '@tanstack/react-router'
import { IntentTreePage } from '@/features/admin/admin-placeholder-routes'

export const Route = createFileRoute('/_authenticated/admin/intent/tree')({
  component: IntentTreePage,
})
