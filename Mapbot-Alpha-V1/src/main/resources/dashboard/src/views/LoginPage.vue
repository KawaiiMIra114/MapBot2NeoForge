<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function handleLogin() {
  error.value = ''
  loading.value = true
  
  const success = await auth.login(username.value, password.value)
  
  if (success) {
    router.push('/')
  } else {
    error.value = '用户名或密码错误'
  }
  
  loading.value = false
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-bg-dark">
    <div class="glass-card w-full max-w-md">
      <div class="text-center mb-8">
        <h1 class="text-3xl font-bold text-primary">MapBot Alpha</h1>
        <p class="text-txt-sub mt-2">控制面板登录</p>
      </div>
      
      <form @submit.prevent="handleLogin" class="space-y-4">
        <div>
          <label class="text-sm text-txt-sub block mb-1">用户名</label>
          <input v-model="username" type="text" 
            class="w-full bg-bg-dark rounded-lg px-4 py-3 text-white border border-white/10 focus:border-primary focus:outline-none"
            placeholder="admin">
        </div>
        
        <div>
          <label class="text-sm text-txt-sub block mb-1">密码</label>
          <input v-model="password" type="password"
            class="w-full bg-bg-dark rounded-lg px-4 py-3 text-white border border-white/10 focus:border-primary focus:outline-none"
            placeholder="••••••••">
        </div>
        
        <p v-if="error" class="text-red-400 text-sm">{{ error }}</p>
        
        <button type="submit" :disabled="loading"
          class="w-full py-3 bg-primary hover:bg-primary-dark text-bg-dark rounded-xl font-bold transition-all disabled:opacity-50">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>
    </div>
  </div>
</template>
