import { cn } from '@/lib/utils'
import { Badge } from '@/components/ui/badge'

const statusBadgeClassName = 'h-4 px-1.5 text-[10px]'

export function DocumentStatusBadge({ status }: { status: string }) {
  if (status === 'READY') {
    return (
      <Badge
        variant='outline'
        className={cn(
          statusBadgeClassName,
          'border-success/40 bg-success/5 text-success'
        )}
      >
        READY
      </Badge>
    )
  }

  if (status === 'PROCESSING') {
    return (
      <Badge
        variant='outline'
        className={cn(
          statusBadgeClassName,
          'border-warning/40 bg-warning/5 text-warning'
        )}
      >
        PROCESSING
      </Badge>
    )
  }

  if (status === 'FAILED') {
    return (
      <Badge
        variant='outline'
        className={cn(
          statusBadgeClassName,
          'border-destructive/40 bg-destructive/5 text-destructive'
        )}
      >
        FAILED
      </Badge>
    )
  }

  return (
    <Badge variant='outline' className={statusBadgeClassName}>
      {status}
    </Badge>
  )
}
