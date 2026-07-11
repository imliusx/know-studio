import { createFileRoute } from '@tanstack/react-router'
import { PipelinesPage } from '@/features/admin/admin-placeholder-routes'

export const Route = createFileRoute(
  '/_authenticated/admin/data-channels/pipelines'
)({
  component: PipelinesPage,
})
