import { createFileRoute } from '@tanstack/react-router'
import { PipelineTasksPage } from '@/features/admin/admin-placeholder-routes'

export const Route = createFileRoute('/_authenticated/admin/data-channels/tasks')({
  component: PipelineTasksPage,
})
