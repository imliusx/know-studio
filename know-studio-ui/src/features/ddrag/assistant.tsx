import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bot, MessageSquarePlus, RefreshCw, Send, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import {
  createAssistantSession,
  deleteAssistantSession,
  getAssistantContext,
  listAssistantSessions,
  streamAssistantChat,
  type AssistantMessage,
  type AssistantToolMode,
} from '@/api/assistant'
import { getMyGroups } from '@/api/groups'
import { extractApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth-store'
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
import { Field, FieldGroup, FieldLabel } from '@/components/ui/field'
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Textarea } from '@/components/ui/textarea'
import { formatDateTime, mergeGroups } from './shared'

interface LocalMessage {
  key: string
  role: string
  content: string
  toolMode?: AssistantToolMode | null
}

export function AssistantPage() {
  const queryClient = useQueryClient()
  const abortRef = useRef<AbortController | null>(null)
  const accessToken = useAuthStore((state) => state.auth.accessToken)
  const [selectedSessionId, setSelectedSessionId] = useState<string>('')
  const [selectedGroupId, setSelectedGroupId] = useState<string>('')
  const [toolMode, setToolMode] = useState<AssistantToolMode>('CHAT')
  const [message, setMessage] = useState('')
  const [messages, setMessages] = useState<LocalMessage[]>([])
  const [isStreaming, setIsStreaming] = useState(false)

  const sessionsQuery = useQuery({
    queryKey: ['assistant', 'sessions'],
    queryFn: listAssistantSessions,
  })
  const groupsQuery = useQuery({
    queryKey: ['groups', 'my'],
    queryFn: getMyGroups,
  })
  const groups = useMemo(() => mergeGroups(groupsQuery.data), [groupsQuery.data])
  const sessionId = Number(selectedSessionId)
  const groupId = Number(selectedGroupId)

  useEffect(() => {
    if (!selectedSessionId && sessionsQuery.data?.[0]) {
      const timeout = window.setTimeout(() => {
        setSelectedSessionId(String(sessionsQuery.data[0].sessionId))
      })
      return () => window.clearTimeout(timeout)
    }
    return
  }, [selectedSessionId, sessionsQuery.data])

  useEffect(() => {
    if (!selectedGroupId && groups[0]) {
      const timeout = window.setTimeout(() => {
        setSelectedGroupId(String(groups[0].groupId))
      })
      return () => window.clearTimeout(timeout)
    }
    return
  }, [groups, selectedGroupId])

  const contextQuery = useQuery({
    queryKey: ['assistant', 'sessions', sessionId, 'context'],
    queryFn: () => getAssistantContext(sessionId, 24),
    enabled: Number.isFinite(sessionId) && sessionId > 0,
  })

  useEffect(() => {
    const recentMessages = contextQuery.data?.recentMessages ?? []
    const timeout = window.setTimeout(() => {
      setMessages(recentMessages.map(toLocalMessage))
    })
    return () => window.clearTimeout(timeout)
  }, [contextQuery.data])

  const createMutation = useMutation({
    mutationFn: () => createAssistantSession(),
    onSuccess: (session) => {
      toast.success('会话已创建')
      setSelectedSessionId(String(session.sessionId))
      queryClient.invalidateQueries({ queryKey: ['assistant', 'sessions'] })
    },
    onError: (error) => toast.error(extractApiError(error, '创建会话失败')),
  })
  const deleteMutation = useMutation({
    mutationFn: deleteAssistantSession,
    onSuccess: () => {
      toast.success('会话已删除')
      setSelectedSessionId('')
      setMessages([])
      queryClient.invalidateQueries({ queryKey: ['assistant', 'sessions'] })
    },
    onError: (error) => toast.error(extractApiError(error, '删除会话失败')),
  })

  async function handleSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!message.trim() || !Number.isFinite(sessionId) || sessionId <= 0) {
      return
    }
    if (toolMode === 'KB_SEARCH' && (!Number.isFinite(groupId) || groupId <= 0)) {
      toast.error('知识库模式需要选择小组')
      return
    }

    const userMessage = message.trim()
    const assistantKey = `assistant-${Date.now()}`
    setMessage('')
    setMessages((prev) => [
      ...prev,
      {
        key: `user-${Date.now()}`,
        role: 'USER',
        content: userMessage,
        toolMode,
      },
      { key: assistantKey, role: 'ASSISTANT', content: '', toolMode },
    ])

    const abortController = new AbortController()
    abortRef.current = abortController
    setIsStreaming(true)

    try {
      await streamAssistantChat(
        {
          sessionId,
          message: userMessage,
          toolMode,
          groupId: toolMode === 'KB_SEARCH' ? groupId : undefined,
        },
        accessToken,
        (eventData) => {
          if (eventData.event === 'delta' && eventData.delta) {
            setMessages((prev) =>
              prev.map((item) =>
                item.key === assistantKey
                  ? { ...item, content: item.content + eventData.delta }
                  : item
              )
            )
          }

          if (eventData.event === 'done') {
            setMessages((prev) =>
              prev.map((item) =>
                item.key === assistantKey
                  ? { ...item, content: eventData.reply ?? item.content }
                  : item
              )
            )
          }

          if (eventData.event === 'error') {
            throw new Error(eventData.error ?? '助手响应失败')
          }
        },
        abortController.signal
      )
      queryClient.invalidateQueries({ queryKey: ['assistant', 'sessions'] })
      queryClient.invalidateQueries({
        queryKey: ['assistant', 'sessions', sessionId, 'context'],
      })
    } catch (error) {
      toast.error(extractApiError(error, '发送失败'))
    } finally {
      setIsStreaming(false)
      abortRef.current = null
    }
  }

  function stopStreaming() {
    abortRef.current?.abort()
    setIsStreaming(false)
  }

  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>
      <Main fixed className='grid gap-4 xl:grid-cols-[320px_1fr]'>
        <div className='flex flex-wrap items-end justify-between gap-3 xl:col-span-2'>
          <div className='min-w-0'>
            <h1 className='text-2xl font-bold tracking-tight'>AI 助手</h1>
            <p className='text-sm text-muted-foreground'>
              管理会话并通过 POST SSE 获取流式回复
            </p>
          </div>
          <Button
            variant='outline'
            size='sm'
            onClick={() => sessionsQuery.refetch()}
          >
            <RefreshCw data-icon='inline-start' />
            刷新
          </Button>
        </div>
        <Card className='min-h-0'>
          <CardHeader>
            <div className='flex items-center justify-between gap-3'>
              <div>
                <CardTitle>会话</CardTitle>
                <CardDescription>我的助手会话</CardDescription>
              </div>
              <Button
                size='icon'
                variant='outline'
                onClick={() => createMutation.mutate()}
                disabled={createMutation.isPending}
              >
                <MessageSquarePlus />
                <span className='sr-only'>新建会话</span>
              </Button>
            </div>
          </CardHeader>
          <CardContent className='flex max-h-[calc(100svh-13rem)] flex-col gap-2 overflow-auto'>
            {(sessionsQuery.data ?? []).map((session) => (
              <button
                key={session.sessionId}
                type='button'
                onClick={() => setSelectedSessionId(String(session.sessionId))}
                className='flex w-full flex-col gap-1 rounded-lg border p-3 text-left text-sm transition-colors hover:bg-muted/50 data-[active=true]:bg-muted'
                data-active={selectedSessionId === String(session.sessionId)}
              >
                <span className='truncate font-medium'>{session.title}</span>
                <span className='text-xs text-muted-foreground'>
                  {formatDateTime(session.lastMessageAt)}
                </span>
              </button>
            ))}
            {(sessionsQuery.data ?? []).length === 0 ? (
              <div className='rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground'>
                暂无会话
              </div>
            ) : null}
          </CardContent>
        </Card>

        <Card className='flex min-h-0 flex-col'>
          <CardHeader className='gap-4 border-b xl:flex-row xl:items-center xl:justify-between'>
            <div>
              <CardTitle>对话</CardTitle>
              <CardDescription>
                CHAT 直接对话，KB_SEARCH 会调用知识库检索工具
              </CardDescription>
            </div>
            <div className='grid gap-3 sm:grid-cols-[180px_240px_auto]'>
              <Select
                value={toolMode}
                onValueChange={(value) => setToolMode(value as AssistantToolMode)}
              >
                <SelectTrigger className='w-full'>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    <SelectItem value='CHAT'>CHAT</SelectItem>
                    <SelectItem value='KB_SEARCH'>KB_SEARCH</SelectItem>
                  </SelectGroup>
                </SelectContent>
              </Select>
              <Select
                value={selectedGroupId}
                onValueChange={setSelectedGroupId}
                disabled={toolMode !== 'KB_SEARCH'}
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
              <Button
                variant='outline'
                disabled={!selectedSessionId || deleteMutation.isPending}
                onClick={() => deleteMutation.mutate(sessionId)}
              >
                <Trash2 data-icon='inline-start' />
                删除
              </Button>
            </div>
          </CardHeader>
          <CardContent className='flex min-h-0 flex-1 flex-col gap-4 p-4'>
            <div className='flex min-h-0 flex-1 flex-col gap-3 overflow-auto rounded-lg border bg-muted/20 p-4'>
              {messages.map((item) => (
                <div
                  key={item.key}
                  className='max-w-[85%] rounded-lg border bg-background p-3 data-[role=user]:ml-auto data-[role=user]:bg-primary data-[role=user]:text-primary-foreground'
                  data-role={item.role.toLowerCase()}
                >
                  <div className='mb-2 flex items-center gap-2 text-xs opacity-80'>
                    <Bot />
                    <span>{item.role}</span>
                    {item.toolMode ? (
                      <Badge variant='secondary'>{item.toolMode}</Badge>
                    ) : null}
                  </div>
                  <div className='whitespace-pre-wrap text-sm leading-6'>
                    {item.content || '...'}
                  </div>
                </div>
              ))}
              {messages.length === 0 ? (
                <div className='flex min-h-80 items-center justify-center text-sm text-muted-foreground'>
                  选择或新建会话后开始对话。
                </div>
              ) : null}
            </div>

            <form onSubmit={handleSend}>
              <FieldGroup className='grid gap-3 lg:grid-cols-[1fr_auto] lg:items-end'>
                <Field>
                  <FieldLabel htmlFor='assistantMessage'>消息</FieldLabel>
                  <Textarea
                    id='assistantMessage'
                    className='min-h-20'
                    value={message}
                    disabled={!selectedSessionId || isStreaming}
                    onChange={(event) => setMessage(event.target.value)}
                    placeholder='输入消息，Shift+Enter 换行'
                  />
                </Field>
                {isStreaming ? (
                  <Button type='button' variant='outline' onClick={stopStreaming}>
                    停止
                  </Button>
                ) : (
                  <Button type='submit' disabled={!selectedSessionId}>
                    <Send data-icon='inline-start' />
                    发送
                  </Button>
                )}
              </FieldGroup>
            </form>
          </CardContent>
        </Card>
      </Main>
    </>
  )
}

function toLocalMessage(message: AssistantMessage): LocalMessage {
  return {
    key: String(message.messageId),
    role: message.role,
    content: message.content,
    toolMode: message.toolMode,
  }
}
