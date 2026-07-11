import { type ElementType, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import {
  AlertTriangle,
  Bot,
  Clock3,
  Database,
  FileText,
  RefreshCw,
  SearchCheck,
  ServerCog,
  ShieldCheck,
  Workflow,
  Users,
} from 'lucide-react'
import {
  Area,
  AreaChart,
  Bar,
  BarChart,
  CartesianGrid,
  XAxis,
  YAxis,
} from 'recharts'
import { motion, useReducedMotion } from 'motion/react'
import { useAuthStore } from '@/stores/auth-store'
import { useWorkspaceStore } from '@/stores/workspace-store'
import { listAdminUsers } from '@/api/admin-users'
import { listAssistantSessions } from '@/api/assistant'
import {
  listDocuments,
  type DocumentListItem,
} from '@/api/documents'
import { getMyGroups } from '@/api/groups'
import { cn } from '@/lib/utils'
import { Header } from '@/components/layout/header'
import { HeaderActions } from '@/components/layout/header-actions'
import { Main } from '@/components/layout/main'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import {
  Avatar,
  AvatarFallback,
} from '@/components/ui/avatar'
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
  ChartContainer,
  ChartLegend,
  ChartLegendContent,
  ChartTooltip,
  ChartTooltipContent,
  type ChartConfig,
} from '@/components/ui/chart'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import {
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs'
import { DocumentStatusBadge } from '@/features/ddrag/document-status-badge'
import { formatDate, formatFileSize } from '@/features/ddrag/shared'

const questionTrend = [
  { day: '周一', questions: 124, answered: 116 },
  { day: '周二', questions: 146, answered: 137 },
  { day: '周三', questions: 138, answered: 128 },
  { day: '周四', questions: 172, answered: 162 },
  { day: '周五', questions: 186, answered: 176 },
  { day: '周六', questions: 92, answered: 88 },
  { day: '周日', questions: 108, answered: 101 },
]

const ingestionTrend = [
  { day: '05-27', ready: 28, failed: 2 },
  { day: '05-28', ready: 34, failed: 1 },
  { day: '05-29', ready: 31, failed: 3 },
  { day: '05-30', ready: 42, failed: 2 },
  { day: '05-31', ready: 38, failed: 4 },
  { day: '06-01', ready: 45, failed: 1 },
  { day: '06-02', ready: 51, failed: 2 },
]

const retrievalQuality = [
  { label: '命中充分', value: 76 },
  { label: '需追问', value: 16 },
  { label: '低置信度', value: 8 },
]

const answerQuality = [
  { label: '准确', value: 91 },
  { label: '有引用', value: 86 },
  { label: '可追溯', value: 79 },
  { label: '需复核', value: 12 },
]

const knowledgeBaseRanking = [
  {
    name: '销售作战知识库',
    owner: '林知远',
    documents: 128,
    health: 96,
    questions: 524,
  },
  {
    name: '客服问题库',
    owner: '梁栖',
    documents: 94,
    health: 91,
    questions: 438,
  },
  {
    name: '研发流程中心',
    owner: '周眠',
    documents: 76,
    health: 88,
    questions: 316,
  },
  {
    name: '财务与合规',
    owner: '唐予',
    documents: 52,
    health: 83,
    questions: 207,
  },
]

const lowConfidenceQuestions = [
  {
    question: '合同自动续约条款需要提前多久通知客户？',
    source: '法务合同审查常见条款库',
    score: 62,
    owner: '宋霁',
  },
  {
    question: '海外供应商准入是否需要额外安全评估？',
    source: '供应商准入评分表',
    score: 58,
    owner: '陆宁',
  },
  {
    question: '搜索服务超时时的第一响应动作是什么？',
    source: '生产事故复盘',
    score: 64,
    owner: '赵临川',
  },
]

const systemServices = [
  {
    name: 'RAG API',
    status: '运行中',
    latency: '182ms',
    uptime: '99.98%',
    variant: 'success' as const,
    icon: ServerCog,
  },
  {
    name: '向量检索',
    status: '运行中',
    latency: '47ms',
    uptime: '99.95%',
    variant: 'success' as const,
    icon: Database,
  },
  {
    name: '文档解析',
    status: '队列繁忙',
    latency: '1.8s',
    uptime: '99.71%',
    variant: 'warning' as const,
    icon: FileText,
  },
  {
    name: '模型网关',
    status: '运行中',
    latency: '634ms',
    uptime: '99.91%',
    variant: 'success' as const,
    icon: Workflow,
  },
]

const mockDocuments: DocumentListItem[] = [
  {
    documentId: -2001,
    groupId: 1,
    fileName: '销售团队季度作战手册.pdf',
    fileExt: 'pdf',
    contentType: 'application/pdf',
    fileSize: 8_742_912,
    status: 'READY',
    failureReason: null,
    uploadedAt: '2026-06-28T09:18:32+08:00',
    uploaderUserId: 101,
    uploaderUserCode: 'U-KNOW-101',
    uploaderDisplayName: '林知远',
    previewText: null,
  },
  {
    documentId: -2002,
    groupId: 2,
    fileName: '客户成功续约风险识别清单.xlsx',
    fileExt: 'xlsx',
    contentType:
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    fileSize: 1_483_776,
    status: 'PROCESSING',
    failureReason: null,
    uploadedAt: '2026-06-26T11:07:44+08:00',
    uploaderUserId: 103,
    uploaderUserCode: 'U-KNOW-103',
    uploaderDisplayName: '陈一白',
    previewText: null,
  },
  {
    documentId: -2003,
    groupId: 3,
    fileName: '生产事故复盘-搜索服务超时.pdf',
    fileExt: 'pdf',
    contentType: 'application/pdf',
    fileSize: 5_412_864,
    status: 'FAILED',
    failureReason: '示例：文档包含扫描图片，OCR 质量不足',
    uploadedAt: '2026-06-23T20:35:29+08:00',
    uploaderUserId: 105,
    uploaderUserCode: 'U-KNOW-105',
    uploaderDisplayName: '赵临川',
    previewText: null,
  },
  {
    documentId: -2004,
    groupId: 4,
    fileName: '法务合同审查常见条款库.docx',
    fileExt: 'docx',
    contentType:
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    fileSize: 3_219_456,
    status: 'READY',
    failureReason: null,
    uploadedAt: '2026-06-20T17:56:18+08:00',
    uploaderUserId: 107,
    uploaderUserCode: 'U-KNOW-107',
    uploaderDisplayName: '宋霁',
    previewText: null,
  },
  {
    documentId: -2005,
    groupId: 5,
    fileName: '客服知识库-账号登录问题.csv',
    fileExt: 'csv',
    contentType: 'text/csv',
    fileSize: 684_032,
    status: 'READY',
    failureReason: null,
    uploadedAt: '2026-06-17T19:28:09+08:00',
    uploaderUserId: 110,
    uploaderUserCode: 'U-KNOW-110',
    uploaderDisplayName: '梁栖',
    previewText: null,
  },
]

const questionChartConfig = {
  questions: {
    label: '提问数',
    color: 'var(--chart-1)',
  },
  answered: {
    label: '成功回答',
    color: 'var(--chart-2)',
  },
} satisfies ChartConfig

const ingestionChartConfig = {
  ready: {
    label: '已入库',
    color: 'var(--chart-1)',
  },
  failed: {
    label: '失败',
    color: 'var(--chart-3)',
  },
} satisfies ChartConfig

const qualityChartConfig = {
  value: {
    label: '占比',
    color: 'var(--chart-2)',
  },
} satisfies ChartConfig

export function Dashboard() {
  const currentUser = useAuthStore((state) => state.auth.user)
  const currentWorkspaceId = useWorkspaceStore(
    (state) => state.currentWorkspaceId
  )
  const groupsQuery = useQuery({
    queryKey: ['groups', 'my'],
    queryFn: getMyGroups,
    enabled: Boolean(currentUser),
  })
  const documentsQuery = useQuery({
    queryKey: ['documents', currentWorkspaceId, 'dashboard'],
    queryFn: () => listDocuments(currentWorkspaceId!),
    enabled: Boolean(currentUser && currentWorkspaceId),
  })
  const sessionsQuery = useQuery({
    queryKey: ['assistant', 'sessions', currentWorkspaceId],
    queryFn: () => listAssistantSessions(currentWorkspaceId!),
    enabled: Boolean(currentUser && currentWorkspaceId),
  })
  const usersQuery = useQuery({
    queryKey: ['admin-users'],
    queryFn: listAdminUsers,
    enabled: currentUser?.systemRole === 'ADMIN',
  })

  const documents = documentsQuery.data?.length
    ? documentsQuery.data
    : mockDocuments
  const groups = [
    ...(groupsQuery.data?.ownedGroups ?? []),
    ...(groupsQuery.data?.joinedGroups ?? []),
  ]
  const knowledgeBaseCount =
    groups.length || new Set(documents.map((item) => item.groupId)).size
  const readyDocuments = documents.filter((item) => item.status === 'READY')
  const processingDocuments = documents.filter(
    (item) => item.status === 'PROCESSING'
  )
  const failedDocuments = documents.filter((item) => item.status === 'FAILED')
  const totalStorage = documents.reduce((total, item) => total + item.fileSize, 0)
  const sessionCount = sessionsQuery.data?.length ?? 24
  const userCount =
    currentUser?.systemRole === 'ADMIN' ? (usersQuery.data?.length ?? 18) : 18
  const todayQuestions = 186 + Math.min(sessionCount, 24)
  const successRate = Math.round(
    (questionTrend.reduce((total, item) => total + item.answered, 0) /
      questionTrend.reduce((total, item) => total + item.questions, 0)) *
      100
  )
  const pendingInvitations = groupsQuery.data?.pendingInvitations.length ?? 0

  const stats = useMemo(
    () => [
      {
        title: '知识库数',
        value: knowledgeBaseCount.toLocaleString(),
        description: `${readyDocuments.length} 个文档已可检索`,
        icon: Database,
        trend: '+3 本周',
      },
      {
        title: '文档数',
        value: documents.length.toLocaleString(),
        description: `累计存储 ${formatFileSize(totalStorage)}`,
        icon: FileText,
        trend: `${processingDocuments.length} 个处理中`,
      },
      {
        title: '今日提问',
        value: todayQuestions.toLocaleString(),
        description: `${sessionCount} 个会话持续活跃`,
        icon: Bot,
        trend: '+12.8%',
      },
      {
        title: '成功回答率',
        value: `${successRate}%`,
        description: `${failedDocuments.length} 个入库问题需处理`,
        icon: SearchCheck,
        trend: '+2.4%',
      },
    ],
    [
      documents.length,
      failedDocuments.length,
      knowledgeBaseCount,
      processingDocuments.length,
      readyDocuments.length,
      sessionCount,
      successRate,
      todayQuestions,
      totalStorage,
    ]
  )

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
      <Main className='flex flex-col gap-4 pt-4 sm:gap-6'>
        <div className='flex flex-wrap items-end justify-between gap-3'>
          <div className='min-w-0'>
            <h1 className='text-2xl font-bold tracking-tight'>运营概览</h1>
          </div>
          <Button variant='outline' size='sm' onClick={refreshAll}>
            <RefreshCw data-icon='inline-start' />
            刷新数据
          </Button>
        </div>

        <div className='grid gap-3 sm:grid-cols-2 xl:grid-cols-4'>
          {stats.map((item) => (
            <StatCard key={item.title} {...item} />
          ))}
        </div>

        <Tabs defaultValue='overview' className='flex flex-col gap-4'>
          <div className='w-full overflow-x-auto pb-1'>
            <TabsList>
              <TabsTrigger value='overview'>概览</TabsTrigger>
              <TabsTrigger value='quality'>质量</TabsTrigger>
              <TabsTrigger value='system'>系统</TabsTrigger>
            </TabsList>
          </div>

          <TabsContent value='overview' className='flex flex-col gap-4'>
            <div className='grid grid-cols-1 gap-4 xl:grid-cols-7'>
              <Card className='xl:col-span-4'>
                <CardHeader>
                  <CardTitle>问答趋势</CardTitle>
                  <CardDescription>
                    近 7 天提问量与成功回答量对比
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <QuestionTrendChart />
                </CardContent>
              </Card>

              <Card className='xl:col-span-3'>
                <CardHeader>
                  <CardTitle>热门知识库</CardTitle>
                  <CardDescription>
                    按近期问答调用量和健康度排序
                  </CardDescription>
                </CardHeader>
                <CardContent className='space-y-5'>
                  {knowledgeBaseRanking.map((item) => (
                    <KnowledgeBaseRankItem key={item.name} item={item} />
                  ))}
                </CardContent>
              </Card>
            </div>

            <div className='grid grid-cols-1 gap-4 xl:grid-cols-7'>
              <Card className='xl:col-span-4'>
                <CardHeader>
                  <CardTitle>最近入库</CardTitle>
                  <CardDescription>
                    最新文档解析状态，失败项可进入知识库页面处理
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <RecentDocumentsTable documents={documents.slice(0, 6)} />
                </CardContent>
              </Card>

              <Card className='xl:col-span-3'>
                <CardHeader>
                  <CardTitle>今日关注</CardTitle>
                  <CardDescription>
                    需要运营人员关注的质量和协作事项
                  </CardDescription>
                </CardHeader>
                <CardContent className='space-y-3'>
                  <AttentionAlert
                    icon={AlertTriangle}
                    title='低置信度问题增加'
                    description='最近 24 小时有 8% 的回答需要补充知识源或人工复核。'
                    tone='warning'
                  />
                  <AttentionAlert
                    icon={Clock3}
                    title='文档解析队列繁忙'
                    description={`${processingDocuments.length || 2} 个文档仍在等待解析与向量化。`}
                  />
                  <AttentionAlert
                    icon={Users}
                    title='协作待处理'
                    description={`${pendingInvitations} 个邀请待处理，当前系统用户 ${userCount} 人。`}
                  />
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          <TabsContent value='quality' className='flex flex-col gap-4'>
            <div className='grid grid-cols-1 gap-4 xl:grid-cols-7'>
              <Card className='xl:col-span-4'>
                <CardHeader>
                  <CardTitle>检索质量分布</CardTitle>
                  <CardDescription>
                    根据召回充分度和回答置信度统计
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <RetrievalQualityChart />
                </CardContent>
              </Card>

              <Card className='xl:col-span-3'>
                <CardHeader>
                  <CardTitle>回答质量指标</CardTitle>
                  <CardDescription>
                    从准确性、引用、可追溯性和复核需求观察
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <AnswerQualityChart />
                </CardContent>
              </Card>
            </div>

            <Card>
              <CardHeader>
                <CardTitle>低置信度问题</CardTitle>
                <CardDescription>
                  优先补齐这些问题对应的知识来源
                </CardDescription>
              </CardHeader>
              <CardContent>
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>问题</TableHead>
                      <TableHead>关联知识源</TableHead>
                      <TableHead>负责人</TableHead>
                      <TableHead className='text-right'>置信度</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {lowConfidenceQuestions.map((item) => (
                      <TableRow key={item.question}>
                        <TableCell className='max-w-[420px] truncate font-medium'>
                          {item.question}
                        </TableCell>
                        <TableCell>{item.source}</TableCell>
                        <TableCell>{item.owner}</TableCell>
                        <TableCell className='text-right'>
                          <Badge variant='warning'>{item.score}%</Badge>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value='system' className='flex flex-col gap-4'>
            <div className='grid gap-4 md:grid-cols-2 xl:grid-cols-4'>
              {systemServices.map((item) => (
                <ServiceCard key={item.name} service={item} />
              ))}
            </div>

            <div className='grid grid-cols-1 gap-4 xl:grid-cols-7'>
              <Card className='xl:col-span-4'>
                <CardHeader>
                  <CardTitle>入库吞吐</CardTitle>
                  <CardDescription>
                    近 7 天文档入库成功与失败趋势
                  </CardDescription>
                </CardHeader>
                <CardContent>
                  <IngestionTrendChart />
                </CardContent>
              </Card>

              <Card className='xl:col-span-3'>
                <CardHeader>
                  <CardTitle>运行摘要</CardTitle>
                  <CardDescription>
                    当前系统服务、权限和队列状态
                  </CardDescription>
                </CardHeader>
                <CardContent className='space-y-4'>
                  <div className='flex items-center gap-3 rounded-lg border p-3'>
                    <ShieldCheck className='size-4 text-primary' />
                    <div className='min-w-0'>
                      <div className='truncate font-medium'>
                        {currentUser?.displayName ?? '未加载用户'}
                      </div>
                      <div className='text-sm text-muted-foreground'>
                        {currentUser?.systemRole ?? '-'} ·{' '}
                        {currentUser?.email ?? '-'}
                      </div>
                    </div>
                  </div>

                  <div className='grid grid-cols-2 gap-3'>
                    <SummaryTile label='活跃成员' value={userCount} />
                    <SummaryTile
                      label='失败文档'
                      value={failedDocuments.length}
                      variant='destructive'
                    />
                    <SummaryTile
                      label='处理中'
                      value={processingDocuments.length}
                      variant='warning'
                    />
                    <SummaryTile label='待处理邀请' value={pendingInvitations} />
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>
        </Tabs>
      </Main>
    </>
  )
}

function StatCard({
  title,
  value,
  description,
  icon: Icon,
  trend,
}: {
  title: string
  value: string
  description: string
  icon: ElementType
  trend: string
}) {
  return (
    <Card size='sm'>
      <CardHeader className='flex flex-row items-center justify-between gap-0 pb-1'>
        <CardTitle className='text-sm font-medium'>{title}</CardTitle>
        <Icon className='size-5 text-muted-foreground' />
      </CardHeader>
      <CardContent className='space-y-1'>
        <div className='flex items-baseline justify-between gap-2'>
          <div className='text-2xl font-semibold tracking-tight'>{value}</div>
          <Badge variant='secondary'>{trend}</Badge>
        </div>
        <p className='text-xs text-muted-foreground'>{description}</p>
      </CardContent>
    </Card>
  )
}

function QuestionTrendChart() {
  return (
    <ChartContainer config={questionChartConfig} className='h-[300px] w-full'>
      <AreaChart accessibilityLayer data={questionTrend}>
        <defs>
          <linearGradient id='fillQuestions' x1='0' y1='0' x2='0' y2='1'>
            <stop
              offset='5%'
              stopColor='var(--color-questions)'
              stopOpacity={0.8}
            />
            <stop
              offset='95%'
              stopColor='var(--color-questions)'
              stopOpacity={0.1}
            />
          </linearGradient>
          <linearGradient id='fillAnswered' x1='0' y1='0' x2='0' y2='1'>
            <stop
              offset='5%'
              stopColor='var(--color-answered)'
              stopOpacity={0.8}
            />
            <stop
              offset='95%'
              stopColor='var(--color-answered)'
              stopOpacity={0.1}
            />
          </linearGradient>
        </defs>
        <CartesianGrid vertical={false} />
        <XAxis
          dataKey='day'
          tickLine={false}
          axisLine={false}
          tickMargin={10}
        />
        <YAxis tickLine={false} axisLine={false} tickMargin={8} width={32} />
        <ChartTooltip
          cursor={false}
          content={<ChartTooltipContent indicator='line' />}
        />
        <Area
          dataKey='questions'
          type='natural'
          fill='url(#fillQuestions)'
          fillOpacity={0.4}
          stroke='var(--color-questions)'
          strokeWidth={2}
        />
        <Area
          dataKey='answered'
          type='natural'
          fill='url(#fillAnswered)'
          fillOpacity={0.4}
          stroke='var(--color-answered)'
          strokeWidth={2}
        />
        <ChartLegend content={<ChartLegendContent />} />
      </AreaChart>
    </ChartContainer>
  )
}

function IngestionTrendChart() {
  return (
    <ChartContainer config={ingestionChartConfig} className='h-[280px] w-full'>
      <BarChart accessibilityLayer data={ingestionTrend}>
        <CartesianGrid vertical={false} />
        <XAxis
          dataKey='day'
          tickLine={false}
          axisLine={false}
          tickMargin={10}
        />
        <YAxis tickLine={false} axisLine={false} tickMargin={8} width={32} />
        <ChartTooltip cursor={false} content={<ChartTooltipContent />} />
        <Bar dataKey='ready' fill='var(--color-ready)' radius={[6, 6, 0, 0]} />
        <Bar dataKey='failed' fill='var(--color-failed)' radius={[6, 6, 0, 0]} />
      </BarChart>
    </ChartContainer>
  )
}

function RetrievalQualityChart() {
  return (
    <ChartContainer config={qualityChartConfig} className='h-[280px] w-full'>
      <BarChart
        accessibilityLayer
        data={retrievalQuality}
        layout='vertical'
        margin={{ right: 36 }}
      >
        <CartesianGrid horizontal={false} />
        <YAxis
          dataKey='label'
          type='category'
          tickLine={false}
          axisLine={false}
          tickMargin={10}
          width={72}
        />
        <XAxis dataKey='value' type='number' hide />
        <ChartTooltip cursor={false} content={<ChartTooltipContent />} />
        <Bar dataKey='value' fill='var(--color-value)' radius={8} />
      </BarChart>
    </ChartContainer>
  )
}

function AnswerQualityChart() {
  return (
    <ChartContainer config={qualityChartConfig} className='h-[280px] w-full'>
      <BarChart accessibilityLayer data={answerQuality}>
        <CartesianGrid vertical={false} />
        <XAxis
          dataKey='label'
          tickLine={false}
          axisLine={false}
          tickMargin={10}
        />
        <YAxis tickLine={false} axisLine={false} tickMargin={8} width={32} />
        <ChartTooltip cursor={false} content={<ChartTooltipContent />} />
        <Bar dataKey='value' fill='var(--color-value)' radius={8} />
      </BarChart>
    </ChartContainer>
  )
}

function KnowledgeBaseRankItem({
  item,
}: {
  item: (typeof knowledgeBaseRanking)[number]
}) {
  return (
    <div className='space-y-2'>
      <div className='flex items-start justify-between gap-3'>
        <div className='min-w-0'>
          <div className='truncate text-sm font-medium'>{item.name}</div>
          <div className='text-xs text-muted-foreground'>
            {item.owner} · {item.documents} 篇文档
          </div>
        </div>
        <Badge variant='secondary'>{item.questions} 问</Badge>
      </div>
      <ProgressLine value={item.health} />
    </div>
  )
}

function RecentDocumentsTable({
  documents,
}: {
  documents: DocumentListItem[]
}) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>文件</TableHead>
          <TableHead>状态</TableHead>
          <TableHead>大小</TableHead>
          <TableHead>上传人</TableHead>
          <TableHead>上传时间</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {documents.map((document) => (
          <TableRow key={document.documentId}>
            <TableCell className='max-w-[280px] truncate font-medium'>
              {document.fileName}
            </TableCell>
            <TableCell>
              <DocumentStatusBadge status={document.status} />
            </TableCell>
            <TableCell>{formatFileSize(document.fileSize)}</TableCell>
            <TableCell>
              <div className='flex items-center gap-2'>
                <Avatar size='sm'>
                  <AvatarFallback>
                    {document.uploaderDisplayName.slice(0, 1)}
                  </AvatarFallback>
                </Avatar>
                {document.uploaderDisplayName}
              </div>
            </TableCell>
            <TableCell>{formatDate(document.uploadedAt)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  )
}

function AttentionAlert({
  icon: Icon,
  title,
  description,
  tone = 'default',
}: {
  icon: ElementType
  title: string
  description: string
  tone?: 'default' | 'warning'
}) {
  return (
    <Alert>
      <Icon
        className={cn(
          'size-4',
          tone === 'warning' ? 'text-warning' : 'text-muted-foreground'
        )}
      />
      <AlertTitle>{title}</AlertTitle>
      <AlertDescription>{description}</AlertDescription>
    </Alert>
  )
}

function ServiceCard({
  service,
}: {
  service: (typeof systemServices)[number]
}) {
  const Icon = service.icon

  return (
    <Card size='sm'>
      <CardHeader className='flex flex-row items-center justify-between gap-3'>
        <div>
          <CardTitle className='text-sm'>{service.name}</CardTitle>
          <CardDescription>{service.status}</CardDescription>
        </div>
        <Icon className='size-5 text-muted-foreground' />
      </CardHeader>
      <CardContent className='space-y-3'>
        <div className='flex items-center justify-between text-sm'>
          <span className='text-muted-foreground'>延迟</span>
          <span className='font-medium'>{service.latency}</span>
        </div>
        <div className='flex items-center justify-between text-sm'>
          <span className='text-muted-foreground'>可用性</span>
          <Badge variant={service.variant}>{service.uptime}</Badge>
        </div>
      </CardContent>
    </Card>
  )
}

function SummaryTile({
  label,
  value,
  variant = 'default',
}: {
  label: string
  value: number
  variant?: 'default' | 'warning' | 'destructive'
}) {
  return (
    <div className='rounded-lg border p-3'>
      <div
        className={cn(
          'text-2xl font-semibold',
          variant === 'warning' && 'text-warning',
          variant === 'destructive' && 'text-destructive'
        )}
      >
        {value}
      </div>
      <div className='text-sm text-muted-foreground'>{label}</div>
    </div>
  )
}

function ProgressLine({ value }: { value: number }) {
  const shouldReduceMotion = useReducedMotion()

  return (
    <div className='h-2 overflow-hidden rounded-full bg-muted'>
      <motion.div
        className='h-full rounded-full bg-primary'
        initial={shouldReduceMotion ? false : { width: '0%' }}
        animate={{ width: `${value}%` }}
        transition={{ duration: 0.65, ease: [0.22, 1, 0.36, 1] }}
      />
    </div>
  )
}
