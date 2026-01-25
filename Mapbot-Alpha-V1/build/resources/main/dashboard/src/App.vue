<script setup lang="ts">
import { useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()

const showSidebar = computed(() => route.path !== '/login')

const navItems = [
  { path: '/', name: 'Dashboard', icon: 'dashboard' },
  { path: '/console', name: 'Console', icon: 'terminal' },
  { path: '/files', name: 'Files', icon: 'folder' },
  { path: '/servers', name: 'Servers', icon: 'dns' },
  { path: '/settings', name: 'Settings', icon: 'settings' }
]
</script>

<template>
  <div class="h-screen flex bg-bg-dark text-white">
    <!-- Sidebar -->
    <nav v-if="showSidebar" class="w-16 bg-surface border-r border-white/5 flex flex-col items-center py-4 gap-2">
      <div class="w-10 h-10 bg-primary/20 rounded-xl flex items-center justify-center mb-4">
        <span class="text-primary font-bold">M</span>
      </div>
      
      <router-link v-for="item in navItems" :key="item.path" :to="item.path"
        :class="route.path === item.path ? 'bg-primary/20 text-primary' : 'text-txt-sub hover:text-white hover:bg-white/5'"
        class="w-10 h-10 rounded-xl flex items-center justify-center transition-colors">
        <span class="material-symbols-outlined">{{ item.icon }}</span>
      </router-link>
    </nav>
    
    <!-- Main Content -->
    <main class="flex-1 overflow-hidden">
      <router-view />
    </main>
  </div>
</template>
