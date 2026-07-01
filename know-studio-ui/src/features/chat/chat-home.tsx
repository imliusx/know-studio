import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type UIEvent,
} from 'react'
import { Link } from '@tanstack/react-router'
import Typed from 'typed.js'
import {
  AnimatePresence,
  LayoutGroup,
  motion,
  useReducedMotion,
} from 'motion/react'
import { HeaderActions } from '@/components/layout/header-actions'
import { sidebarData } from '@/components/layout/data/sidebar-data'
import { TeamSwitcher } from '@/components/layout/team-switcher'
import { useLayout, type Collapsible } from '@/context/layout-provider'
import { getCookie } from '@/lib/cookies'
import {
  Archive,
  ArchiveRestore,
  ArrowUpIcon,
  CheckCircle,
  Circle,
  Clipboard,
  Database,
  Download,
  Edit3,
  FileSearch,
  FileUp,
  Inbox,
  MessageSquare,
  MoreHorizontal,
  Paperclip,
  Pin,
  PinOff,
  RotateCcw,
  Search,
  Settings,
  SquarePen,
  Square,
  Star,
  StarOff,
  Trash2,
  X,
} from 'lucide-react'
import {
  ChainOfThought,
  ChainOfThoughtContent,
  ChainOfThoughtItem,
  ChainOfThoughtStep,
  ChainOfThoughtTrigger,
} from '@/components/ui/chain-of-thought'
import {
  ChatContainerContent,
  ChatContainerRoot,
  ChatContainerScrollAnchor,
} from '@/components/ui/chat-container'
import { CodeBlock, CodeBlockCode, CodeBlockGroup } from '@/components/ui/code-block'
import { FeedbackBar } from '@/components/ui/feedback-bar'
import {
  FileUpload,
  FileUploadContent,
  FileUploadTrigger,
} from '@/components/ui/file-upload'
import { Image } from '@/components/ui/image'
import { JSXPreview } from '@/components/ui/jsx-preview'
import { Loader } from '@/components/ui/loader'
import {
  Message,
  MessageAction,
  MessageActions,
  MessageContent,
} from '@/components/ui/message'
import {
  PromptInput,
  PromptInputAction,
  PromptInputActions,
  PromptInputTextarea,
} from '@/components/ui/prompt-input'
import { PromptSuggestion } from '@/components/ui/prompt-suggestion'
import { ResponseStream } from '@/components/ui/response-stream'
import { ScrollButton } from '@/components/ui/scroll-button'
import { Source, SourceContent, SourceTrigger } from '@/components/ui/source'
import { Steps, StepsContent, StepsItem, StepsTrigger } from '@/components/ui/steps'
import { SystemMessage } from '@/components/ui/system-message'
import { TextShimmer } from '@/components/ui/text-shimmer'
import { ThinkingBar } from '@/components/ui/thinking-bar'
import { Tool } from '@/components/ui/tool'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogMedia,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip'
import {
  Empty,
  EmptyContent,
  EmptyDescription,
  EmptyHeader,
  EmptyMedia,
  EmptyTitle,
} from '@/components/ui/empty'
import { Input } from '@/components/ui/input'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInput,
  SidebarInset,
  SidebarMenu,
  SidebarMenuAction,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarRail,
  SidebarTrigger,
  useSidebar,
} from '@/components/ui/sidebar'
import {
  Tabs,
  TabsList,
  TabsTrigger,
} from '@/components/ui/tabs'
import { cn } from '@/lib/utils'
import { toast } from 'sonner'

type ChatMessage = {
  id: number
  role: 'user' | 'assistant'
  content: string
  files?: string[]
  variant?: 'rag' | 'code' | 'document'
  isStreaming?: boolean
}

type ChatConversation = {
  id: string
  title: string
  description: string
  updatedAt: string
  createdAt?: number
  updatedAtValue?: number
  messages: ChatMessage[]
  isPinned?: boolean
  isFavorited?: boolean
  isArchived?: boolean
}

type ChatState = {
  conversations: ChatConversation[]
  activeConversationId: string | null
}

type ChatSidebarView = 'active' | 'favorites' | 'archived'
type ChatSidebarVariant = 'inset' | 'sidebar' | 'floating'

const CHAT_STORAGE_KEY = 'know-studio.chat-ui.conversations'
const DEFAULT_CONVERSATION_TITLE = '新的对话'
const LEGACY_DEFAULT_CONVERSATION_TITLE = '新对话'
const CHAT_EMPTY_TITLES = [
  '企业知识，一问即达',
  '今天想知道什么？',
  '问任何问题',
  '让 AI 帮你查找答案',
  '知识，即刻抵达',
  '连接你的知识宇宙',
  'Ask Anything',
  '与知识对话',
]
const CHAT_EMPTY_TITLE_PLACEHOLDER = CHAT_EMPTY_TITLES.reduce((longest, title) =>
  title.length > longest.length ? title : longest
)
const DEMO_NOW = Date.now()
const CHAT_SIDEBAR_TEAMS = [
  {
    ...sidebarData.teams[0],
    name: 'Know Studio',
    plan: 'Chat Workspace',
  },
]

const suggestions = [
  '从知识库里找出客户流失的主要原因',
  '把这份上传文档整理成会议纪要和待办',
  '对比两版方案的差异、风险和适用场景',
  '根据接口报错日志生成排查步骤',
]

function ChatHeroTitle() {
  const reduceMotion = useReducedMotion()
  const typedElementRef = useRef<HTMLSpanElement>(null)

  useEffect(() => {
    if (reduceMotion || !typedElementRef.current) return

    const typed = new Typed(typedElementRef.current, {
      strings: CHAT_EMPTY_TITLES,
      typeSpeed: 125,
      backSpeed: 100,
      backDelay: 3000,
      startDelay: 450,
      smartBackspace: false,
      shuffle: true,
      loop: true,
      showCursor: true,
      cursorChar: '_',
    })

    return () => {
      typed.destroy()
    }
  }, [reduceMotion])

  return (
    <motion.h1
      aria-label='企业知识问答助手'
      className='text-2xl leading-tight font-semibold text-foreground sm:text-4xl'
      initial={
        reduceMotion
          ? { opacity: 1 }
          : { opacity: 0, y: 10, filter: 'blur(6px)' }
      }
      animate={
        reduceMotion
          ? { opacity: 1 }
          : {
              opacity: 1,
              y: 0,
              filter: 'blur(0px)',
            }
      }
      transition={
        reduceMotion
          ? undefined
          : {
              opacity: { duration: 0.35 },
              y: { duration: 0.5, ease: [0.22, 1, 0.36, 1] },
              filter: { duration: 0.5 },
            }
      }
    >
      <span className='relative inline-grid'>
        <span
          aria-hidden='true'
          className='invisible col-start-1 row-start-1 whitespace-nowrap'
        >
          {CHAT_EMPTY_TITLE_PLACEHOLDER}
        </span>
        <span className='col-start-1 row-start-1 whitespace-nowrap text-left [&_.typed-cursor]:ml-1 [&_.typed-cursor]:font-semibold [&_.typed-cursor]:text-foreground'>
          {reduceMotion ? (
            CHAT_EMPTY_TITLES[0]
          ) : (
            <span ref={typedElementRef} aria-hidden='true' />
          )}
        </span>
      </span>
    </motion.h1>
  )
}

const generatedDiagram =
  'PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI5NjAiIGhlaWdodD0iNDgwIiB2aWV3Qm94PSIwIDAgOTYwIDQ4MCI+PHJlY3Qgd2lkdGg9Ijk2MCIgaGVpZ2h0PSI0ODAiIGZpbGw9IiNmOGZhZmMiLz48cmVjdCB4PSI5NiIgeT0iMTA0IiB3aWR0aD0iMTkyIiBoZWlnaHQ9IjEyOCIgcng9IjE4IiBmaWxsPSIjZmZmIiBzdHJva2U9IiNlMmU4ZjAiLz48cmVjdCB4PSIzODQiIHk9IjEwNCIgd2lkdGg9IjE5MiIgaGVpZ2h0PSIxMjgiIHJ4PSIxOCIgZmlsbD0iI2ZmZiIgc3Ryb2tlPSIjZTJlOGYwIi8+PHJlY3QgeD0iNjcyIiB5PSIxMDQiIHdpZHRoPSIxOTIiIGhlaWdodD0iMTI4IiByeD0iMTgiIGZpbGw9IiNmZmYiIHN0cm9rZT0iI2UyZThmMCIvPjxyZWN0IHg9IjI0MCIgeT0iMjkyIiB3aWR0aD0iMTkyIiBoZWlnaHQ9Ijg0IiByeD0iMTgiIGZpbGw9IiMxODE4MWIiLz48cmVjdCB4PSI1MjgiIHk9IjI5MiIgd2lkdGg9IjE5MiIgaGVpZ2h0PSI4NCIgcng9IjE4IiBmaWxsPSIjMTgxODFiIi8+PHRleHQgeD0iMTkyIiB5PSIxNTYiIGZvbnQtZmFtaWx5PSJBcmlhbCwgc2Fucy1zZXJpZiIgZm9udC1zaXplPSIyNCIgZm9udC13ZWlnaHQ9IjcwMCIgZmlsbD0iIzE4MTgxYiIgdGV4dC1hbmNob3I9Im1pZGRsZSI+RG9jdW1lbnRzPC90ZXh0Pjx0ZXh0IHg9IjQ4MCIgeT0iMTU2IiBmb250LWZhbWlseT0iQXJpYWwsIHNhbnMtc2VyaWYiIGZvbnQtc2l6ZT0iMjQiIGZvbnQtd2VpZ2h0PSI3MDAiIGZpbGw9IiMxODE4MWIiIHRleHQtYW5jaG9yPSJtaWRkbGUiPlJldHJpZXZhbDwvdGV4dD48dGV4dCB4PSI3NjgiIHk9IjE1NiIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjI0IiBmb250LXdlaWdodD0iNzAwIiBmaWxsPSIjMTgxODFiIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj5BbnN3ZXI8L3RleHQ+PHRleHQgeD0iMzM2IiB5PSIzNDIiIGZvbnQtZmFtaWx5PSJBcmlhbCwgc2Fucy1zZXJpZiIgZm9udC1zaXplPSIyMiIgZm9udC13ZWlnaHQ9IjcwMCIgZmlsbD0iI2ZmZiIgdGV4dC1hbmNob3I9Im1pZGRsZSI+U291cmNlczwvdGV4dD48dGV4dCB4PSI2MjQiIHk9IjM0MiIgZm9udC1mYW1pbHk9IkFyaWFsLCBzYW5zLXNlcmlmIiBmb250LXNpemU9IjIyIiBmb250LXdlaWdodD0iNzAwIiBmaWxsPSIjZmZmIiB0ZXh0LWFuY2hvcj0ibWlkZGxlIj5GZWVkYmFjazwvdGV4dD48cGF0aCBkPSJNMjg4IDE2OGg5NiIgZmlsbD0ibm9uZSIgc3Ryb2tlPSIjOTRhM2IzIiBzdHJva2Utd2lkdGg9IjYiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPjxwYXRoIGQ9Ik01NzYgMTY4aDk2IiBmaWxsPSJub25lIiBzdHJva2U9IiM5NGEzYjMiIHN0cm9rZS13aWR0aD0iNiIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+PHBhdGggZD0iTTQ4MCAyMzJ2NjBNNDMyIDMzNGg5Nk02MjQgMjMydjYwIiBmaWxsPSJub25lIiBzdHJva2U9IiM5NGEzYjMiIHN0cm9rZS13aWR0aD0iNiIgc3Ryb2tlLWxpbmVjYXA9InJvdW5kIi8+PC9zdmc+'

