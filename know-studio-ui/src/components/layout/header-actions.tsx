import { Link } from '@tanstack/react-router'
import { ShieldCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { ConfigDrawer } from '@/components/config-drawer'
import { LanguageSwitch } from '@/components/language-switch'
import { ProfileDropdown } from '@/components/profile-dropdown'
import { Search } from '@/components/search'
import { ThemeSwitch } from '@/components/theme-switch'
import { Button } from '@/components/ui/button'
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip'

type HeaderActionsProps = {
  showSearch?: boolean
  showAdminLink?: boolean
}

export function HeaderActions({
  showSearch = true,
  showAdminLink = false,
}: HeaderActionsProps) {
  const { t } = useTranslation()
  const adminLabel = t('userMenu.adminConsole')

  return (
    <>
      {showSearch ? <Search className='me-auto' /> : null}
      {showAdminLink ? (
        <Tooltip>
          <TooltipTrigger asChild>
            <Button asChild variant='ghost' size='icon' aria-label={adminLabel}>
              <Link to='/admin'>
                <ShieldCheck className='size-[1.2rem]' aria-hidden='true' />
              </Link>
            </Button>
          </TooltipTrigger>
          <TooltipContent side='bottom'>{adminLabel}</TooltipContent>
        </Tooltip>
      ) : null}
      <LanguageSwitch />
      <ThemeSwitch />
      <ConfigDrawer />
      <ProfileDropdown />
    </>
  )
}
