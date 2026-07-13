import { type SVGProps } from 'react'
import { Root as Radio, Item } from '@radix-ui/react-radio-group'
import { CircleCheck, Palette, RotateCcw } from 'lucide-react'
import { useTranslation } from 'react-i18next'
import { IconLayoutCompact } from '@/assets/custom/icon-layout-compact'
import { IconLayoutDefault } from '@/assets/custom/icon-layout-default'
import { IconLayoutFull } from '@/assets/custom/icon-layout-full'
import { IconSidebarFloating } from '@/assets/custom/icon-sidebar-floating'
import { IconSidebarInset } from '@/assets/custom/icon-sidebar-inset'
import { IconSidebarSidebar } from '@/assets/custom/icon-sidebar-sidebar'
import { IconThemeDark } from '@/assets/custom/icon-theme-dark'
import { IconThemeLight } from '@/assets/custom/icon-theme-light'
import { IconThemeSystem } from '@/assets/custom/icon-theme-system'
import {
  COLOR_THEME_OPTIONS,
  type ColorTheme,
} from '@/lib/theme/color-theme'
import { cn } from '@/lib/utils'
import { useColorTheme } from '@/context/color-theme-provider'
import { type Collapsible, useLayout } from '@/context/layout-provider'
import { useTheme } from '@/context/theme-provider'
import {
  HeaderIconButton,
  HeaderIconTooltip,
} from '@/components/layout/header-icon-button'
import { Button } from '@/components/ui/button'
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetFooter,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from '@/components/ui/sheet'
import { useSidebar } from './ui/sidebar'

export function ConfigDrawer() {
  const { t } = useTranslation()
  const { setOpen } = useSidebar()
  const { resetColorTheme } = useColorTheme()
  const { resetTheme } = useTheme()
  const { resetLayout } = useLayout()

  const handleReset = () => {
    setOpen(true)
    resetColorTheme()
    resetTheme()
    resetLayout()
  }

  return (
    <Sheet>
      <HeaderIconTooltip label={t('config.trigger')}>
        <SheetTrigger asChild>
          <HeaderIconButton
            label={t('config.trigger')}
            icon={Palette}
            iconScale='sm'
          />
        </SheetTrigger>
      </HeaderIconTooltip>
      <SheetContent className='flex flex-col'>
        <SheetHeader className='pb-0 text-start'>
          <SheetTitle>{t('config.title')}</SheetTitle>
          <SheetDescription>
            {t('config.description')}
          </SheetDescription>
        </SheetHeader>
        <div className='space-y-6 overflow-y-auto px-4'>
          <ThemeConfig />
          <ColorConfig />
          <SidebarConfig />
          <LayoutConfig />
        </div>
        <SheetFooter className='items-end gap-2'>
          <Button
            variant='destructive'
            size='sm'
            onClick={handleReset}
            aria-label={t('config.resetAll')}
          >
            <RotateCcw data-icon='inline-start' />
            {t('config.reset')}
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  )
}

function SectionTitle({
  title,
  showReset = false,
  onReset,
  resetAriaLabel,
  className,
}: {
  title: string
  showReset?: boolean
  onReset?: () => void
  /** Shown on the small per-section reset (RotateCcw) for accessibility and tests. */
  resetAriaLabel?: string
  className?: string
}) {
  return (
    <div
      className={cn(
        'mb-2 flex items-center gap-2 text-sm font-semibold text-muted-foreground',
        className
      )}
    >
      {title}
      {showReset && onReset && (
        <Button
          type='button'
          size='icon'
          variant='secondary'
          className='size-4 rounded-full'
          onClick={onReset}
          aria-label={resetAriaLabel}
        >
          <RotateCcw className='size-3' />
        </Button>
      )}
    </div>
  )
}

function RadioGroupItem({
  item,
  isTheme = false,
}: {
  item: {
    value: string
    label: string
    icon: (props: SVGProps<SVGSVGElement>) => React.ReactElement
  }
  isTheme?: boolean
}) {
  const { t } = useTranslation()

  return (
    <Item
      value={item.value}
      className={cn('group outline-none', 'transition duration-200 ease-in')}
      aria-label={item.label}
      aria-describedby={`${item.value}-description`}
    >
      <div
        className={cn(
          'relative rounded-[6px] ring-[1px] ring-border',
          'group-data-[state=checked]:shadow-2xl group-data-[state=checked]:ring-0',
          'group-focus-visible:ring-2'
        )}
        role='img'
        aria-hidden='false'
        aria-label={t('config.optionPreview', { label: item.label })}
      >
        <CircleCheck
          className={cn(
            'size-6 fill-primary stroke-background',
            'group-data-[state=unchecked]:hidden',
            'absolute top-0 right-0 translate-x-1/2 -translate-y-1/2'
          )}
          aria-hidden='true'
        />
        <item.icon
          className={cn(
            !isTheme &&
              'fill-primary stroke-primary group-data-[state=unchecked]:fill-muted-foreground group-data-[state=unchecked]:stroke-muted-foreground'
          )}
          aria-hidden='true'
        />
      </div>
      <div
        className='mt-1 text-xs'
        id={`${item.value}-description`}
        aria-live='polite'
      >
        {item.label}
      </div>
    </Item>
  )
}

