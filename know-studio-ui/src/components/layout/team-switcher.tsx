import type { ElementType } from 'react'
import { Link, type LinkProps } from '@tanstack/react-router'
import { Check, ChevronsUpDown, Plus } from 'lucide-react'
import { useWorkspaceStore } from '@/stores/workspace-store'
import { CreateWorkspaceDialog } from '@/components/workspace/create-workspace-dialog'
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
  const workspaces = useWorkspaceStore((state) => state.workspaces)
  const currentWorkspaceId = useWorkspaceStore(
    (state) => state.currentWorkspaceId
  )
  const setCurrentWorkspaceId = useWorkspaceStore(
    (state) => state.setCurrentWorkspaceId
  )
  const currentWorkspace = workspaces.find(
    (workspace) => workspace.workspaceId === currentWorkspaceId
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
                  {currentWorkspace?.name ?? activeTeam.name}
                </span>
                <span className='truncate text-xs text-muted-foreground'>
                  {currentWorkspace?.role ?? activeTeam.plan}
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
              工作空间
            </DropdownMenuLabel>
            {workspaces.length ? (
              workspaces.map((workspace) => (
                <DropdownMenuItem
                  key={workspace.workspaceId}
                  onClick={() => {
                    setCurrentWorkspaceId(workspace.workspaceId)
                    setOpenMobile(false)
                  }}
                >
                  <span className='min-w-0 flex-1 truncate'>{workspace.name}</span>
                  <span className='text-xs text-muted-foreground'>
                    {workspace.role}
                  </span>
                  {workspace.workspaceId === currentWorkspaceId ? (
                    <Check className='size-4' />
                  ) : null}
                </DropdownMenuItem>
              ))
            ) : (
              <DropdownMenuItem disabled>暂无工作空间</DropdownMenuItem>
            )}
            <DropdownMenuSeparator />
            <CreateWorkspaceDialog
              trigger={
                <DropdownMenuItem onSelect={(event) => event.preventDefault()}>
                  <span className='flex-1'>创建工作空间</span>
                  <Plus className='size-4' />
                </DropdownMenuItem>
              }
            />
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