const codeSample = `export async function searchKnowledgeBase(query: string) {
  const response = await fetch('/api/v1/rag/query', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ query, topK: 6, rerank: true }),
  })

  if (!response.ok) {
    throw new Error('Knowledge base search failed')
  }

  return response.json()
}`

const artifactJsx = `
<div className="grid gap-3 rounded-lg border bg-background p-4 text-sm">
  <div className="flex items-center justify-between">
    <strong>文档解析队列</strong>
    <span className="rounded-lg bg-secondary px-2 py-1 text-xs">mock</span>
  </div>
  <div className="grid grid-cols-3 gap-2">
    <div className="rounded-lg border p-3">
      <div className="text-2xl font-semibold">128</div>
      <div className="text-muted-foreground">chunks</div>
    </div>
    <div className="rounded-lg border p-3">
      <div className="text-2xl font-semibold">94%</div>
      <div className="text-muted-foreground">indexed</div>
    </div>
    <div className="rounded-lg border p-3">
      <div className="text-2xl font-semibold">6</div>
      <div className="text-muted-foreground">sources</div>
    </div>
  </div>
</div>`

const initialMessages: ChatMessage[] = [
  {
    id: 1,
    role: 'user',
    content: '帮我总结知识库问答链路，并展示检索来源和执行过程。',
    files: ['产品需求.md', '接口说明.pdf'],
  },
  {
    id: 2,
    role: 'assistant',
    variant: 'rag',
    isStreaming: true,
    content:
      '可以。当前 Chat UI 会先围绕知识库问答的真实使用路径组织内容：上传资料、解析入库、混合检索、证据重排、基于来源生成回答，最后保留反馈入口。页面里的工具调用、引用来源、执行步骤、代码块和产物预览都直接嵌在对话消息中，后续只需要把 mock 数据替换成后端接口返回的数据。',
  },
]

