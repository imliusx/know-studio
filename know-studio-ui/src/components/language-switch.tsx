import { Languages } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import {
  LANGUAGE_OPTIONS,
  useLanguage,
} from '@/context/language-provider'
import { type Language } from '@/lib/i18n/languages'
import { Button } from '@/components/ui/button'
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
      <DropdownMenuTrigger asChild>
        <Button
          variant='ghost'
          size='icon'
          aria-label={t('language.select')}
        >
          <Languages className='size-[1.2rem]' aria-hidden='true' />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align='end' className='min-w-36'>
        <DropdownMenuRadioGroup
          value={language}
          onValueChange={(value) => setLanguage(value as Language)}
        >
          {LANGUAGE_OPTIONS.map((option) => (
            <DropdownMenuRadioItem key={option.value} value={option.value}>
              <span className='w-7 text-xs font-medium text-muted-foreground'>
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
