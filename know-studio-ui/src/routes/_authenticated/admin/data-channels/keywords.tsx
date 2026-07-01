import { createFileRoute } from '@tanstack/react-router'
import { KeyRound } from 'lucide-react'
import { AdminPlaceholderPage } from '@/features/admin/admin-placeholder-page'

export const Route = createFileRoute(
  '/_authenticated/admin/data-channels/keywords'
)({
  component: KeywordsPage,
})

function KeywordsPage() {
  return (
    <AdminPlaceholderPage
      title='关键词映射'
      description='维护业务词、同义词和知识片段之间的映射关系。'
      icon={KeyRound}
      items={['业务词表', '同义词扩展', '知识片段映射']}
    />
  )
}
