import { createFileRoute } from '@tanstack/react-router'
import { ClipboardList } from 'lucide-react'
import { AdminPlaceholderPage } from '@/features/admin/admin-placeholder-page'

export const Route = createFileRoute('/_authenticated/admin/intent/list')({
  component: IntentListPage,
})

function IntentListPage() {
  return (
    <AdminPlaceholderPage
      title='意图列表'
      description='管理意图样本、状态、优先级和归属知识范围。'
      icon={ClipboardList}
      items={['意图样本', '优先级配置', '状态流转']}
    />
  )
}
