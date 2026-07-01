import { createFileRoute } from '@tanstack/react-router'
import { AssistantPage } from '@/features/ddrag/assistant'

export const Route = createFileRoute('/_authenticated/admin/assistant/')({
  component: AssistantPage,
})
