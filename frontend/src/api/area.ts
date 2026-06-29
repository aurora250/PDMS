import request from './request'

export const areaApi = {
  /** 按 parentId 获取下级行政区划，parentId 为空时获取省级 */
  list: (parentId?: number) => request.get('/area', { params: { parentId } }),

  /** 获取区域祖先链（省→市→区，从根到叶） */
  getAncestors: (areaId: number) => request.get(`/area/${areaId}/ancestors`),

  /** 获取区域完整路径字符串（省+市+区） */
  getPath: (areaId: number) => request.get(`/area/${areaId}/path`),

  /** 获取全部区域数据（扁平列表，前端自行构建级联树） */
  tree: () => request.get('/area/tree'),
}
