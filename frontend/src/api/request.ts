import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

service.interceptors.request.use(config => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

service.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code !== 200) {
      if (!(response.config as any)?.silent) {
        ElMessage.error(message || '请求失败')
      }
      return Promise.reject(new Error(message))
    }
    return data
  },
  async error => {
    if (error.response?.status === 401) {
      const auth = useAuthStore()
      try {
        await auth.refresh()
        return service(error.config!)
      } catch {
        auth.logout()
        window.location.href = '/login'
      }
    }
    if (error.response?.status === 403) {
      if (!(error.config as any)?.silent) {
        ElMessage.error('权限不足')
      }
    }
    if (error.response?.status >= 500) {
      if (!(error.config as any)?.silent) {
        ElMessage.error('服务器异常')
      }
    }
    return Promise.reject(error)
  }
)

/** Typed wrapper — interceptors unwrap response.data, so return type is the actual data */
const request = {
  get: (url: string, config?: any): any => service.get(url, config),
  post: (url: string, data?: any, config?: any): any => service.post(url, data, config),
  put: (url: string, data?: any, config?: any): any => service.put(url, data, config),
  delete: (url: string, config?: any): any => service.delete(url, config),
}

export default request
