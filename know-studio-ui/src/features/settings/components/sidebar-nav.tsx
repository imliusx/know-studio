import { useState, type JSX } from "react"
import { useLocation, useNavigate, Link } from "@tanstack/react-router"
import { useTranslation } from "react-i18next"
import { cn } from "@/lib/utils"
import { buttonVariants } from "@/components/ui/button"
import { ScrollArea } from "@/components/ui/scroll-area"
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"

type SidebarNavProps = React.HTMLAttributes<HTMLElement> & {
  items: {
    href: string
    title: string
    icon: JSX.Element
  }[]
}

export function SidebarNav({ className, items, ...props }: SidebarNavProps) {
  const { t } = useTranslation()
  const { pathname } = useLocation()
  const navigate = useNavigate()
  const [val, setVal] = useState(pathname ?? "/settings")

  const handleSelect = (e: string) => {
    setVal(e)
    navigate({ to: e })
  }

  return (
    <>
      <div className="p-1 md:hidden">
        <Select value={val} onValueChange={handleSelect}>
          <SelectTrigger className="h-12 sm:w-48">
            <SelectValue placeholder={t("settings.navPlaceholder")} />
          </SelectTrigger>
          <SelectContent>
            <SelectGroup>
              {items.map((item) => (
                <SelectItem key={item.href} value={item.href}>
                  <div className="flex items-center gap-3">
                    <span>{item.icon}</span>
                    <span>
                      {t(`navigation.${item.title}`, {
                        defaultValue: item.title,
                      })}
                    </span>
                  </div>
                </SelectItem>
              ))}
            </SelectGroup>
          </SelectContent>
        </Select>
      </div>

      <ScrollArea
        orientation="horizontal"
        type="always"
        className="hidden w-full min-w-40 bg-background px-1 py-2 md:block"
      >
        <nav
          className={cn("flex gap-2 py-1 lg:flex-col lg:gap-1", className)}
          {...props}
        >
          {items.map((item) => (
            <Link
              key={item.href}
              to={item.href}
              className={cn(
                buttonVariants({ variant: "ghost" }),
                pathname === item.href
                  ? "bg-muted hover:bg-accent"
                  : "hover:bg-accent",
                "justify-start rounded-lg no-underline hover:no-underline"
              )}
            >
              <span className="me-2">{item.icon}</span>
              {t(`navigation.${item.title}`, { defaultValue: item.title })}
            </Link>
          ))}
        </nav>
      </ScrollArea>
    </>
  )
}
