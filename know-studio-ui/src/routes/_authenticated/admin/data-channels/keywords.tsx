import { createFileRoute } from '@tanstack/react-router'
import { KeywordsPage } from '@/features/admin/admin-placeholder-routes'

export const Route = createFileRoute(
  '/_authenticated/admin/data-channels/keywords'
)({
  component: KeywordsPage,
})
