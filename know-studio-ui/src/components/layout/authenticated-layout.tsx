import { Outlet } from "@tanstack/react-router"
import { useQuery } from "@tanstack/react-query"
import { useEffect } from "react"
import { getCookie } from "@/lib/cookies"
import { cn } from "@/lib/utils"
import { getCurrentUser } from "@/api/auth"
import { useAuthStore } from "@/stores/auth-store"
import { SearchProvider } from "@/context/search-provider"
import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar"
import { AppSidebar } from "@/components/layout/app-sidebar"
import { SkipToMain } from "@/components/skip-to-main"

type AuthenticatedLayoutProps = {
  children?: React.ReactNode
}

export function AuthenticatedLayout({ children }: AuthenticatedLayoutProps) {
  const defaultOpen = getCookie("sidebar_state") !== "false"
  const accessToken = useAuthStore((state) => state.auth.accessToken)
  const setUser = useAuthStore((state) => state.auth.setUser)
  const currentUserQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: getCurrentUser,
    enabled: Boolean(accessToken),
  })

  useEffect(() => {
    if (currentUserQuery.data) {
      setUser(currentUserQuery.data)
    }
  }, [currentUserQuery.data, setUser])

  return (
    <SearchProvider>
      <SidebarProvider
        defaultOpen={defaultOpen}
        className="h-svh min-h-0 overflow-hidden"
      >
        <SkipToMain />
        <AppSidebar />
        <SidebarInset
          className={cn(
            // Set content container, so we can use container queries
            "@container/content",

            // Keep the app shell fixed. Page content scrolls inside Main,
            // matching the Chat UI scroll model.
            "h-svh min-h-0 overflow-hidden",

            // If sidebar is inset, account for its outer spacing.
            "md:peer-data-[variant=inset]:h-[calc(100svh-(var(--spacing)*4))]"
          )}
        >
          {children ?? <Outlet />}
        </SidebarInset>
      </SidebarProvider>
    </SearchProvider>
  )
}
