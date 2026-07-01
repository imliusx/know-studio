import { Outlet } from '@tanstack/react-router'
import { Monitor, Bell, Palette, Wrench, UserCog } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { Separator } from '@/components/ui/separator'
import { Header } from '@/components/layout/header'
import { HeaderActions } from '@/components/layout/header-actions'
import { Main } from '@/components/layout/main'
import { SidebarNav } from './components/sidebar-nav'

const sidebarNavItems = [
  {
    title: 'Profile',
    href: '/admin/settings',
    icon: <UserCog size={18} />,
  },
  {
    title: 'Account',
    href: '/admin/settings/account',
    icon: <Wrench size={18} />,
  },
  {
    title: 'Appearance',
    href: '/admin/settings/appearance',
    icon: <Palette size={18} />,
  },
  {
    title: 'Notifications',
    href: '/admin/settings/notifications',
    icon: <Bell size={18} />,
  },
  {
    title: 'Display',
    href: '/admin/settings/display',
    icon: <Monitor size={18} />,
  },
]

export function Settings() {
  const { t } = useTranslation()

  return (
    <>
      <Header fixed>
        <HeaderActions />
      </Header>

      <Main fixed className='pt-4'>
        <div className='flex flex-col'>
          <h1 className='text-2xl font-bold tracking-tight md:text-3xl'>
            {t('settings.title')}
          </h1>
        </div>
        <Separator className='my-4 lg:my-6' />
        <div className='flex flex-1 flex-col gap-2 overflow-hidden lg:flex-row lg:gap-12'>
          <aside className='top-0 lg:sticky lg:w-1/5'>
            <SidebarNav items={sidebarNavItems} />
          </aside>
          <div className='flex w-full overflow-y-hidden p-1'>
            <Outlet />
          </div>
        </div>
      </Main>
    </>
  )
}
