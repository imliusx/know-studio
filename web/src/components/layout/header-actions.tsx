import { useLocation } from '@tanstack/react-router'
import { MessageSquare, ShieldCheck } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { cn } from '@/lib/utils'
import { ConfigDrawer } from '@/components/config-drawer'
import { LanguageSwitch } from '@/components/language-switch'
import { ProfileDropdown } from '@/components/profile-dropdown'
import { Search } from '@/components/search'
import { ThemeSwitch } from '@/components/theme-switch'
import {
  HeaderIconLinkButton,
  HeaderIconTooltip,
} from './header-icon-button'

type HeaderActionsProps = {
  showSearch?: boolean
  showAdminLink?: boolean
  showProfileAccountLinks?: boolean
  className?: string
}

export function HeaderActions({
  showSearch = true,
  showAdminLink = false,
  showProfileAccountLinks = true,
  className,
}: HeaderActionsProps) {
  const { t } = useTranslation()
  const href = useLocation({ select: (location) => location.href })
  const adminLabel = t('userMenu.adminConsole')
  const chatLabel = '回到聊天'
  const shouldShowChatLink = href.split('?')[0] !== '/'

  return (
    <div
      className={cn(
        'flex items-center gap-2',
        showSearch ? 'min-w-0 flex-1' : 'shrink-0',
        className
      )}
    >
      {showSearch ? <Search className='me-auto' /> : null}
      {shouldShowChatLink ? (
        <HeaderIconTooltip label={chatLabel}>
          <HeaderIconLinkButton
            to='/'
            label={chatLabel}
            icon={MessageSquare}
          />
        </HeaderIconTooltip>
      ) : null}
      {showAdminLink ? (
        <HeaderIconTooltip label={adminLabel}>
          <HeaderIconLinkButton
            to='/admin'
            label={adminLabel}
            icon={ShieldCheck}
          />
        </HeaderIconTooltip>
      ) : null}
      <LanguageSwitch />
      <ThemeSwitch />
      <ConfigDrawer />
      <ProfileDropdown showAccountLinks={showProfileAccountLinks} />
    </div>
  )
}
