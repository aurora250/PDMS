/**
 * 地理坐标工具 — 省级行政区中心坐标。
 * 用于迁移轨迹地图中根据地址匹配省份坐标。
 * area 表目前无经纬度，采用省级省会城市坐标作为默认值。
 */

export const PROVINCE_COORDS: Record<string, [number, number]> = {
  '北京市': [116.46, 39.92],
  '天津市': [117.20, 39.13],
  '河北省': [114.48, 38.03],
  '山西省': [112.53, 37.87],
  '内蒙古自治区': [111.65, 40.82],
  '辽宁省': [123.38, 41.80],
  '吉林省': [125.35, 43.88],
  '黑龙江省': [126.63, 45.75],
  '上海市': [121.48, 31.22],
  '江苏省': [118.78, 32.05],
  '浙江省': [120.19, 30.26],
  '安徽省': [117.27, 31.86],
  '福建省': [119.30, 26.08],
  '江西省': [115.89, 28.68],
  '山东省': [117.00, 36.67],
  '河南省': [113.65, 34.76],
  '湖北省': [114.31, 30.52],
  '湖南省': [112.98, 28.19],
  '广东省': [113.23, 23.16],
  '广西壮族自治区': [108.33, 22.84],
  '海南省': [110.35, 20.02],
  '重庆市': [106.54, 29.59],
  '四川省': [104.06, 30.67],
  '贵州省': [106.71, 26.57],
  '云南省': [102.73, 25.04],
  '西藏自治区': [91.11, 29.97],
  '陕西省': [108.95, 34.27],
  '甘肃省': [103.73, 36.03],
  '青海省': [101.74, 36.56],
  '宁夏回族自治区': [106.27, 38.47],
  '新疆维吾尔自治区': [87.68, 43.77],
  '台湾省': [121.50, 25.05],
  '香港特别行政区': [114.17, 22.28],
  '澳门特别行政区': [113.55, 22.19],
}

/** 从地址文本中提取省份关键词并返回坐标 */
export function resolveCoord(address: string): [number, number] | null {
  if (!address) return null
  for (const [province, coord] of Object.entries(PROVINCE_COORDS)) {
    if (address.includes(province) || address.includes(province.substring(0, 2))) {
      return coord
    }
  }
  return null
}

/** 从地址文本中提取省份名称 */
export function extractProvince(address: string): string | null {
  if (!address) return null
  // 优先完整名称匹配（如 "广东省广州市..." startsWith "广东省"）
  for (const prov of Object.keys(PROVINCE_COORDS)) {
    if (address.startsWith(prov)) return prov
  }
  // 简写匹配：去后缀后比前2字（如 "广东..." startsWith "广东"）
  for (const prov of Object.keys(PROVINCE_COORDS)) {
    const short = prov.replace(/省|市|自治区|壮族|回族|维吾尔/g, '')
    if (address.startsWith(short)) return prov
  }
  // 直辖市/特别行政区简称匹配
  const shortMap: Record<string, string> = {
    '北京': '北京市', '上海': '上海市', '天津': '天津市', '重庆': '重庆市',
    '香港': '香港特别行政区', '澳门': '澳门特别行政区',
    '内蒙古': '内蒙古自治区', '广西': '广西壮族自治区',
    '西藏': '西藏自治区', '新疆': '新疆维吾尔自治区',
    '宁夏': '宁夏回族自治区',
  }
  for (const [key, full] of Object.entries(shortMap)) {
    if (address.startsWith(key)) return full
  }
  return null
}
