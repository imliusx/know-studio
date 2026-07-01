import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, RefreshCw, RotateCcw, Trash2, Upload } from 'lucide-react'
import { toast } from 'sonner'
import {
  deleteDocument,
  listDocuments,
  previewDocument,
  retryDocumentIngestion,
  uploadDocument,
  type DocumentPreview,
} from '@/api/documents'
import { getMyGroups } from '@/api/groups'
import { extractApiError } from '@/api/http'
import { Header } from '@/components/layout/header'
import { HeaderActions } from '@/components/layout/header-actions'
import { Main } from '@/components/layout/main'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import { Input } from '@/components/ui/input'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatDateTime, formatFileSize, mergeGroups } from './shared'

export function DocumentsPage() {
  const queryClient = useQueryClient()
  const [selectedGroupId, setSelectedGroupId] = useState<string>('')
  const [fileName, setFileName] = useState('')
  const [status, setStatus] = useState('ALL')
  const [uploadFile, setUploadFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<DocumentPreview | null>(null)

  const groupsQuery = useQuery({
    queryKey: ['groups', 'my'],
    queryFn: getMyGroups,
  })
  const groups = useMemo(() => mergeGroups(groupsQuery.data), [groupsQuery.data])
  const groupId = Number(selectedGroupId)

  useEffect(() => {
    if (!selectedGroupId && groups[0]) {
      const timeout = window.setTimeout(() => {
        setSelectedGroupId(String(groups[0].groupId))
      })
      return () => window.clearTimeout(timeout)
    }
    return
  }, [groups, selectedGroupId])

  const documentsQuery = useQuery({
    queryKey: ['documents', { groupId, fileName, status }],
    queryFn: () =>
      listDocuments({
        groupId: Number.isFinite(groupId) && groupId > 0 ? groupId : undefined,
        fileName: fileName.trim() || undefined,
        status: status === 'ALL' ? undefined : status,
      }),
    enabled: Number.isFinite(groupId) && groupId > 0,
  })

  const uploadMutation = useMutation({
    mutationFn: ({ targetGroupId, file }: { targetGroupId: number; file: File }) =>
      uploadDocument(targetGroupId, file),
    onSuccess: () => {
      toast.success('文档已上传，后台将异步入库')
      setUploadFile(null)
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
    onError: (error) => toast.error(extractApiError(error, '上传失败')),
  })
  const actionMutation = useMutation({
    mutationFn: async (fn: () => Promise<void>) => fn(),
    onSuccess: () => {
      toast.success('操作成功')
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
    onError: (error) => toast.error(extractApiError(error, '操作失败')),
  })
  const previewMutation = useMutation({
    mutationFn: ({
      documentId,
      targetGroupId,
    }: {
      documentId: number
      targetGroupId: number
    }) => previewDocument(documentId, targetGroupId),
    onSuccess: setPreview,
    onError: (error) => toast.error(extractApiError(error, '获取预览失败')),
  })

  function handleUpload(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!uploadFile || !Number.isFinite(groupId) || groupId <= 0) return
    uploadMutation.mutate({ targetGroupId: groupId, file: uploadFile })
  }

  const documents = documentsQuery.data ?? []

  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>
      <Main className='flex flex-col gap-6'>
        <div className='flex flex-wrap items-end justify-between gap-3'>
          <div className='min-w-0'>
            <h1 className='text-2xl font-bold tracking-tight'>文档知识库</h1>
            <p className='text-sm text-muted-foreground'>
              上传文档、查看入库状态、预览可用于检索的内容
            </p>
          </div>
          <Button
            variant='outline'
            size='sm'
            onClick={() => documentsQuery.refetch()}
          >
            <RefreshCw data-icon='inline-start' />
            刷新
          </Button>
        </div>
        <Card>
          <CardHeader>
            <CardTitle>上传文档</CardTitle>
            <CardDescription>
              当前实现使用后端兼容直传接口 POST /api/documents/upload
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleUpload}>
              <FieldGroup className='grid gap-4 lg:grid-cols-[280px_1fr_auto] lg:items-end'>
                <Field>
                  <FieldLabel>目标小组</FieldLabel>
                  <Select
                    value={selectedGroupId}
                    onValueChange={setSelectedGroupId}
                  >
                    <SelectTrigger className='w-full'>
                      <SelectValue placeholder='选择小组' />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectGroup>
                        {groups.map((group) => (
                          <SelectItem
                            key={group.groupId}
                            value={String(group.groupId)}
                          >
                            {group.groupName}
                          </SelectItem>
                        ))}
                      </SelectGroup>
                    </SelectContent>
                  </Select>
                </Field>
                <Field>
                  <FieldLabel htmlFor='documentFile'>文件</FieldLabel>
                  <Input
                    id='documentFile'
                    type='file'
                    onChange={(event) =>
                      setUploadFile(event.target.files?.[0] ?? null)
                    }
                  />
                </Field>
                <Button type='submit' disabled={!uploadFile || uploadMutation.isPending}>
                  <Upload data-icon='inline-start' />
                  上传
                </Button>
              </FieldGroup>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className='gap-4 xl:flex-row xl:items-center xl:justify-between'>
            <div>
              <CardTitle>文档列表</CardTitle>
              <CardDescription>
                后端接口：GET /api/documents
              </CardDescription>
            </div>
            <div className='grid gap-3 sm:grid-cols-[280px_220px_180px]'>
              <Select value={selectedGroupId} onValueChange={setSelectedGroupId}>
                <SelectTrigger className='w-full'>
                  <SelectValue placeholder='选择小组' />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    {groups.map((group) => (
                      <SelectItem
                        key={group.groupId}
                        value={String(group.groupId)}
                      >
                        {group.groupName}
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
              <Input
                placeholder='按文件名搜索'
                value={fileName}
                onChange={(event) => setFileName(event.target.value)}
              />
              <Select value={status} onValueChange={setStatus}>
                <SelectTrigger className='w-full'>
                  <SelectValue placeholder='状态' />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    <SelectItem value='ALL'>全部状态</SelectItem>
                    <SelectItem value='READY'>READY</SelectItem>
                    <SelectItem value='PROCESSING'>PROCESSING</SelectItem>
                    <SelectItem value='FAILED'>FAILED</SelectItem>
                  </SelectGroup>
                </SelectContent>
              </Select>
            </div>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>文件</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>大小</TableHead>
                  <TableHead>上传人</TableHead>
                  <TableHead>上传时间</TableHead>
                  <TableHead className='text-right'>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {documents.map((document) => (
                  <TableRow key={document.documentId}>
                    <TableCell>
                      <div className='max-w-[320px] truncate font-medium'>
                        {document.fileName}
                      </div>
                      {document.failureReason ? (
                        <div className='max-w-[320px] truncate text-sm text-muted-foreground'>
                          {document.failureReason}
                        </div>
                      ) : null}
                    </TableCell>
                    <TableCell>
                      <Badge
                        variant={
                          document.status === 'FAILED'
                            ? 'destructive'
                            : 'secondary'
                        }
                      >
                        {document.status}
                      </Badge>
                    </TableCell>
                    <TableCell>{formatFileSize(document.fileSize)}</TableCell>
                    <TableCell>{document.uploaderDisplayName}</TableCell>
                    <TableCell>{formatDateTime(document.uploadedAt)}</TableCell>
                    <TableCell className='text-right'>
                      <div className='flex justify-end gap-2'>
                        <Button
                          variant='outline'
                          size='sm'
                          disabled={previewMutation.isPending}
                          onClick={() =>
                            previewMutation.mutate({
                              documentId: document.documentId,
                              targetGroupId: document.groupId,
                            })
                          }
                        >
                          <Eye data-icon='inline-start' />
                          预览
                        </Button>
                        {document.status === 'FAILED' ? (
                          <Button
                            variant='outline'
                            size='sm'
                            disabled={actionMutation.isPending}
                            onClick={() =>
                              actionMutation.mutate(() =>
                                retryDocumentIngestion(
                                  document.documentId,
                                  document.groupId
                                )
                              )
                            }
                          >
                            <RotateCcw data-icon='inline-start' />
                            重试
                          </Button>
                        ) : null}
                        <Button
                          variant='outline'
                          size='sm'
                          disabled={actionMutation.isPending}
                          onClick={() =>
                            actionMutation.mutate(() =>
                              deleteDocument(
                                document.documentId,
                                document.groupId
                              )
                            )
                          }
                        >
                          <Trash2 data-icon='inline-start' />
                          删除
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
                {documents.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={6}
                      className='h-24 text-center text-muted-foreground'
                    >
                      暂无文档
                    </TableCell>
                  </TableRow>
                ) : null}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </Main>

      <Dialog open={Boolean(preview)} onOpenChange={() => setPreview(null)}>
        <DialogContent className='sm:max-w-3xl'>
          <DialogHeader>
            <DialogTitle>{preview?.fileName ?? '文档预览'}</DialogTitle>
            <DialogDescription>
              后端返回的 previewText，可用于确认入库文本质量。
            </DialogDescription>
          </DialogHeader>
          <div className='max-h-[60svh] overflow-auto rounded-lg border bg-muted/30 p-4 whitespace-pre-wrap'>
            {preview?.previewText || '暂无预览内容'}
          </div>
        </DialogContent>
      </Dialog>
    </>
  )
}
