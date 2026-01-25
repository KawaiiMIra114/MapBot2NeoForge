<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { api } from '../api/client'
import { useAuthStore } from '../stores/auth'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

// 配置
const config = ref({
  wsUrl: '',
  reconnectInterval: 5,
  playerGroupId: '',
  adminGroupId: '',
  botQQ: '',
  debugMode: false
})

// 用户管理
const users = ref<{username: string, role: string}[]>([])

async function loadConfig() {
  const data = await api.getConfig()
  config.value = { ...config.value, ...data }
}

async function saveConfig() {
  const result = await api.saveConfig(config.value)
  alert(result.success ? '配置已保存' : '保存失败: ' + result.error)
}

async function loadUsers() {
  users.value = await api.getUsers()
}

async function deleteUser(username: string) {
  if (!confirm(`确定删除用户 "${username}"？`)) return
  await api.deleteUser(username)
  loadUsers()
}

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  loadConfig()
  loadUsers()
})
</script>

<template>
  <div class="p-6 space-y-6 max-w-2xl mx-auto">
    <h1 class="text-2xl font-bold">设置中心</h1>
    
    <!-- OneBot 配置 -->
    <div class="glass-card">
      <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
        <span class="material-symbols-outlined text-primary">link</span> OneBot 连接
      </h3>
      <div class="space-y-4">
        <div>
          <label class="text-sm text-txt-sub block mb-1">WebSocket URL</label>
          <input v-model="config.wsUrl" type="text"
            class="w-full bg-bg-dark rounded-lg px-4 py-2 text-white border border-white/10 focus:border-primary focus:outline-none">
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="text-sm text-txt-sub block mb-1">玩家群号</label>
            <input v-model="config.playerGroupId" type="text"
              class="w-full bg-bg-dark rounded-lg px-4 py-2 text-white border border-white/10 focus:border-primary focus:outline-none">
          </div>
          <div>
            <label class="text-sm text-txt-sub block mb-1">管理群号</label>
            <input v-model="config.adminGroupId" type="text"
              class="w-full bg-bg-dark rounded-lg px-4 py-2 text-white border border-white/10 focus:border-primary focus:outline-none">
          </div>
        </div>
      </div>
    </div>
    
    <!-- 用户管理 -->
    <div class="glass-card">
      <h3 class="text-lg font-bold mb-4 flex items-center gap-2">
        <span class="material-symbols-outlined text-primary">group</span> 用户管理
      </h3>
      <div class="space-y-2">
        <div v-for="u in users" :key="u.username"
          class="flex items-center justify-between bg-bg-dark rounded-lg px-4 py-2">
          <div class="flex items-center gap-3">
            <span class="material-symbols-outlined text-primary">person</span>
            <span class="text-white">{{ u.username }}</span>
            <span :class="u.role === 'ADMIN' ? 'bg-red-500/20 text-red-400' : 'bg-gray-500/20 text-gray-400'"
              class="text-xs px-2 py-0.5 rounded">{{ u.role }}</span>
          </div>
          <button v-if="u.username !== 'admin'" @click="deleteUser(u.username)" class="text-red-400 hover:text-red-300">
            <span class="material-symbols-outlined text-sm">delete</span>
          </button>
        </div>
      </div>
    </div>
    
    <!-- 操作按钮 -->
    <div class="flex gap-4">
      <button @click="saveConfig"
        class="flex-1 py-3 bg-primary hover:bg-primary-dark text-bg-dark rounded-xl font-bold">
        保存配置
      </button>
      <button @click="logout"
        class="py-3 px-6 bg-red-500/20 hover:bg-red-500/30 text-red-400 rounded-xl font-bold border border-red-500/30">
        登出
      </button>
    </div>
  </div>
</template>
