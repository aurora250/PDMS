import request from './request'

export const alertApi = {
  pending: () => request.get('/alert/pending'),
  search: (params?: any) => request.get('/alert/search', { params }),
  handle: (id: number, handledBy: string) => request.put(`/alert/${id}/handle`, null, { params: { handledBy } }),
}
