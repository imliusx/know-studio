import { useMemo, useState } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { Link } from "@tanstack/react-router"
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
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { DocumentStatusBadge } from "./document-status-badge"
import { formatDateTime, formatFileSize } from "./shared"

export function DocumentsPage() {
  const documentManager = useDocumentManagement()
  const [activeView, setActiveView] = useState("knowledge-bases")
  const stats = useMemo(
    () => getKnowledgeBaseStats(documentManager.documents),
    [documentManager.documents]
  )
  const knowledgeBases = useMemo(
    () => getKnowledgeBases(documentManager.documents),
    [documentManager.documents]
  )

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
          <KnowledgeBasePrimaryButtons />
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
              isLoading={documentManager.isLoading}
            />
          </TabsContent>
          <TabsContent value="documents" className="m-0 flex flex-1 flex-col">
            <KnowledgeBaseTable
              data={documentManager.documents}
              isLoading={documentManager.isLoading}
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
  const knowledgeBases = useMemo(
    () => getKnowledgeBases(documentManager.documents),
    [documentManager.documents]
  )
  const knowledgeBase = knowledgeBases.find((item) => item.groupId === groupId)
  const knowledgeBaseName =
    knowledgeBase?.name ?? formatKnowledgeBaseName(groupId)
  const documents = useMemo(
    () =>
      documentManager.documents.filter(
        (document) => document.groupId === groupId
      ),
    [documentManager.documents, groupId]
  )

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
            <Button onClick={() => toast.info("文档上传流程稍后接入")}>
              上传文档
              <Upload data-icon="inline-end" />
            </Button>
          </div>
        </div>

        <KnowledgeBaseTable
          data={documents}
          isLoading={documentManager.isLoading}
          knowledgeBaseName={knowledgeBaseName}
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
  const isUsingMockDocuments = backendDocuments.length === 0
  const documents = isUsingMockDocuments
    ? mockKnowledgeDocuments
    : backendDocuments

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
    isLoading: documentsQuery.isPending && !isUsingMockDocuments,
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
  updatedAt: string
  uploaderNames: string[]
}

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

function getKnowledgeBases(documents: DocumentListItem[]) {
  const byGroupId = new Map<number, KnowledgeBaseItem>()

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
      updatedAt: document.uploadedAt,
      uploaderNames: [],
    }

    item.documentCount += 1
    item.totalSize += document.fileSize
    item.updatedAt =
      new Date(document.uploadedAt) > new Date(item.updatedAt)
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
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
  )
}

