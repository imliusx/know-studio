import { createFileRoute } from '@tanstack/react-router'
import { Route as RouteIcon } from 'lucide-react'
import { AdminPlaceholderPage } from '@/features/admin/admin-placeholder-page'

export const Route = createFileRoute('/_authenticated/admin/data-channels/traces')({
  component: TracesPage,
})

function TracesPage() {
  return (
    <AdminPlaceholderPage
      title='链路追踪'
      description='追踪检索、重排、生成和外部工具调用的完整执行链路。'
      icon={RouteIcon}
      items={['请求链路', '检索证据', '工具调用']}
    />
  )
}
