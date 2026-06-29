import request from './request'

export const statisticsApi = {
  /** 按省份聚合常住人口数据（中国地图着色） */
  getProvincePopulation: () => request.get('/statistics/province-population'),
  /** 仪表盘概览数据 */
  getDashboard: () => request.get('/statistics/dashboard'),
  /** 户籍迁移流向数据（省份间流动） */
  getMigrationFlows: () => request.get('/statistics/migration-flows'),
  /** 某省份下各城市人口分布 */
  getCityPopulation: (province: string) => request.get('/statistics/city-population', { params: { province } }),
}
