import {
  Bot,
  FileText,
  FolderKanban,
  KeyRound,
  LayoutDashboard,
  MessageCircleQuestion,
  Settings,
  Users,
  DraftingCompass as LuDraftingCompass,
} from 'lucide-react'
import { type SidebarData } from '../types'

export const sidebarData: SidebarData = {
  user: {
    name: 'Know Studio Admin',
    email: 'admin',
    avatar: '/avatars/shadcn.jpg',
  },
  teams: [
    {
      name: 'Know Studio Admin',
      logo: LuDraftingCompass,
      plan: 'Knowledge Admin',
    },
  ],
  navGroups: [
    {
      title: 'Workspace',
      items: [
        {
          title: '控制台',
          url: '/admin',
          icon: LayoutDashboard,
        },
        {
          title: '小组协作',
          url: '/admin/groups',
          icon: FolderKanban,
        },
        {
          title: '文档知识库',
          url: '/admin/documents',
          icon: FileText,
        },
        {
          title: '知识库问答',
          url: '/admin/qa',
          icon: MessageCircleQuestion,
        },
        {
          title: 'AI 助手',
          url: '/admin/assistant',
          icon: Bot,
        },
      ],
    },
    {
      title: 'Administration',
      items: [
        {
          title: '用户管理',
          url: '/admin/users',
          icon: Users,
        },
        {
          title: '设置',
          icon: Settings,
          items: [
            {
              title: '账号安全',
              url: '/admin/settings/account',
              icon: KeyRound,
            },
          ],
        },
      ],
    },
  ],
}
