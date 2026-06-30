import request from './request'
import type { Resident, ResidentSearchRequest, ResidentRelation, ResidentRelationVO, ResidentChangeRequest } from '@/types/resident'

export const residentApi = {
  search: (data: ResidentSearchRequest) => request.post('/resident/search', data),
  getByUuid: (uuid: string) => request.get(`/resident/${uuid}`),
  create: (data: Resident) => request.post('/resident', data),
  update: (uuid: string, data: Partial<Resident>) => request.put(`/resident/${uuid}`, data),
  delete: (uuid: string) => request.delete(`/resident/${uuid}`),
  getRelations: (uuid: string) => request.get(`/resident/${uuid}/relations`),
  /** 获取关系（含姓名+子女） */
  getRelationsDetail: (uuid: string): Promise<ResidentRelationVO> => request.get(`/resident/${uuid}/relations-detail`),
  /** 获取子女列表 */
  getChildren: (uuid: string) => request.get(`/resident/${uuid}/children`),
  setRelations: (uuid: string, data: ResidentRelation) => request.post(`/resident/${uuid}/relations`, data),
  export: (params?: any) => request.get('/resident/export', { params, responseType: 'blob' }),
  import: (formData: FormData) => request.post('/resident/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } }),
  listChangeRequests: (params?: any) => request.get('/resident/change-request', { params }),
  submitChangeRequest: (data: ResidentChangeRequest) => request.post('/resident/change-request', data),
  approveChangeRequest: (rid: number, status: string) => request.put(`/resident/change-request/${rid}/approve?status=${status}`),
}
