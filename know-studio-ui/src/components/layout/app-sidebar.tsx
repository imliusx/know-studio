import { Fragment } from 'react'
import { useQuery } from '@tanstack/react-query'
import { listTeams } from '@/api/teams'
import { useLayout } from '@/context/layout-provider'
import { useAuthStore } from '@/stores/auth-store'
import { useKnowledgeBaseStore } from '@/stores/knowledge-base-store'
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
  const teamsQuery = useQuery({ queryKey: ['teams'], queryFn: listTeams })
  const canManageTeams = teamsQuery.data?.some(
    (team) => team.role === 'TEAM_ADMIN'
  )
  const canManageKnowledge = useKnowledgeBaseStore((state) =>
    state.knowledgeBases.some((item) => item.permission === 'MANAGE')
  )
  const managementUrls = new Set([
    ...(canManageKnowledge
      ? ['/admin', '/admin/documents', '/admin/evaluations']
      : []),
    ...(canManageTeams || canManageKnowledge ? ['/admin/groups'] : []),
  ])
  const navGroups = sidebarData.navGroups.map((group) => ({
    ...group,
    items: group.items.filter(
      (item) => isSystemAdmin || (item.url && managementUrls.has(item.url))
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
