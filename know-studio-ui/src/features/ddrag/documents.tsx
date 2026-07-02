import {
  useEffect,
  useMemo,
  useState,
  type ChangeEvent,
  type FormEvent,
} from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link, useNavigate } from "@tanstack/react-router"
import {
  type ColumnDef,
  type ColumnFiltersState,
  type PaginationState,
  type SortingState,
  type VisibilityState,
  flexRender,
  getCoreRowModel,
  getFacetedRowModel,
  getFacetedUniqueValues,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
} from "@tanstack/react-table"
import {
  CheckCircledIcon,
  CrossCircledIcon,
  StopwatchIcon,
} from "@radix-ui/react-icons"
import {
  ArrowLeft,
  CheckSquare,
  Database,
  Eye,
  FileText,
  MoreHorizontal,
  Pencil,
  Plus,
  RotateCcw,
  Trash2,
  Upload,
} from "lucide-react"
import { motion, useReducedMotion } from "motion/react"
import { toast } from "sonner"
import { cn } from "@/lib/utils"
import {
  deleteDocument,
  listDocuments,
  previewDocument,
  retryDocumentIngestion,
  type DocumentListItem,
  type DocumentPreview,
} from "@/api/documents"
import { createGroup, getMyGroups } from "@/api/groups"
import { extractApiError } from "@/api/http"
import { Header } from "@/components/layout/header"
import { HeaderActions } from "@/components/layout/header-actions"
import { Main } from "@/components/layout/main"
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from "@/components/ui/breadcrumb"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Checkbox } from "@/components/ui/checkbox"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  DataTableBulkActions,
  DataTableColumnHeader,
  DataTablePagination,
  DataTableToolbar,
} from "@/components/data-table"
import {
  Field,
  FieldDescription,
  FieldGroup,
  FieldLabel,
} from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Loader } from "@/components/ui/loader"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Textarea } from "@/components/ui/textarea"
import {
  uploadDocumentWithResume,
  type UploadProgressPayload,
} from "./document-upload"
import { DocumentStatusBadge } from "./document-status-badge"
import { formatDateTime, formatFileSize, mergeGroups } from "./shared"

export function DocumentsPage() {
  const documentManager = useDocumentManagement()
  const groupsQuery = useQuery({
    queryKey: ["groups", "my"],
    queryFn: getMyGroups,
  })
  const groups = useMemo(() => mergeGroups(groupsQuery.data), [groupsQuery.data])
  const groupNameById = useMemo(() => getGroupNameById(groups), [groups])
  const [activeView, setActiveView] = useState("knowledge-bases")
  const stats = useMemo(
    () => getKnowledgeBaseStats(groups, documentManager.documents),
    [groups, documentManager.documents]
  )
  const knowledgeBases = useMemo(
    () => getKnowledgeBases(groups, documentManager.documents),
    [groups, documentManager.documents]
  )
  const isLoading = documentManager.isLoading || groupsQuery.isPending

  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>

      <Main className="flex flex-1 flex-col gap-4 pt-4 sm:gap-6">
        <div className="flex flex-wrap items-end justify-between gap-2">
          <div className="flex flex-col gap-2">
            <KnowledgeBaseBreadcrumb />
            <h2 className="text-2xl font-bold tracking-tight">知识库管理</h2>
          </div>
          <KnowledgeBasePrimaryButtons groups={groups} />
        </div>

        <KnowledgeBaseStats stats={stats} />

        <Tabs
          value={activeView}
          onValueChange={setActiveView}
          className="flex flex-1 flex-col gap-4"
        >
          <TabsList className="w-fit">
            <TabsTrigger value="knowledge-bases">知识库</TabsTrigger>
            <TabsTrigger value="documents">文档</TabsTrigger>
          </TabsList>
          <TabsContent value="knowledge-bases" className="m-0 flex flex-col">
            <KnowledgeBaseList
              data={knowledgeBases}
              isLoading={isLoading}
            />
          </TabsContent>
          <TabsContent value="documents" className="m-0 flex flex-1 flex-col">
            <KnowledgeBaseTable
              data={documentManager.documents}
              isLoading={isLoading}
              groupNameById={groupNameById}
              isActionPending={documentManager.isActionPending}
              isPreviewPending={documentManager.isPreviewPending}
              onPreview={documentManager.handlePreview}
              onRetry={documentManager.handleRetry}
              onDelete={documentManager.handleDelete}
              onDeleteMany={documentManager.handleDeleteMany}
            />
          </TabsContent>
        </Tabs>
      </Main>

      <DocumentPreviewDialog
        preview={documentManager.preview}
        onOpenChange={(open) => {
          if (!open) documentManager.closePreview()
        }}
      />
    </>
  )
}