const initialConversations: ChatConversation[] = [
  {
    id: 'rag-overview',
    title: 'RAG 总览：从上传到回答',
    description: '来源、工具、执行步骤',
    updatedAt: '刚刚',
    createdAt: DEMO_NOW - 1000 * 60 * 20,
    updatedAtValue: DEMO_NOW,
    isPinned: true,
    isFavorited: true,
    messages: initialMessages,
  },
  {
    id: 'api-adapter',
    title: '检索 API',
    description: '前端调用模型',
    updatedAt: '12 分钟前',
    createdAt: DEMO_NOW - 1000 * 60 * 80,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 12,
    isPinned: false,
    isFavorited: true,
    messages: [
      {
        id: 101,
        role: 'user',
        content: '写一段后端检索接口示例。',
      },
      {
        id: 102,
        role: 'assistant',
        variant: 'code',
        content:
          '下面先给出前端可以直接对接的检索接口调用模型。真实接入时，建议后端返回 answer、sources、toolCalls 和 feedbackId，前端按当前消息结构渲染即可。',
      },
    ],
  },
  {
    id: 'document-status',
    title: '解析队列',
    description: '队列、切分、索引进度',
    updatedAt: '昨天',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 26,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24,
    isPinned: false,
    isFavorited: false,
    messages: [
      {
        id: 201,
        role: 'user',
        content: '生成一个文档解析状态面板。',
        files: ['合同样例.docx'],
      },
      {
        id: 202,
        role: 'assistant',
        variant: 'document',
        content:
          '文档解析页面可以先展示队列状态、切分数量、索引进度和失败项。这里先用 mock 产物预览承载结构，后续接入后端解析状态即可。',
      },
    ],
  },
  {
    id: 'contract-risk',
    title: '合同里的风险',
    description: '付款、违约、交付边界',
    updatedAt: '2 小时前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 8,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 2,
    isPinned: true,
    isFavorited: false,
    messages: [
      {
        id: 301,
        role: 'user',
        content: '帮我从合同知识库里找出付款、违约和交付相关风险。',
        files: ['年度采购合同.pdf'],
      },
      {
        id: 302,
        role: 'assistant',
        content:
          '已按条款类型聚合风险点：付款节点需要补充验收口径，违约责任存在单边约束，交付范围建议增加附件版本号和变更流程。',
      },
    ],
  },
  {
    id: 'customer-churn',
    title: '近两季客户为什么流失',
    description: '工单、回访、续费记录',
    updatedAt: '3 小时前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 14,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 3,
    isPinned: false,
    isFavorited: true,
    messages: [
      {
        id: 401,
        role: 'user',
        content: '从近两季度客户反馈里总结流失原因，按影响程度排序。',
      },
      {
        id: 402,
        role: 'assistant',
        content:
          '高频原因集中在响应时效、权限配置复杂、迁移成本和培训不足。建议先处理企业版客户的工单响应 SLA，并补齐迁移手册。',
      },
    ],
  },
  {
    id: 'employee-onboarding',
    title: '新人入职',
    description: '权限、设备、流程',
    updatedAt: '5 小时前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 28,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 5,
    isPinned: false,
    isFavorited: false,
    messages: [
      {
        id: 501,
        role: 'user',
        content: '整理一份新人第一天需要完成的入职清单。',
      },
      {
        id: 502,
        role: 'assistant',
        content:
          '第一天建议按账号开通、设备验收、组织架构、信息安全培训和直属团队同步五个步骤推进，并在系统里确认每项负责人。',
      },
    ],
  },
  {
    id: 'incident-review',
    title: '登录故障复盘摘要',
    description: '日志、影响面、改进项',
    updatedAt: '昨天',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 42,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 30,
    isPinned: false,
    isFavorited: true,
    messages: [
      {
        id: 601,
        role: 'user',
        content: '根据故障记录生成一份复盘摘要，重点写根因和改进项。',
        files: ['gateway-error.log'],
      },
      {
        id: 602,
        role: 'assistant',
        content:
          '根因是网关连接池耗尽后重试放大流量，影响登录和文档上传。改进项包括熔断阈值、连接池告警、灰度压测和回滚演练。',
      },
    ],
  },
  {
    id: 'sales-playbook',
    title: '销售话术：竞品异议怎么答',
    description: '竞品、异议、案例',
    updatedAt: '2 天前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 72,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 48,
    isPinned: false,
    isFavorited: false,
    messages: [
      {
        id: 701,
        role: 'user',
        content: '客户问我们和竞品的差异，帮我从销售知识库里整理回答。',
      },
      {
        id: 702,
        role: 'assistant',
        content:
          '可以围绕私有化部署、权限隔离、混合检索和证据引用展开。避免泛泛比较，优先引用已成交客户的业务场景。',
      },
    ],
  },
  {
    id: 'policy-search',
    title: '差旅报销',
    description: '差旅、票据、审批',
    updatedAt: '3 天前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 96,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 72,
    isPinned: false,
    isFavorited: false,
    messages: [
      {
        id: 801,
        role: 'user',
        content: '出差期间打车和酒店发票分别需要哪些材料？',
      },
      {
        id: 802,
        role: 'assistant',
        content:
          '打车需行程单和发票，酒店需住宿发票、入住明细和审批单。跨城市行程建议补充会议邀请或客户拜访记录。',
      },
    ],
  },
  {
    id: 'meeting-actions',
    title: '项目例会待办拆解',
    description: '决议、负责人、截止时间',
    updatedAt: '4 天前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 120,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 96,
    isPinned: false,
    isFavorited: true,
    messages: [
      {
        id: 901,
        role: 'user',
        content: '把这份会议记录整理成决议和待办。',
        files: ['项目例会记录.md'],
      },
      {
        id: 902,
        role: 'assistant',
        content:
          '已拆成三项决议和六个待办：知识库权限方案本周确认，上传体验下周提测，问答评测集由产品和实施共同补充。',
      },
    ],
  },
  {
    id: 'permission-design',
    title: '跨部门知识库权限隔离方案',
    description: '组织、成员、可见范围',
    updatedAt: '5 天前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 144,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 120,
    isPinned: false,
    isFavorited: false,
    messages: [
      {
        id: 1001,
        role: 'user',
        content: '如何设计知识库的组织权限，避免跨部门误检索？',
      },
      {
        id: 1002,
        role: 'assistant',
        content:
          '建议把系统角色和知识库成员角色拆开：管理员管平台，OWNER 管空间，MEMBER 只读检索。检索接口必须带 groupId 做硬隔离。',
      },
    ],
  },
  {
    id: 'rag-evaluation',
    title: '问答评测集',
    description: '命中率、拒答、引用质量',
    updatedAt: '上周',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 24 * 9,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24 * 7,
    isPinned: false,
    isFavorited: false,
    isArchived: true,
    messages: [
      {
        id: 1101,
        role: 'user',
        content: '帮我设计一套 RAG 问答评测指标。',
      },
      {
        id: 1102,
        role: 'assistant',
        content:
          '建议评估检索命中、证据覆盖、引用准确、拒答合理性和回答可读性。每类准备正例、相似干扰和无答案问题。',
      },
    ],
  },
  {
    id: 'migration-plan',
    title: '旧库迁移到 Know Studio 前的检查清单',
    description: '字段映射、批量导入',
    updatedAt: '上周',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 24 * 10,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24 * 8,
    isPinned: false,
    isFavorited: true,
    isArchived: true,
    messages: [
      {
        id: 1201,
        role: 'user',
        content: '旧知识库迁移到新系统需要注意什么？',
      },
      {
        id: 1202,
        role: 'assistant',
        content:
          '迁移前先统一文档归属、权限、文件哈希和版本号。导入后抽样检查解析文本、chunk 数量、向量索引和关键词索引一致性。',
      },
    ],
  },
  {
    id: 'pricing-faq',
    title: '价格口径',
    description: '套餐、折扣、续费',
    updatedAt: '9 天前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 24 * 12,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24 * 9,
    isPinned: false,
    isFavorited: false,
    messages: [
      {
        id: 1301,
        role: 'user',
        content: '客户问企业版怎么收费，哪些信息可以直接对外说？',
      },
      {
        id: 1302,
        role: 'assistant',
        content:
          '可以直接说明按账号、知识库容量和私有化部署范围报价。折扣、赠送额度和定制条款需要销售负责人确认后再发出。',
      },
    ],
  },
  {
    id: 'release-note',
    title: '这周发版说明怎么写',
    description: '更新点、风险、回滚',
    updatedAt: '10 天前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 24 * 13,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24 * 10,
    isPinned: false,
    isFavorited: true,
    messages: [
      {
        id: 1401,
        role: 'user',
        content: '把本周更新整理成给客户看的发版说明，不要太技术化。',
      },
      {
        id: 1402,
        role: 'assistant',
        content:
          '本周重点可以写成三块：文档上传更稳定、问答来源更清晰、管理后台操作更顺手。技术细节放到内部变更记录。',
      },
    ],
  },
  {
    id: 'security-check',
    title: '安全检查项',
    description: '账号、权限、审计',
    updatedAt: '11 天前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 24 * 14,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24 * 11,
    isPinned: false,
    isFavorited: false,
    messages: [
      {
        id: 1501,
        role: 'user',
        content: '上线前帮我列一份安全检查清单。',
      },
      {
        id: 1502,
        role: 'assistant',
        content:
          '上线前重点检查默认密码、JWT 密钥、对象存储权限、跨组检索隔离、管理员操作审计和外部端口暴露范围。',
      },
    ],
  },
  {
    id: 'support-reply',
    title: '给客户的一封解释邮件',
    description: '语气、原因、处理方案',
    updatedAt: '12 天前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 24 * 15,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24 * 12,
    isPinned: false,
    isFavorited: false,
    messages: [
      {
        id: 1601,
        role: 'user',
        content: '客户反馈问答结果没引用来源，帮我写一封解释和处理邮件。',
      },
      {
        id: 1602,
        role: 'assistant',
        content:
          '邮件建议先承认影响，再说明原因是部分文档未完成索引，最后给出重建索引、补充引用展示和回访时间。',
      },
    ],
  },
  {
    id: 'procurement-comparison',
    title: '采购方案 A/B 对比表',
    description: '成本、周期、风险',
    updatedAt: '13 天前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 24 * 16,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24 * 13,
    isPinned: false,
    isFavorited: true,
    messages: [
      {
        id: 1701,
        role: 'user',
        content: '根据两份采购方案，总结成本、交付周期和主要风险。',
        files: ['方案A.xlsx', '方案B.xlsx'],
      },
      {
        id: 1702,
        role: 'assistant',
        content:
          '方案 A 成本低但交付周期长，方案 B 交付快但后续维护成本更高。若项目窗口固定，建议优先评估方案 B 的维护条款。',
      },
    ],
  },
  {
    id: 'data-retention',
    title: '数据保留多久合适？',
    description: '归档、删除、合规',
    updatedAt: '两周前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 24 * 18,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24 * 14,
    isPinned: false,
    isFavorited: false,
    messages: [
      {
        id: 1801,
        role: 'user',
        content: '企业知识库里的历史会话和上传文档一般保留多久？',
      },
      {
        id: 1802,
        role: 'assistant',
        content:
          '建议分层设置：活跃知识长期保留，历史会话按 180 天归档，包含敏感信息的临时材料按项目周期自动清理。',
      },
    ],
  },
  {
    id: 'okr-draft',
    title: 'Q3 OKR 草稿',
    description: '目标、关键结果',
    updatedAt: '两周前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 24 * 20,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24 * 15,
    isPinned: false,
    isFavorited: false,
    isArchived: true,
    messages: [
      {
        id: 1901,
        role: 'user',
        content: '结合产品路线图，帮我起草 Q3 的知识库产品 OKR。',
      },
      {
        id: 1902,
        role: 'assistant',
        content:
          '目标可以聚焦“让企业知识可验证、可治理、可持续运营”。关键结果围绕接入数量、检索质量、引用准确率和管理后台效率。',
      },
    ],
  },
  {
    id: 'short-question',
    title: '服务 SLA',
    description: '响应时间',
    updatedAt: '半个月前',
    createdAt: DEMO_NOW - 1000 * 60 * 60 * 24 * 22,
    updatedAtValue: DEMO_NOW - 1000 * 60 * 60 * 24 * 16,
    isPinned: false,
    isFavorited: false,
    isArchived: true,
    messages: [
      {
        id: 2001,
        role: 'user',
        content: '企业版 SLA 怎么定义比较合适？',
      },
      {
        id: 2002,
        role: 'assistant',
        content:
          '可以按故障等级拆分响应和恢复目标：P0 立即响应，P1 一小时内响应，普通问题按工作日 SLA 处理。',
      },
    ],
  },
]

function createEmptyConversation(): ChatConversation {
  const now = Date.now()
  return {
    id: `chat-${Date.now()}`,
    title: DEFAULT_CONVERSATION_TITLE,
    description: '开始知识库问答',
    updatedAt: '刚刚',
    createdAt: now,
    updatedAtValue: now,
    isPinned: false,
    isFavorited: false,
    isArchived: false,
    messages: [],
  }
}

function formatRelativeTime(value?: number, fallback = '刚刚') {
  if (!value) return fallback

  const diff = Date.now() - value
  const minute = 1000 * 60
  const hour = minute * 60
  const day = hour * 24

  if (diff < minute) return '刚刚'
  if (diff < hour) return `${Math.floor(diff / minute)} 分钟前`
  if (diff < day) return `${Math.floor(diff / hour)} 小时前`
  if (diff < day * 2) return '昨天'
  return `${Math.floor(diff / day)} 天前`
}

function normalizeConversations(conversations: ChatConversation[]) {
  if (conversations.length === 0) return []

  const normalizedConversations = conversations
    .map((conversation) => ({
      ...conversation,
      createdAt: conversation.createdAt ?? Date.now(),
      updatedAtValue: conversation.updatedAtValue ?? Date.now(),
      updatedAt: formatRelativeTime(
        conversation.updatedAtValue,
        conversation.updatedAt
      ),
      isPinned: Boolean(conversation.isPinned),
      isFavorited: Boolean(conversation.isFavorited),
      isArchived: Boolean(conversation.isArchived),
    }))
    .filter((conversation) => !isDraftConversation(conversation))

  return normalizedConversations
}

function loadConversations() {
  if (typeof window === 'undefined') return initialConversations

  try {
    const stored = window.localStorage.getItem(CHAT_STORAGE_KEY)
    if (!stored) return initialConversations
    const parsed = JSON.parse(stored) as ChatConversation[]
    if (!Array.isArray(parsed)) return initialConversations
    return normalizeConversations(parsed)
  } catch {
    return initialConversations
  }
}

function getInitialActiveConversationId(conversations: ChatConversation[]) {
  return (
    conversations.find((conversation) => !conversation.isArchived)?.id ??
    conversations[0]?.id ??
    null
  )
}

function isDefaultConversationTitle(title: string) {
  return (
    title === DEFAULT_CONVERSATION_TITLE ||
    title === LEGACY_DEFAULT_CONVERSATION_TITLE
  )
}

