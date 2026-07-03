import { cn } from '@/lib/utils'

export function SidebarActiveIndicator({
  className,
}: {
  className?: string
}) {
  return (
    <span
      aria-hidden='true'
      className={cn(
        'absolute top-1/2 left-1 h-4 w-1 -translate-y-1/2 rounded-full bg-primary group-data-[collapsible=icon]:hidden',
        className
      )}
    />
  )
}
