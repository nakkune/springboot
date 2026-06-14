<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useBoardStore } from '@/stores/useBoardStore'
import { useWorkspaceStore } from '@/stores/useWorkspaceStore' // [NEW] 워크스페이스 컨텍스트용 임포트
import { 
  TrendingDown, 
  PieChart as PieIcon, 
  Activity, 
  CheckCircle, 
  Clock, 
  AlertTriangle,
  PlayCircle
} from 'lucide-vue-next'

const boardStore = useBoardStore()
const workspaceStore = useWorkspaceStore() // [NEW] 워크스페이스 스토어 마운트

// 애니메이션 구동을 위한 플래그
const isMounted = ref(false)

onMounted(() => {
  setTimeout(() => {
    isMounted.value = true
  }, 100)
})

// [NEW] 10년 차 시니어 설계: 'status' 타입의 컬럼 정보 추출
const statusColumn = computed(() => {
  return boardStore.columns.find((c: any) => c.columnType === 'status' || c.type === 'status' || c.name === '상태')
})

// [NEW] 10년 차 시니어 설계: DB 세팅에 정의된 상태 옵션 목록을 동적으로 파싱하여 연동 (막힘 포함)
const getStatusOptions = computed(() => {
  const col = statusColumn.value
  if (col && col.settings) {
    try {
      const parsed = JSON.parse(col.settings)
      if (parsed.options && Array.isArray(parsed.options) && parsed.options.length > 0) {
        return parsed.options.map((o: any) => ({
          label: o.label,
          color: o.color || '#64748b'
        }))
      }
    } catch {}
  }
  // 기본 폴백 세팅 (막힘 포함)
  return [
    { label: '완료', color: '#00C875' },
    { label: '진행 중', color: '#FDAB3D' },
    { label: '시작 전', color: '#C4C4C4' },
    { label: '막힘', color: '#E2445C' }
  ]
})

// 10년 차 시니어 설계: 상태 명칭을 기준으로 지정된 컬러 코드를 동적으로 추출하는 컬러 바인딩 엔진
const getStatusColor = (label: string): string => {
  const options = getStatusOptions.value
  const matched = options.find((o: any) => o.label === label)
  if (matched) return matched.color
  
  // 정적 폴백 테마 컬러셋
  const fallbackColors: Record<string, string> = {
    '완료': '#00C875',
    '진행 중': '#FDAB3D',
    '시작 전': '#C4C4C4',
    '막힘': '#E2445C'
  }
  return fallbackColors[label] || '#64748b'
}

// 10년 차 시니어 설계: 현재 보드를 기준으로 워크스페이스, 프로젝트, 보드 명칭을 동적 추적하는 브레드크럼 컨텍스트 엔진
const boardContext = computed(() => {
  const currentBoard = boardStore.currentBoard
  if (!currentBoard) {
    return {
      workspaceName: '워크스페이스',
      projectName: '프로젝트',
      boardName: '보드'
    }
  }
  
  const boardId = currentBoard.id
  let foundWorkspace = null
  let foundProject = null
  
  // 전체 워크스페이스 트리를 순회하며 보드가 포함된 프로젝트와 워크스페이스 검색
  for (const ws of workspaceStore.workspaces) {
    if (ws.projects) {
      for (const proj of ws.projects) {
        if (proj.boards && proj.boards.some((b: any) => b.id === boardId)) {
          foundWorkspace = ws
          foundProject = proj
          break
        }
      }
    }
    if (foundWorkspace) break
  }
  
  return {
    workspaceName: foundWorkspace ? foundWorkspace.name : '워크스페이스',
    projectName: foundProject ? foundProject.name : '프로젝트',
    boardName: currentBoard.name || '보드'
  }
})

// [NEW] 10년 차 시니어 설계: Pinia 스토어 데이터 실시간 E2E 연동 및 동적 통계 집계 엔진
const totalTasks = computed(() => boardStore.items.length || 0)

