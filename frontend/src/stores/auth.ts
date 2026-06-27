import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, refreshTokenApi } from '@/utils/auth'
import type { LoginResponse } from '@/types/common'

export const useAuthStore = defineStore('auth', () => {
  const token = ref('')
  const refreshToken = ref('')
  const role = ref('')
  const permissions = ref<string[]>([])
  const username = ref('')
  const mustChangePassword = ref(false)

  const isLoggedIn = computed(() => !!token.value)
  const isCitizen = computed(() => role.value === '普通用户')

  function setAuth(data: LoginResponse) {
    token.value = data.accessToken
    refreshToken.value = data.refreshToken
    role.value = data.role
    permissions.value = data.permissions || []
    mustChangePassword.value = data.mustChangePassword || false
  }

  async function login(user: string, pass: string) {
    const data = await loginApi(user, pass)
    username.value = user
    setAuth(data)
    return data
  }

  async function refresh() {
    if (!refreshToken.value) throw new Error('No refresh token')
    const data = await refreshTokenApi(refreshToken.value)
    setAuth(data)
  }

  function passwordChanged() {
    mustChangePassword.value = false
    persist()
  }

  function logout() {
    token.value = ''
    refreshToken.value = ''
    role.value = ''
    permissions.value = []
    username.value = ''
    mustChangePassword.value = false
  }

  /** 从 sessionStorage 恢复登录态 */
  function restore() {
    const saved = sessionStorage.getItem('pdm-auth')
    if (saved) {
      try {
        const data = JSON.parse(saved) as LoginResponse & { username: string }
        username.value = data.username
        setAuth(data)
        return true
      } catch { /* ignore */ }
    }
    return false
  }

  /** 持久化到 sessionStorage */
  function persist() {
    sessionStorage.setItem('pdm-auth', JSON.stringify({
      accessToken: token.value,
      refreshToken: refreshToken.value,
      role: role.value,
      permissions: permissions.value,
      mustChangePassword: mustChangePassword.value,
      username: username.value,
    }))
  }

  return { token, refreshToken, role, permissions, username, mustChangePassword,
           isLoggedIn, isCitizen, login, refresh, logout, passwordChanged, restore, persist }
})
