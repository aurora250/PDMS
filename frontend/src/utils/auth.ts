import { ElMessage } from 'element-plus'
import type { LoginResponse } from '@/types/common'

/** 模拟登录 API（开发阶段），生产替换为真实 Axios 调用 */
export async function loginApi(username: string, password: string): Promise<LoginResponse> {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, password }),
  })
  const json = await res.json()
  if (json.code !== 200) throw new Error(json.message || '登录失败')
  return json.data
}

export async function refreshTokenApi(refreshToken: string): Promise<LoginResponse> {
  const res = await fetch('/api/auth/refresh', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken }),
  })
  const json = await res.json()
  if (json.code !== 200) throw new Error(json.message || '刷新失败')
  return json.data
}

export function showError(msg: string) {
  ElMessage.error(msg)
}

export function showSuccess(msg: string) {
  ElMessage.success(msg)
}