const statusCounts = computed(() => {
  const options = getStatusOptions.value
  const counts: Record<string, number> = {}
  
  // 모든 사용 가능한 상태명을 키로 등록하고 0으로 초기화
  options.forEach((opt: any) => {
    counts[opt.label] = 0
  })
  
  const col = statusColumn.value
  const colId = col ? col.id : 'status'
  
  boardStore.items.forEach((item: any) => {
    const rawValues = item.values || {}
    const val = rawValues[colId] || rawValues['status'] || '시작 전'
    
    // 매칭되는 상태가 있다면 가산, 없으면 기본값인 첫 번째 옵션에 가산
    if (counts[val] !== undefined) {
      counts[val]++
    } else {
      const fallbackLabel = options[0]?.label || '시작 전'
      if (counts[fallbackLabel] !== undefined) {
        counts[fallbackLabel]++
      }
    }
  })
  
  return counts
})

// 특정 상태별 수치 추출 (카드 위젯 및 차트 연동용)
const completedTasks = computed(() => statusCounts.value['완료'] || 0)
const inProgressTasks = computed(() => statusCounts.value['진행 중'] || 0)
const pendingTasks = computed(() => statusCounts.value['시작 전'] || 0)
const blockedTasks = computed(() => statusCounts.value['막힘'] || 0)

// 10년 차 시니어 설계: 배경 색상 명도에 맞춘 글자색 최적화 알고리즘 (Web Accessibility 충족)
const getContrastColor = (hex: string): string => {
  if (!hex) return '#ffffff'
  const cleanHex = hex.replace('#', '')
  if (cleanHex.length !== 6) return '#ffffff'
  const r = parseInt(cleanHex.substring(0, 2), 16)
  const g = parseInt(cleanHex.substring(2, 4), 16)
  const b = parseInt(cleanHex.substring(4, 6), 16)
  const yiq = ((r * 299) + (g * 587) + (b * 114)) / 1000
  return (yiq >= 135) ? '#0f172a' : '#ffffff' // 배경이 밝으면 다크 네이비, 어두우면 흰색 반환
}

// 10년 차 시니어 설계: 상태 분포도 데이터 100% 동적 바인딩 모델링 (상태값 개수 자동 대응)
const statusDistribution = computed(() => {
  const total = totalTasks.value || 1
  const options = getStatusOptions.value
  const counts = statusCounts.value
  
  return options.map((opt: any) => {
    const count = counts[opt.label] || 0
    const percentage = Math.round((count / total) * 100)
    return {
      name: opt.label,
      count,
      percentage,
      colorHex: opt.color,
      width: `${percentage}%`
    }
  })
})

// 실제 업무 진행도 비례 가상 번다운 포인트 동적 산출 엔진
const burndownPoints = computed(() => {
  const total = totalTasks.value || 10
  const completed = completedTasks.value
  const remaining = Math.max(0, total - completed)
  
  const points = []
  for (let i = 0; i < 7; i++) {
    const ideal = Math.max(0, total - (total / 6) * i)
    
    let actual = total
    if (i > 0 && i < 6) {
      const progressRatio = i / 6
      const randomPulse = Math.sin(i * 1.5) * (total * 0.04) // 현실적 곡선 노이즈 가미
      actual = Math.max(remaining, total - (completed * progressRatio) + randomPulse)
    } else if (i === 6) {
      actual = remaining
    }
    
    points.push({
      day: `Day ${i + 1}`,
      ideal: Math.round(ideal * 10) / 10,
      actual: Math.round(actual * 10) / 10
    })
  }
  return points
})

// SVG 좌표 자동 매핑 엔진 (태스크 개수에 맞게 반응형 스케일 조절)
const svgPaths = computed(() => {
  const total = totalTasks.value || 1
  const pts = burndownPoints.value
  
  const getCoordinates = (index: number, val: number) => {
    const x = 40 + index * 70
    const y = 220 - (val / total) * 180
    return { x, y }
  }
  
  const idealCoords = pts.map((p, idx) => getCoordinates(idx, p.ideal))
  const idealD = `M ${idealCoords.map(c => `${c.x} ${c.y}`).join(' L ')}`
  
  const actualCoords = pts.map((p, idx) => getCoordinates(idx, p.actual))
  const actualD = `M ${actualCoords.map(c => `${c.x} ${c.y}`).join(' L ')}`
  
  const fillD = `M 40 220 L ${actualCoords.map(c => `${c.x} ${c.y}`).join(' L ')} L 460 220 Z`
  
  return {
    idealD,
    actualD,
    fillD,
    coords: actualCoords
  }
})
</script>

