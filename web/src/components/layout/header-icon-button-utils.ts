import { cn } from '@/lib/utils'

export type HeaderIconScale = 'default' | 'sm' | 'lg'

const iconScaleClass: Record<HeaderIconScale, string> = {
  default: 'size-[1.2rem]',
  sm: 'size-[1.125rem]',
  lg: 'size-[1.25rem]',
}

export function headerIconButtonClass(className?: string) {
  return cn('relative', className)
}

export function headerIconClass(iconScale: HeaderIconScale = 'default') {
  return iconScaleClass[iconScale]
}
