import request from './request'

export const keypersonApi = {
  search: (params?: any, config?: any) => request.get('/keyperson/search', { params, ...config }),
  create: (data: any) => request.post('/keyperson/', data),
  update: (uuid: string, data: any) => request.put(`/keyperson/${uuid}`, data),
  delete: (uuid: string) => request.delete(`/keyperson/${uuid}`),
  // 走访计划
  listVisitPlan: (params?: any) => request.get('/keyperson/visit-plan', { params }),
  createVisitPlan: (data: any) => request.post('/keyperson/visit-plan', data),
  updateVisitPlan: (id: number, data: any) => request.put(`/keyperson/visit-plan/${id}`, data),
  // 信访
  listPetition: (params?: any) => request.get('/keyperson/petition', { params }),
  createPetition: (data: any) => request.post('/keyperson/petition', data),
}
