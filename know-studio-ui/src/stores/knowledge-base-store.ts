import { create } from 'zustand'
import type { KnowledgeBaseInfo } from '@/api/knowledge-bases'

const CURRENT_KNOWLEDGE_BASE_ID = 'know-studio.current-knowledge-base-id'

interface KnowledgeBaseState {
  knowledgeBases: KnowledgeBaseInfo[]
  currentKnowledgeBaseId: number | null
  setKnowledgeBases: (knowledgeBases: KnowledgeBaseInfo[]) => void
  setCurrentKnowledgeBaseId: (knowledgeBaseId: number | null) => void
  reset: () => void
}

export const useKnowledgeBaseStore = create<KnowledgeBaseState>()((set) => ({
  knowledgeBases: [],
  currentKnowledgeBaseId: readKnowledgeBaseId(),
  setKnowledgeBases: (knowledgeBases) =>
    set((state) => {
      const currentIsValid = knowledgeBases.some(
        (knowledgeBase) => knowledgeBase.knowledgeBaseId === state.currentKnowledgeBaseId
      )
      const currentKnowledgeBaseId = currentIsValid
        ? state.currentKnowledgeBaseId
        : (knowledgeBases[0]?.knowledgeBaseId ?? null)
      writeKnowledgeBaseId(currentKnowledgeBaseId)
      return { knowledgeBases, currentKnowledgeBaseId }
    }),
  setCurrentKnowledgeBaseId: (knowledgeBaseId) =>
    set((state) => {
      const nextId = state.knowledgeBases.some(
        (knowledgeBase) => knowledgeBase.knowledgeBaseId === knowledgeBaseId
      )
        ? knowledgeBaseId
        : null
      writeKnowledgeBaseId(nextId)
      return { currentKnowledgeBaseId: nextId }
    }),
  reset: () => {
    writeKnowledgeBaseId(null)
    set({ knowledgeBases: [], currentKnowledgeBaseId: null })
  },
}))

export function getCurrentKnowledgeBase() {
  const state = useKnowledgeBaseStore.getState()
  return (
    state.knowledgeBases.find(
      (knowledgeBase) => knowledgeBase.knowledgeBaseId === state.currentKnowledgeBaseId
    ) ?? null
  )
}

function readKnowledgeBaseId() {
  const value = window.localStorage.getItem(CURRENT_KNOWLEDGE_BASE_ID)
  if (!value) return null
  const knowledgeBaseId = Number(value)
  return Number.isSafeInteger(knowledgeBaseId) && knowledgeBaseId > 0 ? knowledgeBaseId : null
}

function writeKnowledgeBaseId(knowledgeBaseId: number | null) {
  if (knowledgeBaseId === null) {
    window.localStorage.removeItem(CURRENT_KNOWLEDGE_BASE_ID)
    return
  }
  window.localStorage.setItem(CURRENT_KNOWLEDGE_BASE_ID, String(knowledgeBaseId))
}
