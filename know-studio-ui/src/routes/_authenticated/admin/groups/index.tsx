import { createFileRoute } from '@tanstack/react-router'
import { GroupsPage } from '@/features/ddrag/groups'

export const Route = createFileRoute('/_authenticated/admin/groups/')({
  component: GroupsPage,
})