function isDraftConversation(conversation: ChatConversation) {
  return conversation.messages.length === 0
}

function orderConversations(conversations: ChatConversation[]) {
  return [...conversations].sort((a, b) => {
    if (a.isArchived !== b.isArchived) return a.isArchived ? 1 : -1
    if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1
    return (b.updatedAtValue ?? 0) - (a.updatedAtValue ?? 0)
  })
}

function touchConversation<T extends ChatConversation>(conversation: T): T {
  const now = Date.now()
  return {
    ...conversation,
    updatedAt: '刚刚',
    updatedAtValue: now,
  }
}

function createInitialChatState(): ChatState {
  const conversations = orderConversations(loadConversations())

  return {
    conversations,
    activeConversationId: null,
  }
}

function createAssistantMessage(input: string, id = Date.now() + 1): ChatMessage {
  const normalizedInput = input.toLowerCase()
  const variant = normalizedInput.includes('代码') || normalizedInput.includes('接口')
    ? 'code'
    : normalizedInput.includes('文档') || normalizedInput.includes('解析')
      ? 'document'
      : 'rag'

  return {
    id,
    role: 'assistant',
    variant,
    isStreaming: true,
    content:
      variant === 'code'
        ? '下面先给出前端可以直接对接的检索接口调用模型。真实接入时，建议后端返回 answer、sources、toolCalls 和 feedbackId，前端按当前消息结构渲染即可。'
        : variant === 'document'
          ? '文档解析页面可以先展示队列状态、切分数量、索引进度和失败项。这里先用 mock 产物预览承载结构，后续接入后端解析状态即可。'
          : '我会按 RAG 问答链路回答：先检索知识库，再整理证据，最后给出可复核的结论和来源。当前页面使用 mock 数据，组件已经直接出现在 Chat UI 消息内。',
  }
}

