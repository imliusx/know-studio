import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, RefreshCw, ShieldAlert, UserCheck, UserX } from 'lucide-react'
import { toast } from 'sonner'
import {
  getAdminUserDetail,
  listAdminUsers,
  updateUserStatus,
} from '@/api/admin-users'
import { extractApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth-store'
import { Header } from '@/components/layout/header'
import { HeaderActions } from '@/components/layout/header-actions'
import { Main } from '@/components/layout/main'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
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
import { Input } from '@/components/ui/input'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { formatDateTime } from '@/features/ddrag/shared'

export function Users() {
  const queryClient = useQueryClient()
  const currentUser = useAuthStore((state) => state.auth.user)
  const [keyword, setKeyword] = useState('')
  const [detailUserId, setDetailUserId] = useState<number | null>(null)
  const usersQuery = useQuery({
    queryKey: ['admin-users'],
    queryFn: listAdminUsers,
    enabled: currentUser?.systemRole === 'ADMIN',
  })
  const detailQuery = useQuery({
    queryKey: ['admin-users', detailUserId],
    queryFn: () => getAdminUserDetail(detailUserId!),
    enabled: currentUser?.systemRole === 'ADMIN' && detailUserId !== null,
  })
  const statusMutation = useMutation({
    mutationFn: ({ userId, status }: { userId: number; status: string }) =>
      updateUserStatus(userId, status),
    onSuccess: () => {
      toast.success('用户状态已更新')
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
    onError: (error) => toast.error(extractApiError(error, '更新失败')),
  })

  const filteredUsers = (usersQuery.data ?? []).filter((user) => {
    const text = `${user.username} ${user.email} ${user.displayName} ${user.userCode}`
    return text.toLowerCase().includes(keyword.trim().toLowerCase())
  })
  const detailUser = detailQuery.data

  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>
      <Main className='flex flex-col gap-6'>
        <div className='flex flex-wrap items-end justify-between gap-3'>
          <div className='min-w-0'>
            <h1 className='text-2xl font-bold tracking-tight'>用户管理</h1>
            <p className='text-sm text-muted-foreground'>
              管理系统账号、角色和账号状态
            </p>
          </div>
          <Button variant='outline' size='sm' onClick={() => usersQuery.refetch()}>
            <RefreshCw data-icon='inline-start' />
            刷新
          </Button>
        </div>
        {currentUser?.systemRole !== 'ADMIN' ? (
          <Alert>
            <ShieldAlert />
            <AlertTitle>需要管理员权限</AlertTitle>
            <AlertDescription>
              当前账号不是 ADMIN，后端会拒绝访问用户管理接口。
            </AlertDescription>
          </Alert>
        ) : null}

        <Card>
          <CardHeader className='gap-4 sm:flex-row sm:items-center sm:justify-between'>
            <div>
              <CardTitle>账号列表</CardTitle>
              <CardDescription>
                后端接口：GET /api/admin/users
              </CardDescription>
            </div>
            <Input
              className='w-full sm:w-72'
              placeholder='搜索用户名、邮箱或用户编码'
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>用户</TableHead>
                  <TableHead>角色</TableHead>
                  <TableHead>状态</TableHead>
                  <TableHead>强制改密</TableHead>
                  <TableHead>最后登录</TableHead>
                  <TableHead className='text-right'>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredUsers.map((user) => {
                  const isDisabled = user.status === 'DISABLED'
                  const nextStatus = isDisabled ? 'ACTIVE' : 'DISABLED'

                  return (
                    <TableRow key={user.userId}>
                      <TableCell>
                        <div className='font-medium'>{user.displayName}</div>
                        <div className='text-sm text-muted-foreground'>
                          {user.username} · {user.email} · {user.userCode}
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant='secondary'>{user.systemRole}</Badge>
                      </TableCell>
                      <TableCell>
                        <Badge variant={isDisabled ? 'destructive' : 'default'}>
                          {user.status}
                        </Badge>
                      </TableCell>
                      <TableCell>{user.mustChangePassword ? '是' : '否'}</TableCell>
                      <TableCell>{formatDateTime(user.lastLoginAt)}</TableCell>
                      <TableCell className='text-right'>
                        <div className='flex justify-end gap-2'>
                          <Button
                            variant='outline'
                            size='sm'
                            onClick={() => setDetailUserId(user.userId)}
                          >
                            <Eye data-icon='inline-start' />
                            详情
                          </Button>
                          <Button
                            variant='outline'
                            size='sm'
                            disabled={statusMutation.isPending}
                            onClick={() =>
                              statusMutation.mutate({
                                userId: user.userId,
                                status: nextStatus,
                              })
                            }
                          >
                            {isDisabled ? (
                              <UserCheck data-icon='inline-start' />
                            ) : (
                              <UserX data-icon='inline-start' />
                            )}
                            {isDisabled ? '启用' : '禁用'}
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })}
                {filteredUsers.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={6}
                      className='h-24 text-center text-muted-foreground'
                    >
                      暂无用户
                    </TableCell>
                  </TableRow>
                ) : null}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </Main>

      <Dialog
        open={detailUserId !== null}
        onOpenChange={(open) => {
          if (!open) setDetailUserId(null)
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{detailUser?.displayName ?? '用户详情'}</DialogTitle>
            <DialogDescription>
              查看单个账号的身份、状态与登录信息
            </DialogDescription>
          </DialogHeader>
          {detailQuery.isLoading ? (
            <p className='text-sm text-muted-foreground'>正在加载用户详情...</p>
          ) : detailQuery.isError ? (
            <p className='text-sm text-destructive'>
              {extractApiError(detailQuery.error, '加载用户详情失败')}
            </p>
          ) : detailUser ? (
            <div className='grid gap-3 text-sm'>
              {[
                ['用户 ID', detailUser.userId],
                ['账号编码', detailUser.userCode],
                ['用户名', detailUser.username],
                ['邮箱', detailUser.email],
                ['显示名称', detailUser.displayName],
                ['系统角色', detailUser.systemRole],
                ['账号状态', detailUser.status],
                ['密码状态', detailUser.mustChangePassword ? '待改密' : '正常'],
                ['最近登录', formatDateTime(detailUser.lastLoginAt)],
              ].map(([label, value]) => (
                <div
                  key={label}
                  className='grid gap-1 rounded-md border bg-muted/30 p-3 sm:grid-cols-[120px_1fr] sm:items-center'
                >
                  <span className='text-muted-foreground'>{label}</span>
                  <span className='font-medium break-all'>{value}</span>
                </div>
              ))}
            </div>
          ) : null}
        </DialogContent>
      </Dialog>
    </>
  )
}
