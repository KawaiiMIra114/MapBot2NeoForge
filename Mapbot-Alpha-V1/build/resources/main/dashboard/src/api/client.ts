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

function resolveAppBasePath(): string {
    const pathname = window.location.pathname || '/'
    const vueIndex = pathname.indexOf('/vue')
    if (vueIndex >= 0) {
        return '/vue/'
    }
    return '/'
}

function redirectToLogin() {
    clearToken()
    const loginUrl = `${resolveAppBasePath()}#/login`
    if (`${window.location.pathname}${window.location.hash}` !== loginUrl) {
        window.location.replace(loginUrl)
    }
}

async function parseResponseData(res: Response): Promise<unknown> {
    const text = await res.text()
    if (!text) return null
    try {
        return JSON.parse(text)
    } catch {
        return text
    }
}

function extractErrorMessage(data: unknown, fallback: string): string {
    if (typeof data === 'string' && data.trim()) return data
    if (data && typeof data === 'object' && 'error' in data) {
        const error = (data as { error?: unknown }).error
        if (typeof error === 'string' && error.trim()) return error
    }
    return fallback
}

async function requestJson(url: string, options: RequestInit = {}, requireSuccess = false): Promise<any> {
    const res = await apiFetch(url, options)
    const data = await parseResponseData(res)

    if (res.status === 401 || res.status === 403) {
        redirectToLogin()
        throw new Error(extractErrorMessage(data, '登录已过期，请重新登录'))
    }

    if (!res.ok) {
        throw new Error(extractErrorMessage(data, `请求失败 (${res.status})`))
    }

    if (requireSuccess && data && typeof data === 'object' && 'success' in data) {
        const success = (data as { success?: unknown }).success
        if (success === false) {
            throw new Error(extractErrorMessage(data, '操作失败'))
        }
    }

    return data
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
        return requestJson('/api/servers')
    },

    async getStatus() {
        return requestJson('/api/status')
    },

    async getMetricsHistory(serverId: string) {
        return requestJson(`/api/metrics/${serverId}/history`)
    },

    async sendCommand(serverId: string, command: string) {
        return requestJson(`/api/servers/${serverId}/command`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ command })
        })
    },

    // 文件相关
    async listFiles(path: string, serverId?: string) {
        const base = serverId ? `/api/remote/${serverId}/files` : '/api/files'
        return requestJson(`${base}/list?path=${encodeURIComponent(path)}`)
    },

    async readFile(path: string, serverId?: string) {
        const base = serverId ? `/api/remote/${serverId}/files` : '/api/files'
        return requestJson(`${base}/read?path=${encodeURIComponent(path)}`)
    },

    async writeFile(path: string, content: string, serverId?: string) {
        const base = serverId ? `/api/remote/${serverId}/files` : '/api/files'
        return requestJson(`${base}/write`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path, content })
        }, true)
    },

    async mkdir(path: string, serverId?: string) {
        const base = serverId ? `/api/remote/${serverId}/files` : '/api/files'
        return requestJson(`${base}/mkdir`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path })
        }, true)
    },

    async uploadFile(path: string, content: string, encoding = 'utf-8', serverId?: string) {
        const base = serverId ? `/api/remote/${serverId}/files` : '/api/files'
        return requestJson(`${base}/upload`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ path, content, encoding })
        }, true)
    },

    // 用户相关
    async getUsers() {
        return requestJson('/api/users')
    },

    async createUser(username: string, password: string, role: string) {
        return requestJson('/api/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, role })
        })
    },

    async deleteUser(username: string) {
        return requestJson(`/api/users/${username}`, { method: 'DELETE' })
    },

    // 配置相关
    async getConfig() {
        return requestJson('/api/config')
    },

    async saveConfig(config: Record<string, unknown>) {
        return requestJson('/api/config', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        })
    }
}
