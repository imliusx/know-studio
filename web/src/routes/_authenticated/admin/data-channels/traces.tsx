import { createFileRoute } from '@tanstack/react-router'
import { TracesPage } from '@/features/admin/admin-placeholder-routes'

export const Route = createFileRoute('/_authenticated/admin/data-channels/traces')({
  component: TracesPage,
})
