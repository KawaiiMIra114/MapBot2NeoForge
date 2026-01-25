<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

const logs = ref<string[]>([])
const cmdInput = ref('')
const searchKeyword = ref('')
const wsStatus = ref(false)

let ws: WebSocket | null = null

function connectWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.host}/ws`
  
  ws = new WebSocket(wsUrl)
  
  ws.onopen = () => {
    wsStatus.value = true
    logs.value.push('[SYSTEM] WebSocket 已连接')
  }
  
  ws.onmessage = (e) => {
    logs.value.push(e.data)
    if (logs.value.length > 500) logs.value = logs.value.slice(-500)
  }
  
  ws.onclose = () => {
    wsStatus.value = false
    logs.value.push('[SYSTEM] WebSocket 已断开')
  }
}

function sendCommand() {
  if (!cmdInput.value.trim() || !ws) return
  ws.send(cmdInput.value)
  logs.value.push('> ' + cmdInput.value)
  cmdInput.value = ''
}

function clearLogs() {
  logs.value = []
}

const filteredLogs = computed(() => {
  if (!searchKeyword.value) return logs.value
  return logs.value.filter(l => l.toLowerCase().includes(searchKeyword.value.toLowerCase()))
})

onMounted(() => {
  connectWebSocket()
  onUnmounted(() => ws?.close())
})
</script>

<template>
  <div class="h-full flex flex-col bg-[#0f0f0f]">
    <!-- Header -->
    <div class="h-14 flex items-center justify-between px-6 bg-surface border-b border-white/5 shrink-0">
      <div class="flex items-center gap-3">
        <span class="text-primary font-bold font-mono text-lg">>_ Console</span>
        <span :class="wsStatus ? 'bg-green-900/30 text-green-400 border-green-800' : 'bg-red-900/30 text-red-400 border-red-800'"
          class="text-xs px-2 py-0.5 rounded border">
          {{ wsStatus ? 'CONNECTED' : 'DISCONNECTED' }}
        </span>
      </div>
      <div class="flex items-center gap-2">
        <div class="flex items-center bg-black/30 rounded-lg border border-white/10 px-3 py-1.5">
          <span class="material-symbols-outlined text-gray-500 text-sm mr-2">search</span>
          <input v-model="searchKeyword" type="text" placeholder="搜索日志..."
            class="bg-transparent border-none focus:ring-0 focus:outline-none text-white text-xs w-32">
        </div>
        <button @click="clearLogs" class="p-2 hover:bg-white/5 rounded-lg text-txt-sub hover:text-white">
          <span class="material-symbols-outlined text-lg">delete</span>
        </button>
      </div>
    </div>
    
    <!-- Logs -->
    <div class="flex-1 overflow-y-auto p-4 font-mono text-sm space-y-0.5">
      <p v-for="(log, i) in filteredLogs" :key="i" class="text-gray-300">{{ log }}</p>
    </div>
    
    <!-- Input -->
    <div class="p-4 bg-surface border-t border-white/5">
      <div class="bg-black/40 rounded-full border border-white/10 flex items-center px-4 py-2">
        <span class="text-gray-500 font-mono mr-3">></span>
        <input v-model="cmdInput" @keyup.enter="sendCommand"
          class="flex-1 bg-transparent border-none focus:ring-0 focus:outline-none text-white text-sm font-mono"
          placeholder="输入指令...">
        <button @click="sendCommand" class="text-primary hover:text-white">
          <span class="material-symbols-outlined">send</span>
        </button>
      </div>
    </div>
  </div>
</template>
