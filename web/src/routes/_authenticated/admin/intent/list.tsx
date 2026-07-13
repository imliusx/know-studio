import { createFileRoute } from '@tanstack/react-router'
import { IntentListPage } from '@/features/admin/admin-placeholder-routes'

export const Route = createFileRoute('/_authenticated/admin/intent/list')({
  component: IntentListPage,
})
