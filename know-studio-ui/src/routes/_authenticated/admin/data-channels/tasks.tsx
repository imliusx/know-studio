import { createFileRoute } from '@tanstack/react-router'
import { ClipboardCheck } from 'lucide-react'
import { AdminPlaceholderPage } from '@/features/admin/admin-placeholder-page'

export const Route = createFileRoute('/_authenticated/admin/data-channels/tasks')({
  component: PipelineTasksPage,
})

function PipelineTasksPage() {
  return (
    <AdminPlaceholderPage
      title='流水线任务'
      description='查看数据处理任务的执行状态、耗时和失败原因。'
      icon={ClipboardCheck}
      items={['任务队列', '执行日志', '失败重试']}
    />
  )
}