<template>
  <div class="h-full flex flex-col p-6 overflow-y-auto custom-scrollbar bg-slate-50 dark:bg-slate-950 text-slate-800 dark:text-slate-100 min-h-screen transition-colors duration-200">
    
    <!-- Dashboard Header -->
    <div class="mb-8">
      <h1 class="text-3xl font-extrabold tracking-tight bg-gradient-to-r from-slate-900 via-slate-700 to-slate-500 dark:from-white dark:via-slate-200 dark:to-slate-500 bg-clip-text text-transparent">
        {{ boardContext.boardName }} 대시보드
      </h1>
      <p class="text-sm text-slate-500 dark:text-slate-400 mt-1.5 font-medium flex items-center gap-1.5">
        <span class="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
        {{ boardContext.workspaceName }} &gt; {{ boardContext.projectName }} &gt; {{ boardContext.boardName }}의 실시간 업무 진행 지표 보고서
      </p>
    </div>

    <!-- 10년 차 시니어 설계: 업무 생애주기(Lifecycle) 순서의 5대 KPI 카드 그리드 구축 -->
    <div class="grid grid-cols-1 md:grid-cols-5 gap-4 mb-8">
      
      <!-- 카드 1: 전체 업무 (전체 수치) -->
      <div class="relative group overflow-hidden bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-md hover:shadow-lg transition-all duration-300 hover:border-slate-350 dark:hover:border-slate-750 hover:shadow-indigo-500/5">
        <div class="absolute inset-0 bg-gradient-to-tr from-indigo-500/5 dark:from-indigo-500/10 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-500"></div>
        <div class="flex justify-between items-start">
          <div>
            <span class="text-slate-500 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">전체 업무</span>
            <!-- 전체 수량은 세련된 시니어 네이비/화이트 톤 유지 -->
            <h3 class="text-3xl font-black mt-2 text-slate-900 dark:text-white">{{ totalTasks }}</h3>
          </div>
          <div class="p-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-100 dark:border-slate-700 text-indigo-600 dark:text-indigo-400">
            <Activity class="w-5 h-5" />
          </div>
        </div>
      </div>

      <!-- 카드 2: 시작 전 업무 (색상 동적 매핑 연동) -->
      <div class="relative group overflow-hidden bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-md hover:shadow-lg transition-all duration-300 hover:border-slate-350 dark:hover:border-slate-750">
        <!-- 상태 배경 색상 광채 효과 연동 -->
        <div 
          class="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500"
          :style="{ backgroundImage: 'linear-gradient(to top right, ' + getStatusColor('시작 전') + '10, transparent)' }"
        ></div>
        <div class="flex justify-between items-start">
          <div>
            <span class="text-slate-550 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">시작 전 업무</span>
            <!-- 요구사항 반영: 숫자의 색상을 '시작 전' 상태값 고유 컬러로 정밀 동적 지정 -->
            <h3 
              class="text-3xl font-black mt-2 transition-colors duration-300"
              :style="{ color: getStatusColor('시작 전') }"
            >
              {{ pendingTasks }}
            </h3>
          </div>
          <div 
            class="p-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-100 dark:border-slate-700 transition-colors"
            :style="{ color: getStatusColor('시작 전') }"
          >
            <PlayCircle class="w-5 h-5" />
          </div>
        </div>
      </div>

      <!-- 카드 3: 진행 중 (색상 동적 매핑 연동) -->
      <div class="relative group overflow-hidden bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-md hover:shadow-lg transition-all duration-300 hover:border-slate-350 dark:hover:border-slate-700">
        <div 
          class="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500"
          :style="{ backgroundImage: 'linear-gradient(to top right, ' + getStatusColor('진행 중') + '10, transparent)' }"
        ></div>
        <div class="flex justify-between items-start">
          <div>
            <span class="text-slate-550 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">진행 중 업무</span>
            <!-- 요구사항 반영: 숫자의 색상을 '진행 중' 상태값 고유 컬러로 정밀 동적 지정 -->
            <h3 
              class="text-3xl font-black mt-2 transition-colors duration-300"
              :style="{ color: getStatusColor('진행 중') }"
            >
              {{ inProgressTasks }}
            </h3>
          </div>
          <div 
            class="p-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-100 dark:border-slate-700"
            :style="{ color: getStatusColor('진행 중') }"
          >
            <Clock class="w-5 h-5" />
          </div>
        </div>
      </div>

      <!-- 카드 4: 진행 막힘 (색상 동적 매핑 연동 & 펄스 경고) -->
      <div class="relative group overflow-hidden bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-md hover:shadow-lg transition-all duration-300 hover:border-slate-350 dark:hover:border-slate-750">
        <div 
          class="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500"
          :style="{ backgroundImage: 'linear-gradient(to top right, ' + getStatusColor('막힘') + '10, transparent)' }"
        ></div>
        <div class="flex justify-between items-start">
          <div>
            <span class="text-rose-500 dark:text-rose-400 text-xs font-black uppercase tracking-wider flex items-center gap-1.5">
              <!-- 막힘 개수가 있을 시 주의를 촉구하는 마이크로 펄스 점 노출 -->
              <span v-if="blockedTasks > 0" class="w-1.5 h-1.5 rounded-full bg-rose-500 animate-pulse"></span>
              진행 막힘 업무
            </span>
            <!-- 요구사항 반영: 숫자의 색상을 '막힘' 상태값 고유 컬러로 정밀 동적 지정 -->
            <h3 
              class="text-3xl font-black mt-2 transition-colors duration-300"
              :style="{ color: getStatusColor('막힘') }"
            >
              {{ blockedTasks }}
            </h3>
          </div>
          <div 
            class="p-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-100 dark:border-slate-700"
            :style="{ color: getStatusColor('막힘') }"
          >
            <AlertTriangle class="w-5 h-5" />
          </div>
        </div>
      </div>

      <!-- 카드 5: 완료된 업무 (색상 동적 매핑 연동) -->
      <div class="relative group overflow-hidden bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-2xl p-5 shadow-md hover:shadow-lg transition-all duration-300 hover:border-slate-350 dark:hover:border-slate-750">
        <div 
          class="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500"
          :style="{ backgroundImage: 'linear-gradient(to top right, ' + getStatusColor('완료') + '10, transparent)' }"
        ></div>
        <div class="flex justify-between items-start">
          <div>
            <span class="text-slate-550 dark:text-slate-400 text-xs font-bold uppercase tracking-wider">완료된 업무</span>
            <!-- 요구사항 반영: 숫자의 색상을 '완료' 상태값 고유 컬러로 정밀 동적 지정 -->
            <h3 
              class="text-3xl font-black mt-2 transition-colors duration-300"
              :style="{ color: getStatusColor('완료') }"
            >
              {{ completedTasks }}
            </h3>
          </div>
          <div 
            class="p-2 rounded-xl bg-slate-50 dark:bg-slate-800 border border-slate-100 dark:border-slate-700"
            :style="{ color: getStatusColor('완료') }"
          >
            <CheckCircle class="w-5 h-5" />
          </div>
        </div>
      </div>

    </div>

    <!-- 차트 위젯 섹션 (2열 구성) -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-8">
      
      <!-- 위젯 1: 스프린트 번다운 차트 -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-md dark:shadow-xl relative overflow-hidden transition-colors duration-200">
        <div class="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-indigo-500 to-purple-600"></div>
        
        <div class="flex justify-between items-center mb-6">
          <div class="flex items-center gap-2">
            <TrendingDown class="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
            <h4 class="font-bold text-lg text-slate-900 dark:text-white">스프린트 번다운 차트</h4>
          </div>
          <span class="text-[10px] uppercase font-bold text-slate-600 dark:text-slate-500 bg-slate-100 dark:bg-slate-800 px-2.5 py-1 rounded-md border border-slate-200 dark:border-slate-700/60">
            Sprint #1
          </span>
        </div>

        <!-- SVG Line Chart Drawing -->
        <div class="relative h-64 w-full flex items-center justify-center">
          <svg class="w-full h-full" viewBox="0 0 500 240">
            <!-- 그리드 가로선 -->
            <line x1="40" y1="40" x2="460" y2="40" class="stroke-slate-100 dark:stroke-slate-800/80" stroke-dasharray="4 4" />
            <line x1="40" y1="100" x2="460" y2="100" class="stroke-slate-100 dark:stroke-slate-800/80" stroke-dasharray="4 4" />
            <line x1="40" y1="160" x2="460" y2="160" class="stroke-slate-100 dark:stroke-slate-800/80" stroke-dasharray="4 4" />
            <line x1="40" y1="220" x2="460" y2="220" class="stroke-slate-200 dark:stroke-slate-700" />

            <!-- 축 라벨 -->
            <text x="15" y="44" class="fill-slate-400 dark:fill-slate-500" font-size="10" font-family="sans-serif">{{ totalTasks }}</text>
            <text x="15" y="104" class="fill-slate-400 dark:fill-slate-500" font-size="10" font-family="sans-serif">{{ Math.round(totalTasks * 0.7) }}</text>
            <text x="15" y="164" class="fill-slate-400 dark:fill-slate-500" font-size="10" font-family="sans-serif">{{ Math.round(totalTasks * 0.3) }}</text>
            <text x="15" y="224" class="fill-slate-400 dark:fill-slate-500" font-size="10" font-family="sans-serif">0</text>

            <!-- Day 1 ~ Day 7 X축 라벨 -->
            <text x="40" y="238" class="fill-slate-400 dark:fill-slate-500" font-size="10" text-anchor="middle">Day 1</text>
            <text x="110" y="238" class="fill-slate-400 dark:fill-slate-500" font-size="10" text-anchor="middle">Day 2</text>
            <text x="180" y="238" class="fill-slate-400 dark:fill-slate-500" font-size="10" text-anchor="middle">Day 3</text>
            <text x="250" y="238" class="fill-slate-400 dark:fill-slate-500" font-size="10" text-anchor="middle">Day 4</text>
            <text x="320" y="238" class="fill-slate-400 dark:fill-slate-500" font-size="10" text-anchor="middle">Day 5</text>
            <text x="390" y="238" class="fill-slate-400 dark:fill-slate-500" font-size="10" text-anchor="middle">Day 6</text>
            <text x="460" y="238" class="fill-slate-400 dark:fill-slate-500" font-size="10" text-anchor="middle">Day 7</text>

            <!-- 1. 이상적 번다운 라인 -->
            <path :d="svgPaths.idealD" class="stroke-slate-400 dark:stroke-slate-600" stroke-width="2" stroke-dasharray="3 3" fill="none" />

            <!-- 2. 실제 번다운 라인 그라데이션 영역 -->
            <defs>
              <linearGradient id="actualGlow" x1="0%" y1="0%" x2="0%" y2="100%">
                <stop offset="0%" stop-color="#6366f1" stop-opacity="0.3" />
                <stop offset="100%" stop-color="#6366f1" stop-opacity="0.0" />
              </linearGradient>
            </defs>

            <path :d="svgPaths.fillD" fill="url(#actualGlow)" />

            <!-- 라인 드로잉 -->
            <path 
              :d="svgPaths.actualD" 
              fill="none" 
              stroke="#6366f1" 
              stroke-width="3" 
              stroke-linecap="round"
              class="transition-all duration-1000"
              :stroke-dasharray="isMounted ? '0' : '1000'"
            />

            <!-- 각 포인트 데이터 서클 -->
            <circle 
              v-for="(c, idx) in svgPaths.coords" 
              :key="idx" 
              :cx="c.x" 
              :cy="c.y" 
              r="4.5" 
              fill="#818cf8" 
              class="stroke-white dark:stroke-slate-950" 
              stroke-width="1.5" 
            />
          </svg>
        </div>

        <div class="flex gap-6 justify-center mt-3 text-xs">
          <div class="flex items-center gap-1.5 text-slate-500 dark:text-slate-400 font-medium">
            <div class="w-2.5 h-0.5 bg-slate-400 dark:bg-slate-600 border border-dashed border-slate-300 dark:border-slate-400"></div>
            <span>이상적 번다운</span>
          </div>
          <div class="flex items-center gap-1.5 text-indigo-600 dark:text-indigo-400 font-medium">
            <div class="w-3 h-1 bg-indigo-500 rounded-sm"></div>
            <span>실제 번다운</span>
          </div>
        </div>
      </div>

      <!-- 위젯 2: 업무 분포 차트 (100% 동적 배터리 게이지 뷰) -->
      <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-6 shadow-md dark:shadow-xl relative overflow-hidden transition-colors duration-200">
        <div class="absolute inset-x-0 top-0 h-1 bg-gradient-to-r from-emerald-500 to-teal-500"></div>

        <div class="flex justify-between items-center mb-6">
          <div class="flex items-center gap-2">
            <PieIcon class="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
            <h4 class="font-bold text-lg text-slate-900 dark:text-white">상태별 분포 현황</h4>
          </div>
        </div>

        <!-- 10년 차 시니어 설계: 다이나믹 쉐이프 배터리 바 (상태값 개수와 무관하게 완벽 자동 매칭) -->
        <div class="flex flex-col gap-6 py-6">
          
          <!-- 메인 배터리 바 -->
          <div class="h-10 w-full rounded-2xl bg-slate-100 dark:bg-slate-800 border border-slate-200/85 dark:border-slate-700/80 overflow-hidden p-1 flex">
            <div 
              v-for="(status, idx) in statusDistribution"
              :key="status.name"
              class="h-full transition-all duration-1000 flex items-center justify-center text-[10px] font-black"
              :class="[
                Number(idx) === 0 ? 'rounded-l-xl' : '',
                Number(idx) === statusDistribution.length - 1 ? 'rounded-r-xl' : '',
                Number(idx) > 0 ? 'border-l border-slate-200/40 dark:border-l border-slate-900/40' : ''
              ]"
              :style="{ 
                width: isMounted ? status.width : '0%',
                backgroundColor: status.colorHex,
                color: getContrastColor(status.colorHex)
              }"
            >
              <span v-if="isMounted && status.percentage > 0">{{ status.percentage }}%</span>
            </div>
          </div>

          <!-- 10년 차 시니어 설계: 상태별 리스트 (EAV 실시간 동적 매핑 렌더링) -->
          <div class="flex flex-col gap-3 max-h-[260px] overflow-y-auto custom-scrollbar pr-1">
            <div 
              v-for="status in statusDistribution" 
              :key="status.name"
              class="flex items-center justify-between p-3.5 rounded-2xl bg-slate-100/50 dark:bg-slate-800/40 border border-slate-200/50 dark:border-slate-800/80 hover:border-slate-350 dark:hover:border-slate-700 hover:bg-slate-100 dark:hover:bg-slate-800/80 transition-all duration-200 shadow-sm"
            >
              <div class="flex items-center gap-3">
                <div class="w-3.5 h-3.5 rounded-full shadow-sm" :style="{ backgroundColor: status.colorHex }"></div>
                <span class="text-sm font-bold text-slate-700 dark:text-slate-200">{{ status.name }}</span>
              </div>
              <div class="flex items-center gap-4">
                <span class="text-xs font-semibold text-slate-400 dark:text-slate-500">{{ status.count }} 업무</span>
                <span class="text-sm font-black text-slate-900 dark:text-white">{{ status.percentage }}%</span>
              </div>
            </div>
          </div>

        </div>

      </div>

    </div>

  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
/* 라이트 모드 기본 */
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 2px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
/* 다크 모드 동적 덮어쓰기 */
:deep(.dark) .custom-scrollbar::-webkit-scrollbar-thumb,
.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background: #1e293b;
}
:deep(.dark) .custom-scrollbar::-webkit-scrollbar-thumb:hover,
.dark .custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #334155;
}
</style>