export function ChatHome() {
  const defaultOpen = getCookie('sidebar_state') !== 'false'
  const { collapsible, variant } = useLayout()
  const reduceMotion = useReducedMotion()
  const [initialChatState] = useState(createInitialChatState)
  const [input, setInput] = useState('')
  const [files, setFiles] = useState<File[]>([])
  const [isHeaderGlass, setIsHeaderGlass] = useState(false)
  const messageIdRef = useRef(DEMO_NOW)
  const [conversations, setConversations] = useState<ChatConversation[]>(
    initialChatState.conversations
  )
  const [activeConversationId, setActiveConversationId] = useState(
    initialChatState.activeConversationId
  )

  const activeConversation =
    conversations.find((conversation) => conversation.id === activeConversationId) ??
    null
  const messages = activeConversation?.messages ?? []
  const hasMessages = messages.length > 0
  const isStreaming = messages.some((message) => message.isStreaming)
  const activeConversationTitle =
    activeConversation?.title ?? DEFAULT_CONVERSATION_TITLE

  useEffect(() => {
    window.localStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(conversations))
  }, [conversations])

  function handleFilesAdded(nextFiles: File[]) {
    setFiles((prev) => [...prev, ...nextFiles])
  }

  function createNextMessageId() {
    messageIdRef.current += 1
    return messageIdRef.current
  }

  function handleRemoveFile(index: number) {
    setFiles((prev) => prev.filter((_, currentIndex) => currentIndex !== index))
  }

  function handleChatScroll(event: UIEvent<HTMLDivElement>) {
    const nextIsHeaderGlass = event.currentTarget.scrollTop > 8
    setIsHeaderGlass((current) =>
      current === nextIsHeaderGlass ? current : nextIsHeaderGlass
    )
  }

  function markMessageComplete(messageId: number) {
    setConversations((prev) =>
      orderConversations(
        prev.map((conversation) => ({
          ...(conversation.messages.some((message) => message.id === messageId)
            ? touchConversation(conversation)
            : conversation),
          messages: conversation.messages.map((message) =>
            message.id === messageId ? { ...message, isStreaming: false } : message
          ),
        }))
      )
    )
  }

  function handleNewConversation() {
    setActiveConversationId(null)
    setInput('')
    setFiles([])
    toast.success('已准备新对话')
  }

  function handleSelectConversation(conversationId: string) {
    setActiveConversationId(conversationId)
    setInput('')
    setFiles([])
  }

  function handleDeleteConversation(conversationId: string) {
    const deletedConversation = conversations.find(
      (conversation) => conversation.id === conversationId
    )
    if (!deletedConversation) return

    const nextConversations = conversations.filter(
      (conversation) => conversation.id !== conversationId
    )
    if (nextConversations.length === 0) {
      setConversations([])
      setActiveConversationId(null)
      toast.success('已删除对话')
      return
    }

    setConversations(orderConversations(nextConversations))
    if (conversationId === activeConversationId) {
      setActiveConversationId(getInitialActiveConversationId(nextConversations))
    }
    toast.success('已删除对话')
  }

  function handleRenameConversation(conversationId: string, title: string) {
    const nextTitle = title.trim()
    if (!nextTitle) {
      toast.error('标题不能为空')
      return
    }

    setConversations((prev) =>
      orderConversations(
        prev.map((conversation) =>
          conversation.id === conversationId
            ? touchConversation({
                ...conversation,
                title: nextTitle.slice(0, 40),
              })
            : conversation
        )
      )
    )
    toast.success('已重命名对话')
  }

  function handleTogglePin(conversationId: string) {
    let nextPinned = false
    setConversations((prev) =>
      orderConversations(
        prev.map((conversation) => {
          if (conversation.id !== conversationId) return conversation
          nextPinned = !conversation.isPinned
          return {
            ...touchConversation(conversation),
            isPinned: nextPinned,
          }
        })
      )
    )
    toast.success(nextPinned ? '已置顶对话' : '已取消置顶')
  }

  function handleToggleFavorite(conversationId: string) {
    let nextFavorited = false
    setConversations((prev) =>
      orderConversations(
        prev.map((conversation) => {
          if (conversation.id !== conversationId) return conversation
          nextFavorited = !conversation.isFavorited
          return {
            ...touchConversation(conversation),
            isFavorited: nextFavorited,
          }
        })
      )
    )
    toast.success(nextFavorited ? '已收藏对话' : '已取消收藏')
  }

  function handleToggleArchive(conversationId: string) {
    const target = conversations.find(
      (conversation) => conversation.id === conversationId
    )
    if (!target) return

    const nextArchived = !target.isArchived
    const nextConversations = orderConversations(
      conversations.map((conversation) =>
        conversation.id === conversationId
          ? touchConversation({
              ...conversation,
              isArchived: nextArchived,
              isPinned: nextArchived ? false : conversation.isPinned,
            })
          : conversation
      )
    )

    setConversations(nextConversations)
    if (conversationId === activeConversationId && nextArchived) {
      setActiveConversationId(getInitialActiveConversationId(nextConversations))
    }
    toast.success(nextArchived ? '已归档对话' : '已恢复对话')
  }

  function handleDuplicateConversation(conversationId: string) {
    const source = conversations.find(
      (conversation) => conversation.id === conversationId
    )
    if (!source) return

    const duplicatedConversation: ChatConversation = {
      ...source,
      id: `chat-${Date.now()}`,
      title: `${source.title} 副本`.slice(0, 40),
      updatedAt: '刚刚',
      createdAt: Date.now(),
      updatedAtValue: Date.now(),
      isPinned: false,
      isFavorited: false,
      isArchived: false,
    }

    setConversations((prev) =>
      orderConversations([duplicatedConversation, ...prev])
    )
    setActiveConversationId(duplicatedConversation.id)
    toast.success('已复制对话')
  }

  async function handleExportConversation(conversationId: string) {
    const source = conversations.find(
      (conversation) => conversation.id === conversationId
    )
    if (!source) return

    try {
      await navigator.clipboard.writeText(JSON.stringify(source, null, 2))
      toast.success('对话 JSON 已复制')
    } catch {
      toast.error('复制失败，请检查浏览器剪贴板权限')
    }
  }

  function handleClearArchived() {
    if (!conversations.some((conversation) => conversation.isArchived)) {
      toast.info('没有归档对话')
      return
    }

    setConversations((prev) =>
      orderConversations(prev.filter((conversation) => !conversation.isArchived))
    )
    toast.success('已清空归档')
  }

  function handleResetDemo() {
    setConversations(orderConversations(initialConversations))
    setActiveConversationId(null)
    setInput('')
    setFiles([])
    toast.success('已恢复默认演示数据')
  }

  function updateActiveConversation(messagesUpdater: (messages: ChatMessage[]) => ChatMessage[]) {
    setConversations((prev) =>
      orderConversations(
        prev.map((conversation) =>
          conversation.id === activeConversationId
            ? touchConversation({
                ...conversation,
                title:
                  isDefaultConversationTitle(conversation.title) && input.trim()
                    ? input.trim().slice(0, 24)
                    : conversation.title,
                description: 'mock 对话，后续接后端历史',
                messages: messagesUpdater(conversation.messages),
              })
            : conversation
        )
      )
    )
  }

  function stopStreaming() {
    updateActiveConversation((prev) =>
      prev.map((message) =>
        message.isStreaming ? { ...message, isStreaming: false } : message
      )
    )
  }

  function handleRegenerateAnswer(messageId: number) {
    setConversations((prev) =>
      orderConversations(
        prev.map((conversation) =>
          conversation.id === activeConversationId
            ? touchConversation({
                ...conversation,
                messages: conversation.messages.map((message) =>
                  message.id === messageId && message.role === 'assistant'
                    ? {
                        ...message,
                        id: createNextMessageId(),
                        isStreaming: true,
                        content: createAssistantMessage(
                          message.content,
                          message.id
                        ).content,
                      }
                    : message
                ),
              })
            : conversation
        )
      )
    )
    toast.success('已重新生成回答')
  }

  function handleSubmit(nextInput = input) {
    const trimmedInput = nextInput.trim()
    if ((!trimmedInput && files.length === 0) || isStreaming) return

    const baseId = createNextMessageId()
    const userMessage: ChatMessage = {
      id: baseId,
      role: 'user',
      content: trimmedInput || '请分析我上传的文件。',
      files: files.map((file) => file.name),
    }
    const assistantMessage = createAssistantMessage(
      userMessage.content,
      createNextMessageId()
    )

    if (!activeConversation) {
      const nextConversation = touchConversation({
        ...createEmptyConversation(),
        title: userMessage.content.slice(0, 24),
        description: 'mock 对话，后续接后端历史',
        messages: [userMessage, assistantMessage],
      })
      setConversations((prev) => orderConversations([nextConversation, ...prev]))
      setActiveConversationId(nextConversation.id)
    } else {
      updateActiveConversation((prev) => [
        ...prev,
        userMessage,
        assistantMessage,
      ])
    }
    setInput('')
    setFiles([])
  }

  function renderPromptComposer({
    showSuggestions = false,
    className,
    inputClassName,
  }: {
    showSuggestions?: boolean
    className?: string
    inputClassName?: string
  } = {}) {
    return (
      <motion.div
        layout={!reduceMotion}
        layoutId={reduceMotion ? undefined : 'chat-prompt-composer'}
        className={cn('mx-auto w-full px-4', className)}
        initial={reduceMotion ? false : { opacity: 0, y: 8, scale: 0.99 }}
        animate={reduceMotion ? undefined : { opacity: 1, y: 0, scale: 1 }}
        transition={
          reduceMotion
            ? undefined
            : {
                duration: 0.28,
                ease: [0.22, 1, 0.36, 1],
                layout: { duration: 0.42, ease: [0.22, 1, 0.36, 1] },
              }
        }
      >
        {showSuggestions ? (
          <div className='mb-3 flex flex-wrap gap-2'>
            {suggestions.map((suggestion, index) => (
              <motion.div
                key={suggestion}
                initial={reduceMotion ? false : { opacity: 0, y: 6 }}
                animate={reduceMotion ? undefined : { opacity: 1, y: 0 }}
                transition={
                  reduceMotion
                    ? undefined
                    : {
                        delay: 0.08 + index * 0.04,
                        duration: 0.24,
                        ease: [0.22, 1, 0.36, 1],
                      }
                }
              >
                <PromptSuggestion onClick={() => setInput(suggestion)}>
                  {suggestion}
                </PromptSuggestion>
              </motion.div>
            ))}
          </div>
        ) : null}

        <PromptInput
          className={cn(
            'border-input rounded-xl border bg-background shadow-xs',
            inputClassName
          )}
          value={input}
          onValueChange={setInput}
          isLoading={isStreaming}
          onSubmit={() => handleSubmit()}
        >
          <AnimatePresence initial={false}>
            {files.length > 0 ? (
              <motion.div
                className='flex flex-wrap gap-2 pb-2'
                initial={reduceMotion ? false : { opacity: 0, height: 0 }}
                animate={reduceMotion ? undefined : { opacity: 1, height: 'auto' }}
                exit={reduceMotion ? undefined : { opacity: 0, height: 0 }}
                transition={
                  reduceMotion
                    ? undefined
                    : { duration: 0.2, ease: [0.22, 1, 0.36, 1] }
                }
              >
                {files.map((file, index) => (
                  <motion.div
                    key={`${file.name}-${index}`}
                    layout={!reduceMotion}
                    initial={reduceMotion ? false : { opacity: 0, scale: 0.96 }}
                    animate={reduceMotion ? undefined : { opacity: 1, scale: 1 }}
                    exit={reduceMotion ? undefined : { opacity: 0, scale: 0.96 }}
                    transition={
                      reduceMotion
                        ? undefined
                        : { duration: 0.18, ease: [0.22, 1, 0.36, 1] }
                    }
                  >
                    <Badge
                      variant='secondary'
                      className='h-auto py-1.5 pr-1 text-sm'
                      onClick={(event) => event.stopPropagation()}
                    >
                      <Paperclip data-icon='inline-start' />
                      <span className='max-w-[140px] truncate'>{file.name}</span>
                      <Button
                        type='button'
                        variant='ghost'
                        size='icon-xs'
                        onClick={() => handleRemoveFile(index)}
                      >
                        <X />
                        <span className='sr-only'>Remove file</span>
                      </Button>
                    </Badge>
                  </motion.div>
                ))}
              </motion.div>
            ) : null}
          </AnimatePresence>

          <PromptInputTextarea
            placeholder='询问知识库、粘贴材料，或描述要分析的问题...'
            disabled={isStreaming}
            disableAutosize
            className='h-30 min-h-30 max-h-30 overflow-y-auto [field-sizing:fixed]'
          />

          <PromptInputActions className='flex items-center justify-between gap-2 pt-2'>
            <PromptInputAction tooltip='Attach files'>
              <FileUploadTrigger asChild>
                <Button
                  type='button'
                  variant='ghost'
                  size='icon'
                  disabled={isStreaming}
                >
                  <Paperclip />
                  <span className='sr-only'>Attach files</span>
                </Button>
              </FileUploadTrigger>
            </PromptInputAction>

            <PromptInputAction
              tooltip={isStreaming ? 'Stop generation' : 'Send message'}
            >
              <Button
                type='button'
                size='icon-lg'
                className='rounded-2xl'
                onClick={() => (isStreaming ? stopStreaming() : handleSubmit())}
                disabled={!isStreaming && !input.trim() && files.length === 0}
                aria-label={isStreaming ? 'Stop generation' : 'Send message'}
              >
                {isStreaming ? (
                  <Square className='fill-current' />
                ) : (
                  <ArrowUpIcon className='size-5' />
                )}
              </Button>
            </PromptInputAction>
          </PromptInputActions>
        </PromptInput>
      </motion.div>
    )
  }

  return (
    <FileUpload
      onFilesAdded={handleFilesAdded}
      multiple
      disabled={isStreaming}
    >
      <SidebarProvider
        defaultOpen={defaultOpen}
      >
        <ChatHistorySidebar
          collapsible={collapsible}
          variant={variant}
          conversations={conversations}
          activeConversationId={activeConversationId}
          onNewConversation={handleNewConversation}
          onSelectConversation={handleSelectConversation}
          onDeleteConversation={handleDeleteConversation}
          onRenameConversation={handleRenameConversation}
          onTogglePin={handleTogglePin}
          onToggleFavorite={handleToggleFavorite}
          onToggleArchive={handleToggleArchive}
          onDuplicateConversation={handleDuplicateConversation}
          onExportConversation={handleExportConversation}
          onClearArchived={handleClearArchived}
          onResetDemo={handleResetDemo}
        />

        <SidebarInset
          className={cn(
            '@container/content h-svh overflow-hidden',
            'md:peer-data-[variant=inset]:h-[calc(100svh-(var(--spacing)*4))]'
          )}
        >
          <div className='relative flex h-full min-h-0 flex-col overflow-hidden bg-background'>
            <FileUploadContent>
              <div className='flex flex-col items-center gap-3 rounded-lg border bg-background p-8 shadow-sm'>
                <FileUp />
                <div className='text-sm font-medium'>拖拽文件到这里</div>
                <div className='text-xs text-muted-foreground'>
                  文件会作为当前问题的附件进入 Chat UI
                </div>
              </div>
            </FileUploadContent>

            <header
              className={cn(
                'pointer-events-none absolute inset-x-0 top-0 z-30 flex h-16 items-center justify-between bg-background/95 px-4 transition-all duration-200',
                isHeaderGlass &&
                  'bg-background/80 shadow-sm backdrop-blur-xl supports-[backdrop-filter]:bg-background/70'
              )}
            >
              <div className='pointer-events-auto flex items-center gap-2'>
                <SidebarTrigger />
              </div>
              <div className='pointer-events-auto absolute left-1/2 flex max-w-[min(32rem,calc(100%-12rem))] -translate-x-1/2 items-center justify-center gap-2 text-sm font-medium'>
                <span className='truncate'>{activeConversationTitle}</span>
              </div>
              <div className='pointer-events-auto flex items-center'>
                <HeaderActions showSearch={false} showAdminLink />
              </div>
            </header>

            <main className='flex min-h-0 flex-1'>
              <LayoutGroup id='chat-thread-layout'>
                <div className='flex min-w-0 flex-1 flex-col'>
                {hasMessages ? (
                  <>
                    <div className='relative min-h-0 flex-1'>
                      <ChatContainerRoot className='h-full'>
                        <ChatContainerContent
                          className='mx-auto flex w-full max-w-(--breakpoint-md) flex-col gap-5 px-4 pt-24 pb-6'
                          scrollClassName='no-scrollbar overflow-y-auto'
                          onScroll={handleChatScroll}
                        >
                          <SystemMessage fill className='mx-auto w-full'>
                            默认进入 Chat UI。左侧可新建对话、切换历史会话，当前数据先使用 mock。
                          </SystemMessage>

                          <AnimatePresence initial={false}>
                            {messages.map((message) => (
                              <motion.div
                                key={message.id}
                                layout={!reduceMotion}
                                initial={
                                  reduceMotion
                                    ? false
                                    : { opacity: 0, y: 12, scale: 0.98 }
                                }
                                animate={
                                  reduceMotion
                                    ? undefined
                                    : { opacity: 1, y: 0, scale: 1 }
                                }
                                exit={
                                  reduceMotion
                                    ? undefined
                                    : { opacity: 0, y: -8, scale: 0.98 }
                                }
                                transition={
                                  reduceMotion
                                    ? undefined
                                    : { duration: 0.26, ease: [0.22, 1, 0.36, 1] }
                                }
                              >
                                <ChatMessageItem
                                  message={message}
                                  onRegenerate={() => handleRegenerateAnswer(message.id)}
                                  onStreamingComplete={() => markMessageComplete(message.id)}
                                />
                              </motion.div>
                            ))}
                          </AnimatePresence>
                          <ChatContainerScrollAnchor />
                        </ChatContainerContent>

                        <div className='absolute bottom-4 left-1/2 -translate-x-1/2'>
                          <ScrollButton className='shadow-sm' />
                        </div>
                      </ChatContainerRoot>
                    </div>

                    {renderPromptComposer({
                      className: 'max-w-(--breakpoint-md) pt-1 pb-4',
                    })}
                  </>
                ) : (
                  <div
                    className='no-scrollbar min-h-0 flex-1 overflow-y-auto px-4 pt-24 pb-8'
                    onScroll={handleChatScroll}
                  >
                    <motion.div
                      className='mx-auto flex min-h-full w-full max-w-4xl flex-col items-center justify-center gap-15 pt-6 pb-[10vh]'
                      initial={
                        reduceMotion ? false : { opacity: 0, y: 10, scale: 0.99 }
                      }
                      animate={
                        reduceMotion ? undefined : { opacity: 1, y: 0, scale: 1 }
                      }
                      transition={
                        reduceMotion
                          ? undefined
                          : { duration: 0.36, ease: [0.22, 1, 0.36, 1] }
                      }
                    >
                      <div className='flex max-w-3xl flex-col items-center gap-3 text-center'>
                        <div className='flex flex-col'>
                          <ChatHeroTitle />
                        </div>
                      </div>

                      {renderPromptComposer({
                        className: 'max-w-(--breakpoint-md)',
                      })}
                    </motion.div>
                  </div>
                )}
                </div>
              </LayoutGroup>

            </main>
          </div>
        </SidebarInset>
      </SidebarProvider>
    </FileUpload>
  )
}

