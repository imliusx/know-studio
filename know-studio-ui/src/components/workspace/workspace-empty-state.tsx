import { DraftingCompass } from 'lucide-react'
import { CreateWorkspaceDialog } from './create-workspace-dialog'

export function WorkspaceEmptyState() {
  return (
    <main className='flex min-h-svh items-center justify-center px-6 py-12'>
      <div className='flex max-w-md flex-col items-center text-center'>
        <div className='mb-5 flex size-12 items-center justify-center rounded-md border bg-muted/40'>
          <DraftingCompass className='size-6 text-primary' />
        </div>
        <h1 className='text-xl font-semibold'>创建第一个工作空间</h1>
        <p className='mt-2 mb-6 text-sm leading-6 text-muted-foreground'>
          工作空间用于隔离知识库、会话和评测数据。创建后即可开始上传文档。
        </p>
        <CreateWorkspaceDialog />
      </div>
    </main>
  )
}