function getKnowledgeBaseStats(documents: DocumentListItem[]) {
  const knowledgeBaseCount = new Set(documents.map((item) => item.groupId)).size
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

function KnowledgeBasePrimaryButtons() {
  return (
    <div className="flex gap-2">
      <Button
        variant="outline"
        onClick={() => toast.info("文档导入流程稍后接入")}
      >
        导入文档
        <Upload data-icon="inline-end" />
      </Button>
      <Button onClick={() => toast.info("知识库创建流程稍后接入")}>
        新建
        <Plus data-icon="inline-end" />
      </Button>
    </div>
  )
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

const mockKnowledgeDocuments: DocumentListItem[] = [
  {
    documentId: -1001,
    groupId: 1,
    fileName: "销售团队季度作战手册.pdf",
    fileExt: "pdf",
    contentType: "application/pdf",
    fileSize: 8_742_912,
    status: "READY",
    failureReason: null,
    uploadedAt: "2026-06-28T09:18:32+08:00",
    uploaderUserId: 101,
    uploaderUserCode: "U-KNOW-101",
    uploaderDisplayName: "林知远",
    previewText:
      "本手册覆盖线索分层、商机推进节奏、客户异议处理、报价策略与季度复盘模板。重点强调企业知识库中可复用的话术、行业案例和赢单记录。",
  },
  {
    documentId: -1002,
    groupId: 1,
    fileName: "研发效能度量指标说明.md",
    fileExt: "md",
    contentType: "text/markdown",
    fileSize: 126_384,
    status: "READY",
    failureReason: null,
    uploadedAt: "2026-06-27T16:42:05+08:00",
    uploaderUserId: 102,
    uploaderUserCode: "U-KNOW-102",
    uploaderDisplayName: "周眠",
    previewText:
      "指标包含需求吞吐、交付周期、缺陷逃逸率、构建稳定性和知识沉淀覆盖率。文档说明了每个指标的计算口径和使用边界。",
  },
  {
    documentId: -1003,
    groupId: 2,
    fileName: "客户成功续约风险识别清单.xlsx",
    fileExt: "xlsx",
    contentType:
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    fileSize: 1_483_776,
    status: "PROCESSING",
    failureReason: null,
    uploadedAt: "2026-06-26T11:07:44+08:00",
    uploaderUserId: 103,
    uploaderUserCode: "U-KNOW-103",
    uploaderDisplayName: "陈一白",
    previewText:
      "该清单包含合同到期时间、产品使用频次、工单健康度、关键联系人变化、预算风险和竞品触达迹象等字段。",
  },
  {
    documentId: -1004,
    groupId: 2,
    fileName: "数据安全分级与脱敏规范.docx",
    fileExt: "docx",
    contentType:
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    fileSize: 2_908_160,
    status: "READY",
    failureReason: null,
    uploadedAt: "2026-06-24T14:23:11+08:00",
    uploaderUserId: 104,
    uploaderUserCode: "U-KNOW-104",
    uploaderDisplayName: "许若安",
    previewText:
      "规范将数据分为公开、内部、敏感和高敏四级，并定义了展示脱敏、导出审批、日志审计和访问权限控制要求。",
  },
  {
    documentId: -1005,
    groupId: 3,
    fileName: "生产事故复盘-搜索服务超时.pdf",
    fileExt: "pdf",
    contentType: "application/pdf",
    fileSize: 5_412_864,
    status: "FAILED",
    failureReason: "示例：文档包含扫描图片，OCR 质量不足",
    uploadedAt: "2026-06-23T20:35:29+08:00",
    uploaderUserId: 105,
    uploaderUserCode: "U-KNOW-105",
    uploaderDisplayName: "赵临川",
    previewText:
      "事故影响搜索接口 P95 延迟和问答召回稳定性。复盘内容包括告警时间线、根因、止血动作、长期治理项和责任人。",
  },
  {
    documentId: -1006,
    groupId: 3,
    fileName: "新员工入职知识导航.html",
    fileExt: "html",
    contentType: "text/html",
    fileSize: 342_512,
    status: "READY",
    failureReason: null,
    uploadedAt: "2026-06-22T10:12:50+08:00",
    uploaderUserId: 106,
    uploaderUserCode: "U-KNOW-106",
    uploaderDisplayName: "何青棠",
    previewText:
      "导航汇总了账号开通、办公系统、研发流程、财务报销、行政支持和常用 FAQ，适合作为企业助手的入门知识源。",
  },
  {
    documentId: -1007,
    groupId: 4,
    fileName: "法务合同审查常见条款库.docx",
    fileExt: "docx",
    contentType:
      "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    fileSize: 3_219_456,
    status: "READY",
    failureReason: null,
    uploadedAt: "2026-06-20T17:56:18+08:00",
    uploaderUserId: 107,
    uploaderUserCode: "U-KNOW-107",
    uploaderDisplayName: "宋霁",
    previewText:
      "条款库覆盖保密义务、知识产权归属、违约责任、数据处理、服务可用性、终止条件和争议解决等审查要点。",
  },
  {
    documentId: -1008,
    groupId: 4,
    fileName: "市场活动复盘与线索转化分析.pptx",
    fileExt: "pptx",
    contentType:
      "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    fileSize: 12_684_288,
    status: "PROCESSING",
    failureReason: null,
    uploadedAt: "2026-06-19T13:04:27+08:00",
    uploaderUserId: 108,
    uploaderUserCode: "U-KNOW-108",
    uploaderDisplayName: "孟知夏",
    previewText:
      "复盘材料包含活动主题、渠道投放、报名转化、现场互动、销售跟进和 ROI 评估，可用于回答市场投放相关问题。",
  },
  {
    documentId: -1009,
    groupId: 5,
    fileName: "财务报销制度与审批口径.pdf",
    fileExt: "pdf",
    contentType: "application/pdf",
    fileSize: 2_146_304,
    status: "READY",
    failureReason: null,
    uploadedAt: "2026-06-18T08:45:13+08:00",
    uploaderUserId: 109,
    uploaderUserCode: "U-KNOW-109",
    uploaderDisplayName: "唐予",
    previewText:
      "制度说明差旅、招待、采购、培训和日常办公费用的报销标准、凭证要求、审批层级和异常处理方式。",
  },
  {
    documentId: -1010,
    groupId: 5,
    fileName: "客服知识库-账号登录问题.csv",
    fileExt: "csv",
    contentType: "text/csv",
    fileSize: 684_032,
    status: "READY",
    failureReason: null,
    uploadedAt: "2026-06-17T19:28:09+08:00",
    uploaderUserId: 110,
    uploaderUserCode: "U-KNOW-110",
    uploaderDisplayName: "梁栖",
    previewText:
      "CSV 包含登录失败、验证码收不到、账号锁定、组织切换、单点登录配置和密码重置等高频客服问答。",
  },
  {
    documentId: -1011,
    groupId: 6,
    fileName: "供应商准入评分表.xlsx",
    fileExt: "xlsx",
    contentType:
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    fileSize: 958_464,
    status: "FAILED",
    failureReason: "示例：表格存在合并单元格，解析结构需要人工确认",
    uploadedAt: "2026-06-16T15:17:36+08:00",
    uploaderUserId: 111,
    uploaderUserCode: "U-KNOW-111",
    uploaderDisplayName: "陆宁",
    previewText:
      "评分表包含资质、交付能力、价格、服务响应、安全合规和历史合作记录等维度，用于采购准入评估。",
  },
  {
    documentId: -1012,
    groupId: 6,
    fileName: "RAG 问答效果评测样本集.json",
    fileExt: "json",
    contentType: "application/json",
    fileSize: 512_768,
    status: "READY",
    failureReason: null,
    uploadedAt: "2026-06-15T21:09:02+08:00",
    uploaderUserId: 112,
    uploaderUserCode: "U-KNOW-112",
    uploaderDisplayName: "顾北辰",
    previewText:
      "样本集包含问题、标准答案、参考文档、召回片段和评分维度，可用于验证知识库问答链路的准确性与稳定性。",
  },
]

function isMockDocument(document: DocumentListItem) {
  return document.documentId < 0
}

function KnowledgeBaseTable({
  data,
  isLoading,
  knowledgeBaseName = null,
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
              cell: ({ row }) => formatKnowledgeBaseName(row.original.groupId),
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
        formatKnowledgeBaseName(document.groupId),
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
