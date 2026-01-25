<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '../api/client'
import { useServersStore } from '../stores/servers'

const serversStore = useServersStore()
const currentPath = ref('')
const files = ref<{name: string, isDir: boolean, size: number}[]>([])
const selectedFile = ref<string | null>(null)
const fileContent = ref('')

async function loadFiles() {
  const serverId = serversStore.selectedServerId
  const data = await api.listFiles(currentPath.value, serverId || undefined)
  files.value = data
}

async function openItem(item: {name: string, isDir: boolean}) {
  if (item.isDir) {
    currentPath.value = currentPath.value ? `${currentPath.value}/${item.name}` : item.name
    await loadFiles()
  } else {
    selectedFile.value = currentPath.value ? `${currentPath.value}/${item.name}` : item.name
    const data = await api.readFile(selectedFile.value, serversStore.selectedServerId || undefined)
    fileContent.value = data.content || ''
  }
}

function goUp() {
  const parts = currentPath.value.split('/')
  parts.pop()
  currentPath.value = parts.join('/')
  loadFiles()
}

async function saveFile() {
  if (!selectedFile.value) return
  await api.writeFile(selectedFile.value, fileContent.value, serversStore.selectedServerId || undefined)
  alert('文件已保存')
}

onMounted(() => {
  serversStore.fetchServers()
  loadFiles()
})
</script>

<template>
  <div class="h-full flex flex-col p-6">
    <header class="flex justify-between items-center mb-4">
      <div>
        <h1 class="text-2xl font-bold">文件管理</h1>
        <p class="text-txt-sub text-sm">{{ currentPath || '/' }}</p>
      </div>
      <div class="flex gap-2">
        <button @click="goUp" class="p-2 bg-surface hover:bg-white/5 rounded-lg border border-white/5">
          <span class="material-symbols-outlined">arrow_upward</span>
        </button>
        <button @click="loadFiles" class="p-2 bg-surface hover:bg-white/5 rounded-lg border border-white/5">
          <span class="material-symbols-outlined">refresh</span>
        </button>
      </div>
    </header>
    
    <div class="flex gap-4 flex-1 min-h-0">
      <!-- 文件列表 -->
      <div class="w-1/3 bg-surface rounded-2xl border border-white/5 overflow-y-auto p-2">
        <div v-for="f in files" :key="f.name" @click="openItem(f)"
          class="flex items-center gap-2 p-2 hover:bg-white/5 rounded-lg cursor-pointer">
          <span class="material-symbols-outlined text-primary">{{ f.isDir ? 'folder' : 'description' }}</span>
          <span class="text-white text-sm">{{ f.name }}</span>
        </div>
        <p v-if="files.length === 0" class="text-gray-500 text-center py-4">空目录</p>
      </div>
      
      <!-- 编辑器 -->
      <div class="flex-1 flex flex-col bg-surface rounded-2xl border border-white/5 overflow-hidden">
        <div class="px-4 py-3 border-b border-white/5 flex justify-between items-center">
          <span class="font-mono text-sm text-txt-sub">{{ selectedFile || '选择文件' }}</span>
          <button @click="saveFile" class="text-xs bg-primary text-bg-dark px-3 py-1 rounded-full font-bold">保存</button>
        </div>
        <textarea v-model="fileContent"
          class="flex-1 bg-transparent text-white font-mono text-sm p-4 resize-none focus:outline-none"
          placeholder="选择文件以编辑..."></textarea>
      </div>
    </div>
  </div>
</template>
