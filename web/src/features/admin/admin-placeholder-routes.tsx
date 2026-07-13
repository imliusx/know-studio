import {
  ClipboardCheck,
  ClipboardList,
  FolderKanban,
  GitBranch,
  KeyRound,
  Route as RouteIcon,
} from 'lucide-react'
import { AdminPlaceholderPage } from './admin-placeholder-page'

export function KeywordsPage() {
  return (
    <AdminPlaceholderPage
      title='关键词映射'
      description='维护业务词、同义词和知识片段之间的映射关系。'
      icon={KeyRound}
      items={['业务词表', '同义词扩展', '知识片段映射']}
    />
  )
}

export function PipelinesPage() {
  return (
    <AdminPlaceholderPage
      title='流水线管理'
      description='管理文档入库、解析、切片、索引构建等数据处理流水线。'
      icon={FolderKanban}
      items={['入库流水线', '解析策略', '索引构建']}
    />
  )
}

export function PipelineTasksPage() {
  return (
    <AdminPlaceholderPage
      title='流水线任务'
      description='查看数据处理任务的执行状态、耗时和失败原因。'
      icon={ClipboardCheck}
      items={['任务队列', '执行日志', '失败重试']}
    />
  )
}

export function TracesPage() {
  return (
    <AdminPlaceholderPage
      title='链路追踪'
      description='追踪检索、重排、生成和外部工具调用的完整执行链路。'
      icon={RouteIcon}
      items={['请求链路', '检索证据', '工具调用']}
    />
  )
}

export function IntentListPage() {
  return (
    <AdminPlaceholderPage
      title='意图列表'
      description='管理意图样本、状态、优先级和归属知识范围。'
      icon={ClipboardList}
      items={['意图样本', '优先级配置', '状态流转']}
    />
  )
}

export function IntentTreePage() {
  return (
    <AdminPlaceholderPage
      title='意图树配置'
      description='维护意图分类、层级关系和路由规则。'
      icon={GitBranch}
      items={['意图分类树', '命中规则', '兜底策略']}
    />
  )
}
