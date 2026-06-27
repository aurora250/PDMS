/** GB/T 2261.2-2003 婚姻状况代码 */
export const MARITAL_STATUSES = [
  { code: '10', name: '未婚' },
  { code: '20', name: '已婚' },
  { code: '21', name: '初婚' },
  { code: '22', name: '再婚' },
  { code: '23', name: '复婚' },
  { code: '30', name: '丧偶' },
  { code: '40', name: '离婚' },
  { code: '90', name: '未说明的婚姻状况' },
]

export const MARITAL_CODE_MAP: Record<string, string> = Object.fromEntries(
  MARITAL_STATUSES.map(m => [m.code, m.name])
)