function ThemeConfig() {
  const { t } = useTranslation()
  const { defaultTheme, theme, setTheme } = useTheme()
  return (
    <div>
      <SectionTitle
        title={t('config.theme')}
        showReset={theme !== defaultTheme}
        onReset={() => setTheme(defaultTheme)}
        resetAriaLabel={t('config.resetTheme')}
      />
      <Radio
        value={theme}
        onValueChange={setTheme}
        className='grid w-full max-w-md grid-cols-3 gap-4'
        aria-label={t('config.selectTheme')}
        aria-describedby='theme-description'
      >
        {[
          {
            value: 'system',
            label: t('theme.system'),
            icon: IconThemeSystem,
          },
          {
            value: 'light',
            label: t('theme.light'),
            icon: IconThemeLight,
          },
          {
            value: 'dark',
            label: t('theme.dark'),
            icon: IconThemeDark,
          },
        ].map((item) => (
          <RadioGroupItem key={item.value} item={item} isTheme />
        ))}
      </Radio>
      <div id='theme-description' className='sr-only'>
        {t('config.themeDescription')}
      </div>
    </div>
  )
}

function ColorConfig() {
  const { t } = useTranslation()
  const {
    colorTheme,
    defaultColorTheme,
    resetColorTheme,
    setColorTheme,
  } = useColorTheme()

  return (
    <div>
      <SectionTitle
        title={t('config.color')}
        showReset={colorTheme !== defaultColorTheme}
        onReset={resetColorTheme}
        resetAriaLabel={t('config.resetColor')}
      />
      <Radio
        value={colorTheme}
        onValueChange={(value) => setColorTheme(value as ColorTheme)}
        className='grid w-full max-w-md grid-cols-3 gap-3'
        aria-label={t('config.selectColor')}
        aria-describedby='color-theme-description'
      >
        {COLOR_THEME_OPTIONS.map((option) => {
          return (
            <Item
              key={option.value}
              value={option.value}
              className='group outline-none'
              aria-label={t(`config.colors.${option.value}`)}
            >
              <div
                className={cn(
                  'relative flex h-8 items-center gap-2 rounded-lg px-2 text-sm transition-colors',
                  'text-foreground',
                  option.optionClassName,
                  'group-focus-visible:ring-2 group-focus-visible:ring-ring'
                )}
              >
                <CircleCheck
                  className={cn(
                    'size-6 fill-primary stroke-background',
                    'group-data-[state=unchecked]:hidden',
                    'absolute top-0 right-0 translate-x-1/2 -translate-y-1/2'
                  )}
                  aria-hidden='true'
                />
                <span
                  aria-hidden='true'
                  className={cn(
                    'size-3.5 shrink-0 rounded-full ring-1 ring-border',
                    option.swatchClassName
                  )}
                />
                <span className='min-w-0 truncate'>
                  {t(`config.colors.${option.value}`)}
                </span>
              </div>
            </Item>
          )
        })}
      </Radio>
      <div id='color-theme-description' className='sr-only'>
        {t('config.colorDescription')}
      </div>
    </div>
  )
}

function SidebarConfig() {
  const { t } = useTranslation()
  const { defaultVariant, variant, setVariant } = useLayout()
  return (
    <div className='max-md:hidden'>
      <SectionTitle
        title={t('config.sidebar')}
        showReset={defaultVariant !== variant}
        onReset={() => setVariant(defaultVariant)}
        resetAriaLabel={t('config.resetSidebar')}
      />
      <Radio
        value={variant}
        onValueChange={setVariant}
        className='grid w-full max-w-md grid-cols-3 gap-4'
        aria-label={t('config.selectSidebar')}
        aria-describedby='sidebar-description'
      >
        {[
          {
            value: 'inset',
            label: t('config.sidebarVariants.inset'),
            icon: IconSidebarInset,
          },
          {
            value: 'floating',
            label: t('config.sidebarVariants.floating'),
            icon: IconSidebarFloating,
          },
          {
            value: 'sidebar',
            label: t('config.sidebarVariants.sidebar'),
            icon: IconSidebarSidebar,
          },
        ].map((item) => (
          <RadioGroupItem key={item.value} item={item} />
        ))}
      </Radio>
      <div id='sidebar-description' className='sr-only'>
        {t('config.sidebarDescription')}
      </div>
    </div>
  )
}

function LayoutConfig() {
  const { t } = useTranslation()
  const { open, setOpen } = useSidebar()
  const { defaultCollapsible, collapsible, setCollapsible } = useLayout()

  const radioState = open ? 'default' : collapsible

  return (
    <div className='max-md:hidden'>
      <SectionTitle
        title={t('config.layout')}
        showReset={radioState !== 'default'}
        onReset={() => {
          setOpen(true)
          setCollapsible(defaultCollapsible)
        }}
        resetAriaLabel={t('config.resetLayout')}
      />
      <Radio
        value={radioState}
        onValueChange={(v) => {
          if (v === 'default') {
            setOpen(true)
            return
          }
          setOpen(false)
          setCollapsible(v as Collapsible)
        }}
        className='grid w-full max-w-md grid-cols-3 gap-4'
        aria-label={t('config.selectLayout')}
        aria-describedby='layout-description'
      >
        {[
          {
            value: 'default',
            label: t('config.layoutVariants.default'),
            icon: IconLayoutDefault,
          },
          {
            value: 'icon',
            label: t('config.layoutVariants.compact'),
            icon: IconLayoutCompact,
          },
          {
            value: 'offcanvas',
            label: t('config.layoutVariants.full'),
            icon: IconLayoutFull,
          },
        ].map((item) => (
          <RadioGroupItem key={item.value} item={item} />
        ))}
      </Radio>
      <div id='layout-description' className='sr-only'>
        {t('config.layoutDescription')}
      </div>
    </div>
  )
}
