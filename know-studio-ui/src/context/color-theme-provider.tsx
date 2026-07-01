import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import { getCookie, removeCookie, setCookie } from '@/lib/cookies'
import {
  COLOR_THEME_COOKIE_MAX_AGE,
  COLOR_THEME_COOKIE_NAME,
  DEFAULT_COLOR_THEME,
  isColorTheme,
  type ColorTheme,
} from '@/lib/theme/color-theme'

type ColorThemeContextType = {
  colorTheme: ColorTheme
  defaultColorTheme: ColorTheme
  setColorTheme: (theme: ColorTheme) => void
  resetColorTheme: () => void
}

const ColorThemeContext = createContext<ColorThemeContextType | null>(null)

function getInitialColorTheme(): ColorTheme {
  const saved = getCookie(COLOR_THEME_COOKIE_NAME)
  return saved && isColorTheme(saved) ? saved : DEFAULT_COLOR_THEME
}

function applyColorTheme(theme: ColorTheme) {
  document.documentElement.dataset.palette = theme
}

export function ColorThemeProvider({
  children,
}: {
  children: React.ReactNode
}) {
  const [colorTheme, setColorThemeState] =
    useState<ColorTheme>(getInitialColorTheme)

  useEffect(() => {
    applyColorTheme(colorTheme)
  }, [colorTheme])

  const setColorTheme = useCallback((theme: ColorTheme) => {
    setColorThemeState(theme)
    setCookie(COLOR_THEME_COOKIE_NAME, theme, COLOR_THEME_COOKIE_MAX_AGE)
  }, [])

  const resetColorTheme = useCallback(() => {
    setColorThemeState(DEFAULT_COLOR_THEME)
    removeCookie(COLOR_THEME_COOKIE_NAME)
  }, [])

  const contextValue = useMemo(
    () => ({
      colorTheme,
      defaultColorTheme: DEFAULT_COLOR_THEME,
      resetColorTheme,
      setColorTheme,
    }),
    [colorTheme, resetColorTheme, setColorTheme]
  )

  return (
    <ColorThemeContext value={contextValue}>{children}</ColorThemeContext>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useColorTheme() {
  const context = useContext(ColorThemeContext)
  if (!context) {
    throw new Error('useColorTheme must be used within a ColorThemeProvider')
  }
  return context
}
