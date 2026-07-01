import { createFileRoute } from '@tanstack/react-router'
import { GitBranch } from 'lucide-react'
import { AdminPlaceholderPage } from '@/features/admin/admin-placeholder-page'

export const Route = createFileRoute('/_authenticated/admin/intent/tree')({
  component: IntentTreePage,
})

function IntentTreePage() {
  return (
    <AdminPlaceholderPage
      title='意图树配置'
      description='维护意图分类、层级关系和路由规则。'
      icon={GitBranch}
      items={['意图分类树', '命中规则', '兜底策略']}
    />
  )
}
