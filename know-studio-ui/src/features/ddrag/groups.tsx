import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Check, RefreshCw, Send, UserMinus, X } from 'lucide-react'
import { toast } from 'sonner'
import {
  acceptInvitation,
  approveJoinRequest,
  createGroup,
  createInvitation,
  getMyGroups,
  leaveGroup,
  listGroupMembers,
  listMyJoinRequests,
  listOwnerJoinRequests,
  rejectInvitation,
  rejectJoinRequest,
  removeGroupMember,
  submitJoinRequest,
} from '@/api/groups'
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
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from '@/components/ui/field'
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
import { formatDateTime, mergeGroups } from './shared'

export function GroupsPage() {
  const queryClient = useQueryClient()
  const [selectedGroupId, setSelectedGroupId] = useState<string>('')
  const [createValues, setCreateValues] = useState({
    name: '',
    description: '',
  })
  const [groupCode, setGroupCode] = useState('')
  const [inviteeUserId, setInviteeUserId] = useState('')

  const groupsQuery = useQuery({
    queryKey: ['groups', 'my'],
    queryFn: getMyGroups,
  })
  const groups = useMemo(() => mergeGroups(groupsQuery.data), [groupsQuery.data])
  const selectedGroup = groups.find(
    (group) => String(group.groupId) === selectedGroupId
  )
  const selectedGroupNumericId = selectedGroup
    ? Number(selectedGroup.groupId)
    : undefined

  useEffect(() => {
    if (!selectedGroupId && groups[0]) {
      const timeout = window.setTimeout(() => {
        setSelectedGroupId(String(groups[0].groupId))
      })
      return () => window.clearTimeout(timeout)
    }
    return
  }, [groups, selectedGroupId])

  const membersQuery = useQuery({
    queryKey: ['groups', selectedGroupNumericId, 'members'],
    queryFn: () => listGroupMembers(selectedGroupNumericId ?? 0),
    enabled: Boolean(selectedGroupNumericId),
  })
  const myRequestsQuery = useQuery({
    queryKey: ['groups', 'join-requests', 'my'],
    queryFn: listMyJoinRequests,
  })
  const ownerRequestsQuery = useQuery({
    queryKey: ['groups', selectedGroupNumericId, 'join-requests'],
    queryFn: () => listOwnerJoinRequests(selectedGroupNumericId ?? 0),
    enabled: selectedGroup?.relation === 'OWNER',
  })

  const invalidateGroups = () => {
    queryClient.invalidateQueries({ queryKey: ['groups'] })
  }
  const notifyError = (error: unknown) =>
    toast.error(extractApiError(error, '操作失败'))

  const createMutation = useMutation({
    mutationFn: createGroup,
    onSuccess: () => {
      toast.success('小组已创建')
      setCreateValues({ name: '', description: '' })
      invalidateGroups()
    },
    onError: notifyError,
  })
  const joinMutation = useMutation({
    mutationFn: submitJoinRequest,
    onSuccess: () => {
      toast.success('加入申请已提交')
      setGroupCode('')
      invalidateGroups()
    },
    onError: notifyError,
  })
  const inviteMutation = useMutation({
    mutationFn: ({
      groupId,
      userId,
    }: {
      groupId: number
      userId: number
    }) => createInvitation(groupId, userId),
    onSuccess: () => {
      toast.success('邀请已创建')
      setInviteeUserId('')
    },
    onError: notifyError,
  })
  const simpleMutation = useMutation({
    mutationFn: async (fn: () => Promise<void>) => fn(),
    onSuccess: () => {
      toast.success('操作成功')
      invalidateGroups()
    },
    onError: notifyError,
  })

  function handleCreateGroup(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!createValues.name.trim()) return
    createMutation.mutate({
      name: createValues.name.trim(),
      description: createValues.description.trim() || undefined,
    })
  }

  function handleJoinRequest(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!groupCode.trim()) return
    joinMutation.mutate(groupCode.trim())
  }

  function handleInvitation(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const userId = Number(inviteeUserId)
    if (!selectedGroupNumericId || !Number.isFinite(userId) || userId <= 0) {
      return
    }
    inviteMutation.mutate({ groupId: selectedGroupNumericId, userId })
  }

  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>
      <Main className='flex flex-col gap-6'>
        <div className='flex flex-wrap items-end justify-between gap-3'>
          <div className='min-w-0'>
            <h1 className='text-2xl font-bold tracking-tight'>小组协作</h1>
            <p className='text-sm text-muted-foreground'>
              创建知识库小组、管理成员和处理邀请/申请
            </p>
          </div>
          <Button variant='outline' size='sm' onClick={() => invalidateGroups()}>
            <RefreshCw data-icon='inline-start' />
            刷新
          </Button>
        </div>
        <div className='grid gap-4 lg:grid-cols-2'>
          <Card>
            <CardHeader>
              <CardTitle>创建小组</CardTitle>
              <CardDescription>创建后当前账号自动成为 OWNER</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleCreateGroup}>
                <FieldGroup>
                  <Field>
                    <FieldLabel htmlFor='groupName'>小组名称</FieldLabel>
                    <Input
                      id='groupName'
                      value={createValues.name}
                      onChange={(event) =>
                        setCreateValues((prev) => ({
                          ...prev,
                          name: event.target.value,
                        }))
                      }
                    />
                  </Field>
                  <Field>
                    <FieldLabel htmlFor='groupDescription'>描述</FieldLabel>
                    <Input
                      id='groupDescription'
                      value={createValues.description}
                      onChange={(event) =>
                        setCreateValues((prev) => ({
                          ...prev,
                          description: event.target.value,
                        }))
                      }
                    />
                  </Field>
                  <Button type='submit' disabled={createMutation.isPending}>
                    创建
                  </Button>
                </FieldGroup>
              </form>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>申请加入小组</CardTitle>
              <CardDescription>通过 groupCode 向小组 Owner 发起申请</CardDescription>
            </CardHeader>
            <CardContent>
              <form onSubmit={handleJoinRequest}>
                <FieldGroup>
                  <Field>
                    <FieldLabel htmlFor='groupCode'>小组编码</FieldLabel>
                    <Input
                      id='groupCode'
                      value={groupCode}
                      onChange={(event) => setGroupCode(event.target.value)}
                    />
                    <FieldDescription>
                      编码可由小组 Owner 从小组列表中复制。
                    </FieldDescription>
                  </Field>
                  <Button type='submit' disabled={joinMutation.isPending}>
                    <Send data-icon='inline-start' />
                    提交申请
                  </Button>
                </FieldGroup>
              </form>
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader className='gap-4 sm:flex-row sm:items-center sm:justify-between'>
            <div>
              <CardTitle>我的小组</CardTitle>
              <CardDescription>选择小组后查看成员和 Owner 审批项</CardDescription>
            </div>
            <Select value={selectedGroupId} onValueChange={setSelectedGroupId}>
              <SelectTrigger className='w-full sm:w-72'>
                <SelectValue placeholder='选择小组' />
              </SelectTrigger>
              <SelectContent>
                <SelectGroup>
                  {groups.map((group) => (
                    <SelectItem
                      key={group.groupId}
                      value={String(group.groupId)}
                    >
                      {group.groupName} · {group.relation}
                    </SelectItem>
                  ))}
                </SelectGroup>
              </SelectContent>
            </Select>
          </CardHeader>
          <CardContent className='flex flex-col gap-6'>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>小组</TableHead>
                  <TableHead>编码</TableHead>
                  <TableHead>关系</TableHead>
                  <TableHead className='text-right'>操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {groups.map((group) => (
                  <TableRow key={group.groupId}>
                    <TableCell className='font-medium'>
                      {group.groupName}
                    </TableCell>
                    <TableCell>{group.groupCode}</TableCell>
                    <TableCell>
                      <Badge variant='secondary'>{group.relation}</Badge>
                    </TableCell>
                    <TableCell className='text-right'>
                      {group.relation === 'MEMBER' ? (
                        <Button
                          variant='outline'
                          size='sm'
                          disabled={simpleMutation.isPending}
                          onClick={() =>
                            simpleMutation.mutate(() =>
                              leaveGroup(group.groupId)
                            )
                          }
                        >
                          退出
                        </Button>
                      ) : (
                        <span className='text-sm text-muted-foreground'>-</span>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
                {groups.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={4}
                      className='h-24 text-center text-muted-foreground'
                    >
                      暂无可见小组
                    </TableCell>
                  </TableRow>
                ) : null}
              </TableBody>
            </Table>

            {selectedGroup ? (
              <div className='grid gap-4 xl:grid-cols-2'>
                <div className='flex flex-col gap-4 rounded-lg border p-4'>
                  <div>
                    <h2 className='font-medium'>成员列表</h2>
                    <p className='text-sm text-muted-foreground'>
                      {selectedGroup.groupName}
                    </p>
                  </div>
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>成员</TableHead>
                          <TableHead>角色</TableHead>
                          <TableHead className='text-right'>操作</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {(membersQuery.data ?? []).map((member) => (
                          <TableRow key={member.userId}>
                            <TableCell>
                              <div className='font-medium'>
                                {member.displayName}
                              </div>
                              <div className='text-sm text-muted-foreground'>
                                {member.userCode} · ID {member.userId}
                              </div>
                            </TableCell>
                            <TableCell>
                              <Badge variant='secondary'>{member.role}</Badge>
                            </TableCell>
                            <TableCell className='text-right'>
                              {selectedGroup.relation === 'OWNER' &&
                              member.role !== 'OWNER' ? (
                                <Button
                                  variant='outline'
                                  size='sm'
                                  disabled={simpleMutation.isPending}
                                  onClick={() =>
                                    simpleMutation.mutate(() =>
                                      removeGroupMember(
                                        selectedGroup.groupId,
                                        member.userId
                                      )
                                    )
                                  }
                                >
                                  <UserMinus data-icon='inline-start' />
                                  移除
                                </Button>
                              ) : (
                                <span className='text-sm text-muted-foreground'>
                                  -
                                </span>
                              )}
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                </div>

                <div className='flex flex-col gap-4 rounded-lg border p-4'>
                  <div>
                    <h2 className='font-medium'>邀请成员</h2>
                    <p className='text-sm text-muted-foreground'>
                      需要当前账号是小组 OWNER
                    </p>
                  </div>
                    <form onSubmit={handleInvitation}>
                      <FieldGroup>
                        <Field>
                          <FieldLabel htmlFor='inviteeUserId'>
                            被邀请用户 ID
                          </FieldLabel>
                          <Input
                            id='inviteeUserId'
                            inputMode='numeric'
                            disabled={selectedGroup.relation !== 'OWNER'}
                            value={inviteeUserId}
                            onChange={(event) =>
                              setInviteeUserId(event.target.value)
                            }
                          />
                        </Field>
                        <Button
                          type='submit'
                          disabled={
                            selectedGroup.relation !== 'OWNER' ||
                            inviteMutation.isPending
                          }
                        >
                          创建邀请
                        </Button>
                      </FieldGroup>
                    </form>
                </div>
              </div>
            ) : null}
          </CardContent>
        </Card>

        <div className='grid gap-4 xl:grid-cols-2'>
          <Card>
            <CardHeader>
              <CardTitle>待处理邀请</CardTitle>
              <CardDescription>别人邀请你加入的小组</CardDescription>
            </CardHeader>
            <CardContent>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>小组</TableHead>
                    <TableHead>邀请人</TableHead>
                    <TableHead className='text-right'>操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(groupsQuery.data?.pendingInvitations ?? []).map(
                    (invitation) => (
                      <TableRow key={invitation.invitationId}>
                        <TableCell>{invitation.groupName}</TableCell>
                        <TableCell>{invitation.inviterDisplayName}</TableCell>
                        <TableCell className='text-right'>
                          <div className='flex justify-end gap-2'>
                            <Button
                              variant='outline'
                              size='sm'
                              disabled={simpleMutation.isPending}
                              onClick={() =>
                                simpleMutation.mutate(() =>
                                  acceptInvitation(invitation.invitationId)
                                )
                              }
                            >
                              <Check data-icon='inline-start' />
                              接受
                            </Button>
                            <Button
                              variant='outline'
                              size='sm'
                              disabled={simpleMutation.isPending}
                              onClick={() =>
                                simpleMutation.mutate(() =>
                                  rejectInvitation(invitation.invitationId)
                                )
                              }
                            >
                              <X data-icon='inline-start' />
                              拒绝
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    )
                  )}
                  {(groupsQuery.data?.pendingInvitations ?? []).length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={3}
                        className='h-20 text-center text-muted-foreground'
                      >
                        暂无邀请
                      </TableCell>
                    </TableRow>
                  ) : null}
                </TableBody>
              </Table>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>加入申请</CardTitle>
              <CardDescription>我的申请和当前 Owner 小组收到的申请</CardDescription>
            </CardHeader>
            <CardContent className='flex flex-col gap-6'>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>我的申请</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead>时间</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(myRequestsQuery.data ?? []).map((request) => (
                    <TableRow key={request.requestId}>
                      <TableCell>{request.groupName}</TableCell>
                      <TableCell>
                        <Badge variant='secondary'>{request.status}</Badge>
                      </TableCell>
                      <TableCell>{formatDateTime(request.createdAt)}</TableCell>
                    </TableRow>
                  ))}
                  {(myRequestsQuery.data ?? []).length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={3}
                        className='h-20 text-center text-muted-foreground'
                      >
                        暂无申请
                      </TableCell>
                    </TableRow>
                  ) : null}
                </TableBody>
              </Table>

              {selectedGroup?.relation === 'OWNER' ? (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>申请人</TableHead>
                      <TableHead>状态</TableHead>
                      <TableHead className='text-right'>操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {(ownerRequestsQuery.data ?? []).map((request) => (
                      <TableRow key={request.requestId}>
                        <TableCell>
                          {request.applicantDisplayName} ·{' '}
                          {request.applicantUserCode}
                        </TableCell>
                        <TableCell>
                          <Badge variant='secondary'>{request.status}</Badge>
                        </TableCell>
                        <TableCell className='text-right'>
                          <div className='flex justify-end gap-2'>
                            <Button
                              variant='outline'
                              size='sm'
                              disabled={simpleMutation.isPending}
                              onClick={() =>
                                simpleMutation.mutate(() =>
                                  approveJoinRequest(
                                    request.groupId,
                                    request.requestId
                                  )
                                )
                              }
                            >
                              批准
                            </Button>
                            <Button
                              variant='outline'
                              size='sm'
                              disabled={simpleMutation.isPending}
                              onClick={() =>
                                simpleMutation.mutate(() =>
                                  rejectJoinRequest(
                                    request.groupId,
                                    request.requestId
                                  )
                                )
                              }
                            >
                              拒绝
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))}
                    {(ownerRequestsQuery.data ?? []).length === 0 ? (
                      <TableRow>
                        <TableCell
                          colSpan={3}
                          className='h-20 text-center text-muted-foreground'
                        >
                          当前小组暂无申请
                        </TableCell>
                      </TableRow>
                    ) : null}
                  </TableBody>
                </Table>
              ) : null}
            </CardContent>
          </Card>
        </div>
      </Main>
    </>
  )
}
