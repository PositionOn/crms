import { createPinia } from 'pinia'

// 单例 Pinia，便于在路由守卫/axios 拦截器中使用
export const pinia = createPinia()

