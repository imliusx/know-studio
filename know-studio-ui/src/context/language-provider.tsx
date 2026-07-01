import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react'
import { setCookie } from '@/lib/cookies'
import { i18n } from '@/lib/i18n'
import {
  DEFAULT_LANGUAGE,
  LANGUAGE_COOKIE_MAX_AGE,
  LANGUAGE_COOKIE_NAME,
  LANGUAGE_OPTIONS,
  getInitialLanguage,
  type Language,
} from '@/lib/i18n/languages'

type LanguageContextType = {
  defaultLanguage: Language
  language: Language
  setLanguage: (language: Language) => void
}

const LanguageContext = createContext<LanguageContextType | null>(null)

export function LanguageProvider({
  children,
}: {
  children: React.ReactNode
}) {
  const [language, setLanguageState] = useState<Language>(getInitialLanguage)

  useEffect(() => {
    void i18n.changeLanguage(language)
    document.documentElement.lang = language
    setCookie(LANGUAGE_COOKIE_NAME, language, LANGUAGE_COOKIE_MAX_AGE)
  }, [language])

  const setLanguage = useCallback((nextLanguage: Language) => {
    setLanguageState(nextLanguage)
  }, [])

  const contextValue = useMemo(
    () => ({
      defaultLanguage: DEFAULT_LANGUAGE,
      language,
      setLanguage,
    }),
    [language, setLanguage]
  )

  return (
    <LanguageContext value={contextValue}>{children}</LanguageContext>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useLanguage() {
  const context = useContext(LanguageContext)
  if (!context) {
    throw new Error('useLanguage must be used within a LanguageProvider')
  }
  return context
}

export { LANGUAGE_OPTIONS }
