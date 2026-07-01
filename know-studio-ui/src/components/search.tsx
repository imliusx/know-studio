import { SearchIcon } from "lucide-react"
import { useTranslation } from "react-i18next"
import { cn } from "@/lib/utils"
import { useSearch } from "@/context/search-provider"
import { Button } from "./ui/button"
import { Kbd } from "./ui/kbd"

export function Search({
  className = "",
  placeholder,
  ...props
}: React.ComponentProps<"button"> & { placeholder?: string }) {
  const { t } = useTranslation()
  const { setOpen } = useSearch()
  return (
    <Button
      {...props}
      type="button"
      variant="outline"
      className={cn(
        "w-full flex-1 justify-start text-muted-foreground sm:w-40 md:flex-none lg:w-52 xl:w-64",
        className
      )}
      aria-keyshortcuts="Meta+K Control+K"
      onClick={() => setOpen(true)}
    >
      <SearchIcon data-icon="inline-start" />
      <span className="truncate">{placeholder ?? t("search.button")}</span>
      <Kbd className="ms-auto hidden sm:inline-flex">⌘ K</Kbd>
    </Button>
  )
}
