import { getCookie } from '@/lib/cookies'

export type Language = 'zh-CN' | 'en'

export type LanguageOption = {
  value: Language
  labelKey: string
  shortLabel: string
}

export const LANGUAGE_COOKIE_NAME = 'language'
export const LANGUAGE_COOKIE_MAX_AGE = 60 * 60 * 24 * 365
export const DEFAULT_LANGUAGE: Language = 'zh-CN'

export const LANGUAGE_OPTIONS: LanguageOption[] = [
  {
    value: 'zh-CN',
    labelKey: 'language.chinese',
    shortLabel: '中',
  },
  {
    value: 'en',
    labelKey: 'language.english',
    shortLabel: 'EN',
  },
]

export const LANGUAGE_VALUES = new Set<Language>(
  LANGUAGE_OPTIONS.map((option) => option.value)
)

export function isLanguage(value: string): value is Language {
  return LANGUAGE_VALUES.has(value as Language)
}

function getBrowserLanguage(): Language | undefined {
  if (typeof navigator === 'undefined') return undefined

  const languages = navigator.languages?.length
    ? navigator.languages
    : [navigator.language]

  for (const language of languages) {
    if (language.toLowerCase().startsWith('zh')) return 'zh-CN'
    if (language.toLowerCase().startsWith('en')) return 'en'
  }

  return undefined
}

export function getInitialLanguage(): Language {
  const saved = getCookie(LANGUAGE_COOKIE_NAME)
  if (saved && isLanguage(saved)) return saved

  return getBrowserLanguage() ?? DEFAULT_LANGUAGE
}
