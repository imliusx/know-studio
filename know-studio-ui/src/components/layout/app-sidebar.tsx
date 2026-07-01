import { Fragment } from 'react'
import { Link, useLocation } from '@tanstack/react-router'
import { KeyRound } from 'lucide-react'
import { useLayout } from '@/context/layout-provider'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarRail,
  SidebarSeparator,
  useSidebar,
} from '@/components/ui/sidebar'
// import { AppTitle } from './app-title'
import { sidebarData } from './data/sidebar-data'
import { NavGroup } from './nav-group'
import { TeamSwitcher } from './team-switcher'

export function AppSidebar() {
  const { collapsible, variant } = useLayout()
  const { setOpenMobile } = useSidebar()
  const href = useLocation({ select: (location) => location.href })
  const isAccountActive = href.split('?')[0] === '/admin/settings/account'

  return (
    <Sidebar collapsible={collapsible} variant={variant}>
      <SidebarHeader>
        <TeamSwitcher teams={sidebarData.teams} />

        {/* Replace <TeamSwitch /> with the following <AppTitle />
         /* if you want to use the normal app title instead of TeamSwitch dropdown */}
        {/* <AppTitle /> */}
      </SidebarHeader>
      <SidebarContent>
        {sidebarData.navGroups.map((props, index) => (
          <Fragment key={props.title}>
            {index > 0 && (
              <SidebarSeparator className='my-1 mx-auto hidden data-horizontal:w-6 group-data-[collapsible=icon]:block' />
            )}
            <NavGroup {...props} />
          </Fragment>
        ))}
      </SidebarContent>
      <SidebarFooter>
        <SidebarMenu className='gap-1'>
          <SidebarMenuItem>
            <SidebarMenuButton
              asChild
              isActive={isAccountActive}
              tooltip='账号安全'
            >
              <Link to='/admin/settings/account' onClick={() => setOpenMobile(false)}>
                <KeyRound />
                <span>账号安全</span>
              </Link>
            </SidebarMenuButton>
          </SidebarMenuItem>
        </SidebarMenu>
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  )
}
