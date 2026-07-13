import { Languages } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  LANGUAGE_OPTIONS,
  useLanguage,
} from '@/context/language-provider'
import { type Language } from '@/lib/i18n/languages'
import {
  HeaderIconButton,
  HeaderIconTooltip,
} from '@/components/layout/header-icon-button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

export function LanguageSwitch() {
  const { t } = useTranslation()
  const { language, setLanguage } = useLanguage()

  return (
    <DropdownMenu modal={false}>
      <HeaderIconTooltip label={t('language.select')}>
        <DropdownMenuTrigger asChild>
          <HeaderIconButton
            label={t('language.select')}
            icon={Languages}
          />
        </DropdownMenuTrigger>
      </HeaderIconTooltip>
      <DropdownMenuContent align='end'>
        <DropdownMenuRadioGroup
          value={language}
          onValueChange={(value) => setLanguage(value as Language)}
        >
          {LANGUAGE_OPTIONS.map((option) => (
            <DropdownMenuRadioItem key={option.value} value={option.value}>
              <span className='inline-flex size-4 shrink-0 items-center justify-center text-xs font-medium text-muted-foreground'>
                {option.shortLabel}
              </span>
              {t(option.labelKey)}
            </DropdownMenuRadioItem>
          ))}
        </DropdownMenuRadioGroup>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}
