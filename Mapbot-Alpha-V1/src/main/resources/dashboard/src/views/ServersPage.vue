<script setup lang="ts">
import { onMounted } from 'vue'
import { useServersStore } from '../stores/servers'

const serversStore = useServersStore()

function formatUptime(ms: number): string {
  const sec = Math.floor(ms / 1000)
  const min = Math.floor(sec / 60)
  const hour = Math.floor(min / 60)
  const day = Math.floor(hour / 24)
  if (day > 0) return `${day}天 ${hour % 24}小时`
  if (hour > 0) return `${hour}小时 ${min % 60}分钟`
  return `${min}分钟`
}

onMounted(() => serversStore.fetchServers())
</script>

<template>
  <div class="p-6 space-y-6">
    <header class="flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold">服务器管理</h1>
        <p class="text-txt-sub">已连接的 MC 服务器列表 (Bridge)</p>
      </div>
      <button @click="serversStore.fetchServers" class="p-2 bg-surface hover:bg-white/5 rounded-lg border border-white/5">
        <span class="material-symbols-outlined">refresh</span>
      </button>
    </header>
    
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div v-for="s in serversStore.servers" :key="s.id"
        @click="serversStore.selectServer(s.id)"
        :class="serversStore.selectedServerId === s.id ? 'border-primary' : 'border-white/5'"
        class="glass-card cursor-pointer hover:border-primary/50 transition-colors">
        <div class="flex items-center justify-between mb-4">
          <div class="flex items-center gap-3">
            <span class="material-symbols-outlined text-primary">dns</span>
            <span class="font-bold">{{ s.id }}</span>
          </div>
          <span :class="s.online ? 'bg-green-500 animate-pulse' : 'bg-red-500'" class="w-2 h-2 rounded-full"></span>
        </div>
        <div class="grid grid-cols-3 gap-2 text-sm">
          <div><span class="text-txt-sub">玩家</span><br><span class="font-bold">{{ s.players }}</span></div>
          <div><span class="text-txt-sub">TPS</span><br><span class="font-bold">{{ s.tps }}</span></div>
          <div><span class="text-txt-sub">内存</span><br><span class="font-bold">{{ s.memory }}</span></div>
        </div>
        <div class="text-xs text-gray-500 mt-3">运行时间: {{ formatUptime(s.uptime) }}</div>
      </div>
      
      <div v-if="serversStore.servers.length === 0" class="glass-card text-center py-8 col-span-full">
        <span class="material-symbols-outlined text-4xl text-white/20">cloud_off</span>
        <p class="text-txt-sub mt-2">暂无连接的服务器</p>
      </div>
    </div>
  </div>
</template>
