export type EntityId = string

export function asEntityId(value: unknown): EntityId | null {
  if (typeof value === 'string' && /^\d+$/.test(value)) return value
  if (typeof value === 'number' && Number.isSafeInteger(value) && value > 0) {
    return String(value)
  }
  return null
}
