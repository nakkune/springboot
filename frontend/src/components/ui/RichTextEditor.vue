<script setup lang="ts">
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Placeholder from '@tiptap/extension-placeholder'
import { watch, onBeforeUnmount, ref } from 'vue'
import { Bold, Italic, List, ListOrdered, Heading1, Heading2, Quote, Undo, Redo, Code, Type } from 'lucide-vue-next'

const props = defineProps<{
  modelValue: string
  placeholder?: string
  readOnly?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'blur'): void
}>()

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit,
    Placeholder.configure({
      placeholder: props.placeholder || '여기에 내용을 입력하세요...',
    }),
  ],
  editable: !props.readOnly,
  onUpdate: () => {
    emit('update:modelValue', editor.value?.getHTML() || '')
  },
  onBlur: () => {
    emit('blur')
  },
  editorProps: {
    attributes: {
      class: 'prose dark:prose-invert prose-sm sm:prose-base focus:outline-none max-w-none min-h-[150px] px-4 py-3 text-slate-800 dark:text-slate-200',
    },
  },
})

// 외부에서 값이 변경되었을 때 에디터 내용 동기화
watch(() => props.modelValue, (newValue) => {
  const isSame = editor.value?.getHTML() === newValue
  if (isSame) return
  editor.value?.commands.setContent(newValue, false)
})

watch(() => props.readOnly, (newReadOnly) => {
  editor.value?.setEditable(!newReadOnly)
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<template>
  <div class="rich-text-editor-container border border-slate-200 dark:border-slate-700 rounded-xl overflow-hidden bg-white dark:bg-slate-900 shadow-sm focus-within:ring-2 focus-within:ring-indigo-500/50 transition-all flex flex-col">
    <!-- Toolbar (readonly가 아닐 때만 표시) -->
    <div v-if="editor && !readOnly" class="flex flex-wrap items-center gap-1 p-2 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50">
      <div class="flex items-center gap-1 mr-2 pr-2 border-r border-slate-200 dark:border-slate-700">
        <button @click="editor.chain().focus().toggleHeading({ level: 1 }).run()" :class="{ 'bg-slate-200 dark:bg-slate-700 text-indigo-600 dark:text-indigo-400': editor.isActive('heading', { level: 1 }) }" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors" title="제목 1">
          <Heading1 class="w-4 h-4" />
        </button>
        <button @click="editor.chain().focus().toggleHeading({ level: 2 }).run()" :class="{ 'bg-slate-200 dark:bg-slate-700 text-indigo-600 dark:text-indigo-400': editor.isActive('heading', { level: 2 }) }" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors" title="제목 2">
          <Heading2 class="w-4 h-4" />
        </button>
        <button @click="editor.chain().focus().setParagraph().run()" :class="{ 'bg-slate-200 dark:bg-slate-700 text-indigo-600 dark:text-indigo-400': editor.isActive('paragraph') }" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors" title="본문">
          <Type class="w-4 h-4" />
        </button>
      </div>

      <div class="flex items-center gap-1 mr-2 pr-2 border-r border-slate-200 dark:border-slate-700">
        <button @click="editor.chain().focus().toggleBold().run()" :class="{ 'bg-slate-200 dark:bg-slate-700 text-indigo-600 dark:text-indigo-400': editor.isActive('bold') }" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors" title="굵게">
          <Bold class="w-4 h-4" />
        </button>
        <button @click="editor.chain().focus().toggleItalic().run()" :class="{ 'bg-slate-200 dark:bg-slate-700 text-indigo-600 dark:text-indigo-400': editor.isActive('italic') }" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors" title="기울임">
          <Italic class="w-4 h-4" />
        </button>
        <button @click="editor.chain().focus().toggleCode().run()" :class="{ 'bg-slate-200 dark:bg-slate-700 text-indigo-600 dark:text-indigo-400': editor.isActive('code') }" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors" title="코드">
          <Code class="w-4 h-4" />
        </button>
      </div>

      <div class="flex items-center gap-1 mr-2 pr-2 border-r border-slate-200 dark:border-slate-700">
        <button @click="editor.chain().focus().toggleBulletList().run()" :class="{ 'bg-slate-200 dark:bg-slate-700 text-indigo-600 dark:text-indigo-400': editor.isActive('bulletList') }" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors" title="글머리 기호">
          <List class="w-4 h-4" />
        </button>
        <button @click="editor.chain().focus().toggleOrderedList().run()" :class="{ 'bg-slate-200 dark:bg-slate-700 text-indigo-600 dark:text-indigo-400': editor.isActive('orderedList') }" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors" title="번호 매기기">
          <ListOrdered class="w-4 h-4" />
        </button>
        <button @click="editor.chain().focus().toggleBlockquote().run()" :class="{ 'bg-slate-200 dark:bg-slate-700 text-indigo-600 dark:text-indigo-400': editor.isActive('blockquote') }" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 transition-colors" title="인용구">
          <Quote class="w-4 h-4" />
        </button>
      </div>

      <div class="flex items-center gap-1 ml-auto">
        <button @click="editor.chain().focus().undo().run()" :disabled="!editor.can().undo()" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 disabled:opacity-30 transition-colors" title="실행 취소">
          <Undo class="w-4 h-4" />
        </button>
        <button @click="editor.chain().focus().redo().run()" :disabled="!editor.can().redo()" class="p-1.5 rounded hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-600 dark:text-slate-400 disabled:opacity-30 transition-colors" title="다시 실행">
          <Redo class="w-4 h-4" />
        </button>
      </div>
    </div>
    
    <!-- Editor Content Area -->
    <div class="flex-1 overflow-y-auto max-h-[500px] bg-white dark:bg-slate-900 custom-scrollbar">
      <editor-content :editor="editor" />
    </div>
  </div>
</template>

<style>
/* Tiptap Placeholder CSS */
.tiptap p.is-editor-empty:first-child::before {
  color: #94a3b8;
  content: attr(data-placeholder);
  float: left;
  height: 0;
  pointer-events: none;
}
.dark .tiptap p.is-editor-empty:first-child::before {
  color: #475569;
}

/* Basic Tailwind Typography-like overrides for standard elements if @tailwindcss/typography is not installed */
.tiptap {
  line-height: 1.6;
}
.tiptap h1 {
  font-size: 1.75rem;
  font-weight: 800;
  margin-top: 1.5rem;
  margin-bottom: 0.75rem;
}
.tiptap h2 {
  font-size: 1.4rem;
  font-weight: 700;
  margin-top: 1.25rem;
  margin-bottom: 0.5rem;
}
.tiptap p {
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}
.tiptap ul {
  list-style-type: disc;
  padding-left: 1.5rem;
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}
.tiptap ol {
  list-style-type: decimal;
  padding-left: 1.5rem;
  margin-top: 0.5rem;
  margin-bottom: 0.5rem;
}
.tiptap blockquote {
  border-left: 3px solid #6366f1;
  padding-left: 1rem;
  margin-top: 1rem;
  margin-bottom: 1rem;
  font-style: italic;
  color: #64748b;
}
.dark .tiptap blockquote {
  color: #94a3b8;
}
.tiptap code {
  background-color: #f1f5f9;
  color: #e11d48;
  padding: 0.125rem 0.25rem;
  border-radius: 0.25rem;
  font-size: 0.875em;
}
.dark .tiptap code {
  background-color: #1e293b;
  color: #fb7185;
}
</style>
