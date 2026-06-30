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

/** UUID 格式校验 */
export function isValidUUID(uuid: string): boolean {
  if (!uuid) return false
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(uuid)
}
