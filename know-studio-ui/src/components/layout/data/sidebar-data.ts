import {
  BarChart3,
  ClipboardCheck,
  ClipboardList,
  Database,
  FolderKanban,
  GitBranch,
  KeyRound,
  Layers,
  LayoutDashboard,
  Lightbulb,
  Route,
  Settings,
  SquareStack,
  Upload,
  Users,
  DraftingCompass as LuDraftingCompass,
} from 'lucide-react'
import { type SidebarData } from '../types'

export const sidebarData: SidebarData = {
  user: {
    name: 'KnowStudio Admin',
    email: 'admin',
    avatar: '/avatars/shadcn.jpg',
  },
  teams: [
    {
      name: 'KnowStudio Admin',
      logo: LuDraftingCompass,
      plan: 'Knowledge Admin',
    },
  ],
  navGroups: [
    {
      title: '常用',
      items: [
        {
          title: 'Dashboard',
          url: '/admin',
          icon: LayoutDashboard,
        },
        {
          title: '知识库管理',
          url: '/admin/documents',
          icon: Database,
        },
        {
          title: '检索评测',
          url: '/admin/evaluations',
          icon: BarChart3,
        },
        {
          title: '意图管理',
          icon: Layers,
          items: [
            {
              title: '意图树配置',
              url: '/admin/intent/tree',
              icon: GitBranch,
            },
            {
              title: '意图列表',
              url: '/admin/intent/list',
              icon: ClipboardList,
            },
          ],
        },
        {
          title: '数据通道',
          icon: Upload,
          items: [
            {
              title: '流水线管理',
              url: '/admin/data-channels/pipelines',
              icon: FolderKanban,
            },
            {
              title: '流水线任务',
              url: '/admin/data-channels/tasks',
              icon: ClipboardCheck,
            },
            {
              title: '关键词映射',
              url: '/admin/data-channels/keywords',
              icon: KeyRound,
            },
            {
              title: '链路追踪',
              url: '/admin/data-channels/traces',
              icon: Route,
            },
          ],
        },
      ],
    },
    {
      title: '管理',
      items: [
        {
          title: '用户管理',
          url: '/admin/users',
          icon: Users,
        },
        {
          title: '小组协作',
          url: '/admin/groups',
          icon: SquareStack,
        },
        {
          title: '示例问题',
          url: '/admin/qa',
          icon: Lightbulb,
        },
        {
          title: '系统设置',
          url: '/admin/settings/account',
          icon: Settings,
        },
      ],
    },
  ],
}
