import {
  useEffect,
  useLayoutEffect,
  useMemo,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
  type UIEvent,
} from 'react'
import { useQuery } from '@tanstack/react-query'
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
import {
  createAssistantSession,
  deleteAssistantSession,
  getAssistantContext,
  listAssistantSessions,
  renameAssistantSession,
  streamAssistantChat,
  type AssistantMessage as ApiAssistantMessage,
  type AssistantSessionListItem,
  type AssistantStreamEvent,
  type AssistantToolMode,
} from '@/api/assistant'
import {
  extractApiError,
  HttpStatusError,
  isUnauthorizedError,
} from '@/api/http'
import { useWorkspaceStore } from '@/stores/workspace-store'
import { type Citation } from '@/api/qa'
import { useLayout, type Collapsible } from '@/context/layout-provider'
import { getCookie } from '@/lib/cookies'
import { useAuthStore } from '@/stores/auth-store'
import {
  Archive,
  ArchiveRestore,
  ArrowUpIcon,
  BookOpenText,
  Brain,
  CheckCircle,
  ChevronDown,
  Clipboard,
  ClipboardList,
  Copy,
  Database,
  Download,
  Edit3,
  FileSearch,
  FileUp,
  Inbox,
  MessageSquare,
  Mic,
  MoreHorizontal,
  Paperclip,
  Pin,
  PinOff,
  Plus,
  RotateCcw,
  Search,
  SquarePen,
  Square,
  Star,
  StarOff,
  Trash2,
  X,
  type LucideIcon,
} from 'lucide-react'
import {
  ChatContainerContent,
  ChatContainerRoot,
  ChatContainerScrollAnchor,
  ChatContainerScrollToBottom,
} from '@/components/ui/chat-container'
import {
  FileUpload,
  FileUploadContent,
  FileUploadTrigger,
} from '@/components/ui/file-upload'
import { Loader } from '@/components/ui/loader'
import { Skeleton } from '@/components/ui/skeleton'
import {
  ChainOfThought,
  ChainOfThoughtContent,
  ChainOfThoughtItem,
  ChainOfThoughtStep,
  ChainOfThoughtTrigger,
} from '@/components/ui/chain-of-thought'
import { Tool, type ToolPart } from '@/components/ui/tool'
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
import { ScrollButton } from '@/components/ui/scroll-button'
import { TextShimmer } from '@/components/ui/text-shimmer'
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
  DropdownMenuLabel,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
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
import { Switch } from '@/components/ui/switch'
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
  SidebarMenuSkeleton,
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
  role: 'user' | 'assistant' | 'event'
  content: string
  eventType?: 'mode_changed'
  mode?: ChatAssistantMode
  files?: string[]
  variant?: 'rag' | 'code' | 'document'
  isStreaming?: boolean
  isResponseComplete?: boolean
  isLive?: boolean
  query?: string
  citations?: Citation[]
  thinking?: string
  tools?: ChatToolActivity[]
  streamError?: string
}

type ChatToolActivity = ToolPart & {
  key: string
}

