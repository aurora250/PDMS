import request from './request'

export const areaApi = {
  /** 按 parentId 获取下级行政区划，parentId 为空时获取省级 */
  list: (parentId?: number) => request.get('/area', { params: { parentId } }),
}
