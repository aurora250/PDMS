import request from './request'

export const floatingApi = {
  // 流动人口登记
  listRegister: (params?: any) => request.get('/fp/register', { params }),
  createRegister: (data: any) => request.post('/fp/register', data),
  updateRegister: (id: number, data: any) => request.put(`/fp/register/${id}`, data),
  deleteRegister: (id: number) => request.delete(`/fp/register/${id}`),
  // 居住地
  listResidence: (params?: any) => request.get('/fp/residence', { params }),
  createResidence: (data: any) => request.post('/fp/residence/register', data),
  updateResidence: (id: number, data: any) => request.put(`/fp/residence/${id}`, data),
  deleteResidence: (id: number) => request.delete(`/fp/residence/${id}`),
  // 居住证
  listPermit: (params?: any) => request.get('/fp/permit', { params }),
  applyPermit: (data: any) => request.post('/fp/permit/apply', data),
  renewPermit: (id: number, data: any) => request.post(`/fp/permit/${id}/renew`, data),
  approvePermit: (id: number) => request.put(`/fp/permit/${id}/approve`),
  issuePermit: (id: number, data?: any) => request.put(`/fp/permit/${id}/issue`, data),
  // 统计
  heatmap: () => request.get('/fp/statistics/heatmap'),
  trend: () => request.get('/fp/statistics/trend'),
}
