import { useEffect, useRef, useState } from "react"
import { cn } from "@/lib/utils"
import { Separator } from "@/components/ui/separator"
import { SidebarTrigger } from "@/components/ui/sidebar"

type HeaderProps = React.HTMLAttributes<HTMLElement> & {
  fixed?: boolean
  ref?: React.Ref<HTMLElement>
}

function setRef<T>(ref: React.Ref<T> | undefined, value: T | null) {
  if (!ref) return
  if (typeof ref === "function") {
    ref(value)
    return
  }
  ref.current = value
}

export function Header({
  className,
  fixed,
  children,
  ref,
  ...props
}: HeaderProps) {
  const headerRef = useRef<HTMLElement | null>(null)
  const [offset, setOffset] = useState(0)

  useEffect(() => {
    if (!fixed) return

    const header = headerRef.current
    const shell = header?.closest('[data-slot="sidebar-inset"]')
    const scrollContainer = shell?.querySelector<HTMLElement>(
      '[data-layout-scroll]'
    )

    const onScroll = () => {
      setOffset(
        scrollContainer?.scrollTop ??
          document.body.scrollTop ??
          document.documentElement.scrollTop
      )
    }

    onScroll()

    const scrollTarget = scrollContainer ?? document
    scrollTarget.addEventListener("scroll", onScroll, { passive: true })

    return () => scrollTarget.removeEventListener("scroll", onScroll)
  }, [fixed])

  return (
    <header
      data-fixed-header={fixed ? "true" : undefined}
      ref={(node) => {
        headerRef.current = node
        setRef(ref, node)
      }}
      className={cn(
        "z-50 h-16 bg-background/95 transition-all duration-200 md:in-data-[slot=sidebar-inset]:rounded-t-xl",
        fixed && "header-fixed peer/header absolute inset-x-0 top-0 w-full",
        offset > 10 &&
          fixed &&
          "bg-background/80 shadow-sm backdrop-blur-xl supports-[backdrop-filter]:bg-background/70",
        className
      )}
      {...props}
    >
      <div className="relative flex h-full items-center gap-3 p-4 sm:gap-4">
        <SidebarTrigger />
        <Separator orientation="vertical" className="h-6 data-vertical:self-center" />
        {children}
      </div>
    </header>
  )
}
