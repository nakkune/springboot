<script setup lang="ts">
import { ref, computed } from 'vue'
import { 
  Paperclip, Loader2, 
  FileText, FileSpreadsheet, FileImage, FileArchive, 
  FileJson, FileCode, FileType 
} from 'lucide-vue-next'
import api from '@/services/api'

const props = defineProps<{
  value: string
  itemId: string
  columnId: string
}>()

const emit = defineEmits(['update'])

const fileInputRef = ref<HTMLInputElement | null>(null)
const isUploading = ref(false)

const handleFileChange = async (e: Event) => {
  const target = e.target as HTMLInputElement
  if (!target.files || target.files.length === 0) return

  const file = target.files[0]
  isUploading.value = true
  
  try {
    const formData = new FormData()
    formData.append('file', file)
    
    const res = await api.post('/files/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    }) as any
    
    const fileData = {
      name: res.originalName,
      url: res.url
    }
    
    // JSON 직렬화하여 저장
    emit('update', { itemId: props.itemId, columnId: props.columnId, value: JSON.stringify(fileData) })
  } catch (err) {
    console.error('File upload failed', err)
    alert('파일 업로드에 실패했습니다.')
  } finally {
    isUploading.value = false
    if (fileInputRef.value) fileInputRef.value.value = ''
  }
}

const downloadFile = async () => {
  if (props.value) {
    try {
      const data = JSON.parse(props.value)
      if (data.url) {
        const url = `http://localhost:9090/api${data.url}`
        const response = await fetch(url)
        const blob = await response.blob()
        const blobUrl = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = blobUrl
        a.download = data.name || 'download'
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        URL.revokeObjectURL(blobUrl)
      }
    } catch (e) {
      console.error('Download failed', e)
    }
  }
}

const parsedValue = ref<any>(null)
if (props.value) {
  try {
    parsedValue.value = JSON.parse(props.value)
  } catch(e) {}
}

const triggerUpload = () => {
  fileInputRef.value?.click()
}

// ===== 파일 타입별 아이콘 및 색상 매핑 =====
type FileIconInfo = {
  icon: any
  color: string
  bgColor: string
  label: string
}

const getFileInfo = (fileName: string): FileIconInfo => {
  const ext = fileName.split('.').pop()?.toLowerCase() || ''
  
  const iconMap: Record<string, FileIconInfo> = {
    pdf:    { icon: FileText, color: '#EF4444', bgColor: 'bg-red-500/10 dark:bg-red-500/15', label: 'PDF' },
    xlsx:   { icon: FileSpreadsheet, color: '#10B981', bgColor: 'bg-emerald-500/10 dark:bg-emerald-500/15', label: 'Excel' },
    xls:    { icon: FileSpreadsheet, color: '#10B981', bgColor: 'bg-emerald-500/10 dark:bg-emerald-500/15', label: 'Excel' },
    csv:    { icon: FileSpreadsheet, color: '#10B981', bgColor: 'bg-emerald-500/10 dark:bg-emerald-500/15', label: 'CSV' },
    doc:    { icon: FileText, color: '#3B82F6', bgColor: 'bg-blue-500/10 dark:bg-blue-500/15', label: 'Word' },
    docx:   { icon: FileText, color: '#3B82F6', bgColor: 'bg-blue-500/10 dark:bg-blue-500/15', label: 'Word' },
    txt:    { icon: FileText, color: '#64748B', bgColor: 'bg-slate-500/10 dark:bg-slate-500/15', label: 'Text' },
    md:     { icon: FileText, color: '#64748B', bgColor: 'bg-slate-500/10 dark:bg-slate-500/15', label: 'Markdown' },
    hwp:    { icon: FileText, color: '#EC4899', bgColor: 'bg-pink-500/10 dark:bg-pink-500/15', label: 'HWP' },
    png:    { icon: FileImage, color: '#8B5CF6', bgColor: 'bg-violet-500/10 dark:bg-violet-500/15', label: 'Image' },
    jpg:    { icon: FileImage, color: '#8B5CF6', bgColor: 'bg-violet-500/10 dark:bg-violet-500/15', label: 'Image' },
    jpeg:   { icon: FileImage, color: '#8B5CF6', bgColor: 'bg-violet-500/10 dark:bg-violet-500/15', label: 'Image' },
    gif:    { icon: FileImage, color: '#8B5CF6', bgColor: 'bg-violet-500/10 dark:bg-violet-500/15', label: 'GIF' },
    svg:    { icon: FileImage, color: '#8B5CF6', bgColor: 'bg-violet-500/10 dark:bg-violet-500/15', label: 'SVG' },
    webp:   { icon: FileImage, color: '#8B5CF6', bgColor: 'bg-violet-500/10 dark:bg-violet-500/15', label: 'WebP' },
    zip:    { icon: FileArchive, color: '#F59E0B', bgColor: 'bg-amber-500/10 dark:bg-amber-500/15', label: 'ZIP' },
    rar:    { icon: FileArchive, color: '#F59E0B', bgColor: 'bg-amber-500/10 dark:bg-amber-500/15', label: 'RAR' },
    '7z':   { icon: FileArchive, color: '#F59E0B', bgColor: 'bg-amber-500/10 dark:bg-amber-500/15', label: '7z' },
    tar:    { icon: FileArchive, color: '#F59E0B', bgColor: 'bg-amber-500/10 dark:bg-amber-500/15', label: 'TAR' },
    gz:     { icon: FileArchive, color: '#F59E0B', bgColor: 'bg-amber-500/10 dark:bg-amber-500/15', label: 'GZip' },
    json:   { icon: FileJson, color: '#EAB308', bgColor: 'bg-yellow-500/10 dark:bg-yellow-500/15', label: 'JSON' },
    xml:    { icon: FileCode, color: '#EAB308', bgColor: 'bg-yellow-500/10 dark:bg-yellow-500/15', label: 'XML' },
    js:     { icon: FileCode, color: '#4F46E5', bgColor: 'bg-indigo-500/10 dark:bg-indigo-500/15', label: 'JS' },
    ts:     { icon: FileCode, color: '#4F46E5', bgColor: 'bg-indigo-500/10 dark:bg-indigo-500/15', label: 'TS' },
    jsx:    { icon: FileCode, color: '#4F46E5', bgColor: 'bg-indigo-500/10 dark:bg-indigo-500/15', label: 'JSX' },
    tsx:    { icon: FileCode, color: '#4F46E5', bgColor: 'bg-indigo-500/10 dark:bg-indigo-500/15', label: 'TSX' },
    py:     { icon: FileCode, color: '#4F46E5', bgColor: 'bg-indigo-500/10 dark:bg-indigo-500/15', label: 'Python' },
    java:   { icon: FileCode, color: '#4F46E5', bgColor: 'bg-indigo-500/10 dark:bg-indigo-500/15', label: 'Java' },
    html:   { icon: FileCode, color: '#EF4444', bgColor: 'bg-red-500/10 dark:bg-red-500/15', label: 'HTML' },
    css:    { icon: FileCode, color: '#3B82F6', bgColor: 'bg-blue-500/10 dark:bg-blue-500/15', label: 'CSS' },
    sql:    { icon: FileCode, color: '#4F46E5', bgColor: 'bg-indigo-500/10 dark:bg-indigo-500/15', label: 'SQL' },
    sh:     { icon: FileCode, color: '#10B981', bgColor: 'bg-emerald-500/10 dark:bg-emerald-500/15', label: 'Shell' },
    ppt:    { icon: FileText, color: '#EF4444', bgColor: 'bg-red-500/10 dark:bg-red-500/15', label: 'PPT' },
    pptx:   { icon: FileText, color: '#EF4444', bgColor: 'bg-red-500/10 dark:bg-red-500/15', label: 'PPT' },
  }

  return iconMap[ext] || { icon: FileType, color: '#94A3B8', bgColor: 'bg-slate-500/10 dark:bg-slate-500/15', label: ext.toUpperCase() || 'File' }
}

const fileInfo = computed<FileIconInfo | null>(() => {
  if (!parsedValue.value?.name) return null
  return getFileInfo(parsedValue.value.name)
})
</script>

<template>
  <div class="w-full h-full flex items-center justify-center p-1">
    <input 
      type="file" 
      ref="fileInputRef"
      @change="handleFileChange"
      class="hidden"
    />
    
    <div v-if="isUploading" class="flex items-center gap-1 text-xs text-indigo-400">
      <Loader2 class="w-3 h-3 animate-spin" /> 업로드 중...
    </div>
    
    <!-- 파일 타입 아이콘 이미지 표시 (호버 시 파일명 노출) -->
    <div 
      v-else-if="parsedValue && fileInfo"
      @click.stop="downloadFile"
      class="flex items-center justify-center rounded cursor-pointer transition-all group relative w-full h-full"
    >
      <!-- 아이콘 이미지: 타입별 색상 배지 -->
      <div 
        class="w-5 h-5 rounded-md flex items-center justify-center shrink-0 border transition-transform group-hover:scale-110"
        :style="{ 
          backgroundColor: fileInfo.color + '18', 
          borderColor: fileInfo.color + '35',
          color: fileInfo.color 
        }"
      >
        <component :is="fileInfo.icon" class="w-3 h-3" />
      </div>
      <!-- 파일명 툴팁 (호버 시 표시) -->
      <div class="absolute left-0 top-full mt-1.5 z-50 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 pointer-events-none">
        <div class="bg-slate-900 dark:bg-slate-700 text-white text-[10px] font-medium px-2.5 py-1.5 rounded-lg shadow-xl whitespace-nowrap border border-slate-700 dark:border-slate-600">
          {{ parsedValue.name }}
        </div>
      </div>
    </div>
    
    <div 
      v-else 
      @click.stop="triggerUpload"
      class="w-full h-full flex items-center justify-center cursor-pointer group"
    >
      <Paperclip class="w-3.5 h-3.5 text-slate-400 dark:text-slate-500 group-hover:text-indigo-500 transition-all duration-200 transform group-hover:scale-110" />
    </div>
  </div>
</template>
