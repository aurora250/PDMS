import request from './request'

export const logApi = {
  audit: (params?: any) => request.get('/log/audit', { params }),
  login: (params?: any) => request.get('/log/login', { params }),
  export: (params?: any) => request.get('/log/export', { params, responseType: 'blob' }),
}
