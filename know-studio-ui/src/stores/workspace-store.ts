import { create } from 'zustand'
import type { WorkspaceInfo } from '@/api/workspaces'

const CURRENT_WORKSPACE_ID = 'know-studio.current-workspace-id'

interface WorkspaceState {
  workspaces: WorkspaceInfo[]
  currentWorkspaceId: number | null
  setWorkspaces: (workspaces: WorkspaceInfo[]) => void
  setCurrentWorkspaceId: (workspaceId: number | null) => void
  reset: () => void
}

export const useWorkspaceStore = create<WorkspaceState>()((set) => ({
  workspaces: [],
  currentWorkspaceId: readWorkspaceId(),
  setWorkspaces: (workspaces) =>
    set((state) => {
      const currentIsValid = workspaces.some(
        (workspace) => workspace.workspaceId === state.currentWorkspaceId
      )
      const currentWorkspaceId = currentIsValid
        ? state.currentWorkspaceId
        : (workspaces[0]?.workspaceId ?? null)
      writeWorkspaceId(currentWorkspaceId)
      return { workspaces, currentWorkspaceId }
    }),
  setCurrentWorkspaceId: (workspaceId) =>
    set((state) => {
      const nextId = state.workspaces.some(
        (workspace) => workspace.workspaceId === workspaceId
      )
        ? workspaceId
        : null
      writeWorkspaceId(nextId)
      return { currentWorkspaceId: nextId }
    }),
  reset: () => {
    writeWorkspaceId(null)
    set({ workspaces: [], currentWorkspaceId: null })
  },
}))

export function getCurrentWorkspace() {
  const state = useWorkspaceStore.getState()
  return (
    state.workspaces.find(
      (workspace) => workspace.workspaceId === state.currentWorkspaceId
    ) ?? null
  )
}

function readWorkspaceId() {
  const value = window.localStorage.getItem(CURRENT_WORKSPACE_ID)
  if (!value) return null
  const workspaceId = Number(value)
  return Number.isSafeInteger(workspaceId) && workspaceId > 0 ? workspaceId : null
}

function writeWorkspaceId(workspaceId: number | null) {
  if (workspaceId === null) {
    window.localStorage.removeItem(CURRENT_WORKSPACE_ID)
    return
  }
  window.localStorage.setItem(CURRENT_WORKSPACE_ID, String(workspaceId))
}
