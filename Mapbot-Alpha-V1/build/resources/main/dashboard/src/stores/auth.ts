import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getToken, setToken, clearToken, login as apiLogin } from '../api/client'

export const useAuthStore = defineStore('auth', () => {
    const token = ref<string | null>(getToken())
    const username = ref<string | null>(null)

    const isAuthenticated = computed(() => !!token.value)

    async function login(user: string, password: string): Promise<boolean> {
        const result = await apiLogin(user, password)
        if (result.token) {
            token.value = result.token
            username.value = user
            setToken(result.token)
            return true
        }
        return false
    }

    function logout() {
        token.value = null
        username.value = null
        clearToken()
    }

    return { token, username, isAuthenticated, login, logout }
})
