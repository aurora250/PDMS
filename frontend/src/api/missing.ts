import request from './request'

export const missingApi = {
  search: (params?: any, config?: any) => request.get('/missing/search', { params, ...config }),
  create: (data: any) => request.post('/missing', data),
  delete: (rid: number) => request.delete(`/missing/${rid}`),
  recovery: (data: any) => request.post('/missing/recovery', data),
  statistics: () => request.get('/missing/statistics'),
}
