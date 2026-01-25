// API 客户端 - 统一处理认证和请求
const API_BASE = ''  // 开发时使用代理

export function getToken(): string | null {
    return localStorage.getItem('token')
}

export function setToken(token: string) {
    localStorage.setItem('token', token)
}

export function clearToken() {
    localStorage.removeItem('token')
}

export async function apiFetch(url: string, options: RequestInit = {}): Promise<Response> {
    const token = getToken()
    const headers = new Headers(options.headers)

    if (token) {
        headers.set('Authorization', `Bearer ${token}`)
    }

    return fetch(API_BASE + url, {
        ...options,
        headers
    })
}

// 登录
export async function login(username: string, password: string): Promise<{ token?: string, error?: string }> {
    const res = await fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    })
    return res.json()
}

// 通用 API 调用
export const api = {
    // 服务器相关
    async getServers() {
        const res = await apiFetch('/api/servers')
        return res.json()
    },

    async getStatus() {
        const res = await apiFetch('/api/status')
        return res.json()
    },

    async getMetricsHistory(serverId: string) {
        const res = await apiFetch(`/api/metrics/${serverId}/history`)
        return res.json()
    },

    async sendCommand(serverId: string, command: string) {
        const res = await apiFetch(`/api/servers/${serverId}/command`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ command })
        })
        return res.json()
    },

    // 文件相关
    async listFiles(path: string, serverId?: string) {
        const base = serverId ? `/api/remote/${serverId}/files` : '/api/files'
        const res = await apiFetch(`${base}/list?path=${encodeURIComponent(path)}`)
        return res.json()
    },

    async readFile(path: string, serverId?: string) {
        const base = serverId ? `/api/remote/${serverId}/files` : '/api/files'
        const res = await apiFetch(`${base}/read?path=${encodeURIComponent(path)}`)
        return res.json()
    },

    async writeFile(path: string, content: string, serverId?: string) {
        const base = serverId ? `/api/remote/${serverId}/files` : '/api/files'
        const res = await apiFetch(`${base}/write`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path, content })
        })
        return res.json()
    },

    // 用户相关
    async getUsers() {
        const res = await apiFetch('/api/users')
        return res.json()
    },

    async createUser(username: string, password: string, role: string) {
        const res = await apiFetch('/api/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, role })
        })
        return res.json()
    },

    async deleteUser(username: string) {
        const res = await apiFetch(`/api/users/${username}`, { method: 'DELETE' })
        return res.json()
    },

    // 配置相关
    async getConfig() {
        const res = await apiFetch('/api/config')
        return res.json()
    },

    async saveConfig(config: Record<string, unknown>) {
        const res = await apiFetch('/api/config', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        })
        return res.json()
    }
}
