import { Link } from '@tanstack/react-router'
import { type LucideIcon } from 'lucide-react'
import { type ComponentProps, type ReactNode } from 'react'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'

type HeaderIconScale = 'default' | 'sm' | 'lg'

const iconScaleClass: Record<HeaderIconScale, string> = {
  default: '[&_svg]:size-[1.2rem]',
  sm: '[&_svg]:size-[1.125rem]',
  lg: '[&_svg]:size-[1.25rem]',
}

type HeaderIconButtonProps = Omit<
  ComponentProps<typeof Button>,
  'children' | 'size' | 'variant'
> & {
  label: string
  icon?: LucideIcon
  iconScale?: HeaderIconScale
  children?: ReactNode
}

type HeaderIconLinkButtonProps = HeaderIconButtonProps & {
  to: ComponentProps<typeof Link>['to']
}

export function headerIconButtonClass(
  iconScale: HeaderIconScale = 'default',
  className?: string
) {
  return cn('relative', iconScaleClass[iconScale], className)
}

export function HeaderIconTooltip({
  label,
  children,
}: {
  label: ReactNode
  children: ReactNode
}) {
  return (
    <Tooltip>
      <TooltipTrigger asChild>{children}</TooltipTrigger>
      <TooltipContent side='bottom'>{label}</TooltipContent>
    </Tooltip>
  )
}

export function HeaderIconButton({
  label,
  icon: Icon,
  iconScale = 'default',
  className,
  children,
  ...props
}: HeaderIconButtonProps) {
  return (
    <Button
      variant='ghost'
      size='icon'
      className={headerIconButtonClass(iconScale, className)}
      aria-label={label}
      {...props}
    >
      {children ?? (Icon ? <Icon aria-hidden='true' /> : null)}
      <span className='sr-only'>{label}</span>
    </Button>
  )
}

export function HeaderIconLinkButton({
  label,
  to,
  icon: Icon,
  iconScale = 'default',
  className,
  ...props
}: HeaderIconLinkButtonProps) {
  return (
    <Button
      asChild
      variant='ghost'
      size='icon'
      className={headerIconButtonClass(iconScale, className)}
      aria-label={label}
      {...props}
    >
      <Link to={to}>
        {Icon ? <Icon aria-hidden='true' /> : null}
        <span className='sr-only'>{label}</span>
      </Link>
    </Button>
  )
}