function ChatHistorySidebar({
  collapsible,
  variant,
  conversations,
  activeConversationId,
  onNewConversation,
  onSelectConversation,
  onDeleteConversation,
  onRenameConversation,
  onTogglePin,
  onToggleFavorite,
  onToggleArchive,
  onDuplicateConversation,
  onExportConversation,
  onClearArchived,
  onResetDemo,
}: {
  collapsible: Collapsible
  variant: ChatSidebarVariant
  conversations: ChatConversation[]
  activeConversationId: string | null
  onNewConversation: () => void
  onSelectConversation: (conversationId: string) => void
  onDeleteConversation: (conversationId: string) => void
  onRenameConversation: (conversationId: string, title: string) => void
  onTogglePin: (conversationId: string) => void
  onToggleFavorite: (conversationId: string) => void
  onToggleArchive: (conversationId: string) => void
  onDuplicateConversation: (conversationId: string) => void
  onExportConversation: (conversationId: string) => void
  onClearArchived: () => void
  onResetDemo: () => void
}) {
  const { setOpen } = useSidebar()
  const reduceMotion = useReducedMotion()
  const searchInputRef = useRef<HTMLInputElement>(null)
  const [query, setQuery] = useState('')
  const [view, setView] = useState<ChatSidebarView>('active')
  const [renamingConversation, setRenamingConversation] =
    useState<ChatConversation | null>(null)
  const [renameTitle, setRenameTitle] = useState('')
  const [deletingConversation, setDeletingConversation] =
    useState<ChatConversation | null>(null)
  const [isClearArchiveOpen, setIsClearArchiveOpen] = useState(false)
  const [isResetOpen, setIsResetOpen] = useState(false)

  const activeCount = conversations.filter(
    (conversation) => !conversation.isArchived
  ).length
  const favoriteCount = conversations.filter(
    (conversation) => conversation.isFavorited && !conversation.isArchived
  ).length
  const archivedCount = conversations.filter(
    (conversation) => conversation.isArchived
  ).length

  const visibleConversations = useMemo(() => {
    return conversations.filter((conversation) => {
      if (view === 'active' && conversation.isArchived) return false
      if (
        view === 'favorites' &&
        (!conversation.isFavorited || conversation.isArchived)
      ) {
        return false
      }
      if (view === 'archived' && !conversation.isArchived) return false

      const normalizedQuery = query.trim().toLowerCase()
      if (!normalizedQuery) return true
      return `${conversation.title} ${conversation.description}`
        .toLowerCase()
        .includes(normalizedQuery)
    })
  }, [conversations, query, view])
  const pinnedVisibleConversations =
    view === 'active'
      ? visibleConversations.filter(
          (conversation) => conversation.isPinned && !conversation.isArchived
        )
      : []
  const historyVisibleConversations =
    view === 'active'
      ? visibleConversations.filter(
          (conversation) => !conversation.isPinned && !conversation.isArchived
        )
      : visibleConversations

  function openRenameDialog(conversation: ChatConversation) {
    setRenamingConversation(conversation)
    setRenameTitle(conversation.title)
  }

  function handleRenameSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!renamingConversation) return
    onRenameConversation(renamingConversation.id, renameTitle)
    setRenamingConversation(null)
  }

  function getEmptyTitle() {
    if (query.trim()) return '没有匹配的会话'
    if (view === 'favorites') return '还没有收藏对话'
    if (view === 'archived') return '归档为空'
    return '还没有历史消息'
  }

  function getEmptyDescription() {
    if (query.trim()) return '换一个关键词，或清空搜索后再试。'
    if (view === 'favorites') return '在会话菜单里选择“收藏”，常用对话会显示在这里。'
    if (view === 'archived') return '归档后的对话会显示在这里，可以随时恢复。'
    return '新建一个对话后，消息会保存在本机浏览器。'
  }

  function openSidebarView(nextView: ChatSidebarView, options?: { search?: boolean }) {
    setView(nextView)
    setOpen(true)

    if (options?.search) {
      window.setTimeout(() => searchInputRef.current?.focus(), 220)
    }
  }

  function renderConversationItem(conversation: ChatConversation) {
    const isActive = conversation.id === activeConversationId

    return (
      <motion.li
        key={conversation.id}
        data-slot='sidebar-menu-item'
        data-sidebar='menu-item'
        layout={!reduceMotion}
        initial={reduceMotion ? false : { opacity: 0, y: 6, scale: 0.98 }}
        animate={reduceMotion ? undefined : { opacity: 1, y: 0, scale: 1 }}
        exit={reduceMotion ? undefined : { opacity: 0, x: -8, scale: 0.98 }}
        transition={
          reduceMotion
            ? undefined
            : { duration: 0.2, ease: [0.22, 1, 0.36, 1] }
        }
        className='group/menu-item relative'
      >
        <SidebarMenuButton
          isActive={isActive}
          tooltip={conversation.title}
          onClick={() => onSelectConversation(conversation.id)}
          className='relative min-h-8 [--sidebar-menu-icon-size:1rem] px-3 pr-10 font-normal leading-5 group-hover/menu-item:bg-sidebar-accent group-hover/menu-item:text-sidebar-accent-foreground group-focus-within/menu-item:bg-sidebar-accent group-focus-within/menu-item:text-sidebar-accent-foreground data-active:bg-primary/10 data-active:font-medium data-active:text-foreground dark:data-active:bg-primary/15 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:pr-0'
        >
          {isActive ? (
            <span
              aria-hidden='true'
              className='absolute top-1/2 left-1 h-4 w-1 -translate-y-1/2 rounded-full bg-primary'
            />
          ) : null}
          {conversation.isPinned && !conversation.isArchived ? (
            <Pin data-icon='inline-start' />
          ) : null}
          <span className='block min-w-0 flex-1 truncate text-sm leading-5 group-data-[collapsible=icon]:hidden'>
            {conversation.title}
          </span>
        </SidebarMenuButton>
        <DropdownMenu modal={false}>
          <DropdownMenuTrigger asChild>
            <SidebarMenuAction
              type='button'
              showOnHover
              onClick={(event) => event.stopPropagation()}
              aria-label='Conversation actions'
              className='top-1.5 right-2 size-5 rounded-md hover:bg-sidebar-accent-foreground/10 hover:text-sidebar-accent-foreground'
            >
              <MoreHorizontal />
            </SidebarMenuAction>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            side='right'
            align='start'
            className='w-max min-w-36 [&_[data-slot=dropdown-menu-item]]:px-2.5'
          >
            <DropdownMenuGroup>
              <DropdownMenuItem
                onSelect={() => openRenameDialog(conversation)}
              >
                <Edit3 />
                重命名
              </DropdownMenuItem>
              <DropdownMenuItem
                onSelect={() => onTogglePin(conversation.id)}
                disabled={conversation.isArchived}
              >
                {conversation.isPinned ? <PinOff /> : <Pin />}
                {conversation.isPinned ? '取消置顶' : '置顶'}
              </DropdownMenuItem>
              <DropdownMenuItem
                onSelect={() => onToggleFavorite(conversation.id)}
                disabled={conversation.isArchived}
              >
                {conversation.isFavorited ? <StarOff /> : <Star />}
                {conversation.isFavorited ? '取消收藏' : '收藏'}
              </DropdownMenuItem>
              <DropdownMenuItem
                onSelect={() => onDuplicateConversation(conversation.id)}
              >
                <Clipboard />
                复制对话
              </DropdownMenuItem>
              <DropdownMenuItem
                onSelect={() => onExportConversation(conversation.id)}
              >
                <Download />
                复制 JSON
              </DropdownMenuItem>
            </DropdownMenuGroup>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onSelect={() => onToggleArchive(conversation.id)}
            >
              {conversation.isArchived ? (
                <ArchiveRestore />
              ) : (
                <Archive />
              )}
              {conversation.isArchived ? '恢复对话' : '归档'}
            </DropdownMenuItem>
            <DropdownMenuItem
              variant='destructive'
              onSelect={() => setDeletingConversation(conversation)}
            >
              <Trash2 />
              删除
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </motion.li>
    )
  }

  return (
    <>
      <Sidebar collapsible={collapsible} variant={variant}>
        <SidebarHeader>
          <TeamSwitcher teams={CHAT_SIDEBAR_TEAMS} />
          <div className='flex items-center gap-2 group-data-[collapsible=icon]:hidden'>
            <div className='relative min-w-0 flex-1'>
              <Search className='pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground' />
              <SidebarInput
                ref={searchInputRef}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder='Search chats...'
                className='pl-8'
              />
            </div>
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Button
                    type='button'
                    aria-label='新建对话'
                    variant='outline'
                    size='icon'
                    onClick={onNewConversation}
                    className='shrink-0'
                  >
                    <SquarePen />
                    <span className='sr-only'>新建对话</span>
                  </Button>
                </TooltipTrigger>
                <TooltipContent side='bottom'>新建对话</TooltipContent>
              </Tooltip>
            </TooltipProvider>
          </div>
          <Tabs
            value={view}
            onValueChange={(value) => setView(value as ChatSidebarView)}
            className='mt-1 w-full group-data-[collapsible=icon]:hidden'
          >
            <TabsList className='grid w-full grid-cols-3'>
              <TabsTrigger value='active'>
                <MessageSquare data-icon='inline-start' />
                全部
              </TabsTrigger>
              <TabsTrigger value='favorites'>
                <Star data-icon='inline-start' />
                收藏
              </TabsTrigger>
              <TabsTrigger value='archived'>
                <Archive data-icon='inline-start' />
                归档
              </TabsTrigger>
            </TabsList>
          </Tabs>
        </SidebarHeader>

        <SidebarContent className='overflow-hidden'>
          <SidebarGroup className='hidden group-data-[collapsible=icon]:flex'>
            <SidebarGroupContent>
              <SidebarMenu className='items-center gap-2'>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip='新建对话'
                    onClick={onNewConversation}
                    className='justify-center'
                  >
                    <SquarePen />
                    <span className='sr-only'>新建对话</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip='搜索历史'
                    onClick={() => openSidebarView('active', { search: true })}
                    className='justify-center'
                  >
                    <Search />
                    <span className='sr-only'>搜索历史</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip={`全部历史 ${activeCount}`}
                    onClick={() => openSidebarView('active')}
                    isActive={view === 'active'}
                    className='justify-center'
                  >
                    <Inbox />
                    <span className='sr-only'>全部历史</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip={`收藏对话 ${favoriteCount}`}
                    onClick={() => openSidebarView('favorites')}
                    isActive={view === 'favorites'}
                    className='justify-center'
                  >
                    <Star />
                    <span className='sr-only'>收藏对话</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip={`归档对话 ${archivedCount}`}
                    onClick={() => openSidebarView('archived')}
                    isActive={view === 'archived'}
                    className='justify-center'
                  >
                    <Archive />
                    <span className='sr-only'>归档对话</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>

          <SidebarGroup className='min-h-0 flex-1 px-2 pt-0 pb-2 group-data-[collapsible=icon]:hidden'>
            <SidebarGroupContent
              className='flex h-full min-h-0 flex-col overflow-hidden'
            >
              {visibleConversations.length > 0 ? (
                <div className='no-scrollbar min-h-0 flex-1 overflow-x-hidden overflow-y-auto'>
                  {view === 'active' ? (
                    <>
                      {pinnedVisibleConversations.length > 0 ? (
                        <>
                          <SidebarGroupLabel>置顶</SidebarGroupLabel>
                          <SidebarMenu className='gap-1'>
                            <AnimatePresence initial={false}>
                              {pinnedVisibleConversations.map(renderConversationItem)}
                            </AnimatePresence>
                          </SidebarMenu>
                        </>
                      ) : null}
                      {historyVisibleConversations.length > 0 ? (
                        <>
                          <SidebarGroupLabel
                            className={cn(
                              pinnedVisibleConversations.length > 0 && 'mt-2'
                            )}
                          >
                            最近
                          </SidebarGroupLabel>
                          <SidebarMenu className='gap-1'>
                            <AnimatePresence initial={false}>
                              {historyVisibleConversations.map(renderConversationItem)}
                            </AnimatePresence>
                          </SidebarMenu>
                        </>
                      ) : null}
                    </>
                  ) : (
                    <>
                      <SidebarGroupLabel>
                        {view === 'archived' ? '归档对话' : '收藏'}
                      </SidebarGroupLabel>
                      <SidebarMenu className='gap-1'>
                        <AnimatePresence initial={false}>
                          {historyVisibleConversations.map(renderConversationItem)}
                        </AnimatePresence>
                      </SidebarMenu>
                    </>
                  )}
                </div>
              ) : (
                <motion.div
                  key={`${view}-${query ? 'search' : 'empty'}`}
                  initial={reduceMotion ? false : { opacity: 0, y: 8 }}
                  animate={reduceMotion ? undefined : { opacity: 1, y: 0 }}
                  transition={
                    reduceMotion
                      ? undefined
                      : { duration: 0.24, ease: [0.22, 1, 0.36, 1] }
                  }
                >
                  <Empty className='border-0 px-2 py-8'>
                    <EmptyHeader>
                      <EmptyMedia variant='icon'>
                        {view === 'favorites' ? (
                          <Star />
                        ) : view === 'archived' ? (
                          <Archive />
                        ) : (
                          <Inbox />
                        )}
                      </EmptyMedia>
                      <EmptyTitle>{getEmptyTitle()}</EmptyTitle>
                      <EmptyDescription>{getEmptyDescription()}</EmptyDescription>
                    </EmptyHeader>
                    {query.trim() ? (
                      <EmptyContent>
                        <Button
                          type='button'
                          size='sm'
                          variant='outline'
                          onClick={() => setQuery('')}
                        >
                          清空搜索
                        </Button>
                      </EmptyContent>
                    ) : null}
                  </Empty>
                </motion.div>
              )}
            </SidebarGroupContent>
          </SidebarGroup>
        </SidebarContent>

        <SidebarFooter>
          <SidebarMenu className='gap-1'>
            <SidebarMenuItem>
              <SidebarMenuButton
                asChild
                tooltip='设置'
                className='h-8 text-sm font-normal'
              >
                <Link to='/admin/settings/account'>
                  <Settings />
                  <span className='group-data-[collapsible=icon]:hidden'>
                    设置
                  </span>
                </Link>
              </SidebarMenuButton>
            </SidebarMenuItem>
            <SidebarMenuItem>
              <DropdownMenu modal={false}>
                <DropdownMenuTrigger asChild>
                  <SidebarMenuButton
                    tooltip='更多操作'
                    className='h-8 text-sm font-normal'
                  >
                    <MoreHorizontal />
                    <span className='group-data-[collapsible=icon]:hidden'>
                      更多
                    </span>
                  </SidebarMenuButton>
                </DropdownMenuTrigger>
                <DropdownMenuContent side='right' align='end' className='w-44'>
                  <DropdownMenuItem
                    disabled={archivedCount === 0}
                    onSelect={() => setIsClearArchiveOpen(true)}
                  >
                    <Archive />
                    清空归档
                  </DropdownMenuItem>
                  <DropdownMenuSeparator />
                  <DropdownMenuItem onSelect={() => setIsResetOpen(true)}>
                    <RotateCcw />
                    恢复演示数据
                  </DropdownMenuItem>
                </DropdownMenuContent>
              </DropdownMenu>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarFooter>
        <SidebarRail />
      </Sidebar>

      <Dialog
        open={Boolean(renamingConversation)}
        onOpenChange={(open) => {
          if (!open) setRenamingConversation(null)
        }}
      >
        <DialogContent>
          <form onSubmit={handleRenameSubmit} className='flex flex-col gap-4'>
            <DialogHeader>
              <DialogTitle>重命名对话</DialogTitle>
              <DialogDescription>
                修改后的标题只保存在当前浏览器，后续可接入后端同步。
              </DialogDescription>
            </DialogHeader>
            <Input
              value={renameTitle}
              onChange={(event) => setRenameTitle(event.target.value)}
              autoFocus
              maxLength={40}
              placeholder='输入对话标题'
            />
            <DialogFooter>
              <Button type='button' variant='outline' onClick={() => setRenamingConversation(null)}>
                取消
              </Button>
              <Button type='submit'>保存</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <AlertDialog
        open={Boolean(deletingConversation)}
        onOpenChange={(open) => {
          if (!open) setDeletingConversation(null)
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogMedia>
              <Trash2 />
            </AlertDialogMedia>
            <AlertDialogTitle>删除这个对话？</AlertDialogTitle>
            <AlertDialogDescription>
              “{deletingConversation?.title}” 会从历史消息中移除，此操作不可恢复。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction
              variant='destructive'
              onClick={() => {
                if (deletingConversation) {
                  onDeleteConversation(deletingConversation.id)
                }
                setDeletingConversation(null)
              }}
            >
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={isClearArchiveOpen} onOpenChange={setIsClearArchiveOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogMedia>
              <Archive />
            </AlertDialogMedia>
            <AlertDialogTitle>清空归档？</AlertDialogTitle>
            <AlertDialogDescription>
              会删除所有已归档对话，此操作不可恢复。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction
              variant='destructive'
              onClick={() => {
                onClearArchived()
                setIsClearArchiveOpen(false)
              }}
            >
              清空
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      <AlertDialog open={isResetOpen} onOpenChange={setIsResetOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogMedia>
              <RotateCcw />
            </AlertDialogMedia>
            <AlertDialogTitle>恢复演示数据？</AlertDialogTitle>
            <AlertDialogDescription>
              当前本地历史会话会被替换为默认 mock 数据。
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>取消</AlertDialogCancel>
            <AlertDialogAction
              onClick={() => {
                onResetDemo()
                setIsResetOpen(false)
              }}
            >
              恢复
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}

function ChatMessageItem({
  message,
  onRegenerate,
  onStreamingComplete,
}: {
  message: ChatMessage
  onRegenerate: () => void
  onStreamingComplete: () => void
}) {
  const isAssistant = message.role === 'assistant'

  return (
    <Message className={message.role === 'user' ? 'justify-end' : 'justify-start'}>
      <div
        className={cn(
          'flex flex-col gap-3',
          isAssistant ? 'w-full' : 'max-w-[88%] sm:max-w-[78%]'
        )}
      >
        {isAssistant ? (
          <AssistantMessage
            message={message}
            onRegenerate={onRegenerate}
            onStreamingComplete={onStreamingComplete}
          />
        ) : (
          <UserMessage message={message} />
        )}
      </div>
    </Message>
  )
}

function UserMessage({ message }: { message: ChatMessage }) {
  return (
    <div className='flex flex-col items-end gap-2'>
      <MessageContent className='bg-primary text-sm leading-5 text-primary-foreground'>
        {message.content}
      </MessageContent>
      {message.files?.length ? (
        <div className='flex flex-wrap justify-end gap-2'>
          {message.files.map((file) => (
            <span
              key={file}
              className='inline-flex items-center gap-1 rounded-lg border bg-background px-2 py-1 text-xs text-muted-foreground'
            >
              <Paperclip className='size-4 shrink-0' />
              {file}
            </span>
          ))}
        </div>
      ) : null}
    </div>
  )
}

function AssistantMessage({
  message,
  onRegenerate,
  onStreamingComplete,
}: {
  message: ChatMessage
  onRegenerate: () => void
  onStreamingComplete: () => void
}) {
  return (
    <div className='flex flex-col gap-3'>
      {message.isStreaming ? (
        <div className='rounded-lg border bg-background p-3'>
          <ThinkingBar
            text='Retrieving and composing'
            stopLabel='显示完整回答'
            onStop={onStreamingComplete}
          />
          <div className='mt-3 flex items-center gap-2 text-sm text-muted-foreground'>
            <Loader variant='typing' size='sm' />
            <TextShimmer>正在读取 mock 知识库证据</TextShimmer>
          </div>
        </div>
      ) : null}

      {message.isStreaming ? (
        <div className='prose text-sm leading-6 text-foreground dark:prose-invert'>
          <ResponseStream
            textStream={message.content}
            speed={38}
            characterChunkSize={3}
            onComplete={onStreamingComplete}
          />
        </div>
      ) : (
        <MessageContent
          markdown
          className='bg-transparent p-0 text-sm leading-6'
          children={message.content}
        />
      )}

      <RetrievalTrace />
      <ToolCall />
      {message.variant === 'code' ? <CodeAnswer /> : null}
      {message.variant === 'document' ? <ArtifactPreview /> : null}
      {message.variant === 'rag' ? (
        <>
          <SourceList />
          <GeneratedFlowImage />
        </>
      ) : null}
      <AnswerActions content={message.content} onRegenerate={onRegenerate} />
    </div>
  )
}

function RetrievalTrace() {
  return (
    <div className='rounded-lg border bg-background p-3'>
      <Steps>
        <StepsTrigger leftIcon={<FileSearch />}>
          检索执行步骤
        </StepsTrigger>
        <StepsContent>
          <StepsItem>解析用户问题，提取“知识库问答链路”和“引用来源”。</StepsItem>
          <StepsItem>同时执行关键词检索和向量检索，召回 6 条候选片段。</StepsItem>
          <StepsItem>按相关性重排，保留可引用的来源并生成回答。</StepsItem>
        </StepsContent>
      </Steps>

      <ChainOfThought className='mt-4'>
        <ChainOfThoughtStep defaultOpen>
          <ChainOfThoughtTrigger leftIcon={<Search />}>
            可见推理摘要
          </ChainOfThoughtTrigger>
          <ChainOfThoughtContent>
            <ChainOfThoughtItem>
              先回答业务链路，再展示前端需要渲染的结构化数据。
            </ChainOfThoughtItem>
          </ChainOfThoughtContent>
        </ChainOfThoughtStep>
        <ChainOfThoughtStep>
          <ChainOfThoughtTrigger leftIcon={<Circle />}>
            质量检查
          </ChainOfThoughtTrigger>
          <ChainOfThoughtContent>
            <ChainOfThoughtItem>
              回答必须包含来源、工具状态和反馈入口，方便后续接真实接口。
            </ChainOfThoughtItem>
          </ChainOfThoughtContent>
        </ChainOfThoughtStep>
      </ChainOfThought>
    </div>
  )
}

function ToolCall() {
  return (
    <Tool
      defaultOpen={false}
      toolPart={{
        type: 'knowledgeBase.search',
        state: 'output-available',
        toolCallId: 'mock-rag-001',
        input: {
          query: '知识库问答链路 引用来源',
          topK: 6,
          rerank: true,
        },
        output: {
          matches: 6,
          topScore: 0.91,
          collections: ['产品文档', '接口说明'],
        },
      }}
    />
  )
}

function SourceList() {
  return (
    <div className='flex flex-wrap items-center gap-2'>
      <Source href='https://www.prompt-kit.com/docs'>
        <SourceTrigger label='prompt-kit docs' showFavicon />
        <SourceContent
          title='prompt-kit documentation'
          description='Prompt-kit 官方组件文档，用于 Chat UI 组件组合。'
        />
      </Source>
      <Source href='https://tanstack.com/router/latest'>
        <SourceTrigger label='tanstack router' showFavicon />
        <SourceContent
          title='TanStack Router'
          description='当前前端项目使用的文件路由系统。'
        />
      </Source>
      <Source href='mock://know-studio/product-doc'>
        <SourceTrigger label='产品需求.md' />
        <SourceContent
          title='产品需求.md'
          description='本地 mock 来源，后续替换为后端返回的文档片段。'
        />
      </Source>
    </div>
  )
}

function GeneratedFlowImage() {
  return (
    <div className='overflow-hidden rounded-lg border bg-background p-2'>
      <Image
        base64={generatedDiagram}
        mediaType='image/svg+xml'
        alt='RAG workflow diagram'
        className='aspect-[2/1] w-full object-cover'
      />
    </div>
  )
}

function CodeAnswer() {
  return (
    <CodeBlock>
      <CodeBlockGroup className='border-b px-4 py-2'>
        <div className='flex items-center gap-2 text-sm font-medium'>
          <Database />
          search-knowledge-base.ts
        </div>
        <span className='text-xs text-muted-foreground'>mock adapter</span>
      </CodeBlockGroup>
      <CodeBlockCode code={codeSample} language='ts' />
    </CodeBlock>
  )
}

function ArtifactPreview() {
  return (
    <div className='rounded-lg border bg-background p-3'>
      <div className='mb-3 flex items-center gap-2 text-sm font-medium'>
        <CheckCircle />
        文档处理产物预览
      </div>
      <JSXPreview jsx={artifactJsx} />
    </div>
  )
}

function AnswerActions({
  content,
  onRegenerate,
}: {
  content: string
  onRegenerate: () => void
}) {
  const [isFeedbackVisible, setIsFeedbackVisible] = useState(true)

  async function handleCopyAnswer() {
    try {
      await navigator.clipboard.writeText(content)
      toast.success('回答已复制')
    } catch {
      toast.error('复制失败，请检查浏览器剪贴板权限')
    }
  }

  function handleFeedback(type: 'helpful' | 'not-helpful') {
    toast.success(type === 'helpful' ? '已标记有帮助' : '已记录反馈')
    setIsFeedbackVisible(false)
  }

  return (
    <div className='flex flex-col gap-2'>
      <MessageActions>
        <MessageAction tooltip='Copy answer'>
          <Button type='button' variant='ghost' size='sm' onClick={handleCopyAnswer}>
            <Clipboard data-icon='inline-start' />
            复制
          </Button>
        </MessageAction>
        <MessageAction tooltip='Regenerate answer'>
          <Button type='button' variant='ghost' size='sm' onClick={onRegenerate}>
            <RotateCcw data-icon='inline-start' />
            重试
          </Button>
        </MessageAction>
      </MessageActions>
      {isFeedbackVisible ? (
        <FeedbackBar
          title='这条回答有帮助吗？'
          onHelpful={() => handleFeedback('helpful')}
          onNotHelpful={() => handleFeedback('not-helpful')}
          onClose={() => setIsFeedbackVisible(false)}
        />
      ) : null}
    </div>
  )
}