type ChatConversation = {
  id: string
  title: string
  description: string
  assistantSessionId?: number
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

type ChatSessionMetadata = Record<
  string,
  Pick<ChatConversation, 'isPinned' | 'isFavorited' | 'isArchived'>
>

type ChatSidebarView = 'active' | 'favorites' | 'archived'
type ChatSidebarVariant = 'inset' | 'sidebar' | 'floating'
type ChatAssistantMode = Extract<AssistantToolMode, 'CHAT' | 'KB_SEARCH'>

const CHAT_ASSISTANT_MODE_OPTIONS: {
  value: ChatAssistantMode
  label: string
  icon: LucideIcon
}[] = [
  {
    value: 'KB_SEARCH',
    label: '本地知识',
    icon: Database,
  },
  {
    value: 'CHAT',
    label: '通用助手',
    icon: MessageSquare,
  },
]

const CHAT_SESSION_METADATA_KEY = 'know-studio.chat-ui.session-metadata'
const LEGACY_CHAT_STORAGE_KEY = 'know-studio.chat-ui.conversations'
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
const CHAT_SIDEBAR_TEAMS = [
  {
    ...sidebarData.teams[0],
    name: 'KnowStudio',
    plan: 'Chat Workspace',
  },
]

const suggestions: Array<{
  label: string
  icon: LucideIcon
  prompts: string[]
}> = [
  {
    label: '制度查询',
    icon: ClipboardList,
    prompts: [
      '制度查询 远程办公申请需要满足哪些条件，审批流程怎么走',
      '制度查询 帮我总结绩效申诉的受理范围、时限和材料要求',
      '制度查询 员工外部分享资料前，需要经过哪些合规检查',
      '制度查询 试用期、转正和岗位调整分别有哪些规定',
    ],
  },
  {
    label: '费用报销',
    icon: Clipboard,
    prompts: [
      '费用报销 客户拜访产生的交通、餐饮和住宿费用分别怎么报',
      '费用报销 加班后返程交通补贴有哪些适用条件和限制',
      '费用报销 发票抬头、税号和附件不完整时应该怎么处理',
      '费用报销 帮我整理一份常见报销被退回的原因清单',
    ],
  },
  {
    label: '入职成长',
    icon: BookOpenText,
    prompts: [
      '入职成长 新同事入职第一周需要完成哪些系统开通和培训',
      '入职成长 帮我梳理导师、直属主管和 HRBP 的支持事项',
      '入职成长 试用期转正前需要准备哪些材料和评估记录',
      '入职成长 公司有哪些学习平台、认证课程和内部分享机制',
    ],
  },
  {
    label: '办公支持',
    icon: FileSearch,
    prompts: [
      '办公支持 会议室设备连不上投屏时，应该按什么顺序排查',
      '办公支持 办公网络、VPN 和邮箱异常分别找哪个团队处理',
      '办公支持 公司常用软件权限如何申请，审批人是谁',
      '办公支持 工位、门禁、访客和快递相关问题应该怎么处理',
    ],
  },
  {
    label: '假勤福利',
    icon: CheckCircle,
    prompts: [
      '假勤福利 年假、病假和调休的申请规则有什么区别',
      '假勤福利 异地办公期间考勤异常，应该如何补充说明',
      '假勤福利 帮我查一下年度体检、补充保险和节日福利安排',
      '假勤福利 出差期间遇到周末或节假日，考勤应该如何计算',
    ],
  },
  {
    label: '采购合同',
    icon: Database,
    prompts: [
      '采购合同 新供应商准入需要哪些材料，谁负责审核',
      '采购合同 合同评审中法务、财务和业务部门分别看什么',
      '采购合同 小额采购和框架协议采购的流程有什么差异',
      '采购合同 合同变更、续签和终止分别需要走哪些流程',
    ],
  },
  {
    label: '项目资料',
    icon: Brain,
    prompts: [
      '项目资料 帮我汇总某个项目的里程碑、风险和待办事项',
      '项目资料 对比两版方案在范围、成本和交付周期上的变化',
      '项目资料 从复盘记录里提取可复用经验和需要避免的问题',
      '项目资料 查找客户沟通纪要中承诺过的交付内容和时间点',
    ],
  },
]

function ChatHeroTitle() {
  const reduceMotion = useReducedMotion()
  const typedElementRef = useRef<HTMLSpanElement>(null)
  const typedTitles = useMemo(() => {
    const titles = CHAT_EMPTY_TITLES.slice(1)

    for (let index = titles.length - 1; index > 0; index -= 1) {
      const randomIndex = Math.floor(Math.random() * (index + 1))
      const currentTitle = titles[index]
      titles[index] = titles[randomIndex]
      titles[randomIndex] = currentTitle
    }

    return titles
  }, [])

  useLayoutEffect(() => {
    if (reduceMotion || !typedElementRef.current) return

    typedElementRef.current.textContent = CHAT_EMPTY_TITLES[0]

    const typed = new Typed(typedElementRef.current, {
      strings: typedTitles,
      typeSpeed: 125,
      backSpeed: 100,
      backDelay: 3000,
      startDelay: 3000,
      smartBackspace: false,
      shuffle: false,
      loop: true,
      showCursor: true,
      cursorChar: '_',
    })

    return () => {
      typed.destroy()
    }
  }, [reduceMotion, typedTitles])

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

function parseTimeValue(value?: string | null) {
  if (!value) return undefined
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? undefined : parsed
}

function getConversationId(sessionId: number) {
  return `session-${sessionId}`
}

function getSessionMetadataKey(conversation: ChatConversation) {
  return String(conversation.assistantSessionId ?? conversation.id)
}

function loadSessionMetadata(): ChatSessionMetadata {
  if (typeof window === 'undefined') return {}

  try {
    const stored = window.localStorage.getItem(CHAT_SESSION_METADATA_KEY)
    if (!stored) return {}
    const parsed = JSON.parse(stored) as ChatSessionMetadata
    return parsed && typeof parsed === 'object' ? parsed : {}
  } catch {
    return {}
  }
}

function saveSessionMetadata(conversations: ChatConversation[]) {
  if (typeof window === 'undefined') return

  const metadata = conversations.reduce<ChatSessionMetadata>(
    (result, conversation) => {
      result[getSessionMetadataKey(conversation)] = {
        isPinned: Boolean(conversation.isPinned),
        isFavorited: Boolean(conversation.isFavorited),
        isArchived: Boolean(conversation.isArchived),
      }
      return result
    },
    {}
  )

  window.localStorage.setItem(
    CHAT_SESSION_METADATA_KEY,
    JSON.stringify(metadata)
  )
}

function getModeLabel(mode?: ChatAssistantMode | null) {
  if (mode === 'KB_SEARCH') return '本地知识'
  return '通用助手'
}

function buildConversationFromSession(
  session: AssistantSessionListItem,
  existingConversation?: ChatConversation,
  metadata = loadSessionMetadata()
): ChatConversation {
  const updatedAtValue = parseTimeValue(session.lastMessageAt) ?? Date.now()
  const sessionMetadata = metadata[String(session.sessionId)] ?? {}

  return {
    id: existingConversation?.id ?? getConversationId(session.sessionId),
    title: session.title || DEFAULT_CONVERSATION_TITLE,
    description: existingConversation?.description ?? '历史会话',
    assistantSessionId: session.sessionId,
    updatedAt: formatRelativeTime(updatedAtValue),
    createdAt: existingConversation?.createdAt ?? updatedAtValue,
    updatedAtValue,
    messages: existingConversation?.messages ?? [],
    isPinned: Boolean(sessionMetadata.isPinned),
    isFavorited: Boolean(sessionMetadata.isFavorited),
    isArchived: Boolean(sessionMetadata.isArchived),
  }
}

function parseMessageCitations(structuredPayload: string | null): Citation[] {
  if (!structuredPayload) return []

  try {
    const parsed = JSON.parse(structuredPayload) as {
      citations?: unknown
    }
    if (!Array.isArray(parsed.citations)) return []

    return parsed.citations.filter((citation): citation is Citation => {
      if (!citation || typeof citation !== 'object') return false
      const candidate = citation as Record<string, unknown>

      return (
        typeof candidate.documentId === 'number' &&
        typeof candidate.chunkId === 'number' &&
        typeof candidate.chunkIndex === 'number' &&
        typeof candidate.fileName === 'string' &&
        typeof candidate.score === 'number' &&
        typeof candidate.snippet === 'string'
      )
    })
  } catch {
    return []
  }
}

function buildMessageFromApiMessage(message: ApiAssistantMessage): ChatMessage {
  const role = message.role === 'USER' ? 'user' : 'assistant'
  const mode = message.toolMode ?? 'CHAT'

  return {
    id: message.messageId,
    role,
    mode,
    variant: role === 'assistant' && mode === 'KB_SEARCH' ? 'rag' : undefined,
    content: message.content,
    citations: parseMessageCitations(message.structuredPayload),
  }
}

function buildModeChangedMessage(mode: ChatAssistantMode, id: number): ChatMessage {
  const modeLabel =
    CHAT_ASSISTANT_MODE_OPTIONS.find((option) => option.value === mode)?.label ??
    '当前模式'

  return {
    id,
    role: 'event',
    eventType: 'mode_changed',
    mode,
    content: `已切换到 ${modeLabel}`,
  }
}

function insertModeChangeEvents(messages: ChatMessage[]) {
  const nextMessages: ChatMessage[] = []
  let previousMode: ChatAssistantMode | null = null

  messages.forEach((message) => {
    const messageMode = message.mode

    if (message.role !== 'event' && messageMode) {
      if (previousMode && previousMode !== messageMode) {
        nextMessages.push(buildModeChangedMessage(messageMode, -message.id))
      }
      previousMode = messageMode
    }

    nextMessages.push(message)
  })

  return nextMessages
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

function orderConversations(conversations: ChatConversation[]) {
  return [...conversations].sort((a, b) => {
    const aArchived = Boolean(a.isArchived)
    const bArchived = Boolean(b.isArchived)
    const aPinned = Boolean(a.isPinned)
    const bPinned = Boolean(b.isPinned)

    if (aArchived !== bArchived) return aArchived ? 1 : -1
    if (aPinned !== bPinned) return aPinned ? -1 : 1
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
  return {
    conversations: [],
    activeConversationId: null,
  }
}

function isAbortError(error: unknown) {
  return error instanceof DOMException && error.name === 'AbortError'
}

function getStreamErrorMessage(error: unknown) {
  if (error instanceof HttpStatusError) {
    if (error.status === 403) return '当前账号无权在此工作空间发起对话'
    if (error.status === 429) return '请求过于频繁，请稍后再试'
  }

  return extractApiError(error, '提问失败')
}

function applyToolResult(
  tools: ChatToolActivity[] | undefined,
  result: Extract<AssistantStreamEvent, { event: 'tool_result' }>['result']
) {
  const nextTools = [...(tools ?? [])]
  let targetIndex = -1

  for (let index = nextTools.length - 1; index >= 0; index -= 1) {
    const tool = nextTools[index]
    if (
      tool.state !== 'output-available' &&
      tool.state !== 'output-error' &&
      (tool.type === result.name || targetIndex === -1)
    ) {
      targetIndex = index
      if (tool.type === result.name) break
    }
  }

  const output = { content: result.content, ...result.metadata }
  if (targetIndex >= 0) {
    nextTools[targetIndex] = {
      ...nextTools[targetIndex],
      state: 'output-available',
      output,
    }
    return nextTools
  }

  return [
    ...nextTools,
    {
      key: `tool-result-${result.name}-${nextTools.length}`,
      type: result.name,
      state: 'output-available' as const,
      output,
    },
  ]
}

export function ChatHome() {
  const defaultOpen = getCookie('sidebar_state') !== 'false'
  const { collapsible, variant } = useLayout()
  const reduceMotion = useReducedMotion()
  const accessToken = useAuthStore((state) => state.auth.accessToken)
  const refreshSession = useAuthStore((state) => state.auth.refreshSession)
  const workspaces = useWorkspaceStore((state) => state.workspaces)
  const currentWorkspaceId = useWorkspaceStore(
    (state) => state.currentWorkspaceId
  )
  const sessionsQuery = useQuery({
    queryKey: ['assistant', 'sessions', currentWorkspaceId],
    queryFn: () => listAssistantSessions(currentWorkspaceId!),
    enabled: Boolean(currentWorkspaceId),
  })
  const groups = useMemo(
    () =>
      workspaces.map((workspace) => ({
        groupId: workspace.workspaceId,
        groupCode: String(workspace.workspaceId),
        groupName: workspace.name,
      })),
    [workspaces]
  )
  const [initialChatState] = useState(createInitialChatState)
  const [input, setInput] = useState('')
  const [files, setFiles] = useState<File[]>([])
  const [selectedGroupId, setSelectedGroupId] = useState('')
  const [assistantMode, setAssistantMode] =
    useState<ChatAssistantMode>('CHAT')
  const [deepThinking, setDeepThinking] = useState(false)
  const [isHeaderGlass, setIsHeaderGlass] = useState(false)
  const [scrollToLatestRequest, setScrollToLatestRequest] = useState(0)
  const messageIdRef = useRef(Date.now())
  const messageLoadRequestRef = useRef(0)
  const streamAbortControllerRef = useRef<AbortController | null>(null)
  const [conversations, setConversations] = useState<ChatConversation[]>(
    initialChatState.conversations
  )
  const [activeConversationId, setActiveConversationId] = useState(
    initialChatState.activeConversationId
  )
  const [loadingConversationId, setLoadingConversationId] = useState<string | null>(
    null
  )
  const [showConversationSkeleton, setShowConversationSkeleton] = useState(false)

  const activeConversation =
    conversations.find((conversation) => conversation.id === activeConversationId) ??
    null
  const messages = activeConversation?.messages ?? []
  const hasMessages = messages.length > 0
  const isLoadingActiveConversation =
    Boolean(activeConversationId) && loadingConversationId === activeConversationId
  const isStreaming = messages.some((message) => message.isStreaming)
  const lastAssistantMessageId = useMemo(() => {
    for (let index = messages.length - 1; index >= 0; index -= 1) {
      if (messages[index]?.role === 'assistant') {
        return messages[index].id
      }
    }

    return null
  }, [messages])
  const activeConversationTitle =
    activeConversation?.title ?? DEFAULT_CONVERSATION_TITLE
  const selectedGroup = groups.find(
    (group) => String(group.groupId) === selectedGroupId
  )
  const activeGroupId = selectedGroup?.groupId ?? groups[0]?.groupId ?? null
  const activeMode =
    CHAT_ASSISTANT_MODE_OPTIONS.find((mode) => mode.value === assistantMode) ??
    CHAT_ASSISTANT_MODE_OPTIONS[0]
  const activeModeLabel = activeMode.label
  const ActiveModeIcon = activeMode.icon

  useEffect(() => {
    window.localStorage.removeItem(LEGACY_CHAT_STORAGE_KEY)
  }, [])

  useEffect(() => {
    if (!sessionsQuery.data) return

    const metadata = loadSessionMetadata()

    setConversations((prev) => {
      const backendConversations = sessionsQuery.data.map((session) => {
        const existingConversation = prev.find(
          (conversation) => conversation.assistantSessionId === session.sessionId
        )

        return buildConversationFromSession(
          session,
          existingConversation,
          metadata
        )
      })

      const localPendingConversations = prev.filter(
        (conversation) =>
          !conversation.assistantSessionId && conversation.messages.length > 0
      )

      return orderConversations([
        ...localPendingConversations,
        ...backendConversations,
      ])
    })
  }, [sessionsQuery.data])

  useEffect(() => {
    if (!sessionsQuery.data) return
    saveSessionMetadata(conversations)
  }, [conversations, sessionsQuery.data])

  useEffect(() => {
    if (
      activeConversationId &&
      !conversations.some((conversation) => conversation.id === activeConversationId)
    ) {
      setActiveConversationId(null)
    }
  }, [activeConversationId, conversations])

  useEffect(() => {
    if (!isLoadingActiveConversation) {
      setShowConversationSkeleton(false)
      return
    }

    const timer = window.setTimeout(() => {
      setShowConversationSkeleton(true)
    }, 150)

    return () => window.clearTimeout(timer)
  }, [isLoadingActiveConversation])

  useEffect(() => {
    if (!selectedGroupId && groups[0]) {
      setSelectedGroupId(String(groups[0].groupId))
    }
  }, [groups, selectedGroupId])

  useEffect(
    () =>
      useWorkspaceStore.subscribe((state, previousState) => {
        if (state.currentWorkspaceId === previousState.currentWorkspaceId) return
        streamAbortControllerRef.current?.abort()
        streamAbortControllerRef.current = null
        messageLoadRequestRef.current += 1
        setLoadingConversationId(null)
        setConversations([])
        setActiveConversationId(null)
        setSelectedGroupId('')
        setInput('')
        setFiles([])
      }),
    []
  )

  useEffect(() => () => streamAbortControllerRef.current?.abort(), [])

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
      prev.map((conversation) =>
        conversation.messages.some((message) => message.id === messageId)
          ? {
              ...conversation,
              messages: conversation.messages.map((message) =>
                message.id === messageId
                  ? { ...message, isStreaming: false, isResponseComplete: true }
                  : message
              ),
            }
          : conversation
      )
    )
  }

  function abortActiveStream() {
    if (!streamAbortControllerRef.current) return
    streamAbortControllerRef.current.abort()
    streamAbortControllerRef.current = null
    setConversations((prev) =>
      prev.map((conversation) => ({
        ...conversation,
        messages: conversation.messages.map((message) =>
          message.isStreaming
            ? { ...message, isStreaming: false, isResponseComplete: true }
            : message
        ),
      }))
    )
  }

  function handleNewConversation() {
    abortActiveStream()
    messageLoadRequestRef.current += 1
    setLoadingConversationId(null)
    setActiveConversationId(null)
    setInput('')
    setFiles([])
  }

  async function loadConversationMessages(conversationId: string) {
    const requestId = messageLoadRequestRef.current + 1
    messageLoadRequestRef.current = requestId
    const targetConversation = conversations.find(
      (conversation) => conversation.id === conversationId
    )
    const sessionId = targetConversation?.assistantSessionId

    if (!sessionId) {
      setLoadingConversationId(null)
      return
    }

    setLoadingConversationId(conversationId)

    try {
      if (!currentWorkspaceId) return
      const context = await getAssistantContext(currentWorkspaceId, sessionId, 100)
      if (messageLoadRequestRef.current !== requestId) return

      const nextMessages = insertModeChangeEvents(
        context.recentMessages.map(buildMessageFromApiMessage)
      )
      const nextMaxMessageId = Math.max(
        messageIdRef.current,
        ...nextMessages
          .filter((message) => message.role !== 'event')
          .map((message) => message.id)
      )
      messageIdRef.current = Number.isFinite(nextMaxMessageId)
        ? nextMaxMessageId
        : messageIdRef.current

      setConversations((prev) =>
        prev.map((conversation) =>
          conversation.id === conversationId
            ? {
                ...conversation,
                description: getModeLabel(
                  nextMessages
                    .slice()
                    .reverse()
                    .find((message) => message.mode)?.mode
                ),
                messages: nextMessages,
              }
            : conversation
        )
      )
    } catch (error) {
      if (messageLoadRequestRef.current !== requestId) return
      toast.error(extractApiError(error, '加载会话失败'))
    } finally {
      if (messageLoadRequestRef.current === requestId) {
        setLoadingConversationId(null)
      }
    }
  }

  function handleSelectConversation(conversationId: string) {
    if (conversationId === activeConversationId && !loadingConversationId) return
    abortActiveStream()
    setActiveConversationId(conversationId)
    setInput('')
    setFiles([])
    void loadConversationMessages(conversationId)
  }

  async function handleDeleteConversation(conversationId: string) {
    const deletedConversation = conversations.find(
      (conversation) => conversation.id === conversationId
    )
    if (!deletedConversation) return

    try {
      if (deletedConversation.assistantSessionId) {
        await deleteAssistantSession(
          currentWorkspaceId!,
          deletedConversation.assistantSessionId
        )
      }
    } catch (error) {
      toast.error(extractApiError(error, '删除对话失败'))
      return
    }

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

  async function handleRenameConversation(conversationId: string, title: string) {
    const nextTitle = title.trim()
    if (!nextTitle) {
      toast.error('标题不能为空')
      return
    }
    const target = conversations.find(
      (conversation) => conversation.id === conversationId
    )
    const normalizedTitle = nextTitle.slice(0, 40)

    try {
      if (target?.assistantSessionId) {
        await renameAssistantSession(
          currentWorkspaceId!,
          target.assistantSessionId,
          normalizedTitle
        )
      }
    } catch (error) {
      toast.error(extractApiError(error, '重命名对话失败'))
      return
    }

    setConversations((prev) =>
      prev.map((conversation) =>
        conversation.id === conversationId
          ? {
              ...conversation,
              title: normalizedTitle,
            }
          : conversation
      )
    )
    toast.success('已重命名对话')
  }

  function handleTogglePin(conversationId: string) {
    const target = conversations.find(
      (conversation) => conversation.id === conversationId
    )
    if (!target) return

    const nextPinned = !target.isPinned
    setConversations((prev) =>
      orderConversations(
        prev.map((conversation) =>
          conversation.id === conversationId
            ? {
                ...conversation,
                isPinned: nextPinned,
              }
            : conversation
        )
      )
    )
    toast.success(nextPinned ? '已置顶对话' : '已取消置顶')
  }

  function handleToggleFavorite(conversationId: string) {
    const target = conversations.find(
      (conversation) => conversation.id === conversationId
    )
    if (!target) return

    const nextFavorited = !target.isFavorited
    setConversations((prev) =>
      prev.map((conversation) =>
        conversation.id === conversationId
          ? {
              ...conversation,
              isFavorited: nextFavorited,
            }
          : conversation
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
          ? {
              ...conversation,
              isArchived: nextArchived,
              isPinned: nextArchived ? false : conversation.isPinned,
            }
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
      assistantSessionId: undefined,
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

  async function handleClearArchived() {
    if (!conversations.some((conversation) => conversation.isArchived)) {
      toast.info('没有归档对话')
      return
    }

    const archivedConversations = conversations.filter(
      (conversation) => conversation.isArchived
    )

    try {
      await Promise.all(
        archivedConversations
          .map((conversation) => conversation.assistantSessionId)
          .filter((sessionId): sessionId is number => Boolean(sessionId))
          .map((sessionId) => deleteAssistantSession(currentWorkspaceId!, sessionId))
      )
    } catch (error) {
      toast.error(extractApiError(error, '清空归档失败'))
      return
    }

    setConversations((prev) =>
      orderConversations(prev.filter((conversation) => !conversation.isArchived))
    )
    toast.success('已清空归档')
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
                description: activeModeLabel,
                messages: messagesUpdater(conversation.messages),
              })
            : conversation
        )
      )
    )
  }

  function handleAssistantModeChange(value: string) {
    const nextMode = value as ChatAssistantMode
    if (nextMode === assistantMode) return

    setAssistantMode(nextMode)

    if (!activeConversation || activeConversation.messages.length === 0) {
      return
    }

    const nextModeLabel =
      CHAT_ASSISTANT_MODE_OPTIONS.find((mode) => mode.value === nextMode)?.label ??
      '当前模式'

    setConversations((prev) =>
      orderConversations(
        prev.map((conversation) =>
          conversation.id === activeConversation.id
            ? touchConversation((() => {
                const messages = conversation.messages
                const lastMessage = messages[messages.length - 1]

                if (lastMessage?.role === 'event') {
                  const previousMessage = [...messages]
                    .slice(0, -1)
                    .reverse()
                    .find((message) => message.role !== 'event')

                  if (previousMessage?.mode === nextMode) {
                    return {
                      ...conversation,
                      description: nextModeLabel,
                      messages: messages.slice(0, -1),
                    }
                  }

                  return {
                    ...conversation,
                    description: nextModeLabel,
                    messages: messages.map((message) =>
                      message.id === lastMessage.id
                        ? {
                            ...message,
                            mode: nextMode,
                            content: buildModeChangedMessage(nextMode, message.id)
                              .content,
                          }
                        : message
                    ),
                  }
                }

                return {
                ...conversation,
                description: nextModeLabel,
                  messages: [
                    ...messages,
                    {
                      ...buildModeChangedMessage(nextMode, createNextMessageId()),
                    },
                  ],
                }
              })())
            : conversation
        )
      )
    )
    setScrollToLatestRequest((current) => current + 1)
  }

  function updateConversationMessage(
    conversationId: string,
    messageId: number,
    messageUpdater: (message: ChatMessage) => ChatMessage
  ) {
    setConversations((prev) =>
      prev.map((conversation) =>
        conversation.id === conversationId
          ? {
              ...conversation,
              messages: conversation.messages.map((message) =>
                message.id === messageId ? messageUpdater(message) : message
              ),
            }
          : conversation
      )
    )
  }

  function updateConversationSession(
    conversationId: string,
    assistantSessionId: number
  ) {
    setConversations((prev) =>
      prev.map((conversation) =>
        conversation.id === conversationId
          ? {
              ...conversation,
              assistantSessionId,
            }
          : conversation
      )
    )
  }

  function stopStreaming() {
    abortActiveStream()
  }

  async function getStreamAccessToken() {
    if (accessToken) return accessToken

    try {
      const refreshedAccessToken = await refreshSession()
      if (refreshedAccessToken) return refreshedAccessToken
    } catch {
      toast.error('登录状态已失效，请重新登录')
      return null
    }

    toast.error('登录状态已失效，请重新登录')
    return null
  }

  async function streamAssistantReply({
    targetConversationId,
    assistantMessageId,
    question,
    assistantSessionId,
    initialAccessToken,
    toolMode,
    useDeepThinking,
  }: {
    targetConversationId: string
    assistantMessageId: number
    question: string
    assistantSessionId: number | null
    initialAccessToken: string
    toolMode: ChatAssistantMode
    useDeepThinking: boolean
  }) {
    const targetGroupId = toolMode === 'KB_SEARCH' ? activeGroupId : null
    if (toolMode === 'KB_SEARCH' && !targetGroupId) {
      const errorMessage = '请先在管理后台创建知识库并上传文档'
      toast.error(errorMessage)
      updateConversationMessage(
        targetConversationId,
        assistantMessageId,
        (message) => ({
          ...message,
          isStreaming: false,
          isResponseComplete: true,
          content: `请求失败：${errorMessage}`,
          citations: [],
        })
      )
      return
    }

    const abortController = new AbortController()
    streamAbortControllerRef.current = abortController
    let receivedTerminalEvent = false
    let currentAccessToken = initialAccessToken
    let currentAssistantSessionId = assistantSessionId

    const runAssistantStream = async (streamAccessToken: string) => {
      if (!currentAssistantSessionId) {
        if (!currentWorkspaceId) throw new Error('请先选择工作空间')
        const session = await createAssistantSession(
          currentWorkspaceId,
          question,
          toolMode,
          useDeepThinking
        )
        currentAssistantSessionId = session.sessionId
        updateConversationSession(targetConversationId, currentAssistantSessionId)
      }

      if (!currentWorkspaceId) throw new Error('请先选择工作空间')
      await streamAssistantChat(
        currentWorkspaceId,
        {
          sessionId: currentAssistantSessionId,
          message: question,
          toolMode,
          deepThinking: useDeepThinking,
        },
        streamAccessToken,
        (event) => {
          if (receivedTerminalEvent) return

          if (event.event === 'token' && event.content) {
            updateConversationMessage(
              targetConversationId,
              assistantMessageId,
              (message) => ({
                ...message,
                content: `${message.content}${event.content}`,
                isStreaming: true,
                isResponseComplete: false,
              })
            )
            return
          }

          if (event.event === 'thinking' && event.content) {
            updateConversationMessage(
              targetConversationId,
              assistantMessageId,
              (message) => ({
                ...message,
                thinking: `${message.thinking ?? ''}${
                  message.thinking && event.content.startsWith('步骤 ') ? '\n' : ''
                }${event.content}`,
              })
            )
            return
          }

          if (event.event === 'tool_call') {
            const toolKey = `tool-${createNextMessageId()}`
            updateConversationMessage(
              targetConversationId,
              assistantMessageId,
              (message) => ({
                ...message,
                tools: [
                  ...(message.tools ?? []),
                  {
                    key: toolKey,
                    type: event.tool.name,
                    state: 'input-streaming',
                    input: event.tool.input,
                    toolCallId: toolKey,
                  },
                ],
              })
            )
            return
          }

          if (event.event === 'tool_result') {
            updateConversationMessage(
              targetConversationId,
              assistantMessageId,
              (message) => ({
                ...message,
                tools: applyToolResult(message.tools, event.result),
              })
            )
            return
          }

          if (event.event === 'citation') {
            updateConversationMessage(
              targetConversationId,
              assistantMessageId,
              (message) => ({
                ...message,
                citations: [
                  ...(message.citations ?? []).filter(
                    (citation) => citation.chunkId !== event.citation.chunkId
                  ),
                  event.citation,
                ],
              })
            )
            return
          }

          if (event.event === 'done') {
            receivedTerminalEvent = true
            updateConversationMessage(
              targetConversationId,
              assistantMessageId,
              (message) => ({
                ...message,
                isStreaming: false,
                isResponseComplete: true,
              })
            )
            return
          }

          if (event.event === 'error') {
            receivedTerminalEvent = true
            const errorMessage = event.error || '助手流式响应失败'
            toast.error(errorMessage)
            updateConversationMessage(
              targetConversationId,
              assistantMessageId,
              (message) => ({
                ...message,
                isStreaming: false,
                isResponseComplete: true,
                streamError: errorMessage,
              })
            )
          }
        },
        abortController.signal
      )
    }

    try {
      try {
        await runAssistantStream(currentAccessToken)
      } catch (error) {
        if (!isUnauthorizedError(error)) {
          throw error
        }

        const refreshedAccessToken = await refreshSession()
        if (!refreshedAccessToken) throw error
        currentAccessToken = refreshedAccessToken
        await runAssistantStream(currentAccessToken)
      }

      if (!receivedTerminalEvent) {
        updateConversationMessage(
          targetConversationId,
          assistantMessageId,
          (message) => ({
            ...message,
            isStreaming: false,
            isResponseComplete: true,
          })
        )
      }

      void sessionsQuery.refetch()
    } catch (error) {
      if (isAbortError(error)) return

      const isUnauthorized = isUnauthorizedError(error)
      const errorMessage = isUnauthorized
        ? '登录状态已失效，请重新登录后再试'
        : getStreamErrorMessage(error)
      toast.error(errorMessage)
      updateConversationMessage(
        targetConversationId,
        assistantMessageId,
        (message) => ({
          ...message,
          isStreaming: false,
          isResponseComplete: true,
          streamError: errorMessage,
        })
      )
    } finally {
      if (streamAbortControllerRef.current === abortController) {
        streamAbortControllerRef.current = null
      }
    }
  }

  async function handleCopyMessage(content: string) {
    try {
      await navigator.clipboard.writeText(content)
      toast.success('消息已复制')
    } catch {
      toast.error('复制失败，请检查浏览器剪贴板权限')
    }
  }

  function handleEditUserMessage(content: string) {
    setInput(content)
    setFiles([])
  }

  async function handleRegenerateMessage(messageId: number) {
    if (isStreaming || !activeConversation) return

    const targetMessageIndex = activeConversation.messages.findIndex(
      (message) => message.id === messageId
    )
    const targetMessage = activeConversation.messages[targetMessageIndex]

    if (!targetMessage || targetMessage.role !== 'assistant') return

    const previousUserMessage = [...activeConversation.messages]
      .slice(0, targetMessageIndex)
      .reverse()
      .find((message) => message.role === 'user')
    const question = (targetMessage.query ?? previousUserMessage?.content ?? '').trim()

    if (!question) {
      toast.error('找不到可重新生成的问题')
      return
    }

    const requestMode = targetMessage.mode ?? assistantMode
    if (requestMode === 'KB_SEARCH' && !activeGroupId) {
      toast.error('请先在管理后台创建知识库并上传文档')
      return
    }

    const currentAccessToken = await getStreamAccessToken()
    if (!currentAccessToken) return

    const assistantMessageId = createNextMessageId()
    const nextAssistantMessage: ChatMessage = {
      id: assistantMessageId,
      role: 'assistant',
      mode: requestMode,
      variant: requestMode === 'KB_SEARCH' ? 'rag' : 'code',
      isLive: true,
      isStreaming: true,
      isResponseComplete: false,
      query: question,
      citations: [],
      content: '',
    }

    setConversations((prev) =>
      orderConversations(
        prev.map((conversation) =>
          conversation.id === activeConversation.id
            ? touchConversation({
                ...conversation,
                messages: conversation.messages.map((message) =>
                  message.id === messageId ? nextAssistantMessage : message
                ),
              })
            : conversation
        )
      )
    )
    setScrollToLatestRequest((current) => current + 1)

    await streamAssistantReply({
      targetConversationId: activeConversation.id,
      assistantMessageId,
      question,
      assistantSessionId: activeConversation.assistantSessionId ?? null,
      initialAccessToken: currentAccessToken,
      toolMode: requestMode,
      useDeepThinking: deepThinking,
    })
  }

  async function handleSubmit(nextInput = input, nextFiles = files) {
    const trimmedInput = nextInput.trim()
    if ((!trimmedInput && nextFiles.length === 0) || isStreaming) return
    const requestMode = assistantMode
    if (requestMode === 'KB_SEARCH' && !activeGroupId) {
      toast.error('请先在管理后台创建知识库并上传文档')
      return
    }

    const currentAccessToken = await getStreamAccessToken()
    if (!currentAccessToken) return

    const baseId = createNextMessageId()
    const userMessage: ChatMessage = {
      id: baseId,
      role: 'user',
      mode: requestMode,
      content: trimmedInput || '请分析我上传的文件。',
      files: nextFiles.map((file) => file.name),
    }
    const assistantMessageId = createNextMessageId()
    const assistantMessage: ChatMessage = {
      id: assistantMessageId,
      role: 'assistant',
      mode: requestMode,
      variant: requestMode === 'KB_SEARCH' ? 'rag' : 'code',
      isLive: true,
      isStreaming: true,
      isResponseComplete: false,
      query: userMessage.content,
      content: '',
    }
    let targetConversationId = activeConversation?.id ?? null
    const assistantSessionId = activeConversation?.assistantSessionId ?? null

    if (!activeConversation) {
      const nextConversation = touchConversation({
        ...createEmptyConversation(),
        title: userMessage.content.slice(0, 24),
        description:
          requestMode === 'KB_SEARCH'
            ? selectedGroup?.groupName ?? '知识问答'
            : '通用助手',
        messages: [userMessage, assistantMessage],
      })
      targetConversationId = nextConversation.id
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
    setScrollToLatestRequest((current) => current + 1)

    if (!targetConversationId) return

    await streamAssistantReply({
      targetConversationId,
      assistantMessageId,
      question: userMessage.content,
      assistantSessionId,
      initialAccessToken: currentAccessToken,
      toolMode: requestMode,
      useDeepThinking: deepThinking,
    })
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
    const activeSuggestion = showSuggestions
      ? suggestions.find((suggestion) => suggestion.label === input.trim())
      : null

    return (
      <motion.div
        layout={!reduceMotion}
        layoutId={reduceMotion ? undefined : 'chat-prompt-composer'}
        className={cn('w-full px-3 md:px-5', className)}
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
        <div className='relative mx-auto w-full max-w-3xl'>
          <PromptInput
            className={cn(
              'border-input rounded-2xl border bg-popover p-0 pt-1 shadow-xs',
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
              disableAutosize
              className='h-22 min-h-22 max-h-22 overflow-y-auto px-5 pt-4 text-sm md:text-[15px] [field-sizing:fixed]'
            />

            <PromptInputActions className='mt-5 flex w-full items-center justify-between gap-2 px-3 pb-3'>
              <div className='flex min-w-0 items-center gap-2'>
                <PromptInputAction tooltip='Attach files'>
                  <FileUploadTrigger asChild>
                    <Button
                      type='button'
                      variant='outline'
                      size='icon-lg'
                      className='rounded-full'
                      disabled={isStreaming}
                    >
                      <Plus />
                      <span className='sr-only'>Attach files</span>
                    </Button>
                  </FileUploadTrigger>
                </PromptInputAction>

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      type='button'
                      variant='outline'
                      size='lg'
                      className='rounded-full'
                      disabled={isStreaming}
                      onClick={(event) => event.stopPropagation()}
                    >
                      <ActiveModeIcon />
                      {activeMode.label}
                      <ChevronDown data-icon='inline-end' />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align='start' className='min-w-44'>
                    <DropdownMenuGroup>
                      <DropdownMenuLabel>选择问答模式</DropdownMenuLabel>
                      <DropdownMenuRadioGroup
                        value={assistantMode}
                        onValueChange={handleAssistantModeChange}
                      >
                        {CHAT_ASSISTANT_MODE_OPTIONS.map((mode) => {
                          const ModeIcon = mode.icon

                          return (
                            <DropdownMenuRadioItem
                              key={mode.value}
                              value={mode.value}
                            >
                              <ModeIcon />
                              {mode.label}
                            </DropdownMenuRadioItem>
                          )
                        })}
                      </DropdownMenuRadioGroup>
                    </DropdownMenuGroup>
                  </DropdownMenuContent>
                </DropdownMenu>

                <div className='flex h-10 items-center gap-2 rounded-full border border-input px-3'>
                  <Brain className='size-4 text-muted-foreground' />
                  <span className='hidden whitespace-nowrap text-sm sm:inline'>
                    深度思考
                  </span>
                  <Switch
                    size='sm'
                    checked={deepThinking}
                    onCheckedChange={setDeepThinking}
                    disabled={isStreaming}
                    aria-label='深度思考'
                  />
                </div>

                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button
                      type='button'
                      variant='outline'
                      size='icon-lg'
                      className='rounded-full'
                      disabled={isStreaming}
                      aria-label='更多输入操作'
                      onClick={(event) => event.stopPropagation()}
                    >
                      <MoreHorizontal />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align='start' className='min-w-44'>
                    <DropdownMenuGroup>
                      <DropdownMenuItem onClick={handleNewConversation}>
                        <SquarePen />
                        新建对话
                      </DropdownMenuItem>
                    </DropdownMenuGroup>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>

              <div className='flex items-center gap-2'>
                <PromptInputAction tooltip='语音输入'>
                  <Button
                    type='button'
                    variant='outline'
                    size='icon-lg'
                    className='rounded-full'
                    disabled={isStreaming}
                    onClick={() => toast.info('语音输入暂未接入')}
                    aria-label='语音输入'
                  >
                    <Mic />
                  </Button>
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
                      <Square className='size-3.5 fill-current' />
                    ) : (
                      <ArrowUpIcon className='size-5' />
                    )}
                  </Button>
                </PromptInputAction>
              </div>
            </PromptInputActions>
          </PromptInput>

          {showSuggestions && activeSuggestion ? (
            <motion.div
              key={activeSuggestion.label}
              className='absolute top-full right-4 left-4 z-10 mt-5 flex flex-col gap-1.5'
              initial={reduceMotion ? false : { opacity: 0, y: 8 }}
              animate={reduceMotion ? undefined : { opacity: 1, y: 0 }}
              exit={reduceMotion ? undefined : { opacity: 0, y: 6 }}
              transition={
                reduceMotion
                  ? undefined
                  : { duration: 0.22, ease: [0.22, 1, 0.36, 1] }
              }
            >
              {activeSuggestion.prompts.map((prompt, index) => (
                <motion.div
                  key={prompt}
                  initial={reduceMotion ? false : { opacity: 0, y: 4 }}
                  animate={reduceMotion ? undefined : { opacity: 1, y: 0 }}
                  transition={
                    reduceMotion
                      ? undefined
                      : {
                          delay: index * 0.04,
                          duration: 0.2,
                          ease: [0.22, 1, 0.36, 1],
                        }
                  }
                >
                  <PromptSuggestion
                    highlight={activeSuggestion.label}
                    onClick={() => setInput(prompt)}
                    className='h-auto min-h-9 rounded-lg px-3 py-2 text-left text-[15px] leading-6'
                  >
                    {prompt}
                  </PromptSuggestion>
                </motion.div>
              ))}
            </motion.div>
          ) : showSuggestions ? (
            <div className='absolute top-full right-4 left-4 z-10 mt-5 flex flex-wrap justify-center gap-2.5'>
              {suggestions.map((suggestion, index) => (
                <motion.div
                  key={suggestion.label}
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
                  <PromptSuggestion
                    size='lg'
                    onClick={() => setInput(suggestion.label)}
                    className='rounded-full px-5 shadow-xs has-data-[icon=inline-start]:px-5'
                  >
                    <suggestion.icon data-icon='inline-start' />
                    <span>{suggestion.label}</span>
                  </PromptSuggestion>
                </motion.div>
              ))}
            </div>
          ) : null}
        </div>
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
          isLoadingConversations={sessionsQuery.isLoading}
          onGoHome={handleNewConversation}
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
                'pointer-events-none absolute inset-x-0 top-0 z-30 grid h-16 grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-2 bg-background/95 px-4 transition-all duration-200',
                isHeaderGlass &&
                  'bg-background/80 shadow-sm backdrop-blur-xl supports-[backdrop-filter]:bg-background/70'
              )}
            >
              <div className='pointer-events-auto flex items-center gap-2'>
                <SidebarTrigger />
              </div>
              <div className='pointer-events-auto flex min-w-0 items-center justify-center gap-2 text-sm font-medium'>
                <span className='truncate'>{activeConversationTitle}</span>
              </div>
              <div className='pointer-events-auto flex min-w-0 items-center justify-end'>
                <HeaderActions
                  showSearch={false}
                  showAdminLink
                  showProfileAccountLinks={false}
                />
              </div>
            </header>

            <main className='flex min-h-0 flex-1'>
              <LayoutGroup id='chat-thread-layout'>
                <div className='flex min-w-0 flex-1 flex-col'>
                {showConversationSkeleton ? (
                  <>
                    <div className='relative min-h-0 flex-1'>
                      <motion.div
                        key='conversation-skeleton'
                        className='h-full'
                        initial={reduceMotion ? false : { opacity: 0 }}
                        animate={reduceMotion ? undefined : { opacity: 1 }}
                        transition={
                          reduceMotion
                            ? undefined
                            : { duration: 0.16, ease: 'easeOut' }
                        }
                      >
                        <ChatConversationSkeleton />
                      </motion.div>
                    </div>

                    {renderPromptComposer({
                      className: 'pt-1 pb-4',
                    })}
                  </>
                ) : hasMessages ? (
                  <>
                    <div className='relative min-h-0 flex-1'>
                      <ChatContainerRoot className='h-full'>
                        <ChatContainerScrollToBottom
                          trigger={scrollToLatestRequest}
                        />
                        <ChatContainerContent
                          className='flex w-full flex-col gap-5 px-5 pt-24 pb-14'
                          scrollClassName='overflow-y-auto [scrollbar-gutter:stable]'
                          onScroll={handleChatScroll}
                        >
                          <motion.div
                            key={activeConversationId ?? 'empty-conversation'}
                            className='flex flex-col gap-5'
                            initial={reduceMotion ? false : { opacity: 0 }}
                            animate={reduceMotion ? undefined : { opacity: 1 }}
                            transition={
                              reduceMotion
                                ? undefined
                                : { duration: 0.16, ease: 'easeOut' }
                            }
                          >
                            <AnimatePresence initial={false}>
                              {messages.map((message) => (
                                <motion.div
                                  key={message.id}
                                  className='mx-auto flex w-full max-w-3xl flex-col px-6'
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
                                    canRegenerate={
                                      message.id === lastAssistantMessageId &&
                                      !message.isStreaming
                                    }
                                    onCopyMessage={handleCopyMessage}
                                    onEditUserMessage={handleEditUserMessage}
                                    onRegenerateMessage={handleRegenerateMessage}
                                    onStreamingComplete={() => markMessageComplete(message.id)}
                                  />
                                </motion.div>
                              ))}
                            </AnimatePresence>
                          </motion.div>
                          <ChatContainerScrollAnchor />
                        </ChatContainerContent>

                        <div className='absolute bottom-4 left-1/2 -translate-x-1/2'>
                          <ScrollButton className='shadow-sm' />
                        </div>
                      </ChatContainerRoot>
                    </div>

                    {renderPromptComposer({
                      className: 'pt-1 pb-4',
                    })}
                  </>
                ) : (
                  <div
                    className='min-h-0 flex-1 overflow-y-auto px-4 pt-20 pb-8 [scrollbar-gutter:stable]'
                    onScroll={handleChatScroll}
                  >
                    <motion.div
                      className='mx-auto flex min-h-full w-full max-w-3xl flex-col items-center justify-center gap-15 pt-4 pb-[14vh]'
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
                      <div className='flex w-full max-w-3xl flex-col items-center gap-3 text-center'>
                        <div className='flex flex-col'>
                          <ChatHeroTitle />
                        </div>
                      </div>

                      {renderPromptComposer({
                        showSuggestions: true,
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

function ChatConversationSkeleton() {
  return (
    <div className='h-full overflow-hidden'>
      <div className='mx-auto flex w-full max-w-3xl flex-col gap-6 px-11 pt-24 pb-14'>
        <div className='flex justify-end'>
          <div className='flex w-full max-w-[78%] flex-col items-end gap-2'>
            <Skeleton className='h-10 w-2/3 rounded-2xl' />
            <Skeleton className='h-4 w-24 rounded-full' />
          </div>
        </div>
        <div className='flex w-full flex-col gap-3'>
          <Skeleton className='h-4 w-28 rounded-full' />
          <Skeleton className='h-4 w-full rounded-full' />
          <Skeleton className='h-4 w-11/12 rounded-full' />
          <Skeleton className='h-4 w-4/5 rounded-full' />
        </div>
        <div className='flex justify-end'>
          <div className='flex w-full max-w-[70%] flex-col items-end gap-2'>
            <Skeleton className='h-10 w-full rounded-2xl' />
            <Skeleton className='h-4 w-20 rounded-full' />
          </div>
        </div>
        <div className='flex w-full flex-col gap-3'>
          <Skeleton className='h-4 w-32 rounded-full' />
          <Skeleton className='h-4 w-10/12 rounded-full' />
          <Skeleton className='h-4 w-9/12 rounded-full' />
        </div>
      </div>
    </div>
  )
}

function ChatHistorySkeleton() {
  return (
    <div className='mr-1 min-h-0 flex-1 overflow-hidden pr-1'>
      <SidebarGroupLabel>最近</SidebarGroupLabel>
      <SidebarMenu className='gap-1'>
        {Array.from({ length: 8 }).map((_, index) => (
          <SidebarMenuSkeleton key={index} className='h-8 px-3' />
        ))}
      </SidebarMenu>
    </div>
  )
}

function ChatHistorySidebar({
  collapsible,
  variant,
  conversations,
  activeConversationId,
  isLoadingConversations,
  onGoHome,
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
}: {
  collapsible: Collapsible
  variant: ChatSidebarVariant
  conversations: ChatConversation[]
  activeConversationId: string | null
  isLoadingConversations: boolean
  onGoHome: () => void
  onNewConversation: () => void
  onSelectConversation: (conversationId: string) => void
  onDeleteConversation: (conversationId: string) => void | Promise<void>
  onRenameConversation: (
    conversationId: string,
    title: string
  ) => void | Promise<void>
  onTogglePin: (conversationId: string) => void
  onToggleFavorite: (conversationId: string) => void
  onToggleArchive: (conversationId: string) => void
  onDuplicateConversation: (conversationId: string) => void
  onExportConversation: (conversationId: string) => void
  onClearArchived: () => void | Promise<void>
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
    void onRenameConversation(renamingConversation.id, renameTitle)
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
        className='group/menu-item relative'
      >
        <SidebarMenuButton
          isActive={isActive}
          tooltip={conversation.title}
          onClick={() => onSelectConversation(conversation.id)}
          className='relative min-h-8 [--sidebar-menu-icon-size:1rem] pl-5 pr-10 font-normal leading-5 group-hover/menu-item:bg-sidebar-accent group-hover/menu-item:text-sidebar-accent-foreground group-focus-within/menu-item:bg-sidebar-accent group-focus-within/menu-item:text-sidebar-accent-foreground data-active:bg-primary/10 data-active:font-normal data-active:text-foreground dark:data-active:bg-primary/15 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:pr-0'
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
          <span className='block min-w-0 max-w-full flex-1 overflow-hidden text-ellipsis whitespace-nowrap text-sm leading-5 group-data-[collapsible=icon]:hidden'>
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
          <TeamSwitcher
            teams={CHAT_SIDEBAR_TEAMS}
            homeHref='/'
            onHomeClick={() => {
              setQuery('')
              setView('active')
              onGoHome()
            }}
          />
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
                    tooltip='搜索'
                    onClick={() => openSidebarView('active', { search: true })}
                    className='justify-center'
                  >
                    <Search />
                    <span className='sr-only'>搜索</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip='对话'
                    onClick={() => openSidebarView('active')}
                    isActive={view === 'active'}
                    className='justify-center'
                  >
                    <MessageSquare />
                    <span className='sr-only'>对话</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip='收藏'
                    onClick={() => openSidebarView('favorites')}
                    isActive={view === 'favorites'}
                    className='justify-center'
                  >
                    <Star />
                    <span className='sr-only'>收藏</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
                <SidebarMenuItem>
                  <SidebarMenuButton
                    tooltip='归档'
                    onClick={() => openSidebarView('archived')}
                    isActive={view === 'archived'}
                    className='justify-center'
                  >
                    <Archive />
                    <span className='sr-only'>归档</span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>

          <SidebarGroup className='min-h-0 flex-1 px-2 pt-0 pb-2 group-data-[collapsible=icon]:hidden'>
            <SidebarGroupContent
              className='flex h-full min-h-0 flex-col overflow-hidden'
            >
              {isLoadingConversations ? (
                <ChatHistorySkeleton />
              ) : visibleConversations.length > 0 ? (
                <div className='mr-1 min-h-0 flex-1 overflow-x-hidden overflow-y-auto pr-1 [scrollbar-gutter:stable]'>
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
                修改后的标题会同步到后端会话。
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
                  void onDeleteConversation(deletingConversation.id)
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
                void onClearArchived()
                setIsClearArchiveOpen(false)
              }}
            >
              清空
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

    </>
  )
}

function ChatMessageItem({
  message,
  canRegenerate,
  onCopyMessage,
  onEditUserMessage,
  onRegenerateMessage,
  onStreamingComplete,
}: {
  message: ChatMessage
  canRegenerate: boolean
  onCopyMessage: (content: string) => void
  onEditUserMessage: (content: string) => void
  onRegenerateMessage: (messageId: number) => void
  onStreamingComplete: () => void
}) {
  if (message.role === 'event') {
    return <ChatModeEvent message={message} />
  }

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
            canRegenerate={canRegenerate}
            onCopy={() => onCopyMessage(message.content)}
            onRegenerate={() => onRegenerateMessage(message.id)}
            onStreamingComplete={onStreamingComplete}
          />
        ) : (
          <UserMessage
            message={message}
            onCopy={() => onCopyMessage(message.content)}
            onEdit={() => onEditUserMessage(message.content)}
          />
        )}
      </div>
    </Message>
  )
}

function ChatModeEvent({ message }: { message: ChatMessage }) {
  return (
    <div className='flex w-full justify-center py-1'>
      <div className='flex w-full max-w-sm items-center gap-3 text-xs text-muted-foreground'>
        <span aria-hidden='true' className='h-px flex-1 bg-border' />
        <span className='shrink-0 whitespace-nowrap'>{message.content}</span>
        <span aria-hidden='true' className='h-px flex-1 bg-border' />
      </div>
    </div>
  )
}

function MessageActionIconButton({
  tooltip,
  onClick,
  disabled,
  children,
}: {
  tooltip: string
  onClick: () => void
  disabled?: boolean
  children: ReactNode
}) {
  return (
    <MessageAction tooltip={tooltip}>
      <Button
        type='button'
        variant='ghost'
        size='icon-sm'
        disabled={disabled}
        onClick={onClick}
        aria-label={tooltip}
        className='text-muted-foreground'
      >
        {children}
      </Button>
    </MessageAction>
  )
}

function UserMessage({
  message,
  onCopy,
  onEdit,
}: {
  message: ChatMessage
  onCopy: () => void
  onEdit: () => void
}) {
  return (
    <div className='flex flex-col items-end gap-2'>
      <MessageContent className='text-[15px] leading-7'>
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
      <MessageActions className='justify-end'>
        <MessageActionIconButton tooltip='编辑消息' onClick={onEdit}>
          <Edit3 />
        </MessageActionIconButton>
        <MessageActionIconButton tooltip='复制消息' onClick={onCopy}>
          <Copy />
        </MessageActionIconButton>
      </MessageActions>
    </div>
  )
}

const ASSISTANT_CONTENT_CLASS_NAME = 'bg-transparent p-0 text-[15px] leading-7'

function StreamingMarkdownContent({
  content,
  isComplete,
  onComplete,
}: {
  content: string
  isComplete: boolean
  onComplete: () => void
}) {
  const reduceMotion = useReducedMotion()
  const [displayedText, setDisplayedText] = useState('')
  const targetRef = useRef(content)
  const displayedRef = useRef('')
  const isCompleteRef = useRef(isComplete)
  const onCompleteRef = useRef(onComplete)
  const didCompleteRef = useRef(false)

  useEffect(() => {
    targetRef.current = content

    if (content.length < displayedRef.current.length) {
      displayedRef.current = content
      setDisplayedText(content)
    }
  }, [content])

  useEffect(() => {
    isCompleteRef.current = isComplete
    if (!isComplete) didCompleteRef.current = false
  }, [isComplete])

  useEffect(() => {
    onCompleteRef.current = onComplete
  }, [onComplete])

  useEffect(() => {
    if (reduceMotion) {
      displayedRef.current = content
      setDisplayedText(content)

      if (isComplete && !didCompleteRef.current) {
        didCompleteRef.current = true
        onCompleteRef.current()
      }

      return
    }

    let frame: number | null = null
    let lastFrameTime = 0

    const tick = (timestamp: number) => {
      const target = targetRef.current
      const current = displayedRef.current

      if (current.length < target.length) {
        if (timestamp - lastFrameTime >= 18) {
          const remaining = target.length - current.length
          const chunkSize = remaining > 80 ? 6 : remaining > 24 ? 4 : 2
          const nextText = target.slice(0, current.length + chunkSize)

          displayedRef.current = nextText
          setDisplayedText(nextText)
          lastFrameTime = timestamp
        }

        frame = requestAnimationFrame(tick)
        return
      }

      if (isCompleteRef.current && !didCompleteRef.current) {
        didCompleteRef.current = true
        onCompleteRef.current()
        return
      }

      frame = requestAnimationFrame(tick)
    }

    frame = requestAnimationFrame(tick)

    return () => {
      if (frame) cancelAnimationFrame(frame)
    }
  }, [content, isComplete, reduceMotion])
  return (
    <MessageContent markdown className={ASSISTANT_CONTENT_CLASS_NAME}>
      {displayedText}
    </MessageContent>
  )
}

function AssistantMessage({
  message,
  canRegenerate,
  onCopy,
  onRegenerate,
  onStreamingComplete,
}: {
  message: ChatMessage
  canRegenerate: boolean
  onCopy: () => void
  onRegenerate: () => void
  onStreamingComplete: () => void
}) {
  const showActions = !message.isStreaming && Boolean(message.content)

  return (
    <div className='flex flex-col gap-3'>
      {message.thinking ? (
        <ChainOfThought>
          <ChainOfThoughtStep defaultOpen={message.isStreaming}>
            <ChainOfThoughtTrigger leftIcon={<Brain />}>
              {message.isStreaming ? '正在思考' : '思考过程'}
            </ChainOfThoughtTrigger>
            <ChainOfThoughtContent>
              <ChainOfThoughtItem className='whitespace-pre-wrap leading-6'>
                {message.thinking}
              </ChainOfThoughtItem>
            </ChainOfThoughtContent>
          </ChainOfThoughtStep>
        </ChainOfThought>
      ) : null}

      {message.tools?.map((tool) => (
        <Tool
          key={tool.key}
          toolPart={tool}
          defaultOpen={tool.state === 'input-streaming'}
          className='mt-0'
        />
      ))}

      {message.isStreaming && !message.content ? (
        <div className='flex items-center gap-2 text-sm text-muted-foreground'>
          <Loader variant='typing' size='sm' />
          <TextShimmer>正在生成回答</TextShimmer>
        </div>
      ) : null}

      {message.isStreaming && message.content ? (
        <StreamingMarkdownContent
          content={message.content}
          isComplete={!message.isLive || Boolean(message.isResponseComplete)}
          onComplete={onStreamingComplete}
        />
      ) : message.content ? (
        <MessageContent
          markdown
          className={ASSISTANT_CONTENT_CLASS_NAME}
          children={message.content}
        />
      ) : null}

      {message.citations?.length ? (
        <div className='flex flex-col gap-2'>
          <div className='text-xs font-medium text-muted-foreground'>引用来源</div>
          <div className='flex flex-wrap gap-2'>
            {message.citations.map((citation, index) => (
              <div
                key={`${citation.documentId}-${citation.chunkId}`}
                className='flex min-w-0 max-w-full items-center gap-2 rounded-md border bg-muted/30 px-2.5 py-1.5 text-xs'
                title={citation.snippet || citation.fileName}
              >
                <span className='shrink-0 font-medium'>[{index + 1}]</span>
                <span className='truncate'>{citation.fileName || '知识文档'}</span>
                {Number.isFinite(citation.score) ? (
                  <span className='shrink-0 text-muted-foreground'>
                    {citation.score.toFixed(3)}
                  </span>
                ) : null}
              </div>
            ))}
          </div>
        </div>
      ) : null}

      {message.streamError ? (
        <div className='rounded-md border border-destructive/30 bg-destructive/5 px-3 py-2 text-sm text-destructive'>
          {message.content ? '回答中断：' : '请求失败：'}
          {message.streamError}
        </div>
      ) : null}

      {showActions ? (
        <MessageActions>
          <MessageActionIconButton tooltip='复制回答' onClick={onCopy}>
            <Copy />
          </MessageActionIconButton>
          {canRegenerate ? (
            <MessageActionIconButton
              tooltip='重新生成'
              onClick={onRegenerate}
            >
              <RotateCcw />
            </MessageActionIconButton>
          ) : null}
        </MessageActions>
      ) : null}

    </div>
  )
}
