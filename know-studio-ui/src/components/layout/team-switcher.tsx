import type { ElementType } from 'react'
import { Link, type LinkProps } from '@tanstack/react-router'
import { Check, ChevronsUpDown } from 'lucide-react'
import { useKnowledgeBaseStore } from '@/stores/knowledge-base-store'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from '@/components/ui/sidebar'

type TeamSwitcherProps = {
  teams: {
    name: string
    logo: ElementType
    plan: string
  }[]
  homeHref?: LinkProps["to"]
  onHomeClick?: () => void
}

export function TeamSwitcher({
  teams,
  homeHref = "/",
  onHomeClick,
}: TeamSwitcherProps) {
  const activeTeam = teams[0]
  const { setOpenMobile } = useSidebar()
  const knowledgeBases = useKnowledgeBaseStore((state) => state.knowledgeBases)
  const currentKnowledgeBaseId = useKnowledgeBaseStore(
    (state) => state.currentKnowledgeBaseId
  )
  const setCurrentKnowledgeBaseId = useKnowledgeBaseStore(
    (state) => state.setCurrentKnowledgeBaseId
  )
  const currentKnowledgeBase = knowledgeBases.find(
    (knowledgeBase) => knowledgeBase.knowledgeBaseId === currentKnowledgeBaseId
  )

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <SidebarMenuButton
              size='lg'
              className='h-11 data-[state=open]:bg-sidebar-accent data-[state=open]:text-sidebar-accent-foreground'
            >
              <div className='flex aspect-square size-8 shrink-0 items-center justify-center text-primary'>
                <activeTeam.logo className='size-7' />
              </div>
              <div className='grid min-w-0 flex-1 text-start leading-tight'>
                <span className='truncate font-semibold'>
                  {currentKnowledgeBase?.name ?? activeTeam.name}
                </span>
                <span className='truncate text-xs text-muted-foreground'>
                  {currentKnowledgeBase?.permission ?? activeTeam.plan}
                </span>
              </div>
              <ChevronsUpDown className='ml-auto size-4' />
            </SidebarMenuButton>
          </DropdownMenuTrigger>
          <DropdownMenuContent
            className='min-w-64 rounded-md'
            align='start'
            side='right'
            sideOffset={4}
          >
            <DropdownMenuLabel className='text-xs text-muted-foreground'>
              知识库
            </DropdownMenuLabel>
            {knowledgeBases.length ? (
              knowledgeBases.map((knowledgeBase) => (
                <DropdownMenuItem
                  key={knowledgeBase.knowledgeBaseId}
                  onClick={() => {
                    setCurrentKnowledgeBaseId(knowledgeBase.knowledgeBaseId)
                    setOpenMobile(false)
                  }}
                >
                  <span className='min-w-0 flex-1 truncate'>{knowledgeBase.name}</span>
                  <span className='text-xs text-muted-foreground'>
                    {knowledgeBase.permission}
                  </span>
                  {knowledgeBase.knowledgeBaseId === currentKnowledgeBaseId ? (
                    <Check className='size-4' />
                  ) : null}
                </DropdownMenuItem>
              ))
            ) : (
              <DropdownMenuItem disabled>暂无可用知识库</DropdownMenuItem>
            )}
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild>
              <Link
                to={homeHref}
                onClick={() => {
                  onHomeClick?.()
                  setOpenMobile(false)
                }}
              >
                返回首页
              </Link>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
