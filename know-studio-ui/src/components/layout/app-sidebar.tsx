import { Fragment } from 'react'
import { useLayout } from '@/context/layout-provider'
import { useAuthStore } from '@/stores/auth-store'
import { useWorkspaceStore } from '@/stores/workspace-store'
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarHeader,
  SidebarRail,
  SidebarSeparator,
} from '@/components/ui/sidebar'
// import { AppTitle } from './app-title'
import { sidebarData } from './data/sidebar-data'
import { NavGroup } from './nav-group'
import { TeamSwitcher } from './team-switcher'

export function AppSidebar() {
  const { collapsible, variant } = useLayout()
  const isSystemAdmin = useAuthStore(
    (state) => state.auth.user?.systemRole === 'ADMIN'
  )
  const canManageEvaluations = useWorkspaceStore((state) => {
    const workspace = state.workspaces.find(
      (item) => item.workspaceId === state.currentWorkspaceId
    )
    return workspace?.role === 'OWNER' || workspace?.role === 'ADMIN'
  })
  const navGroups = sidebarData.navGroups.map((group) => ({
    ...group,
    items: group.items.filter(
      (item) =>
        item.url !== '/admin/evaluations' ||
        isSystemAdmin ||
        canManageEvaluations
    ),
  }))

  return (
    <Sidebar collapsible={collapsible} variant={variant}>
      <SidebarHeader>
        <TeamSwitcher teams={sidebarData.teams} homeHref='/admin' />

        {/* Replace <TeamSwitch /> with the following <AppTitle />
         /* if you want to use the normal app title instead of TeamSwitch dropdown */}
        {/* <AppTitle /> */}
      </SidebarHeader>
      <SidebarContent>
        {navGroups.map((props, index) => (
          <Fragment key={`${index}-${props.title || 'nav-group'}`}>
            {index > 0 && (
              <SidebarGroup className='py-0'>
                <SidebarSeparator className='my-2 group-data-[collapsible=icon]:mx-auto group-data-[collapsible=icon]:data-horizontal:w-6' />
              </SidebarGroup>
            )}
            <NavGroup {...props} />
          </Fragment>
        ))}
      </SidebarContent>
      <SidebarRail />
    </Sidebar>
  )
}
