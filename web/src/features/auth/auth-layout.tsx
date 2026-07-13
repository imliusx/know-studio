import { Palette } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  COLOR_THEME_OPTIONS,
  type ColorTheme,
} from '@/lib/theme/color-theme'
import { cn } from '@/lib/utils'
import { useColorTheme } from '@/context/color-theme-provider'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { LanguageSwitch } from '@/components/language-switch'
import { ThemeSwitch } from '@/components/theme-switch'

type AuthLayoutProps = {
  children: React.ReactNode
}

export function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className='relative flex min-h-svh w-full items-center justify-center p-6 md:p-10'>
      <div className='absolute top-4 right-4 flex items-center gap-1 md:top-6 md:right-6'>
        <ColorThemeMenu />
        <LanguageSwitch />
        <ThemeSwitch />
      </div>
      {children}
    </div>
  )
}

function ColorThemeMenu() {
  const { t } = useTranslation()
  const { colorTheme, setColorTheme } = useColorTheme()

  return (
    <DropdownMenu modal={false}>
      <DropdownMenuTrigger asChild>
        <Button variant='ghost' size='icon' aria-label={t('config.selectColor')}>
          <Palette className='size-[1.2rem]' aria-hidden='true' />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align='end' className='min-w-32'>
        <DropdownMenuRadioGroup
          value={colorTheme}
          onValueChange={(value) => setColorTheme(value as ColorTheme)}
        >
          {COLOR_THEME_OPTIONS.map((option) => (
            <DropdownMenuRadioItem key={option.value} value={option.value}>
              <span
                aria-hidden='true'
                className={cn(
                  'size-3.5 shrink-0 rounded-full ring-1 ring-border',
                  option.swatchClassName
                )}
              />
              {t(`config.colors.${option.value}`)}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
