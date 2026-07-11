import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import axios from 'axios'
import {
  BarChart3,
  Database,
  FlaskConical,
  Play,
  Plus,
  ShieldAlert,
} from 'lucide-react'
import { toast } from 'sonner'
import {
  addEvaluationSample,
  createEvaluationDataset,
  listEvaluationDatasets,
  listEvaluationRuns,
  listEvaluationSamples,
  runEvaluationAblation,
  type EvaluationMetric,
  type RetrievalMode,
} from '@/api/evaluations'
import { extractApiError } from '@/api/http'
import { Header } from '@/components/layout/header'
import { HeaderActions } from '@/components/layout/header-actions'
import { Main } from '@/components/layout/main'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Textarea } from '@/components/ui/textarea'
import { useAuthStore } from '@/stores/auth-store'
import { useWorkspaceStore } from '@/stores/workspace-store'
import { cn } from '@/lib/utils'

const MODES: RetrievalMode[] = [
  'VECTOR_ONLY',
  'HYBRID',
  'HYBRID_RERANK',
]

const MODE_LABELS: Record<RetrievalMode, string> = {
  VECTOR_ONLY: '向量检索',
  HYBRID: '混合检索',
  HYBRID_RERANK: '混合 + 重排',
}

export function EvaluationPage() {
  const queryClient = useQueryClient()
  const currentUser = useAuthStore((state) => state.auth.user)
  const currentWorkspaceId = useWorkspaceStore(
    (state) => state.currentWorkspaceId
  )
  const currentWorkspace = useWorkspaceStore((state) =>
    state.workspaces.find(
      (workspace) => workspace.workspaceId === state.currentWorkspaceId
    )
  )
  const canManage =
    currentUser?.systemRole === 'ADMIN' ||
    currentWorkspace?.role === 'OWNER' ||
    currentWorkspace?.role === 'ADMIN'
  const [selectedDatasetId, setSelectedDatasetId] = useState<number | null>(null)
  const [createDialogOpen, setCreateDialogOpen] = useState(false)
  const [sampleDialogOpen, setSampleDialogOpen] = useState(false)
  const [datasetName, setDatasetName] = useState('')
  const [datasetDescription, setDatasetDescription] = useState('')
  const [sampleQuestion, setSampleQuestion] = useState('')
  const [sampleChunkIds, setSampleChunkIds] = useState('')
  const [sampleExpectedAnswer, setSampleExpectedAnswer] = useState('')
  const [topK, setTopK] = useState(5)
  const [cooldownSeconds, setCooldownSeconds] = useState(0)
  const [latestReport, setLatestReport] = useState<{
    datasetId: number
    topK: number
    metrics: EvaluationMetric[]
  } | null>(null)

  const datasetsQuery = useQuery({
    queryKey: ['evaluation-datasets', currentWorkspaceId],
    queryFn: () => listEvaluationDatasets(currentWorkspaceId!),
    enabled: Boolean(currentWorkspaceId && canManage),
  })
  const activeDataset =
    datasetsQuery.data?.find((dataset) => dataset.id === selectedDatasetId) ??
    datasetsQuery.data?.[0] ??
    null
  const activeDatasetId = activeDataset?.id ?? null
  const samplesQuery = useQuery({
    queryKey: ['evaluation-samples', currentWorkspaceId, activeDatasetId],
    queryFn: () =>
      listEvaluationSamples(currentWorkspaceId!, activeDatasetId!),
    enabled: Boolean(currentWorkspaceId && activeDatasetId && canManage),
  })
  const runsQuery = useQuery({
    queryKey: ['evaluation-runs', currentWorkspaceId, activeDatasetId],
    queryFn: () => listEvaluationRuns(currentWorkspaceId!, activeDatasetId!),
    enabled: Boolean(currentWorkspaceId && activeDatasetId && canManage),
  })

  useEffect(() => {
    if (cooldownSeconds <= 0) return
    const timer = window.setTimeout(
      () => setCooldownSeconds((value) => Math.max(0, value - 1)),
      1000
    )
    return () => window.clearTimeout(timer)
  }, [cooldownSeconds])

  const createDatasetMutation = useMutation({
    mutationFn: () =>
      createEvaluationDataset(currentWorkspaceId!, {
        name: datasetName.trim(),
        description: datasetDescription.trim() || undefined,
      }),
    onSuccess: (dataset) => {
      void queryClient.invalidateQueries({
        queryKey: ['evaluation-datasets', currentWorkspaceId],
      })
      setSelectedDatasetId(dataset.id)
      setDatasetName('')
      setDatasetDescription('')
      setCreateDialogOpen(false)
      toast.success('评测数据集已创建')
    },
    onError: (error) =>
      toast.error(extractApiError(error, '创建评测数据集失败')),
  })

  const addSampleMutation = useMutation({
    mutationFn: (relevantChunkIds: number[]) =>
      addEvaluationSample(currentWorkspaceId!, activeDatasetId!, {
        question: sampleQuestion.trim(),
        relevantChunkIds,
        expectedAnswer: sampleExpectedAnswer.trim() || undefined,
      }),
    onSuccess: () => {
      void queryClient.invalidateQueries({
        queryKey: [
          'evaluation-samples',
          currentWorkspaceId,
          activeDatasetId,
        ],
      })
      setSampleQuestion('')
      setSampleChunkIds('')
      setSampleExpectedAnswer('')
      setSampleDialogOpen(false)
      toast.success('评测样本已添加')
    },
    onError: (error) => toast.error(extractApiError(error, '添加评测样本失败')),
  })

  const runMutation = useMutation({
    mutationFn: () =>
      runEvaluationAblation(currentWorkspaceId!, activeDatasetId!, topK),
    onSuccess: (report) => {
      setLatestReport(report)
      void queryClient.invalidateQueries({
        queryKey: ['evaluation-runs', currentWorkspaceId, activeDatasetId],
      })
      toast.success('消融评测已完成')
    },
    onError: (error) => {
      if (axios.isAxiosError(error) && error.response?.status === 429) {
        setCooldownSeconds(10)
      }
      toast.error(extractApiError(error, '运行消融评测失败'))
    },
  })

  const comparisonRows = useMemo(
    () =>
      MODES.map((mode) => {
        const reportMetric =
          latestReport?.datasetId === activeDatasetId
            ? latestReport.metrics.find((metric) => metric.mode === mode)
            : undefined
        const historicalRun = runsQuery.data?.find((run) => run.mode === mode)
        return {
          mode,
          recallAtK: reportMetric?.recallAtK ?? historicalRun?.recallAtK,
          sampleCount: reportMetric?.sampleCount ?? historicalRun?.sampleCount,
          averageLatencyMillis:
            reportMetric?.averageLatencyMillis ??
            historicalRun?.averageLatencyMillis,
          topK:
            reportMetric && latestReport
              ? latestReport.topK
              : historicalRun?.topK,
        }
      }),
    [activeDatasetId, latestReport, runsQuery.data]
  )

  function handleCreateDataset(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!datasetName.trim()) return
    createDatasetMutation.mutate()
  }

  function handleAddSample(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const chunkIds = Array.from(
      new Set(
        sampleChunkIds
          .split(/[\s,，]+/)
          .map(Number)
          .filter((value) => Number.isSafeInteger(value) && value > 0)
      )
    )
    if (!sampleQuestion.trim() || chunkIds.length === 0) {
      toast.error('请填写问题并输入至少一个有效的 Chunk ID')
      return
    }
    addSampleMutation.mutate(chunkIds)
  }

  if (!currentWorkspaceId) {
    return <EvaluationState title='尚未选择工作空间' description='请先创建或选择工作空间。' />
  }

  if (!canManage) {
    return (
      <EvaluationState
        icon={ShieldAlert}
        title='无评测管理权限'
        description='仅工作空间 OWNER 或 ADMIN 可以管理数据集并运行评测。'
      />
    )
  }

  return (
    <>
      <Header fixed>
        <div className='flex min-w-0 flex-1 items-center gap-2'>
          <BarChart3 className='size-5 text-muted-foreground' />
          <span className='truncate text-sm font-medium'>检索评测</span>
        </div>
        <HeaderActions />
      </Header>
      <Main className='flex flex-col gap-6 pt-4'>
        <div className='flex flex-wrap items-end justify-between gap-4'>
          <div>
            <h1 className='text-2xl font-bold tracking-tight'>检索消融评测</h1>
            <p className='mt-1 text-sm text-muted-foreground'>
              对比向量、混合检索和重排链路的 Recall@K 与平均延迟。
            </p>
          </div>
          <Button onClick={() => setCreateDialogOpen(true)}>
            <Plus data-icon='inline-start' />
            新建数据集
          </Button>
        </div>

        <div className='grid min-h-[640px] overflow-hidden rounded-md border lg:grid-cols-[280px_minmax(0,1fr)]'>
          <aside className='border-b bg-muted/20 p-3 lg:border-r lg:border-b-0'>
            <div className='mb-3 flex items-center justify-between px-2'>
              <span className='text-sm font-medium'>数据集</span>
              <Badge variant='secondary'>{datasetsQuery.data?.length ?? 0}</Badge>
            </div>
            {datasetsQuery.isLoading ? (
              <div className='space-y-2'>
                <Skeleton className='h-16 w-full' />
                <Skeleton className='h-16 w-full' />
              </div>
            ) : datasetsQuery.isError ? (
              <Empty className='min-h-48 border'>
                <EmptyHeader>
                  <EmptyMedia variant='icon'>
                    <Database />
                  </EmptyMedia>
                  <EmptyTitle>数据集加载失败</EmptyTitle>
                  <EmptyDescription>
                    {extractApiError(datasetsQuery.error, '请稍后重试')}
                  </EmptyDescription>
                </EmptyHeader>
                <EmptyContent>
                  <Button variant='outline' onClick={() => datasetsQuery.refetch()}>
                    重新加载
                  </Button>
                </EmptyContent>
              </Empty>
            ) : datasetsQuery.data?.length ? (
              <div className='flex flex-col gap-1'>
                {datasetsQuery.data.map((dataset) => (
                  <button
                    key={dataset.id}
                    type='button'
                    onClick={() => {
                      setSelectedDatasetId(dataset.id)
                      setLatestReport(null)
                    }}
                    className={cn(
                      'w-full rounded-md px-3 py-2.5 text-left transition-colors hover:bg-muted',
                      activeDatasetId === dataset.id && 'bg-muted'
                    )}
                  >
                    <div className='truncate text-sm font-medium'>{dataset.name}</div>
                    <div className='mt-1 line-clamp-2 text-xs text-muted-foreground'>
                      {dataset.description || '暂无描述'}
                    </div>
                  </button>
                ))}
              </div>
            ) : (
              <Empty className='min-h-48 border'>
                <EmptyHeader>
                  <EmptyMedia variant='icon'>
                    <Database />
                  </EmptyMedia>
                  <EmptyTitle>暂无数据集</EmptyTitle>
                  <EmptyDescription>创建数据集后添加标注样本。</EmptyDescription>
                </EmptyHeader>
              </Empty>
            )}
          </aside>

          <section className='min-w-0 p-4 sm:p-6'>
            {activeDataset ? (
              <div className='flex flex-col gap-8'>
                <div className='flex flex-wrap items-start justify-between gap-4'>
                  <div>
                    <h2 className='text-lg font-semibold'>{activeDataset.name}</h2>
                    <p className='mt-1 text-sm text-muted-foreground'>
                      {activeDataset.description || '暂无描述'}
                    </p>
                  </div>
                  <Button variant='outline' onClick={() => setSampleDialogOpen(true)}>
                    <Plus data-icon='inline-start' />
                    添加样本
                  </Button>
                </div>

                <div className='grid gap-4 border-y py-5 md:grid-cols-[1fr_auto] md:items-end'>
                  <div>
                    <div className='text-sm font-medium'>运行消融评测</div>
                    <p className='mt-1 text-sm text-muted-foreground'>
                      一次运行会分别执行三种检索模式，限流窗口为 10 秒。
                    </p>
                  </div>
                  <div className='flex flex-wrap items-end gap-2'>
                    <label className='flex flex-col gap-1 text-xs text-muted-foreground'>
                      Top K
                      <Input
                        type='number'
                        min={1}
                        max={20}
                        value={topK}
                        onChange={(event) =>
                          setTopK(
                            Math.min(20, Math.max(1, Number(event.target.value) || 1))
                          )
                        }
                        className='w-24'
                      />
                    </label>
                    <Button
                      onClick={() => runMutation.mutate()}
                      disabled={
                        runMutation.isPending ||
                        cooldownSeconds > 0 ||
                        !samplesQuery.data?.length
                      }
                    >
                      <Play data-icon='inline-start' />
                      {runMutation.isPending
                        ? '运行中'
                        : cooldownSeconds > 0
                          ? `${cooldownSeconds}s 后重试`
                          : '运行评测'}
                    </Button>
                  </div>
                </div>

                <section>
                  <h3 className='mb-3 text-sm font-medium'>模式对比</h3>
                  <div className='rounded-md border'>
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>模式</TableHead>
                          <TableHead>Recall@K</TableHead>
                          <TableHead>平均延迟</TableHead>
                          <TableHead>样本数</TableHead>
                          <TableHead>Top K</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {comparisonRows.map((row) => (
                          <TableRow key={row.mode}>
                            <TableCell className='font-medium'>
                              {MODE_LABELS[row.mode]}
                            </TableCell>
                            <TableCell>
                              {row.recallAtK === undefined
                                ? '-'
                                : `${(row.recallAtK * 100).toFixed(1)}%`}
                            </TableCell>
                            <TableCell>
                              {row.averageLatencyMillis === undefined
                                ? '-'
                                : `${row.averageLatencyMillis} ms`}
                            </TableCell>
                            <TableCell>{row.sampleCount ?? '-'}</TableCell>
                            <TableCell>{row.topK ?? '-'}</TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </div>
                </section>

                <section>
                  <div className='mb-3 flex items-center justify-between gap-3'>
                    <h3 className='text-sm font-medium'>标注样本</h3>
                    <Badge variant='secondary'>{samplesQuery.data?.length ?? 0}</Badge>
                  </div>
                  <div className='rounded-md border'>
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>问题</TableHead>
                          <TableHead>相关 Chunk</TableHead>
                          <TableHead>期望回答</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {samplesQuery.isLoading ? (
                          <TableRow>
                            <TableCell colSpan={3} className='h-24 text-center'>
                              正在加载样本
                            </TableCell>
                          </TableRow>
                        ) : samplesQuery.isError ? (
                          <TableRow>
                            <TableCell colSpan={3} className='h-24 text-center text-destructive'>
                              {extractApiError(samplesQuery.error, '样本加载失败')}
                            </TableCell>
                          </TableRow>
                        ) : samplesQuery.data?.length ? (
                          samplesQuery.data.map((sample) => (
                            <TableRow key={sample.id}>
                              <TableCell className='max-w-80 whitespace-normal'>
                                {sample.question}
                              </TableCell>
                              <TableCell>
                                {sample.relevantChunkIds.join(', ')}
                              </TableCell>
                              <TableCell className='max-w-96 whitespace-normal text-muted-foreground'>
                                {sample.expectedAnswer || '-'}
                              </TableCell>
                            </TableRow>
                          ))
                        ) : (
                          <TableRow>
                            <TableCell colSpan={3} className='h-24 text-center'>
                              暂无标注样本
                            </TableCell>
                          </TableRow>
                        )}
                      </TableBody>
                    </Table>
                  </div>
                </section>

                <section>
                  <h3 className='mb-3 text-sm font-medium'>运行历史</h3>
                  <div className='rounded-md border'>
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>时间</TableHead>
                          <TableHead>模式</TableHead>
                          <TableHead>Recall@K</TableHead>
                          <TableHead>延迟</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {runsQuery.isLoading ? (
                          <TableRow>
                            <TableCell colSpan={4} className='h-24 text-center'>
                              正在加载运行记录
                            </TableCell>
                          </TableRow>
                        ) : runsQuery.isError ? (
                          <TableRow>
                            <TableCell colSpan={4} className='h-24 text-center text-destructive'>
                              {extractApiError(runsQuery.error, '运行记录加载失败')}
                            </TableCell>
                          </TableRow>
                        ) : runsQuery.data?.length ? (
                          runsQuery.data.map((run) => (
                            <TableRow key={run.id}>
                              <TableCell>{formatTime(run.createdAt)}</TableCell>
                              <TableCell>{MODE_LABELS[run.mode]}</TableCell>
                              <TableCell>
                                {(run.recallAtK * 100).toFixed(1)}%
                              </TableCell>
                              <TableCell>{run.averageLatencyMillis} ms</TableCell>
                            </TableRow>
                          ))
                        ) : (
                          <TableRow>
                            <TableCell colSpan={4} className='h-24 text-center'>
                              暂无运行记录
                            </TableCell>
                          </TableRow>
                        )}
                      </TableBody>
                    </Table>
                  </div>
                </section>
              </div>
            ) : (
              <Empty className='min-h-[520px] border'>
                <EmptyHeader>
                  <EmptyMedia variant='icon'>
                    <FlaskConical />
                  </EmptyMedia>
                  <EmptyTitle>选择或创建评测数据集</EmptyTitle>
                  <EmptyDescription>
                    数据集包含问题、相关 Chunk 和可选的期望回答。
                  </EmptyDescription>
                </EmptyHeader>
                <EmptyContent>
                  <Button onClick={() => setCreateDialogOpen(true)}>
                    <Plus data-icon='inline-start' />
                    新建数据集
                  </Button>
                </EmptyContent>
              </Empty>
            )}
          </section>
        </div>
      </Main>

      <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
        <DialogContent>
          <form onSubmit={handleCreateDataset}>
            <DialogHeader>
              <DialogTitle>新建评测数据集</DialogTitle>
              <DialogDescription>为一组检索标注样本命名。</DialogDescription>
            </DialogHeader>
            <div className='my-5 flex flex-col gap-4'>
              <label className='flex flex-col gap-2 text-sm font-medium'>
                名称
                <Input
                  value={datasetName}
                  onChange={(event) => setDatasetName(event.target.value)}
                  maxLength={200}
                  autoFocus
                />
              </label>
              <label className='flex flex-col gap-2 text-sm font-medium'>
                描述
                <Textarea
                  value={datasetDescription}
                  onChange={(event) => setDatasetDescription(event.target.value)}
                  maxLength={2000}
                />
              </label>
            </div>
            <DialogFooter>
              <Button type='button' variant='outline' onClick={() => setCreateDialogOpen(false)}>
                取消
              </Button>
              <Button type='submit' disabled={!datasetName.trim() || createDatasetMutation.isPending}>
                创建
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={sampleDialogOpen} onOpenChange={setSampleDialogOpen}>
        <DialogContent className='sm:max-w-xl'>
          <form onSubmit={handleAddSample}>
            <DialogHeader>
              <DialogTitle>添加标注样本</DialogTitle>
              <DialogDescription>
                Chunk ID 可用逗号或空格分隔，至少填写一个。
              </DialogDescription>
            </DialogHeader>
            <div className='my-5 flex flex-col gap-4'>
              <label className='flex flex-col gap-2 text-sm font-medium'>
                问题
                <Textarea
                  value={sampleQuestion}
                  onChange={(event) => setSampleQuestion(event.target.value)}
                  maxLength={2000}
                  className='min-h-24'
                  autoFocus
                />
              </label>
              <label className='flex flex-col gap-2 text-sm font-medium'>
                相关 Chunk ID
                <Input
                  value={sampleChunkIds}
                  onChange={(event) => setSampleChunkIds(event.target.value)}
                  placeholder='例如：101, 205, 309'
                />
              </label>
              <label className='flex flex-col gap-2 text-sm font-medium'>
                期望回答（可选）
                <Textarea
                  value={sampleExpectedAnswer}
                  onChange={(event) => setSampleExpectedAnswer(event.target.value)}
                  maxLength={10000}
                />
              </label>
            </div>
            <DialogFooter>
              <Button type='button' variant='outline' onClick={() => setSampleDialogOpen(false)}>
                取消
              </Button>
              <Button type='submit' disabled={addSampleMutation.isPending}>
                添加
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
    </>
  )
}

function EvaluationState({
  icon: Icon = Database,
  title,
  description,
}: {
  icon?: typeof Database
  title: string
  description: string
}) {
  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>
      <Main className='flex min-h-[70vh] items-center justify-center'>
        <Empty className='max-w-lg border'>
          <EmptyHeader>
            <EmptyMedia variant='icon'>
              <Icon />
            </EmptyMedia>
            <EmptyTitle>{title}</EmptyTitle>
            <EmptyDescription>{description}</EmptyDescription>
          </EmptyHeader>
        </Empty>
      </Main>
    </>
  )
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}
