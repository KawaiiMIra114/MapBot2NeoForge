import { createRouter, createWebHashHistory } from 'vue-router'
import { getToken } from '../api/client'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/LoginPage.vue')
    },
    {
        path: '/',
        name: 'Dashboard',
        component: () => import('../views/DashboardPage.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/console',
        name: 'Console',
        component: () => import('../views/ConsolePage.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/files',
        name: 'Files',
        component: () => import('../views/FilesPage.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/servers',
        name: 'Servers',
        component: () => import('../views/ServersPage.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/settings',
        name: 'Settings',
        component: () => import('../views/SettingsPage.vue'),
        meta: { requiresAuth: true }
    }
]

const router = createRouter({
    history: createWebHashHistory(),
    routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
    if (to.meta.requiresAuth && !getToken()) {
        next('/login')
    } else {
        next()
    }
})

export default router
