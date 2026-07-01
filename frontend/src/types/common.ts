/** 通用 API 响应 */
export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

/** 分页请求 */
export interface PageRequest {
  page: number
  size: number
}

/** 分页响应 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** 登录响应 */
export interface LoginResponse {
  accessToken: string
  refreshToken: string
  role: string
  residentUuid?: string
  permissions: string[]
  mustChangePassword: boolean
  username?: string
  userUuid?: string
}
