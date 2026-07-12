import { createFileRoute } from '@tanstack/react-router'
import { TeamKnowledgeAccessPage } from '@/features/admin/team-knowledge-access-page'

export const Route = createFileRoute('/_authenticated/admin/groups/')({
  component: TeamKnowledgeAccessPage,
})