export function KnowledgeBaseDocumentsPage({ groupId }: { groupId: number }) {
  const documentManager = useDocumentManagement()
  const groupsQuery = useQuery({
    queryKey: ["groups", "my"],
    queryFn: getMyGroups,
  })
  const groups = useMemo(() => mergeGroups(groupsQuery.data), [groupsQuery.data])
  const groupNameById = useMemo(() => getGroupNameById(groups), [groups])
  const knowledgeBase = groups.find((item) => item.groupId === groupId)
  const knowledgeBaseName =
    knowledgeBase?.groupName ?? formatKnowledgeBaseName(groupId)
  const documents = useMemo(
    () =>
      documentManager.documents.filter(
        (document) => document.groupId === groupId
      ),
    [documentManager.documents, groupId]
  )
  const [isUploadDialogOpen, setIsUploadDialogOpen] = useState(false)
  const isLoading = documentManager.isLoading || groupsQuery.isPending

  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>

      <Main className="flex flex-1 flex-col gap-4 pt-4 sm:gap-6">
        <div className="flex flex-wrap items-end justify-between gap-2">
          <div className="flex min-w-0 flex-col gap-2">
            <KnowledgeBaseBreadcrumb knowledgeBaseName={knowledgeBaseName} />
            <h2 className="truncate text-2xl font-bold tracking-tight">
              {knowledgeBaseName}
            </h2>
          </div>
          <div className="flex gap-2">
            <Button asChild variant="outline">
              <Link to="/admin/documents">
                <ArrowLeft data-icon="inline-start" />
                返回知识库
              </Link>
            </Button>
            <Button onClick={() => setIsUploadDialogOpen(true)}>
              上传文档
              <Upload data-icon="inline-end" />
            </Button>
          </div>
        </div>

        <KnowledgeBaseTable
          data={documents}
          isLoading={isLoading}
          knowledgeBaseName={knowledgeBaseName}
          groupNameById={groupNameById}
          searchPlaceholder="按文档名称、格式或上传人搜索"
          isActionPending={documentManager.isActionPending}
          isPreviewPending={documentManager.isPreviewPending}
          onPreview={documentManager.handlePreview}
          onRetry={documentManager.handleRetry}
          onDelete={documentManager.handleDelete}
          onDeleteMany={documentManager.handleDeleteMany}
        />
      </Main>

      <DocumentPreviewDialog
        preview={documentManager.preview}
        onOpenChange={(open) => {
          if (!open) documentManager.closePreview()
        }}
      />
      <UploadDocumentDialog
        open={isUploadDialogOpen}
        onOpenChange={setIsUploadDialogOpen}
        groups={groups}
        defaultGroupId={groupId}
      />
    </>
  )
}

type KnowledgeBaseBreadcrumbProps = {
  knowledgeBaseName?: string
}

function KnowledgeBaseBreadcrumb({
  knowledgeBaseName,
}: KnowledgeBaseBreadcrumbProps) {
  return (
    <Breadcrumb>
      <BreadcrumbList>
        <BreadcrumbItem>
          <BreadcrumbLink asChild>
            <Link to="/admin">管理后台</Link>
          </BreadcrumbLink>
        </BreadcrumbItem>
        <BreadcrumbSeparator />
        {knowledgeBaseName ? (
          <>
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to="/admin/documents">知识库</Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem>
              <BreadcrumbPage className="max-w-64 truncate">
                {knowledgeBaseName}
              </BreadcrumbPage>
            </BreadcrumbItem>
          </>
        ) : (
          <BreadcrumbItem>
            <BreadcrumbPage>知识库</BreadcrumbPage>
          </BreadcrumbItem>
        )}
      </BreadcrumbList>
    </Breadcrumb>
  )
}

function useDocumentManagement() {
  const queryClient = useQueryClient()
  const [preview, setPreview] = useState<DocumentPreview | null>(null)

  const documentsQuery = useQuery({
    queryKey: ["documents"],
    queryFn: () => listDocuments(),
  })

  const actionMutation = useMutation({
    mutationFn: async (fn: () => Promise<void>) => fn(),
    onSuccess: () => {
      toast.success("操作成功")
      queryClient.invalidateQueries({ queryKey: ["documents"] })
    },
    onError: (error) => toast.error(extractApiError(error, "操作失败")),
  })

  const previewMutation = useMutation({
    mutationFn: (document: DocumentListItem) =>
      previewDocument(document.documentId, document.groupId),
    onSuccess: setPreview,
    onError: (error) => toast.error(extractApiError(error, "获取预览失败")),
  })

  const backendDocuments = documentsQuery.data ?? []
  const documents = backendDocuments

  function handlePreview(document: DocumentListItem) {
    if (document.previewText) {
      setPreview({
        documentId: document.documentId,
        fileName: document.fileName,
        previewText: document.previewText,
      })
      return
    }

    previewMutation.mutate(document)
  }

  function handleRetry(document: DocumentListItem) {
    if (isMockDocument(document)) {
      toast.info("示例数据暂不支持重新入库")
      return
    }

    actionMutation.mutate(() =>
      retryDocumentIngestion(document.documentId, document.groupId)
    )
  }

  function handleDelete(document: DocumentListItem) {
    if (isMockDocument(document)) {
      toast.info("示例数据暂不支持删除")
      return
    }

    actionMutation.mutate(() =>
      deleteDocument(document.documentId, document.groupId)
    )
  }

  function handleDeleteMany(documents: DocumentListItem[]) {
    if (documents.some(isMockDocument)) {
      toast.info("示例数据暂不支持批量删除")
      return
    }

    actionMutation.mutate(async () => {
      await Promise.all(
        documents.map((document) =>
          deleteDocument(document.documentId, document.groupId)
        )
      )
    })
  }

  return {
    documents,
    preview,
    isLoading: documentsQuery.isPending,
    isActionPending: actionMutation.isPending,
    isPreviewPending: previewMutation.isPending,
    handlePreview,
    handleRetry,
    handleDelete,
    handleDeleteMany,
    closePreview: () => setPreview(null),
  }
}

type KnowledgeBaseStat = {
  title: string
  value: string
  description: string
  icon: typeof FileText | typeof CheckCircledIcon
  iconClassName?: string
}

type KnowledgeBaseItem = {
  groupId: number
  name: string
  embeddingModel: string
  documentCount: number
  readyCount: number
  processingCount: number
  failedCount: number
  totalSize: number
  updatedAt: string | null
  uploaderNames: string[]
}

type ManagedGroup = ReturnType<typeof mergeGroups>[number]

const knowledgeBaseNameByGroupId = new Map<number, string>([
  [1, "销售作战知识库"],
  [2, "客户成功知识库"],
  [3, "研发与运维知识库"],
  [4, "法务与市场资料库"],
  [5, "财务与客服知识库"],
  [6, "采购与评测知识库"],
])

