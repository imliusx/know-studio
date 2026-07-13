export type ColorTheme =
  | 'default'
  | 'violet'
  | 'yellow'
  | 'sky'
  | 'blue'
  | 'red'
  | 'lime'
  | 'emerald'
  | 'pink'

export type ColorThemeOption = {
  value: ColorTheme
  label: string
  swatchClassName: string
  optionClassName: string
}

export const COLOR_THEME_COOKIE_NAME = 'color_theme'
export const COLOR_THEME_COOKIE_MAX_AGE = 60 * 60 * 24 * 365
export const DEFAULT_COLOR_THEME: ColorTheme = 'default'

export const COLOR_THEME_OPTIONS: ColorThemeOption[] = [
  {
    value: 'default',
    label: 'Default',
    swatchClassName: 'bg-(--theme-swatch-default)',
    optionClassName:
      'bg-(--theme-swatch-default)/12 hover:bg-(--theme-swatch-default)/18 group-data-[state=checked]:bg-(--theme-swatch-default)/24',
  },
  {
    value: 'violet',
    label: 'Violet',
    swatchClassName: 'bg-(--theme-swatch-violet)',
    optionClassName:
      'bg-(--theme-swatch-violet)/14 hover:bg-(--theme-swatch-violet)/20 group-data-[state=checked]:bg-(--theme-swatch-violet)/28',
  },
  {
    value: 'yellow',
    label: 'Yellow',
    swatchClassName: 'bg-(--theme-swatch-yellow)',
    optionClassName:
      'bg-(--theme-swatch-yellow)/18 hover:bg-(--theme-swatch-yellow)/24 group-data-[state=checked]:bg-(--theme-swatch-yellow)/32',
  },
  {
    value: 'sky',
    label: 'Sky',
    swatchClassName: 'bg-(--theme-swatch-sky)',
    optionClassName:
      'bg-(--theme-swatch-sky)/16 hover:bg-(--theme-swatch-sky)/22 group-data-[state=checked]:bg-(--theme-swatch-sky)/30',
  },
  {
    value: 'blue',
    label: 'Blue',
    swatchClassName: 'bg-(--theme-swatch-blue)',
    optionClassName:
      'bg-(--theme-swatch-blue)/14 hover:bg-(--theme-swatch-blue)/20 group-data-[state=checked]:bg-(--theme-swatch-blue)/28',
  },
  {
    value: 'red',
    label: 'Red',
    swatchClassName: 'bg-(--theme-swatch-red)',
    optionClassName:
      'bg-(--theme-swatch-red)/14 hover:bg-(--theme-swatch-red)/20 group-data-[state=checked]:bg-(--theme-swatch-red)/28',
  },
  {
    value: 'lime',
    label: 'Lime',
    swatchClassName: 'bg-(--theme-swatch-lime)',
    optionClassName:
      'bg-(--theme-swatch-lime)/18 hover:bg-(--theme-swatch-lime)/24 group-data-[state=checked]:bg-(--theme-swatch-lime)/32',
  },
  {
    value: 'emerald',
    label: 'Emerald',
    swatchClassName: 'bg-(--theme-swatch-emerald)',
    optionClassName:
      'bg-(--theme-swatch-emerald)/16 hover:bg-(--theme-swatch-emerald)/22 group-data-[state=checked]:bg-(--theme-swatch-emerald)/30',
  },
  {
    value: 'pink',
    label: 'Pink',
    swatchClassName: 'bg-(--theme-swatch-pink)',
    optionClassName:
      'bg-(--theme-swatch-pink)/16 hover:bg-(--theme-swatch-pink)/22 group-data-[state=checked]:bg-(--theme-swatch-pink)/30',
  },
]

export const COLOR_THEME_VALUES = new Set<ColorTheme>(
  COLOR_THEME_OPTIONS.map((option) => option.value)
)

export function isColorTheme(value: string): value is ColorTheme {
  return COLOR_THEME_VALUES.has(value as ColorTheme)
}
