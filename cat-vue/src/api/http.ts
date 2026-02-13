import axios from 'axios'
import { setActivePinia } from 'pinia'
import { pinia } from '../store'
import { useAuthStore } from '../store/auth'

const baseURL = import.meta.env.PROD
  ? 'http://localhost:8089/api'
  : '/api'

export const http = axios.create({
  baseURL,
  timeout: 20000
})

http.interceptors.request.use((config) => {
  setActivePinia(pinia)
  const auth = useAuthStore()
  if (auth.token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    // 统一处理后端 ApiResponse：code != 0 视为失败，抛出 message
    if (body && typeof body === 'object' && 'code' in body) {
      const code = (body as any).code
      if (code !== 0) {
        const msg = (body as any).message || '请求失败'
        const e: any = new Error(msg)
        e.code = code
        e.data = (body as any).data
        return Promise.reject(e)
      }
    }
    return body
  },
  (err) => {
    const body = err?.response?.data
    const msg = body?.message || err.message || '请求失败'
    const e: any = new Error(msg)
    if (body && typeof body === 'object' && 'code' in body) e.code = body.code
    return Promise.reject(e)
  }
)

