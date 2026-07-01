import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { RefreshCw, ShieldAlert, UserCheck, UserX } from 'lucide-react'
import { toast } from 'sonner'
import { listAdminUsers, updateUserStatus } from '@/api/admin-users'
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
  const usersQuery = useQuery({
    queryKey: ['admin-users'],
    queryFn: listAdminUsers,
    enabled: currentUser?.systemRole === 'ADMIN',
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
    </>
  )
}