const knowledgeBaseEmbeddingModelByGroupId = new Map<number, string>([
  [1, "bge-m3"],
  [2, "text-embedding-3-large"],
  [3, "gte-Qwen2-7B-instruct"],
  [4, "bge-large-zh-v1.5"],
  [5, "text-embedding-v4"],
  [6, "m3e-large"],
])

function formatKnowledgeBaseName(groupId: number) {
  return knowledgeBaseNameByGroupId.get(groupId) ?? `知识库 ${groupId}`
}

function formatKnowledgeBaseEmbeddingModel(groupId: number) {
  return knowledgeBaseEmbeddingModelByGroupId.get(groupId) ?? "bge-m3"
}

function getKnowledgeBaseDisplayName(
  groupId: number,
  groupNameById?: Map<number, string>
) {
  return groupNameById?.get(groupId) ?? formatKnowledgeBaseName(groupId)
}

function getGroupNameById(groups: ManagedGroup[]) {
  return new Map(groups.map((group) => [group.groupId, group.groupName]))
}

function getKnowledgeBases(
  groups: ManagedGroup[],
  documents: DocumentListItem[]
) {
  const byGroupId = new Map<number, KnowledgeBaseItem>()

  for (const group of groups) {
    byGroupId.set(group.groupId, {
      groupId: group.groupId,
      name: group.groupName,
      embeddingModel: formatKnowledgeBaseEmbeddingModel(group.groupId),
      documentCount: 0,
      readyCount: 0,
      processingCount: 0,
      failedCount: 0,
      totalSize: 0,
      updatedAt: null,
      uploaderNames: [],
    })
  }

  for (const document of documents) {
    const item = byGroupId.get(document.groupId) ?? {
      groupId: document.groupId,
      name: formatKnowledgeBaseName(document.groupId),
      embeddingModel: formatKnowledgeBaseEmbeddingModel(document.groupId),
      documentCount: 0,
      readyCount: 0,
      processingCount: 0,
      failedCount: 0,
      totalSize: 0,
      updatedAt: null,
      uploaderNames: [],
    }

    item.documentCount += 1
    item.totalSize += document.fileSize
    item.updatedAt =
      !item.updatedAt || new Date(document.uploadedAt) > new Date(item.updatedAt)
        ? document.uploadedAt
        : item.updatedAt

    if (!item.uploaderNames.includes(document.uploaderDisplayName)) {
      item.uploaderNames.push(document.uploaderDisplayName)
    }

    if (document.status === "READY") item.readyCount += 1
    if (document.status === "PROCESSING") item.processingCount += 1
    if (document.status === "FAILED") item.failedCount += 1

    byGroupId.set(document.groupId, item)
  }

  return Array.from(byGroupId.values()).sort(
    (a, b) =>
      new Date(b.updatedAt ?? 0).getTime() -
        new Date(a.updatedAt ?? 0).getTime() || b.groupId - a.groupId
  )
}

function getKnowledgeBaseStats(
  groups: ManagedGroup[],
  documents: DocumentListItem[]
) {
  const knowledgeBaseCount =
    groups.length || new Set(documents.map((item) => item.groupId)).size
  const processingCount = documents.filter(
    (item) => item.status === "PROCESSING"
  ).length
  const failedCount = documents.filter(
    (item) => item.status === "FAILED"
  ).length

  return [
    {
      title: "知识库数",
      value: knowledgeBaseCount.toLocaleString(),
      description: "当前接入的知识空间",
      icon: Database,
    },
    {
      title: "文档数",
      value: documents.length.toLocaleString(),
      description: `累计存储 ${formatFileSize(
        documents.reduce((total, item) => total + item.fileSize, 0)
      )}`,
      icon: FileText,
    },
    {
      title: "处理中",
      value: processingCount.toLocaleString(),
      description: "等待解析与向量化",
      icon: StopwatchIcon,
    },
    {
      title: "入库失败",
      value: failedCount.toLocaleString(),
      description: "需要重试或人工处理",
      icon: CrossCircledIcon,
    },
  ] satisfies KnowledgeBaseStat[]
}

function KnowledgeBaseStats({ stats }: { stats: KnowledgeBaseStat[] }) {
  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      {stats.map((item) => {
        const Icon = item.icon

        return (
          <Card key={item.title} size="sm">
            <CardHeader className="flex flex-row items-center justify-between gap-0 pb-1">
              <CardTitle className="text-sm font-medium">
                {item.title}
              </CardTitle>
              <Icon
                className={cn(
                  "size-4 text-muted-foreground",
                  item.iconClassName
                )}
              />
            </CardHeader>
            <CardContent className="space-y-0.5">
              <div className="text-2xl font-semibold tracking-tight">
                {item.value}
              </div>
              <p className="text-xs text-muted-foreground">
                {item.description}
              </p>
            </CardContent>
          </Card>
        )
      })}
    </div>
  )
}

function KnowledgeBasePrimaryButtons({ groups }: { groups: ManagedGroup[] }) {
  const [isCreateDialogOpen, setIsCreateDialogOpen] = useState(false)
  const [isUploadDialogOpen, setIsUploadDialogOpen] = useState(false)

  return (
    <>
      <div className="flex gap-2">
        <Button
          variant="outline"
          disabled={groups.length === 0}
          onClick={() => setIsUploadDialogOpen(true)}
        >
          导入文档
          <Upload data-icon="inline-end" />
        </Button>
        <Button onClick={() => setIsCreateDialogOpen(true)}>
          新建
          <Plus data-icon="inline-end" />
        </Button>
      </div>
      <CreateKnowledgeBaseDialog
        open={isCreateDialogOpen}
        onOpenChange={setIsCreateDialogOpen}
      />
      <UploadDocumentDialog
        open={isUploadDialogOpen}
        onOpenChange={setIsUploadDialogOpen}
        groups={groups}
      />
    </>
  )
}

type CreateKnowledgeBaseDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
}

