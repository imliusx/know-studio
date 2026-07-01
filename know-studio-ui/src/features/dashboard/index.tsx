import { useQuery } from '@tanstack/react-query'
import {
  Bot,
  FileText,
  FolderKanban,
  RefreshCw,
  ShieldCheck,
  Users,
} from 'lucide-react'
import { useAuthStore } from '@/stores/auth-store'
import { listAdminUsers } from '@/api/admin-users'
import { listAssistantSessions } from '@/api/assistant'
import { listDocuments } from '@/api/documents'
import { getMyGroups } from '@/api/groups'
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatDateTime, formatFileSize } from '@/features/ddrag/shared'

export function Dashboard() {
  const currentUser = useAuthStore((state) => state.auth.user)
  const groupsQuery = useQuery({
    queryKey: ['groups', 'my'],
    queryFn: getMyGroups,
    enabled: Boolean(currentUser) && currentUser?.systemRole !== 'ADMIN',
  })
  const documentsQuery = useQuery({
    queryKey: ['documents', 'dashboard'],
    queryFn: () => listDocuments(),
    enabled: Boolean(currentUser) && currentUser?.systemRole !== 'ADMIN',
  })
  const sessionsQuery = useQuery({
    queryKey: ['assistant', 'sessions'],
    queryFn: listAssistantSessions,
    enabled: Boolean(currentUser) && currentUser?.systemRole !== 'ADMIN',
  })
  const usersQuery = useQuery({
    queryKey: ['admin-users'],
    queryFn: listAdminUsers,
    enabled: currentUser?.systemRole === 'ADMIN',
  })

  const ownedGroups = groupsQuery.data?.ownedGroups.length ?? 0
  const joinedGroups = groupsQuery.data?.joinedGroups.length ?? 0
  const pendingInvitations = groupsQuery.data?.pendingInvitations.length ?? 0
  const documents = documentsQuery.data ?? []
  const readyDocuments = documents.filter((item) => item.status === 'READY')
  const failedDocuments = documents.filter((item) => item.status === 'FAILED')

  const refreshAll = () => {
    groupsQuery.refetch()
    documentsQuery.refetch()
    sessionsQuery.refetch()
    usersQuery.refetch()
  }

  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>
      <Main className='flex flex-col gap-6'>
        <div className='flex flex-wrap items-end justify-between gap-3'>
          <div className='min-w-0'>
            <h1 className='text-2xl font-bold tracking-tight'>
              Know Studio Admin 控制台
            </h1>
            <p className='text-sm text-muted-foreground'>
              文档知识库、问答和助手能力的运行概览
            </p>
          </div>
          <Button variant='outline' size='sm' onClick={refreshAll}>
            <RefreshCw data-icon='inline-start' />
            刷新
          </Button>
        </div>
        <div className='grid gap-4 md:grid-cols-2 xl:grid-cols-4'>
          <MetricCard
            title='可见小组'
            value={ownedGroups + joinedGroups}
            description={`拥有 ${ownedGroups} 个，加入 ${joinedGroups} 个`}
            icon={FolderKanban}
          />
          <MetricCard
            title='文档总数'
            value={documents.length}
            description={`READY ${readyDocuments.length} 个，FAILED ${failedDocuments.length} 个`}
            icon={FileText}
          />
          <MetricCard
            title='助手会话'
            value={sessionsQuery.data?.length ?? 0}
            description='当前账号的助手历史会话'
            icon={Bot}
          />
          <MetricCard
            title='后台用户'
            value={
              currentUser?.systemRole === 'ADMIN'
                ? usersQuery.data?.length ?? 0
                : '无权限'
            }
            description={
              currentUser?.systemRole === 'ADMIN'
                ? '系统账号数量'
                : '仅管理员可查看'
            }
            icon={Users}
          />
        </div>

        <div className='grid gap-4 xl:grid-cols-[1fr_380px]'>
          <Card>
            <CardHeader>
              <CardTitle>最近文档</CardTitle>
              <CardDescription>
                文档上传后会异步入库，失败文档可在文档页重试
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>文件名</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>大小</TableHead>
                    <TableHead>上传人</TableHead>
                    <TableHead>上传时间</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {documents.slice(0, 8).map((document) => (
                    <TableRow key={document.documentId}>
                      <TableCell className='max-w-[280px] truncate font-medium'>
                        {document.fileName}
                      </TableCell>
                      <TableCell>
                        <Badge variant='secondary'>{document.status}</Badge>
                      </TableCell>
                      <TableCell>{formatFileSize(document.fileSize)}</TableCell>
                      <TableCell>{document.uploaderDisplayName}</TableCell>
                      <TableCell>{formatDateTime(document.uploadedAt)}</TableCell>
                    </TableRow>
                  ))}
                  {documents.length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={5}
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

          <Card>
            <CardHeader>
              <CardTitle>账号与协作</CardTitle>
              <CardDescription>当前登录人与待处理事项</CardDescription>
            </CardHeader>
            <CardContent className='flex flex-col gap-4 text-sm'>
              <div className='flex items-center gap-3 rounded-lg border p-3'>
                <ShieldCheck className='text-primary' />
                <div className='min-w-0'>
                  <div className='truncate font-medium'>
                    {currentUser?.displayName ?? '未加载'}
                  </div>
                  <div className='text-muted-foreground'>
                    {currentUser?.systemRole ?? '-'} ·{' '}
                    {currentUser?.userCode ?? '-'}
                  </div>
                </div>
              </div>
              <div className='grid grid-cols-2 gap-3'>
                <div className='rounded-lg border p-3'>
                  <div className='text-2xl font-semibold'>
                    {pendingInvitations}
                  </div>
                  <div className='text-muted-foreground'>待处理邀请</div>
                </div>
                <div className='rounded-lg border p-3'>
                  <div className='text-2xl font-semibold'>
                    {failedDocuments.length}
                  </div>
                  <div className='text-muted-foreground'>失败文档</div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      </Main>
    </>
  )
}

function MetricCard({
  title,
  value,
  description,
  icon: Icon,
}: {
  title: string
  value: number | string
  description: string
  icon: React.ElementType
}) {
  return (
    <Card>
      <CardHeader className='flex flex-row items-center justify-between gap-3'>
        <div className='min-w-0'>
          <CardDescription>{title}</CardDescription>
          <CardTitle className='truncate text-2xl'>{value}</CardTitle>
        </div>
        <Icon className='text-muted-foreground' />
      </CardHeader>
      <CardContent>
        <p className='text-sm text-muted-foreground'>{description}</p>
      </CardContent>
    </Card>
  )
}
