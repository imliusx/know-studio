import { useEffect, type MouseEvent } from 'react'
import { Monitor, Moon, Sun } from 'lucide-react'
import { AnimatePresence, motion, useReducedMotion } from 'motion/react'
import { useTranslation } from 'react-i18next'
import { useTheme } from '@/context/theme-provider'
import {
  HeaderIconButton,
  HeaderIconTooltip,
} from '@/components/layout/header-icon-button'

type Theme = 'dark' | 'light' | 'system'

const THEME_SEQUENCE: Theme[] = ['light', 'dark', 'system']
const THEME_ICONS = {
  dark: Moon,
  light: Sun,
  system: Monitor,
} satisfies Record<Theme, typeof Sun>

function getNextTheme(theme: Theme) {
  const currentIndex = THEME_SEQUENCE.indexOf(theme)
  return THEME_SEQUENCE[(currentIndex + 1) % THEME_SEQUENCE.length]
}

export function ThemeSwitch() {
  const { t } = useTranslation()
  const { resolvedTheme, theme, setTheme } = useTheme()
  const reduceMotion = useReducedMotion()

  /* Update theme-color meta tag
   * when theme is updated */
  useEffect(() => {
    const themeColor = resolvedTheme === 'dark' ? '#09090b' : '#fff'
    const metaThemeColor = document.querySelector("meta[name='theme-color']")
    if (metaThemeColor) metaThemeColor.setAttribute('content', themeColor)
  }, [resolvedTheme])

  const getTransitionOrigin = (event: MouseEvent<HTMLElement>) => {
    const { left, top, width, height } = event.currentTarget.getBoundingClientRect()
    return {
      x: left + width / 2,
      y: top + height / 2,
    }
  }

  const nextTheme = getNextTheme(theme)
  const label = t('theme.toggle')
  const ThemeIcon = THEME_ICONS[theme]

  return (
    <HeaderIconTooltip label={label}>
      <HeaderIconButton
        label={label}
        iconScale={theme === 'dark' ? 'lg' : 'default'}
        className='overflow-hidden'
        onClick={(event) =>
          setTheme(nextTheme, { origin: getTransitionOrigin(event) })
        }
      >
        <AnimatePresence mode='wait' initial={false}>
          <motion.span
            key={theme}
            className='absolute inset-0 flex items-center justify-center'
            initial={
              reduceMotion ? false : { opacity: 0, rotate: -45, scale: 0.75 }
            }
            animate={
              reduceMotion
                ? undefined
                : { opacity: 1, rotate: 0, scale: 1 }
            }
            exit={
              reduceMotion ? undefined : { opacity: 0, rotate: 45, scale: 0.75 }
            }
            transition={
              reduceMotion
                ? undefined
                : { duration: 0.18, ease: [0.22, 1, 0.36, 1] }
            }
          >
            <ThemeIcon aria-hidden='true' />
          </motion.span>
        </AnimatePresence>
      </HeaderIconButton>
    </HeaderIconTooltip>
  )
}