function CreateKnowledgeBaseDialog({
  open,
  onOpenChange,
}: CreateKnowledgeBaseDialogProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [values, setValues] = useState({ name: "", description: "" })

  const createMutation = useMutation({
    mutationFn: createGroup,
    onSuccess: (groupId) => {
      toast.success("知识库已创建")
      setValues({ name: "", description: "" })
      onOpenChange(false)
      queryClient.invalidateQueries({ queryKey: ["groups"] })
      queryClient.invalidateQueries({ queryKey: ["documents"] })
      navigate({
        to: "/admin/documents/$groupId",
        params: { groupId: String(groupId) },
      })
    },
    onError: (error) => toast.error(extractApiError(error, "创建知识库失败")),
  })

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const name = values.name.trim()
    if (!name) return

    createMutation.mutate({
      name,
      description: values.description.trim() || undefined,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>新建知识库</DialogTitle>
          <DialogDescription>
            创建企业知识空间后，可以继续上传文档并用于问答检索。
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <FieldGroup>
            <Field>
              <FieldLabel htmlFor="knowledgeBaseName">知识库名称</FieldLabel>
              <Input
                id="knowledgeBaseName"
                value={values.name}
                maxLength={128}
                disabled={createMutation.isPending}
                onChange={(event) =>
                  setValues((prev) => ({ ...prev, name: event.target.value }))
                }
              />
            </Field>
            <Field>
              <FieldLabel htmlFor="knowledgeBaseDescription">描述</FieldLabel>
              <Textarea
                id="knowledgeBaseDescription"
                value={values.description}
                maxLength={512}
                disabled={createMutation.isPending}
                className="min-h-20"
                onChange={(event) =>
                  setValues((prev) => ({
                    ...prev,
                    description: event.target.value,
                  }))
                }
              />
              <FieldDescription>
                可填写资料范围、适用团队或检索场景。
              </FieldDescription>
            </Field>
          </FieldGroup>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              disabled={createMutation.isPending}
              onClick={() => onOpenChange(false)}
            >
              取消
            </Button>
            <Button
              type="submit"
              disabled={createMutation.isPending || !values.name.trim()}
            >
              {createMutation.isPending ? (
                <Loader variant="classic" size="sm" />
              ) : null}
              创建
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

type UploadDocumentDialogProps = {
  open: boolean
  onOpenChange: (open: boolean) => void
  groups: ManagedGroup[]
  defaultGroupId?: number
}

function UploadDocumentDialog({
  open,
  onOpenChange,
  groups,
  defaultGroupId,
}: UploadDocumentDialogProps) {
  const queryClient = useQueryClient()
  const [selectedGroupId, setSelectedGroupId] = useState("")
  const [selectedFile, setSelectedFile] = useState<File | null>(null)
  const [progress, setProgress] = useState<UploadProgressPayload | null>(null)

  useEffect(() => {
    if (!open) return

    setSelectedGroupId((current) => {
      if (defaultGroupId) return String(defaultGroupId)
      if (current) return current
      return groups[0] ? String(groups[0].groupId) : ""
    })
  }, [defaultGroupId, groups, open])

  const uploadMutation = useMutation({
    mutationFn: async () => {
      const groupId = Number(selectedGroupId)
      if (!Number.isFinite(groupId) || groupId <= 0) {
        throw new Error("请选择知识库")
      }
      if (!selectedFile) {
        throw new Error("请选择要上传的文档")
      }

      return uploadDocumentWithResume(groupId, selectedFile, setProgress)
    },
    onSuccess: () => {
      toast.success("文档已上传，正在解析入库")
      queryClient.invalidateQueries({ queryKey: ["documents"] })
      onOpenChange(false)
      setSelectedFile(null)
      setProgress(null)
    },
    onError: (error) => toast.error(extractApiError(error, "上传文档失败")),
  })

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    setSelectedFile(event.target.files?.[0] ?? null)
    setProgress(null)
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    uploadMutation.mutate()
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>上传文档</DialogTitle>
          <DialogDescription>
            文档上传完成后会自动进入解析、切片和向量化流程。
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <FieldGroup>
            <Field>
              <FieldLabel>知识库</FieldLabel>
              <Select
                value={selectedGroupId}
                disabled={Boolean(defaultGroupId) || uploadMutation.isPending}
                onValueChange={setSelectedGroupId}
              >
                <SelectTrigger className="w-full">
                  <SelectValue placeholder="选择知识库" />
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
              <FieldLabel htmlFor="knowledgeBaseDocument">文档文件</FieldLabel>
              <Input
                id="knowledgeBaseDocument"
                type="file"
                disabled={uploadMutation.isPending}
                onChange={handleFileChange}
              />
              <FieldDescription>
                小文件直传，大文件自动使用分片续传。
              </FieldDescription>
            </Field>
            {selectedFile ? (
              <div className="flex items-center justify-between gap-3 rounded-lg border bg-muted/30 px-3 py-2 text-sm">
                <span className="truncate">{selectedFile.name}</span>
                <span className="shrink-0 text-muted-foreground">
                  {formatFileSize(selectedFile.size)}
                </span>
              </div>
            ) : null}
            {progress ? <UploadProgressLine progress={progress} /> : null}
          </FieldGroup>
          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              disabled={uploadMutation.isPending}
              onClick={() => onOpenChange(false)}
            >
              取消
            </Button>
            <Button
              type="submit"
              disabled={
                uploadMutation.isPending ||
                !selectedGroupId ||
                !selectedFile ||
                groups.length === 0
              }
            >
              {uploadMutation.isPending ? (
                <Loader variant="classic" size="sm" />
              ) : null}
              上传
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function UploadProgressLine({
  progress,
}: {
  progress: UploadProgressPayload
}) {
  return (
    <div className="flex flex-col gap-2 text-sm">
      <div className="flex items-center justify-between gap-3">
        <span className="text-muted-foreground">
          {formatUploadStage(progress.stage)}
        </span>
        <span>{progress.percent}%</span>
      </div>
      <div className="h-2 overflow-hidden rounded-full bg-muted">
        <div
          className="h-full rounded-full bg-primary transition-all"
          style={{ width: `${progress.percent}%` }}
        />
      </div>
    </div>
  )
}

function formatUploadStage(stage: UploadProgressPayload["stage"]) {
  const stageText = {
    hashing: "计算文件指纹",
    checking: "检查上传状态",
    uploading: "上传文档",
    completing: "完成入库",
  }

  return stageText[stage]
}

type KnowledgeBaseListProps = {
  data: KnowledgeBaseItem[]
  isLoading: boolean
}

function KnowledgeBaseList({ data, isLoading }: KnowledgeBaseListProps) {
  const shouldReduceMotion = useReducedMotion()
  const [rowSelection, setRowSelection] = useState({})
  const [sorting, setSorting] = useState<SortingState>([])
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({})
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [globalFilter, setGlobalFilter] = useState("")
  const [isSelectionMode, setIsSelectionMode] = useState(false)
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: 6,
  })

  const columns = useMemo<ColumnDef<KnowledgeBaseItem>[]>(
    () => [
      ...(isSelectionMode
        ? [
            {
              id: "select",
              header: ({ table }) => (
                <Checkbox
                  checked={
                    table.getIsAllPageRowsSelected() ||
                    (table.getIsSomePageRowsSelected() && "indeterminate")
                  }
                  onCheckedChange={(value) =>
                    table.toggleAllPageRowsSelected(Boolean(value))
                  }
                  aria-label="选择全部"
                  className="translate-y-0.5"
                />
              ),
              cell: ({ row }) => (
                <Checkbox
                  checked={row.getIsSelected()}
                  onCheckedChange={(value) =>
                    row.toggleSelected(Boolean(value))
                  }
                  aria-label="选择知识库"
                  className="translate-y-0.5"
                />
              ),
              enableSorting: false,
              enableHiding: false,
              meta: {
                className: "w-10",
                tdClassName: "w-10 ps-4",
              },
            } satisfies ColumnDef<KnowledgeBaseItem>,
          ]
        : []),
      {
        accessorKey: "name",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="知识库" />
        ),
        meta: {
          label: "知识库",
          className: "ps-1 max-w-0 w-1/4",
          tdClassName: "ps-4",
        },
        cell: ({ row }) => (
          <Button
            asChild
            variant="ghost"
            className="-ms-2 h-8 max-w-full justify-start px-2 hover:bg-primary/10 hover:text-primary"
          >
            <Link
              to="/admin/documents/$groupId"
              params={{ groupId: String(row.original.groupId) }}
            >
              <span className="truncate font-medium">{row.original.name}</span>
            </Link>
          </Button>
        ),
      },
      {
        accessorKey: "embeddingModel",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="Embedding 模型" />
        ),
        meta: {
          label: "Embedding 模型",
          className: "ps-1 max-w-0 w-[18%]",
          tdClassName: "ps-4",
        },
        cell: ({ row }) => (
          <div className="truncate">{row.original.embeddingModel}</div>
        ),
      },
      {
        accessorKey: "documentCount",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="文档数" />
        ),
        meta: { label: "文档数", className: "ps-1", tdClassName: "ps-4" },
        cell: ({ row }) =>
          row.getValue<number>("documentCount").toLocaleString(),
      },
      {
        id: "status",
        accessorFn: (row) => getKnowledgeBaseStatus(row),
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="状态分布" />
        ),
        meta: {
          label: "状态分布",
          className: "ps-1 w-48",
          tdClassName: "ps-4",
        },
        cell: ({ row }) => (
          <div className="flex flex-nowrap items-center gap-2 text-xs text-muted-foreground">
            <span>可用 {row.original.readyCount}</span>
            <span>处理中 {row.original.processingCount}</span>
            <span>失败 {row.original.failedCount}</span>
          </div>
        ),
        filterFn: (row, id, value) => value.includes(row.getValue(id)),
      },
      {
        accessorKey: "totalSize",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="存储" />
        ),
        meta: { label: "存储", className: "ps-1", tdClassName: "ps-4" },
        cell: ({ row }) => formatFileSize(row.getValue<number>("totalSize")),
      },
      {
        accessorKey: "uploaderNames",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="成员" />
        ),
        meta: { label: "成员", className: "ps-1", tdClassName: "ps-4" },
        cell: ({ row }) => (
          <div className="max-w-48 truncate">
            {row.original.uploaderNames.slice(0, 3).join("、")}
          </div>
        ),
        sortingFn: (a, b) =>
          a.original.uploaderNames.length - b.original.uploaderNames.length,
      },
      {
        accessorKey: "updatedAt",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="最近更新" />
        ),
        meta: { label: "最近更新", className: "ps-1", tdClassName: "ps-4" },
        cell: ({ row }) => formatDateTime(row.getValue<string>("updatedAt")),
      },
      {
        id: "actions",
        cell: ({ row }) => <KnowledgeBaseRowActions item={row.original} />,
        enableSorting: false,
        enableHiding: false,
      },
    ],
    [isSelectionMode]
  )

  const table = useReactTable({
    data,
    columns,
    state: {
      sorting,
      columnVisibility,
      rowSelection,
      columnFilters,
      globalFilter,
      pagination,
    },
    enableRowSelection: true,
    onRowSelectionChange: setRowSelection,
    onSortingChange: setSorting,
    onColumnVisibilityChange: setColumnVisibility,
    onColumnFiltersChange: setColumnFilters,
    onGlobalFilterChange: setGlobalFilter,
    onPaginationChange: setPagination,
    globalFilterFn: (row, _columnId, filterValue) => {
      const searchValue = String(filterValue).toLowerCase()
      const item = row.original
      const text = [
        item.name,
        item.embeddingModel,
        item.documentCount,
        item.readyCount,
        item.processingCount,
        item.failedCount,
        formatFileSize(item.totalSize),
        item.uploaderNames.join(" "),
        formatDateTime(item.updatedAt),
        getKnowledgeBaseStatus(item),
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()

      return text.includes(searchValue)
    },
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFacetedRowModel: getFacetedRowModel(),
    getFacetedUniqueValues: getFacetedUniqueValues(),
  })

  const selectedKnowledgeBases = table
    .getFilteredSelectedRowModel()
    .rows.map((row) => row.original)
  const visibleRows = table.getRowModel().rows
  const paginationAnimationKey = `${pagination.pageIndex}-${pagination.pageSize}`
  const handleToggleSelectionMode = () => {
    if (isSelectionMode) {
      table.resetRowSelection()
      setIsSelectionMode(false)
      return
    }

    setIsSelectionMode(true)
  }

  return (
    <div
      className={cn(
        'max-sm:has-[div[role="toolbar"]]:mb-16',
        "flex flex-1 flex-col gap-4"
      )}
    >
      <DataTableToolbar
        table={table}
        searchPlaceholder="按知识库名称、成员或状态搜索"
        searchAction={
          <SelectAllRowsButton
            isSelectionMode={isSelectionMode}
            onClick={handleToggleSelectionMode}
          />
        }
        filters={[
          {
            columnId: "status",
            title: "状态",
            options: documentStatusFilters,
          },
        ]}
      />

      <motion.div
        key={paginationAnimationKey}
        className="overflow-hidden rounded-md border"
        initial={
          shouldReduceMotion ? false : { opacity: 0, filter: "blur(1px)" }
        }
        animate={
          shouldReduceMotion ? undefined : { opacity: 1, filter: "blur(0px)" }
        }
        transition={{ duration: 0.16, ease: [0.22, 1, 0.36, 1] }}
      >
        <Table className="min-w-xl">
          <TableHeader>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id}>
                {headerGroup.headers.map((header) => (
                  <TableHead
                    key={header.id}
                    colSpan={header.colSpan}
                    className={cn(
                      header.column.columnDef.meta?.className,
                      header.column.columnDef.meta?.thClassName
                    )}
                  >
                    {header.isPlaceholder
                      ? null
                      : flexRender(
                          header.column.columnDef.header,
                          header.getContext()
                        )}
                  </TableHead>
                ))}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {visibleRows.length ? (
              visibleRows.map((row) => (
                <TableRow
                  key={row.id}
                  data-state={row.getIsSelected() && "selected"}
                >
                  {row.getVisibleCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      className={cn(
                        cell.column.columnDef.meta?.className,
                        cell.column.columnDef.meta?.tdClassName
                      )}
                    >
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell
                  colSpan={columns.length}
                  className="h-24 text-center text-muted-foreground"
                >
                  {isLoading ? "加载知识库中" : "暂无知识库"}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </motion.div>

      <DataTablePagination table={table} className="mt-auto" />
      {isSelectionMode ? (
        <DataTableBulkActions
          table={table}
          entityName="knowledge base"
          entityNamePlural="knowledge bases"
        >
          <Button
            variant="destructive"
            size="sm"
            onClick={() => {
              toast.info(`已选择 ${selectedKnowledgeBases.length} 个知识库`)
            }}
          >
            删除
            <Trash2 data-icon="inline-end" />
          </Button>
        </DataTableBulkActions>
      ) : null}
    </div>
  )
}

function getKnowledgeBaseStatus(item: KnowledgeBaseItem) {
  if (item.failedCount > 0) return "FAILED"
  if (item.processingCount > 0) return "PROCESSING"
  return "READY"
}

type SelectAllRowsButtonProps = {
  isSelectionMode: boolean
  onClick: () => void
}

function SelectAllRowsButton({
  isSelectionMode,
  onClick,
}: SelectAllRowsButtonProps) {
  return (
    <Button
      variant={isSelectionMode ? "secondary" : "outline"}
      size="sm"
      onClick={onClick}
      className="h-8 w-fit"
    >
      <CheckSquare data-icon="inline-start" />
      {isSelectionMode ? "取消选择" : "选择"}
    </Button>
  )
}

type KnowledgeBaseRowActionsProps = {
  item: KnowledgeBaseItem
}

function KnowledgeBaseRowActions({ item }: KnowledgeBaseRowActionsProps) {
  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          className="flex size-8 p-0 data-[state=open]:bg-muted"
        >
          <MoreHorizontal />
          <span className="sr-only">打开菜单</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-40">
        <DropdownMenuItem asChild>
          <Link
            to="/admin/documents/$groupId"
            params={{ groupId: String(item.groupId) }}
          >
            管理文档
            <DropdownMenuShortcut>
              <Eye />
            </DropdownMenuShortcut>
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem onClick={() => toast.info("知识库编辑流程稍后接入")}>
          编辑
          <DropdownMenuShortcut>
            <Pencil />
          </DropdownMenuShortcut>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          variant="destructive"
          onClick={() => toast.info("知识库删除流程稍后接入")}
        >
          删除
          <DropdownMenuShortcut>
            <Trash2 />
          </DropdownMenuShortcut>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

type KnowledgeBaseTableProps = {
  data: DocumentListItem[]
  isLoading: boolean
  knowledgeBaseName?: string | null
  groupNameById?: Map<number, string>
  searchPlaceholder?: string
  isActionPending: boolean
  isPreviewPending: boolean
  onPreview: (document: DocumentListItem) => void
  onRetry: (document: DocumentListItem) => void
  onDelete: (document: DocumentListItem) => void
  onDeleteMany: (documents: DocumentListItem[]) => void
}

const documentStatusFilters = [
  {
    label: "READY",
    value: "READY",
    icon: CheckCircledIcon,
    iconClassName: "text-success",
  },
  {
    label: "PROCESSING",
    value: "PROCESSING",
    icon: StopwatchIcon,
    iconClassName: "text-warning",
  },
  {
    label: "FAILED",
    value: "FAILED",
    icon: CrossCircledIcon,
    iconClassName: "text-destructive",
  },
]

function isMockDocument(document: DocumentListItem) {
  return document.documentId < 0
}

function KnowledgeBaseTable({
  data,
  isLoading,
  knowledgeBaseName = null,
  groupNameById,
  searchPlaceholder = "按文档名称、知识库或上传人搜索",
  isActionPending,
  isPreviewPending,
  onPreview,
  onRetry,
  onDelete,
  onDeleteMany,
}: KnowledgeBaseTableProps) {
  const shouldReduceMotion = useReducedMotion()
  const [rowSelection, setRowSelection] = useState({})
  const [sorting, setSorting] = useState<SortingState>([])
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({})
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([])
  const [globalFilter, setGlobalFilter] = useState("")
  const [isSelectionMode, setIsSelectionMode] = useState(false)
  const [pagination, setPagination] = useState<PaginationState>({
    pageIndex: 0,
    pageSize: 6,
  })

  const columns = useMemo<ColumnDef<DocumentListItem>[]>(
    () => [
      ...(isSelectionMode
        ? [
            {
              id: "select",
              header: ({ table }) => (
                <Checkbox
                  checked={
                    table.getIsAllPageRowsSelected() ||
                    (table.getIsSomePageRowsSelected() && "indeterminate")
                  }
                  onCheckedChange={(value) =>
                    table.toggleAllPageRowsSelected(Boolean(value))
                  }
                  aria-label="选择全部"
                  className="translate-y-0.5"
                />
              ),
              cell: ({ row }) => (
                <Checkbox
                  checked={row.getIsSelected()}
                  onCheckedChange={(value) =>
                    row.toggleSelected(Boolean(value))
                  }
                  aria-label="选择文档"
                  className="translate-y-0.5"
                />
              ),
              enableSorting: false,
              enableHiding: false,
              meta: {
                className: "w-10",
                tdClassName: "w-10 ps-4",
              },
            } satisfies ColumnDef<DocumentListItem>,
          ]
        : []),
      {
        accessorKey: "fileName",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="文档" />
        ),
        meta: {
          label: "文档",
          className: "ps-1 max-w-0 w-[30%]",
          tdClassName: "ps-4",
        },
        cell: ({ row }) => (
          <div className="truncate font-medium">{row.original.fileName}</div>
        ),
      },
      ...(knowledgeBaseName
        ? []
        : [
            {
              accessorKey: "groupId",
              header: ({ column }) => (
                <DataTableColumnHeader column={column} title="知识库" />
              ),
              meta: {
                label: "知识库",
                className: "ps-1",
                tdClassName: "ps-4",
              },
              cell: ({ row }) =>
                getKnowledgeBaseDisplayName(row.original.groupId, groupNameById),
            } satisfies ColumnDef<DocumentListItem>,
          ]),
      {
        accessorKey: "status",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="状态" />
        ),
        meta: { label: "状态", className: "ps-1", tdClassName: "ps-4" },
        cell: ({ row }) => {
          const status = row.getValue<string>("status")
          return <DocumentStatusBadge status={status} />
        },
        filterFn: (row, id, value) => {
          return value.includes(row.getValue(id))
        },
      },
      {
        accessorKey: "fileSize",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="大小" />
        ),
        meta: { label: "大小", className: "ps-1", tdClassName: "ps-4" },
        cell: ({ row }) => formatFileSize(row.getValue<number>("fileSize")),
      },
      {
        accessorKey: "uploaderDisplayName",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="上传人" />
        ),
        meta: { label: "上传人", className: "ps-1", tdClassName: "ps-4" },
      },
      {
        accessorKey: "uploadedAt",
        header: ({ column }) => (
          <DataTableColumnHeader column={column} title="更新时间" />
        ),
        meta: { label: "更新时间", className: "ps-1", tdClassName: "ps-4" },
        cell: ({ row }) => formatDateTime(row.getValue<string>("uploadedAt")),
      },
      {
        id: "actions",
        cell: ({ row }) => (
          <DocumentRowActions
            document={row.original}
            isActionPending={isActionPending}
            isPreviewPending={isPreviewPending}
            onPreview={onPreview}
            onRetry={onRetry}
            onDelete={onDelete}
          />
        ),
        enableSorting: false,
        enableHiding: false,
      },
    ],
    [
      isSelectionMode,
      knowledgeBaseName,
      groupNameById,
      isActionPending,
      isPreviewPending,
      onDelete,
      onPreview,
      onRetry,
    ]
  )

  const table = useReactTable({
    data,
    columns,
    state: {
      sorting,
      columnVisibility,
      rowSelection,
      columnFilters,
      globalFilter,
      pagination,
    },
    enableRowSelection: true,
    onRowSelectionChange: setRowSelection,
    onSortingChange: setSorting,
    onColumnVisibilityChange: setColumnVisibility,
    onColumnFiltersChange: setColumnFilters,
    onGlobalFilterChange: setGlobalFilter,
    onPaginationChange: setPagination,
    globalFilterFn: (row, _columnId, filterValue) => {
      const searchValue = String(filterValue).toLowerCase()
      const document = row.original
      const text = [
        document.fileName,
        getKnowledgeBaseDisplayName(document.groupId, groupNameById),
        document.fileExt,
        document.contentType,
        document.failureReason,
        document.uploaderDisplayName,
        document.uploaderUserCode,
        document.status,
      ]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()

      return text.includes(searchValue)
    },
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
    getFacetedRowModel: getFacetedRowModel(),
    getFacetedUniqueValues: getFacetedUniqueValues(),
  })

  const selectedDocuments = table
    .getFilteredSelectedRowModel()
    .rows.map((row) => row.original)
  const visibleRows = table.getRowModel().rows
  const paginationAnimationKey = `${pagination.pageIndex}-${pagination.pageSize}`
  const handleToggleSelectionMode = () => {
    if (isSelectionMode) {
      table.resetRowSelection()
      setIsSelectionMode(false)
      return
    }

    setIsSelectionMode(true)
  }

  return (
    <div
      className={cn(
        'max-sm:has-[div[role="toolbar"]]:mb-16',
        "flex flex-1 flex-col gap-4"
      )}
    >
      <DataTableToolbar
        table={table}
        searchPlaceholder={searchPlaceholder}
        searchAction={
          <SelectAllRowsButton
            isSelectionMode={isSelectionMode}
            onClick={handleToggleSelectionMode}
          />
        }
        filters={[
          {
            columnId: "status",
            title: "状态",
            options: documentStatusFilters,
          },
        ]}
      />

      <motion.div
        key={paginationAnimationKey}
        className="overflow-hidden rounded-md border"
        initial={
          shouldReduceMotion ? false : { opacity: 0, filter: "blur(1px)" }
        }
        animate={
          shouldReduceMotion ? undefined : { opacity: 1, filter: "blur(0px)" }
        }
        transition={{ duration: 0.16, ease: [0.22, 1, 0.36, 1] }}
      >
        <Table className="min-w-xl">
          <TableHeader>
            {table.getHeaderGroups().map((headerGroup) => (
              <TableRow key={headerGroup.id}>
                {headerGroup.headers.map((header) => (
                  <TableHead
                    key={header.id}
                    colSpan={header.colSpan}
                    className={cn(
                      header.column.columnDef.meta?.className,
                      header.column.columnDef.meta?.thClassName
                    )}
                  >
                    {header.isPlaceholder
                      ? null
                      : flexRender(
                          header.column.columnDef.header,
                          header.getContext()
                        )}
                  </TableHead>
                ))}
              </TableRow>
            ))}
          </TableHeader>
          <TableBody>
            {visibleRows.length ? (
              visibleRows.map((row) => (
                <TableRow
                  key={row.id}
                  data-state={row.getIsSelected() && "selected"}
                >
                  {row.getVisibleCells().map((cell) => (
                    <TableCell
                      key={cell.id}
                      className={cn(
                        cell.column.columnDef.meta?.className,
                        cell.column.columnDef.meta?.tdClassName
                      )}
                    >
                      {flexRender(
                        cell.column.columnDef.cell,
                        cell.getContext()
                      )}
                    </TableCell>
                  ))}
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell
                  colSpan={columns.length}
                  className="h-24 text-center text-muted-foreground"
                >
                  {isLoading ? "加载知识库文档中" : "暂无知识库文档"}
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </motion.div>

      <DataTablePagination table={table} className="mt-auto" />
      {isSelectionMode ? (
        <DataTableBulkActions
          table={table}
          entityName="document"
          entityNamePlural="documents"
        >
          <Button
            variant="destructive"
            size="sm"
            disabled={isActionPending}
            onClick={() => onDeleteMany(selectedDocuments)}
          >
            删除
            <Trash2 data-icon="inline-end" />
          </Button>
        </DataTableBulkActions>
      ) : null}
    </div>
  )
}

type DocumentRowActionsProps = {
  document: DocumentListItem
  isActionPending: boolean
  isPreviewPending: boolean
  onPreview: (document: DocumentListItem) => void
  onRetry: (document: DocumentListItem) => void
  onDelete: (document: DocumentListItem) => void
}

function DocumentRowActions({
  document,
  isActionPending,
  isPreviewPending,
  onPreview,
  onRetry,
  onDelete,
}: DocumentRowActionsProps) {
  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger asChild>
        <Button
          variant="ghost"
          className="flex size-8 p-0 data-[state=open]:bg-muted"
        >
          <MoreHorizontal />
          <span className="sr-only">打开菜单</span>
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-40">
        <DropdownMenuItem
          disabled={isPreviewPending}
          onClick={() => onPreview(document)}
        >
          预览
          <DropdownMenuShortcut>
            <Eye />
          </DropdownMenuShortcut>
        </DropdownMenuItem>
        <DropdownMenuItem disabled>编辑</DropdownMenuItem>
        <DropdownMenuItem disabled>收藏</DropdownMenuItem>
        {document.status === "FAILED" ? (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              disabled={isActionPending}
              onClick={() => onRetry(document)}
            >
              重新入库
              <DropdownMenuShortcut>
                <RotateCcw />
              </DropdownMenuShortcut>
            </DropdownMenuItem>
          </>
        ) : null}
        <DropdownMenuSeparator />
        <DropdownMenuItem
          variant="destructive"
          disabled={isActionPending}
          onClick={() => onDelete(document)}
        >
          删除
          <DropdownMenuShortcut>
            <Trash2 />
          </DropdownMenuShortcut>
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

type DocumentPreviewDialogProps = {
  preview: DocumentPreview | null
  onOpenChange: (open: boolean) => void
}

function DocumentPreviewDialog({
  preview,
  onOpenChange,
}: DocumentPreviewDialogProps) {
  return (
    <Dialog open={Boolean(preview)} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-3xl">
        <DialogHeader>
          <DialogTitle>{preview?.fileName ?? "文档预览"}</DialogTitle>
          <DialogDescription>
            查看文档解析后可用于检索的内容。
          </DialogDescription>
        </DialogHeader>
        <div className="max-h-[60svh] overflow-auto rounded-lg border bg-muted/30 p-4 whitespace-pre-wrap">
          {preview?.previewText || "暂无预览内容"}
        </div>
      </DialogContent>
    </Dialog>
  )
}
