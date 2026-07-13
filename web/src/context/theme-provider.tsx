import { createContext, useContext, useEffect, useState, useMemo } from 'react'
import { flushSync } from 'react-dom'
import { getCookie, setCookie, removeCookie } from '@/lib/cookies'

type Theme = 'dark' | 'light' | 'system'
type ResolvedTheme = Exclude<Theme, 'system'>
type ThemeTransitionOrigin = {
  x: number
  y: number
}
type ThemeTransitionOptions = {
  origin?: ThemeTransitionOrigin
}
type ViewTransition = {
  ready: Promise<void>
  finished: Promise<void>
}
type DocumentWithViewTransition = Document & {
  startViewTransition?: (updateCallback: () => void) => ViewTransition
}

const DEFAULT_THEME = 'system'
const THEME_COOKIE_NAME = 'vite-ui-theme'
const THEME_COOKIE_MAX_AGE = 60 * 60 * 24 * 365 // 1 year

type ThemeProviderProps = {
  children: React.ReactNode
  defaultTheme?: Theme
  storageKey?: string
}

type ThemeProviderState = {
  defaultTheme: Theme
  resolvedTheme: ResolvedTheme
  theme: Theme
  setTheme: (theme: Theme, options?: ThemeTransitionOptions) => void
  resetTheme: () => void
}

const initialState: ThemeProviderState = {
  defaultTheme: DEFAULT_THEME,
  resolvedTheme: 'light',
  theme: DEFAULT_THEME,
  setTheme: () => null,
  resetTheme: () => null,
}

const ThemeContext = createContext<ThemeProviderState>(initialState)

function getResolvedTheme(theme: Theme): ResolvedTheme {
  if (theme === 'system') {
    return window.matchMedia('(prefers-color-scheme: dark)').matches
      ? 'dark'
      : 'light'
  }

  return theme
}

function applyResolvedTheme(theme: ResolvedTheme) {
  const root = window.document.documentElement
  root.classList.remove('light', 'dark')
  root.classList.add(theme)
}

function getThemeTransitionClipPaths(
  x: number,
  y: number,
  viewportWidth: number,
  viewportHeight: number
) {
  const maxRadius = Math.hypot(
    Math.max(x, viewportWidth - x),
    Math.max(y, viewportHeight - y)
  )

  return [
    `circle(0px at ${x}px ${y}px)`,
    `circle(${maxRadius}px at ${x}px ${y}px)`,
  ]
}

function runThemeTransition(
  updateTheme: () => void,
  options?: ThemeTransitionOptions
) {
  const documentWithViewTransition = document as DocumentWithViewTransition

  if (
    typeof documentWithViewTransition.startViewTransition !== 'function' ||
    window.matchMedia('(prefers-reduced-motion: reduce)').matches
  ) {
    updateTheme()
    return
  }

  const viewportWidth = window.visualViewport?.width ?? window.innerWidth
  const viewportHeight = window.visualViewport?.height ?? window.innerHeight
  const x = options?.origin?.x ?? viewportWidth / 2
  const y = options?.origin?.y ?? viewportHeight / 2
  const clipPath = getThemeTransitionClipPaths(
    x,
    y,
    viewportWidth,
    viewportHeight
  )
  const root = document.documentElement
  const duration = 420

  root.dataset.themeTransition = 'active'
  root.style.setProperty('--theme-transition-duration', `${duration}ms`)
  root.style.setProperty('--theme-transition-clip-from', clipPath[0])

  const cleanup = () => {
    delete root.dataset.themeTransition
    root.style.removeProperty('--theme-transition-duration')
    root.style.removeProperty('--theme-transition-clip-from')
  }

  const transition = documentWithViewTransition.startViewTransition(() => {
    flushSync(updateTheme)
  })

  transition.finished.finally(cleanup)
  transition.ready.then(() => {
    root.animate(
      { clipPath },
      {
        duration,
        easing: 'ease-in-out',
        fill: 'forwards',
        pseudoElement: '::view-transition-new(root)',
      }
    )
  })
}

export function ThemeProvider({
  children,
  defaultTheme = DEFAULT_THEME,
  storageKey = THEME_COOKIE_NAME,
  ...props
}: ThemeProviderProps) {
  const [theme, _setTheme] = useState<Theme>(
    () => (getCookie(storageKey) as Theme) || defaultTheme
  )

  // Optimized: Memoize the resolved theme calculation to prevent unnecessary re-computations
  const resolvedTheme = useMemo((): ResolvedTheme => {
    return getResolvedTheme(theme)
  }, [theme])

  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')

    const handleChange = () => {
      if (theme === 'system') {
        const systemTheme = mediaQuery.matches ? 'dark' : 'light'
        applyResolvedTheme(systemTheme)
      }
    }

    applyResolvedTheme(resolvedTheme)

    mediaQuery.addEventListener('change', handleChange)

    return () => mediaQuery.removeEventListener('change', handleChange)
  }, [theme, resolvedTheme])

  const setTheme = (nextTheme: Theme, options?: ThemeTransitionOptions) => {
    const nextResolvedTheme = getResolvedTheme(nextTheme)
    const updateTheme = () => {
      setCookie(storageKey, nextTheme, THEME_COOKIE_MAX_AGE)
      applyResolvedTheme(nextResolvedTheme)
      _setTheme(nextTheme)
    }

    if (nextTheme === theme && nextResolvedTheme === resolvedTheme) {
      updateTheme()
      return
    }

    runThemeTransition(updateTheme, options)
  }

  const resetTheme = () => {
    const nextResolvedTheme = getResolvedTheme(DEFAULT_THEME)
    const updateTheme = () => {
      removeCookie(storageKey)
      applyResolvedTheme(nextResolvedTheme)
      _setTheme(DEFAULT_THEME)
    }

    runThemeTransition(updateTheme)
  }

  const contextValue = {
    defaultTheme,
    resolvedTheme,
    resetTheme,
    theme,
    setTheme,
  }

  return (
    <ThemeContext value={contextValue} {...props}>
      {children}
    </ThemeContext>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export const useTheme = () => {
  const context = useContext(ThemeContext)

  if (!context) throw new Error('useTheme must be used within a ThemeProvider')

  return context
}
