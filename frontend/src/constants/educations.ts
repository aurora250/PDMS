/** GB/T 4658-2006 学历代码 */
export const EDUCATIONS = [
  { code: '10', name: '研究生' },
  { code: '20', name: '大学本科' },
  { code: '30', name: '大学专科' },
  { code: '40', name: '中等职业教育' },
  { code: '50', name: '技工学校' },
  { code: '60', name: '高中' },
  { code: '70', name: '初中' },
  { code: '80', name: '小学' },
  { code: '90', name: '文盲或半文盲' },
  { code: '99', name: '未知' },
]

export const EDUCATION_CODE_MAP: Record<string, string> = Object.fromEntries(
  EDUCATIONS.map(e => [e.code, e.name])
)
export const EDUCATION_NAME_MAP: Record<string, string> = Object.fromEntries(
  EDUCATIONS.map(e => [e.name, e.code])
)
