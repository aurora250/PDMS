import request from './request'

export const householdApi = {
  // 户口簿
  searchBook: (params?: any) => request.get('/household/book/search', { params }),
  applyBook: (data: any) => request.post('/household/book/apply', data),
  reissueBook: (data: any) => request.post('/household/book/reissue', data),
  renewBook: (data: any) => request.post('/household/book/renew', data),
  /** 按居民UUID查询户口簿 */
  getBookByResident: (residentUuid: string, config?: any) => request.get(`/household/book/by-resident/${residentUuid}`, config),
  // 业务
  listBusiness: (params?: any, config?: any) => request.get('/household/business', { params, ...config }),
  createBusiness: (data: any) => request.post('/household/business', data),
  approveBusiness: (rid: number, status: string, rejectReason?: string) =>
    request.put(`/household/business/${rid}/approve`, { status, rejectReason }),
  /** 附加审核材料（街道办） */
  attachBusinessMaterial: (rid: number, data: FormData) =>
    request.post(`/household/business/${rid}/material`, data, { headers: { 'Content-Type': 'multipart/form-data' } }),
  // 迁移
  listMigration: (params?: any) => request.get('/household/migration', { params }),
  createMigration: (data: any) => request.post('/household/migration', data),
  approveMigration: (rid: number, status: string, rejectReason?: string) =>
    request.put(`/household/migration/${rid}/approve`, { status, rejectReason }),
  /** 附加审核材料（街道办） */
  attachMigrationMaterial: (rid: number, data: FormData) =>
    request.post(`/household/migration/${rid}/material`, data, { headers: { 'Content-Type': 'multipart/form-data' } }),
  getMigrationTrace: (uuid: string, config?: any) => request.get(`/household/migration/trace/${uuid}`, config),
  // 准迁证
  listApprovalPermit: (params?: any, config?: any) => request.get('/household/approval-permit', { params, ...config }),
  createApprovalPermit: (data: any) => request.post('/household/approval-permit', data),
  voidApprovalPermit: (id: number) => request.put(`/household/approval-permit/${id}/void`),
  // 迁移证
  listMigrationPermit: (params?: any, config?: any) => request.get('/household/migration-permit', { params, ...config }),
  createMigrationPermit: (data: any) => request.post('/household/migration-permit', data),
  voidMigrationPermit: (id: number) => request.put(`/household/migration-permit/${id}/void`),
}
