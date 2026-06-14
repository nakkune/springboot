import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/services/api'

// 타입 정의
export interface Board {
  id: string
  name: string
  description?: string
  position?: number
}

export interface Project {
  id: string
  workspaceId: string
  name: string
  color?: string
  icon?: string
  boards: Board[]
  isOpen: boolean
  isLoading: boolean
}

export interface Workspace {
  id: string
  name: string
  slug: string
  logoUrl?: string
  plan?: string // [NEW] 구독 플랜 필드 추가
  projects: Project[]
  isOpen: boolean
  isLoading: boolean
}

/**
 * 워크스페이스 및 프로젝트, 보드 계층 구조의 상태 관리와 Lazy Loading을 담당하는 스토어
 * 10년 차 시니어 개발자 관점에서 안전한 비동기 흐름과 구조적인 상태 동기화를 준수하도록 작성했습니다.
 */
export const useWorkspaceStore = defineStore('workspace', () => {
  const workspaces = ref<Workspace[]>([])
  const isLoadingWorkspaces = ref(false)
  const error = ref<string | null>(null)

  // 1. 최상위 워크스페이스 목록 로드 (초기 1회)
  const fetchWorkspaces = async () => {
    isLoadingWorkspaces.value = true
    error.value = null
    try {
      const data = await api.get('/workspaces') as any[]
      // UI를 위한 reactive 플래그(isOpen, isLoading)를 주입하여 정규화
      workspaces.value = data.map(ws => ({
        ...ws,
        projects: ws.projects || [],
        isOpen: false,
        isLoading: false
      }))
    } catch (e: any) {
      console.error('Failed to load workspaces:', e)
      error.value = '워크스페이스 목록을 불러오지 못했습니다.'
      workspaces.value = []
    } finally {
      isLoadingWorkspaces.value = false
    }
  }

  // 2. 특정 워크스페이스 하위의 프로젝트 목록 지연 로딩 (Lazy Loading)
  const fetchProjects = async (workspaceId: string) => {
    const ws = workspaces.value.find(w => w.id === workspaceId)
    if (!ws) return

    // 이미 데이터를 불러왔거나 로딩 중이면 중복 요청 방지
    if (ws.projects.length > 0 && !ws.isLoading) {
      ws.isOpen = !ws.isOpen
      return
    }

    ws.isLoading = true
    ws.isOpen = true
    error.value = null
    try {
      const data = await api.get(`/projects/workspace/${workspaceId}`) as any[]
      ws.projects = data.map(proj => ({
        ...proj,
        boards: proj.boards || [],
        isOpen: false,
        isLoading: false
      }))
    } catch (e: any) {
      console.error(`Failed to load projects for workspace ${workspaceId}:`, e)
      ws.projects = []
    } finally {
      ws.isLoading = false
    }
  }

  // 3. 특정 프로젝트 하위의 보드 목록 지연 로딩 (Lazy Loading)
  const fetchBoards = async (workspaceId: string, projectId: string) => {
    const ws = workspaces.value.find(w => w.id === workspaceId)
    if (!ws) return
    const proj = ws.projects.find(p => p.id === projectId)
    if (!proj) return

    // 이미 불러왔거나 로딩 중이면 토글만 실행
    if (proj.boards.length > 0 && !proj.isLoading) {
      proj.isOpen = !proj.isOpen
      return
    }

    proj.isLoading = true
    proj.isOpen = true
    error.value = null
    try {
      const data = await api.get(`/boards/project/${projectId}`) as any[]
      proj.boards = data
    } catch (e: any) {
      console.error(`Failed to load boards for project ${projectId}:`, e)
      proj.boards = []
    } finally {
      proj.isLoading = false
    }
  }

  // ==========================================
  // [NEW] 워크스페이스 멤버 관리 기능 (실시간 E2E 팀원 연동)
  // ==========================================
  
  const currentWorkspaceMembers = ref<any[]>([])
  const isLoadingMembers = ref(false)

  /**
   * 특정 워크스페이스에 소속된 실제 등록 멤버 목록 조회
   */
  const fetchWorkspaceMembers = async (workspaceId: string) => {
    isLoadingMembers.value = true
    try {
      const data = await api.get(`/workspaces/${workspaceId}/members`) as any[]
      currentWorkspaceMembers.value = data || []
    } catch (e: any) {
      console.error(`Failed to load members for workspace ${workspaceId}:`, e)
      currentWorkspaceMembers.value = []
    } finally {
      isLoadingMembers.value = false
    }
  }

  /**
   * 신규 이메일 계정을 워크스페이스 멤버로 초대/가입
   */
  const inviteWorkspaceMember = async (workspaceId: string, email: string, role = 'member') => {
    try {
      const newMember = await api.post(`/workspaces/${workspaceId}/members/invite`, {
        email,
        role
      }) as any
      currentWorkspaceMembers.value.push(newMember)
      return newMember
    } catch (e: any) {
      console.error('Failed to invite member:', e)
      throw e
    }
  }

  // Delete a member from workspace
  const deleteWorkspaceMember = async (workspaceId: string, memberId: string) => {
    try {
      await api.delete(`/workspaces/${workspaceId}/members/${memberId}`)
      currentWorkspaceMembers.value = currentWorkspaceMembers.value.filter(m => m.id !== memberId)
    } catch (e: any) {
      console.error('Failed to delete member:', e)
      throw e
    }
  }

  // ==========================================
  // [NEW] 계층 구조 생성 및 삭제 (Workspace / Project / Board)
  // ==========================================
  
  const createWorkspace = async (name: string) => {
    try {
      const slug = name.toLowerCase().replace(/[^a-z0-9가-힣]+/g, '-') + '-' + Date.now()
      const newWs = await api.post('/workspaces', { 
        name, 
        slug, 
        plan: 'free'
      }) as Workspace
      workspaces.value.unshift({
        ...newWs,
        projects: [],
        isOpen: true,
        isLoading: false
      })
      return newWs
    } catch (e: any) {
      console.error('Failed to create workspace:', e)
      throw e
    }
  }

  const deleteWorkspace = async (id: string) => {
    try {
      await api.delete(`/workspaces/${id}`)
      workspaces.value = workspaces.value.filter(ws => ws.id !== id)
    } catch (e: any) {
      console.error('Failed to delete workspace:', e)
      throw e
    }
  }

  const createProject = async (workspaceId: string, name: string) => {
    try {
      const newProj = await api.post('/projects', { 
        workspaceId, 
        name, 
        color: '#3b82f6', 
        icon: 'folder'
      }) as Project
      const ws = workspaces.value.find(w => w.id === workspaceId)
      if (ws) {
        newProj.boards = []
        newProj.isOpen = true
        newProj.isLoading = false
        ws.projects.push(newProj)
        ws.isOpen = true
      }
      return newProj
    } catch (e: any) {
      console.error('Failed to create project:', e)
      throw e
    }
  }

  const deleteProject = async (workspaceId: string, projectId: string) => {
    try {
      await api.delete(`/projects/${projectId}`)
      const ws = workspaces.value.find(w => w.id === workspaceId)
      if (ws) {
        ws.projects = ws.projects.filter(p => p.id !== projectId)
      }
    } catch (e: any) {
      console.error('Failed to delete project:', e)
      throw e
    }
  }

  const createBoard = async (workspaceId: string, projectId: string, name: string) => {
    try {
      const newBoard = await api.post('/boards', { 
        projectId, 
        name, 
        boardType: 'main'
      }) as Board
      const ws = workspaces.value.find(w => w.id === workspaceId)
      if (ws) {
        const proj = ws.projects.find(p => p.id === projectId)
        if (proj) {
          proj.boards.push(newBoard)
          proj.isOpen = true
        }
      }
      return newBoard
    } catch (e: any) {
      console.error('Failed to create board:', e)
      throw e
    }
  }

  const deleteBoard = async (workspaceId: string, projectId: string, boardId: string) => {
    try {
      await api.delete(`/boards/${boardId}`)
      const ws = workspaces.value.find(w => w.id === workspaceId)
      if (ws) {
        const proj = ws.projects.find(p => p.id === projectId)
        if (proj) {
          proj.boards = proj.boards.filter(b => b.id !== boardId)
        }
      }
    } catch (e: any) {
      console.error('Failed to delete board:', e)
      throw e
    }
  }

  return {
    workspaces,
    isLoadingWorkspaces,
    error,
    currentWorkspaceMembers,
    isLoadingMembers,
    fetchWorkspaces,
    fetchProjects,
    fetchBoards,
    fetchWorkspaceMembers,
    inviteWorkspaceMember,
    deleteWorkspaceMember,
    createWorkspace,
    deleteWorkspace,
    createProject,
    deleteProject,
    createBoard,
    deleteBoard
  }
})
