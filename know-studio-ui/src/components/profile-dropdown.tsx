import { Link } from '@tanstack/react-router'
import { useTranslation } from 'react-i18next'
import {
  CreditCard,
  LogOut,
  Settings,
  UserRound,
  UsersRound,
} from 'lucide-react'
import useDialogState from '@/hooks/use-dialog-state'
import { useAuthStore } from '@/stores/auth-store'
import { getDisplayNameInitials } from '@/lib/utils'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuGroup,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuShortcut,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { SignOutDialog } from '@/components/sign-out-dialog'

export function ProfileDropdown() {
  const { t } = useTranslation()
  const [open, setOpen] = useDialogState()
  const currentUser = useAuthStore((state) => state.auth.user)
  const displayName = currentUser?.displayName ?? '未登录'
  const userCode = currentUser?.userCode ?? '访客'
  const role = currentUser?.systemRole ?? 'GUEST'

  return (
    <>
      <DropdownMenu modal={false}>
        <DropdownMenuTrigger asChild>
          <Button
            variant='ghost'
            size='icon-lg'
            className='relative rounded-full p-0 hover:bg-accent aria-expanded:bg-accent'
          >
            <Avatar className='h-8 w-8'>
              <AvatarImage src='/avatars/01.png' alt={displayName} />
              <AvatarFallback>{getDisplayNameInitials(displayName)}</AvatarFallback>
            </Avatar>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent
          className='w-max min-w-44 [&_[data-slot=dropdown-menu-item]]:px-2.5'
          align='end'
          forceMount
        >
          <DropdownMenuLabel className='px-2.5 font-normal'>
            <div className='flex flex-col gap-1.5'>
              <p className='text-sm leading-none font-medium'>{displayName}</p>
              <p className='text-xs leading-none text-muted-foreground'>
                {userCode} · {role}
              </p>
            </div>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuGroup>
            <DropdownMenuItem asChild>
              <Link to='/admin/settings/account'>
                <UserRound />
                {t('userMenu.profile')}
                <DropdownMenuShortcut>⇧⌘P</DropdownMenuShortcut>
              </Link>
            </DropdownMenuItem>
            <DropdownMenuItem asChild>
              <Link to='/admin/settings/account'>
                <CreditCard />
                {t('userMenu.billing')}
                <DropdownMenuShortcut>⌘B</DropdownMenuShortcut>
              </Link>
            </DropdownMenuItem>
            <DropdownMenuItem asChild>
              <Link to='/admin/settings/account'>
                <Settings />
                {t('userMenu.settings')}
                <DropdownMenuShortcut>⌘S</DropdownMenuShortcut>
              </Link>
            </DropdownMenuItem>
            <DropdownMenuItem>
              <UsersRound />
              {t('userMenu.newTeam')}
            </DropdownMenuItem>
          </DropdownMenuGroup>
          <DropdownMenuSeparator />
          <DropdownMenuItem variant='destructive' onClick={() => setOpen(true)}>
            <LogOut />
            {t('userMenu.signOut')}
            <DropdownMenuShortcut>⇧⌘Q</DropdownMenuShortcut>
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      <SignOutDialog open={!!open} onOpenChange={setOpen} />
    </>
  )
}
