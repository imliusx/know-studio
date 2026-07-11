import { Link } from '@tanstack/react-router'
import { type LucideIcon } from 'lucide-react'
import { type ComponentProps, type ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import {
  headerIconButtonClass,
  headerIconClass,
  type HeaderIconScale,
} from './header-icon-button-utils'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'

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
      className={headerIconButtonClass(className)}
      aria-label={label}
      {...props}
    >
      {children ?? (
        Icon ? (
          <Icon className={headerIconClass(iconScale)} aria-hidden='true' />
        ) : null
      )}
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
      className={headerIconButtonClass(className)}
      aria-label={label}
      {...props}
    >
      <Link to={to}>
        {Icon ? (
          <Icon className={headerIconClass(iconScale)} aria-hidden='true' />
        ) : null}
        <span className='sr-only'>{label}</span>
      </Link>
    </Button>
  )
}
