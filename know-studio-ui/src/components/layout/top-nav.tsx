import { Link } from '@tanstack/react-router'
import { Menu } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

type TopNavProps = React.HTMLAttributes<HTMLElement> & {
  links: {
    title: string
    href: string
    isActive: boolean
    disabled?: boolean
  }[]
}

export function TopNav({ className, links, ...props }: TopNavProps) {
  const { t } = useTranslation()

  return (
    <>
      <DropdownMenu modal={false}>
        <DropdownMenuTrigger asChild>
          <Button
            size='icon'
            variant='outline'
            className={cn('md:size-7 lg:hidden', className)}
          >
            <Menu />
            <span className='sr-only'>Toggle navigation menu</span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent side='bottom' align='start'>
          {links.map(({ title, href, isActive, disabled }) => {
            const label = t(`navigation.${title}`, { defaultValue: title })

            return (
              <DropdownMenuItem key={`${title}-${href}`} asChild>
                <Link
                  to={href}
                  className={!isActive ? 'text-muted-foreground' : ''}
                  disabled={disabled}
                >
                  {label}
                </Link>
              </DropdownMenuItem>
            )
          })}
        </DropdownMenuContent>
      </DropdownMenu>

      <nav
        className={cn(
          'hidden items-center gap-4 lg:flex xl:gap-6',
          className
        )}
        {...props}
      >
        {links.map(({ title, href, isActive, disabled }) => {
          const label = t(`navigation.${title}`, { defaultValue: title })

          return (
            <Link
              key={`${title}-${href}`}
              to={href}
              disabled={disabled}
              className={`text-sm font-medium transition-colors hover:text-primary ${isActive ? '' : 'text-muted-foreground'}`}
            >
              {label}
            </Link>
          )
        })}
      </nav>
    </>
  )
}
