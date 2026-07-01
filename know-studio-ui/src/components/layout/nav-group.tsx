import { type ReactNode } from "react"
import { Link, useLocation } from "@tanstack/react-router"
import { ChevronRight } from "lucide-react"
import { useTranslation } from "react-i18next"
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible"
import {
  SidebarGroup,
  SidebarGroupLabel,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  useSidebar,
} from "@/components/ui/sidebar"
import { Badge } from "../ui/badge"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "../ui/dropdown-menu"
import { cn } from "@/lib/utils"
import {
  type NavCollapsible,
  type NavItem,
  type NavLink,
  type NavGroup as NavGroupProps,
} from "./types"
import { SidebarActiveIndicator } from "./sidebar-active-indicator"

export function NavGroup({ title, items }: NavGroupProps) {
  const { t } = useTranslation()
  const { state, isMobile } = useSidebar()
  const href = useLocation({ select: (location) => location.href })
  const label = t(`navigation.${title}`, { defaultValue: title })
  return (
    <SidebarGroup>
      {title ? <SidebarGroupLabel>{label}</SidebarGroupLabel> : null}
      <SidebarMenu className="gap-1">
        {items.map((item) => {
          const key = `${item.title}-${item.url}`

          if (!item.items)
            return <SidebarMenuLink key={key} item={item} href={href} />

          if (state === "collapsed" && !isMobile)
            return (
              <SidebarMenuCollapsedDropdown key={key} item={item} href={href} />
            )

          return <SidebarMenuCollapsible key={key} item={item} href={href} />
        })}
      </SidebarMenu>
    </SidebarGroup>
  )
}

function NavBadge({ children }: { children: ReactNode }) {
  return (
    <Badge className="ms-auto size-5 shrink-0 rounded-full p-0 text-xs">
      {children}
    </Badge>
  )
}

function SidebarMenuLink({ item, href }: { item: NavLink; href: string }) {
  const { t } = useTranslation()
  const { setOpenMobile } = useSidebar()
  const label = t(`navigation.${item.title}`, { defaultValue: item.title })
  const isActive = checkIsActive(href, item)
  return (
    <SidebarMenuItem>
      <SidebarMenuButton
        asChild
        isActive={isActive}
        tooltip={label}
        className="relative pl-4 group-data-[collapsible=icon]:pl-[0.4rem]"
      >
        <Link to={item.url} onClick={() => setOpenMobile(false)}>
          {isActive ? <SidebarActiveIndicator /> : null}
          {item.icon && <item.icon />}
          <span className="min-w-0 flex-1 truncate">{label}</span>
          {item.badge && <NavBadge>{item.badge}</NavBadge>}
        </Link>
      </SidebarMenuButton>
    </SidebarMenuItem>
  )
}

function SidebarMenuCollapsible({
  item,
  href,
}: {
  item: NavCollapsible
  href: string
}) {
  const { t } = useTranslation()
  const { setOpenMobile } = useSidebar()
  const label = t(`navigation.${item.title}`, { defaultValue: item.title })
  return (
    <Collapsible
      asChild
      defaultOpen={checkIsActive(href, item, true)}
      className="group/collapsible"
    >
      <SidebarMenuItem>
        <CollapsibleTrigger asChild>
          <SidebarMenuButton
            tooltip={label}
            className="relative pl-4 group-data-[collapsible=icon]:pl-[0.4rem]"
          >
            {item.icon && <item.icon />}
            <span>{label}</span>
            {item.badge && <NavBadge>{item.badge}</NavBadge>}
            <ChevronRight className="ms-auto transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90 rtl:rotate-180" />
          </SidebarMenuButton>
        </CollapsibleTrigger>
        <CollapsibleContent>
          <SidebarMenuSub className="mt-1 py-0">
            {item.items.map((subItem) => {
              const subItemActive = checkIsActive(href, subItem)

              return (
                <SidebarMenuSubItem key={subItem.title}>
                  <SidebarMenuSubButton
                    asChild
                    isActive={subItemActive}
                    className="relative pl-4"
                  >
                    <Link to={subItem.url} onClick={() => setOpenMobile(false)}>
                      {subItemActive ? <SidebarActiveIndicator /> : null}
                      {subItem.icon && <subItem.icon />}
                      <span>
                        {t(`navigation.${subItem.title}`, {
                          defaultValue: subItem.title,
                        })}
                      </span>
                      {subItem.badge && <NavBadge>{subItem.badge}</NavBadge>}
                    </Link>
                  </SidebarMenuSubButton>
                </SidebarMenuSubItem>
              )
            })}
          </SidebarMenuSub>
        </CollapsibleContent>
      </SidebarMenuItem>
    </Collapsible>
  )
}

function SidebarMenuCollapsedDropdown({
  item,
  href,
}: {
  item: NavCollapsible
  href: string
}) {
  const { t } = useTranslation()
  const label = t(`navigation.${item.title}`, { defaultValue: item.title })
  const isActive = checkIsActive(href, item)
  return (
    <SidebarMenuItem>
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <SidebarMenuButton
            tooltip={label}
            isActive={isActive}
            className="relative pl-4 group-data-[collapsible=icon]:pl-[0.4rem]"
          >
            {isActive ? <SidebarActiveIndicator /> : null}
            {item.icon && <item.icon />}
            <span>{label}</span>
            {item.badge && <NavBadge>{item.badge}</NavBadge>}
            <ChevronRight className="ms-auto transition-transform duration-200 group-data-[state=open]/collapsible:rotate-90" />
          </SidebarMenuButton>
        </DropdownMenuTrigger>
        <DropdownMenuContent
          side="right"
          align="start"
          sideOffset={4}
          className="w-max min-w-32 rounded-lg"
        >
          <DropdownMenuLabel className="whitespace-nowrap">
            {label} {item.badge ? `(${item.badge})` : ""}
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuGroup>
            {item.items.map((sub) => (
              <DropdownMenuItem key={`${sub.title}-${sub.url}`} asChild>
                <Link
                  to={sub.url}
                  className={cn(
                    checkIsActive(href, sub) &&
                      "bg-foreground/10 text-foreground"
                  )}
                >
                  {sub.icon && <sub.icon />}
                  <span className="whitespace-nowrap">
                    {t(`navigation.${sub.title}`, {
                      defaultValue: sub.title,
                    })}
                  </span>
                  {sub.badge && (
                    <span className="ms-auto text-xs">{sub.badge}</span>
                  )}
                </Link>
              </DropdownMenuItem>
            ))}
          </DropdownMenuGroup>
        </DropdownMenuContent>
      </DropdownMenu>
    </SidebarMenuItem>
  )
}

function checkIsActive(href: string, item: NavItem, mainNav = false) {
  return (
    href === item.url || // /endpint?search=param
    href.split("?")[0] === item.url || // endpoint
    !!item?.items?.filter((i) => i.url === href).length || // if child nav is active
    (mainNav &&
      href.split("/")[1] !== "" &&
      href.split("/")[1] === item?.url?.split("/")[1])
  )
}
