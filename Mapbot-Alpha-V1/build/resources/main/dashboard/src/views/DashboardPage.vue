<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useServersStore } from '../stores/servers'

const serversStore = useServersStore()
const wsStatus = ref(false)

// WebSocket 连接
let ws: WebSocket | null = null
const logs = ref<string[]>([])

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
    if (logs.value.length > 100) logs.value = logs.value.slice(-100)
  }
  
  ws.onclose = () => {
    wsStatus.value = false
    logs.value.push('[SYSTEM] WebSocket 已断开')
  }
}

onMounted(() => {
  serversStore.fetchServers()
  connectWebSocket()
  
  // 定时刷新
  const interval = setInterval(() => serversStore.fetchServers(), 5000)
  onUnmounted(() => {
    clearInterval(interval)
    ws?.close()
  })
})
</script>

<template>
  <div class="p-6 space-y-6">
    <h1 class="text-2xl font-bold">Dashboard</h1>
    
    <!-- 状态卡片 -->
    <div class="grid grid-cols-1 md:grid-cols-4 gap-4">
      <div class="glass-card">
        <div class="flex items-center gap-2 mb-2">
          <span class="material-symbols-outlined text-sm text-txt-sub">lan</span>
          <p class="text-txt-sub text-sm">Bridge 状态</p>
        </div>
        <div class="space-y-1">
          <div v-for="s in serversStore.servers.slice(0, 3)" :key="s.id" 
            class="flex items-center justify-between text-xs">
            <span class="text-white">{{ s.id }}</span>
            <span :class="parseFloat(s.tps) >= 18 ? 'text-green-400' : 'text-yellow-400'">
              {{ s.tps }} TPS
            </span>
          </div>
          <p v-if="serversStore.servers.length === 0" class="text-gray-500 text-xs">暂无服务器</p>
        </div>
      </div>
      
      <div class="glass-card">
        <p class="text-txt-sub text-sm mb-2">已连接服务器</p>
        <p class="text-2xl font-bold">{{ serversStore.servers.length }}</p>
      </div>
      
      <div class="glass-card">
        <p class="text-txt-sub text-sm mb-2">WebSocket</p>
        <p class="text-2xl font-bold" :class="wsStatus ? 'text-green-400' : 'text-red-400'">
          {{ wsStatus ? '已连接' : '断开' }}
        </p>
      </div>
      
      <div class="glass-card">
        <p class="text-txt-sub text-sm mb-2">文件管理</p>
        <router-link to="/files" class="text-primary hover:underline text-sm">打开 →</router-link>
      </div>
    </div>
    
    <!-- 迷你控制台 -->
    <div class="glass-card">
      <h2 class="text-lg font-bold mb-4">实时控制台</h2>
      <div class="bg-bg-dark rounded-lg p-4 h-60 overflow-y-auto font-mono text-xs space-y-1">
        <p v-for="(log, i) in logs.slice(-50)" :key="i" class="text-gray-300">{{ log }}</p>
        <p v-if="logs.length === 0" class="text-gray-500">等待连接...</p>
      </div>
    </div>
  </div>
</template>
