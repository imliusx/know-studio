import { useState, type FormEvent, type ReactNode } from 'react'
import { useMutation } from '@tanstack/react-query'
import { Loader2, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { createWorkspace, listWorkspaces } from '@/api/workspaces'
import { extractApiError } from '@/api/http'
import { useWorkspaceStore } from '@/stores/workspace-store'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'

type CreateWorkspaceDialogProps = {
  trigger?: ReactNode
}

export function CreateWorkspaceDialog({ trigger }: CreateWorkspaceDialogProps) {
  const [open, setOpen] = useState(false)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const setWorkspaces = useWorkspaceStore((state) => state.setWorkspaces)
  const setCurrentWorkspaceId = useWorkspaceStore(
    (state) => state.setCurrentWorkspaceId
  )
  const mutation = useMutation({
    mutationFn: async () => {
      const created = await createWorkspace({
        name: name.trim(),
        description: description.trim() || undefined,
      })
      const workspaces = await listWorkspaces()
      return { created, workspaces }
    },
    onSuccess: ({ created, workspaces }) => {
      setWorkspaces(workspaces)
      setCurrentWorkspaceId(created.workspaceId)
      setName('')
      setDescription('')
      setOpen(false)
      toast.success('工作空间已创建')
    },
    onError: (error) => toast.error(extractApiError(error, '创建工作空间失败')),
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!name.trim() || mutation.isPending) return
    mutation.mutate()
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {trigger ?? (
          <Button>
            <Plus />
            创建工作空间
          </Button>
        )}
      </DialogTrigger>
      <DialogContent>
        <form onSubmit={handleSubmit} className='space-y-5'>
          <DialogHeader>
            <DialogTitle>创建工作空间</DialogTitle>
            <DialogDescription>
              文档、会话和评测数据会按工作空间隔离。
            </DialogDescription>
          </DialogHeader>
          <div className='space-y-4'>
            <div className='space-y-2'>
              <label htmlFor='workspace-name' className='text-sm font-medium'>
                名称
              </label>
              <Input
                id='workspace-name'
                value={name}
                maxLength={120}
                autoFocus
                onChange={(event) => setName(event.target.value)}
                disabled={mutation.isPending}
              />
            </div>
            <div className='space-y-2'>
              <label
                htmlFor='workspace-description'
                className='text-sm font-medium'
              >
                描述
              </label>
              <Textarea
                id='workspace-description'
                value={description}
                maxLength={500}
                onChange={(event) => setDescription(event.target.value)}
                disabled={mutation.isPending}
              />
            </div>
          </div>
          <DialogFooter>
            <Button type='submit' disabled={!name.trim() || mutation.isPending}>
              {mutation.isPending ? <Loader2 className='animate-spin' /> : null}
              创建
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
