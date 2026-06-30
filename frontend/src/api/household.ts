import request from './request'

export const householdApi = {
  // 户口簿
  searchBook: (params?: any) => request.get('/household/book/search', { params }),
  applyBook: (data: any) => request.post('/household/book/apply', data),
  reissueBook: (data: any) => request.post('/household/book/reissue', data),
  renewBook: (data: any) => request.post('/household/book/renew', data),
  /** 按居民UUID查询户口簿 */
  getBookByResident: (residentUuid: string) => request.get(`/household/book/by-resident/${residentUuid}`),
  // 业务
  listBusiness: (params?: any) => request.get('/household/business', { params }),
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
  getMigrationTrace: (uuid: string) => request.get(`/household/migration/trace/${uuid}`),
  // 准迁证
  listApprovalPermit: (params?: any) => request.get('/household/approval-permit', { params }),
  createApprovalPermit: (data: any) => request.post('/household/approval-permit', data),
  // 迁移证
  listMigrationPermit: (params?: any) => request.get('/household/migration-permit', { params }),
  createMigrationPermit: (data: any) => request.post('/household/migration-permit', data),
}
