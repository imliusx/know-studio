import type { GroupQueryResult, VisibleGroup } from '@/api/groups'

export function formatDateTime(value: string | null | undefined) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'short',
    timeStyle: 'short',
  }).format(new Date(value))
}

export function formatDate(value: string | null | undefined) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    dateStyle: 'medium',
  }).format(new Date(value))
}

export function formatFileSize(size: number | null | undefined) {
  if (!size) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = size
  let unitIndex = 0

  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024
    unitIndex += 1
  }

  return `${value.toFixed(unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`
}

export function mergeGroups(groups: GroupQueryResult | undefined) {
  const byId = new Map<number, VisibleGroup & { relation: 'OWNER' | 'MEMBER' }>()

  for (const group of groups?.ownedGroups ?? []) {
    byId.set(group.groupId, { ...group, relation: 'OWNER' })
  }

  for (const group of groups?.joinedGroups ?? []) {
    if (!byId.has(group.groupId)) {
      byId.set(group.groupId, { ...group, relation: 'MEMBER' })
    }
  }

  return Array.from(byId.values())
}
