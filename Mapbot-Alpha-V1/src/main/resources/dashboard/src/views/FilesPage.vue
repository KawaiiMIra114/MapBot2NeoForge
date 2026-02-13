<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '../api/client'
import { useServersStore } from '../stores/servers'

type FileItem = { name: string, isDir: boolean, size: number }

const serversStore = useServersStore()
const currentPath = ref('')
const files = ref<FileItem[]>([])
const selectedFile = ref<string | null>(null)
const fileContent = ref('')
const loading = ref(false)
const saving = ref(false)
const errorMessage = ref('')
const uploadInput = ref<HTMLInputElement | null>(null)

function getErrorMessage(err: unknown): string {
  if (err instanceof Error && err.message) return err.message
  return '请求失败，请稍后重试'
}

function joinPath(base: string, name: string): string {
  const normalizedName = name.replace(/^\/+/, '')
  return base ? `${base}/${normalizedName}` : normalizedName
}

async function loadFiles() {
  const serverId = serversStore.selectedServerId
  loading.value = true
  errorMessage.value = ''
  try {
    const data = await api.listFiles(currentPath.value, serverId || undefined)
    files.value = Array.isArray(data) ? data : []
  } catch (err) {
    files.value = []
    errorMessage.value = `加载失败: ${getErrorMessage(err)}`
  } finally {
    loading.value = false
  }
}

async function openItem(item: {name: string, isDir: boolean}) {
  errorMessage.value = ''
  try {
    if (item.isDir) {
      currentPath.value = joinPath(currentPath.value, item.name)
      await loadFiles()
    } else {
      selectedFile.value = joinPath(currentPath.value, item.name)
      const data = await api.readFile(selectedFile.value, serversStore.selectedServerId || undefined)
      fileContent.value = data?.content || ''
    }
  } catch (err) {
    errorMessage.value = `打开失败: ${getErrorMessage(err)}`
  }
}

function goUp() {
  if (!currentPath.value) return
  const parts = currentPath.value.split('/')
  parts.pop()
  currentPath.value = parts.join('/')
  loadFiles()
}

async function saveFile() {
  if (!selectedFile.value) return
  saving.value = true
  try {
    await api.writeFile(selectedFile.value, fileContent.value, serversStore.selectedServerId || undefined)
    alert('文件已保存')
  } catch (err) {
    alert(`保存失败: ${getErrorMessage(err)}`)
  } finally {
    saving.value = false
  }
}

async function createFolder() {
  const name = prompt('请输入新目录名')
  if (!name) return

  const dirName = name.trim()
  if (!dirName) return

  try {
    await api.mkdir(joinPath(currentPath.value, dirName), serversStore.selectedServerId || undefined)
    await loadFiles()
  } catch (err) {
    alert(`创建目录失败: ${getErrorMessage(err)}`)
  }
}

function triggerUpload() {
  uploadInput.value?.click()
}

function fileToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => {
      const result = typeof reader.result === 'string' ? reader.result : ''
      const idx = result.indexOf(',')
      resolve(idx >= 0 ? result.substring(idx + 1) : result)
    }
    reader.onerror = () => reject(new Error('文件读取失败'))
    reader.readAsDataURL(file)
  })
}

async function uploadFile(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files && input.files[0]
  if (!file) return

  try {
    const content = await fileToBase64(file)
    const targetPath = joinPath(currentPath.value, file.name)
    await api.uploadFile(targetPath, content, 'base64', serversStore.selectedServerId || undefined)
    await loadFiles()
    alert('上传成功')
  } catch (err) {
    const msg = getErrorMessage(err)
    if (msg.includes('does not support this file action')) {
      alert('上传失败：远程服务器尚未升级，不支持 upload 接口')
    } else {
      alert(`上传失败: ${msg}`)
    }
  } finally {
    input.value = ''
  }
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
        <button @click="createFolder" class="p-2 bg-surface hover:bg-white/5 rounded-lg border border-white/5" title="新建目录">
          <span class="material-symbols-outlined">create_new_folder</span>
        </button>
        <button @click="triggerUpload" class="p-2 bg-surface hover:bg-white/5 rounded-lg border border-white/5" title="上传文件">
          <span class="material-symbols-outlined">upload</span>
        </button>
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
        <p v-if="errorMessage" class="text-red-400 text-xs px-2 py-1">{{ errorMessage }}</p>
        <p v-if="loading" class="text-gray-400 text-center py-4">加载中...</p>
        <div v-for="f in files" :key="f.name" @click="openItem(f)"
          class="flex items-center gap-2 p-2 hover:bg-white/5 rounded-lg cursor-pointer">
          <span class="material-symbols-outlined text-primary">{{ f.isDir ? 'folder' : 'description' }}</span>
          <span class="text-white text-sm">{{ f.name }}</span>
        </div>
        <p v-if="!loading && files.length === 0" class="text-gray-500 text-center py-4">空目录</p>
      </div>
      
      <!-- 编辑器 -->
      <div class="flex-1 flex flex-col bg-surface rounded-2xl border border-white/5 overflow-hidden">
        <div class="px-4 py-3 border-b border-white/5 flex justify-between items-center">
          <span class="font-mono text-sm text-txt-sub">{{ selectedFile || '选择文件' }}</span>
          <button @click="saveFile" :disabled="saving || !selectedFile"
            class="text-xs bg-primary text-bg-dark px-3 py-1 rounded-full font-bold disabled:opacity-50 disabled:cursor-not-allowed">
            {{ saving ? '保存中...' : '保存' }}
          </button>
        </div>
        <textarea v-model="fileContent"
          class="flex-1 bg-transparent text-white font-mono text-sm p-4 resize-none focus:outline-none"
          placeholder="选择文件以编辑..."></textarea>
      </div>
    </div>
    <input ref="uploadInput" type="file" class="hidden" @change="uploadFile">
  </div>
</template>
