import type { ElementType } from "react"
import { Link } from "@tanstack/react-router"
import {
  SidebarMenu,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar"

type TeamSwitcherProps = {
  teams: {
    name: string
    logo: ElementType
    plan: string
  }[]
}

export function TeamSwitcher({ teams }: TeamSwitcherProps) {
  const activeTeam = teams[0]
  const { setOpenMobile } = useSidebar()

  return (
    <SidebarMenu>
      <SidebarMenuItem>
        <Link
          to="/"
          aria-label="Go to dashboard"
          onClick={() => setOpenMobile(false)}
          className="flex h-11 w-full select-none items-center gap-1.5 overflow-hidden rounded-lg p-0 text-sidebar-foreground outline-hidden ring-sidebar-ring transition-colors hover:text-sidebar-accent-foreground focus-visible:ring-2 group-data-[collapsible=icon]:size-8 group-data-[collapsible=icon]:justify-center"
        >
          <div className="flex aspect-square size-8 shrink-0 items-center justify-center text-primary">
            <activeTeam.logo className="size-7" />
          </div>
          <div className="grid flex-1 text-start text-lg leading-tight group-data-[collapsible=icon]:hidden">
            <span className="truncate font-semibold">{activeTeam.name}</span>
          </div>
        </Link>
      </SidebarMenuItem>
    </SidebarMenu>
  )
}
