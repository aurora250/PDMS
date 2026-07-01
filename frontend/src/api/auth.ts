import request from './request'
import type { SysUser, Police, PermissionGroup } from '@/types/user'

/** 用户 */
export const userApi = {
  list: (params?: any) => request.get('/auth/users', { params }),
  create: (data: any) => request.post('/auth/users', data),
  update: (uuid: string, data: any) => request.put(`/auth/users/${uuid}`, data),
  updateStatus: (uuid: string, status: string) => request.put(`/auth/users/${uuid}/status`, { status }),
  resetPassword: (uuid: string, password: string) => request.put(`/auth/users/${uuid}/password`, { password }),
  delete: (uuid: string) => request.delete(`/auth/users/${uuid}`),
}

/** 民警 */
export const policeApi = {
  list: (params?: any, config?: any) => request.get('/auth/police', { params, ...config }),
  create: (data: any) => request.post('/auth/police', data),
  update: (no: string, data: any) => request.put(`/auth/police/${no}`, data),
  updateStatus: (no: string, dutyStatus: string) => request.put(`/auth/police/${no}/status`, { dutyStatus }),
}

/** 权限组 */
export const permissionGroupApi = {
  list: (config?: any) => request.get('/auth/permission-groups', config),
  create: (data: any) => request.post('/auth/permission-groups', data),
  update: (id: number, data: any) => request.put(`/auth/permission-groups/${id}`, data),
  delete: (id: number) => request.delete(`/auth/permission-groups/${id}`),
}
