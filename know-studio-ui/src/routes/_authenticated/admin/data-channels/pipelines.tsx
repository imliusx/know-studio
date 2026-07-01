import { createFileRoute } from '@tanstack/react-router'
import { FolderKanban } from 'lucide-react'
import { AdminPlaceholderPage } from '@/features/admin/admin-placeholder-page'

export const Route = createFileRoute(
  '/_authenticated/admin/data-channels/pipelines'
)({
  component: PipelinesPage,
})

function PipelinesPage() {
  return (
    <AdminPlaceholderPage
      title='流水线管理'
      description='管理文档入库、解析、切片、索引构建等数据处理流水线。'
      icon={FolderKanban}
      items={['入库流水线', '解析策略', '索引构建']}
    />
  )
}
