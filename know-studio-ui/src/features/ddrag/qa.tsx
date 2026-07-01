import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { MessageSquareText, Send } from 'lucide-react'
import { toast } from 'sonner'
import { getMyGroups } from '@/api/groups'
import { extractApiError } from '@/api/http'
import { askQuestion, type AskQuestionResponse } from '@/api/qa'
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
import { mergeGroups } from './shared'

export function QaPage() {
  const [selectedGroupId, setSelectedGroupId] = useState('')
  const [question, setQuestion] = useState('')
  const [answer, setAnswer] = useState<AskQuestionResponse | null>(null)
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

  const askMutation = useMutation({
    mutationFn: askQuestion,
    onSuccess: setAnswer,
    onError: (error) => toast.error(extractApiError(error, '提问失败')),
  })

  function handleAsk(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!question.trim() || !Number.isFinite(groupId) || groupId <= 0) return
    askMutation.mutate({ groupId, question: question.trim() })
  }

  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>
      <Main className='grid gap-6 pt-4 xl:grid-cols-[420px_1fr]'>
        <div className='xl:col-span-2'>
          <div className='min-w-0'>
            <h1 className='text-2xl font-bold tracking-tight'>知识库问答</h1>
          </div>
        </div>
        <Card className='self-start'>
          <CardHeader>
            <CardTitle>提问</CardTitle>
            <CardDescription>
              选择小组后，后端会只在该小组知识库内检索。
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleAsk}>
              <FieldGroup>
                <Field>
                  <FieldLabel>小组</FieldLabel>
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
                  <FieldLabel htmlFor='question'>问题</FieldLabel>
                  <Textarea
                    id='question'
                    className='min-h-40'
                    value={question}
                    onChange={(event) => setQuestion(event.target.value)}
                    placeholder='输入一个需要从文档中寻找证据的问题'
                  />
                </Field>
                <Button type='submit' disabled={askMutation.isPending}>
                  <Send data-icon='inline-start' />
                  {askMutation.isPending ? '生成中' : '提问'}
                </Button>
              </FieldGroup>
            </form>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>回答</CardTitle>
            <CardDescription>
              answered=false 时表示后端根据证据阈值拒答。
            </CardDescription>
          </CardHeader>
          <CardContent className='flex flex-col gap-6'>
            {answer ? (
              <>
                <div className='rounded-lg border bg-muted/30 p-4'>
                  <div className='mb-3 flex items-center gap-2'>
                    <MessageSquareText className='text-primary' />
                    <Badge variant={answer.answered ? 'default' : 'secondary'}>
                      {answer.answered ? '已回答' : '证据不足'}
                    </Badge>
                    {answer.reasonCode ? (
                      <Badge variant='secondary'>{answer.reasonCode}</Badge>
                    ) : null}
                  </div>
                  <div className='whitespace-pre-wrap text-sm leading-7'>
                    {answer.answer ??
                      answer.reasonMessage ??
                      '后端没有返回回答内容'}
                  </div>
                </div>

                <div className='flex flex-col gap-3'>
                  <h2 className='text-sm font-medium'>引用证据</h2>
                  {(answer.citations ?? []).map((citation) => (
                    <div
                      key={`${citation.documentId}-${citation.chunkId}`}
                      className='rounded-lg border p-4'
                    >
                      <div className='mb-2 flex flex-wrap items-center gap-2'>
                        <span className='font-medium'>{citation.fileName}</span>
                        <Badge variant='secondary'>
                          chunk {citation.chunkIndex}
                        </Badge>
                        <Badge variant='secondary'>
                          score {citation.score.toFixed(4)}
                        </Badge>
                      </div>
                      <p className='text-sm leading-6 text-muted-foreground'>
                        {citation.snippet}
                      </p>
                    </div>
                  ))}
                  {(answer.citations ?? []).length === 0 ? (
                    <p className='rounded-lg border p-4 text-sm text-muted-foreground'>
                      暂无引用证据。
                    </p>
                  ) : null}
                </div>
              </>
            ) : (
              <div className='flex min-h-80 items-center justify-center rounded-lg border border-dashed text-sm text-muted-foreground'>
                提问后将在这里展示回答和引用。
              </div>
            )}
          </CardContent>
        </Card>
      </Main>
    </>
  )
}
