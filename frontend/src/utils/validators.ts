/**
 * 前端格式校验工具
 * - ID card: GB 11643-1999 校验码
 * - Phone: 中国大陆手机号 1xx-xxxx-xxxx
 */

/** 身份证号校验 (GB 11643-1999) */
export function isValidIdCard(id: string): boolean {
  if (!id || id.length !== 18) return false
  // 前17位必须为数字
  if (!/^\d{17}[\dXx]$/.test(id)) return false
  // 出生日期校验
  const birth = id.substring(6, 14)
  const year = parseInt(birth.substring(0, 4), 10)
  const month = parseInt(birth.substring(4, 6), 10)
  const day = parseInt(birth.substring(6, 8), 10)
  if (year < 1900 || year > 2100 || month < 1 || month > 12 || day < 1) return false
  const daysInMonth = new Date(year, month, 0).getDate()
  if (day > daysInMonth) return false
  // 校验码
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
  const checkCodes = '10X98765432'
  let sum = 0
  for (let i = 0; i < 17; i++) sum += parseInt(id[i], 10) * weights[i]
  const expected = checkCodes[sum % 11]
  return id[17].toUpperCase() === expected
}

export const idCardRule = {
  validator: (_rule: any, value: string, cb: (err?: Error) => void) => {
    if (!value || !isValidIdCard(value)) cb(new Error('请输入合法的18位身份证号'))
    else cb()
  },
  trigger: 'blur',
}

/** 手机号校验（中国大陆） */
export function isValidPhone(phone: string): boolean {
  if (!phone) return false
  return /^1[3-9]\d{9}$/.test(phone)
}

export const phoneRule = {
  pattern: /^1[3-9]\d{9}$/,
  message: '请输入正确的11位手机号',
  trigger: 'blur',
}

/** 邮箱格式校验 */
export function isValidEmail(email: string): boolean {
  if (!email) return false
  return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)
}

export const emailRule = {
  pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
  message: '请输入正确的邮箱地址',
  trigger: 'blur',
}

/** UUID 格式校验 */
export function isValidUUID(uuid: string): boolean {
  if (!uuid) return false
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(uuid)
}

export const uuidRule = {
  pattern: /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  message: '请输入合法的UUID格式',
  trigger: 'blur',
}

/** 警号格式校验 (P + 8位数字) */
export function isValidPoliceNo(no: string): boolean {
  if (!no) return false
  return /^P\d{8}$/.test(no)
}

export const policeNoRule = {
  pattern: /^P\d{8}$/,
  message: '警号格式应为P开头+8位数字，如 P20260001',
  trigger: 'blur',
}

/** 中文姓名校验 (2-20个中文字符) */
export function isValidChineseName(name: string): boolean {
  if (!name) return false
  return /^[一-龥·]{2,20}$/.test(name)
}

export const chineseNameRule = {
  pattern: /^[一-龥·]{2,20}$/,
  message: '请输入2-20位中文姓名',
  trigger: 'blur',
}

/** 密码强度校验 (6-20位) */
export const passwordRule = {
  min: 6,
  max: 20,
  message: '密码长度需在6-20位之间',
  trigger: 'blur',
}

/** 通用必填提示 */
export const requiredMsg = (label: string) => `请输入${label}`
export const selectRequiredMsg = (label: string) => `请选择${label}`
