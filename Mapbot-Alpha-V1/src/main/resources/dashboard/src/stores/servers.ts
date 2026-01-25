import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { api } from '../api/client'

export interface Server {
    id: string
    online: boolean
    players: number
    tps: string
    memory: string
    uptime: number
}

export const useServersStore = defineStore('servers', () => {
    const servers = ref<Server[]>([])
    const selectedServerId = ref<string | null>(null)
    const loading = ref(false)

    const selectedServer = computed(() =>
        servers.value.find(s => s.id === selectedServerId.value)
    )

    async function fetchServers() {
        loading.value = true
        try {
            servers.value = await api.getServers()
        } catch (e) {
            console.error('获取服务器列表失败', e)
        } finally {
            loading.value = false
        }
    }

    function selectServer(id: string | null) {
        selectedServerId.value = id
    }

    return { servers, selectedServerId, selectedServer, loading, fetchServers, selectServer }
})
