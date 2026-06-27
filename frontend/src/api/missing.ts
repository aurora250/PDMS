import request from './request'

export const missingApi = {
  search: (params?: any) => request.get('/missing/search', { params }),
  create: (data: any) => request.post('/missing', data),
  delete: (rid: number) => request.delete(`/missing/${rid}`),
  recovery: (data: any) => request.post('/missing/recovery', data),
  statistics: () => request.get('/missing/statistics'),
}
