export { NATIONS, NATION_CODE_MAP, NATION_NAME_MAP } from '@/constants/nations'
export { EDUCATIONS, EDUCATION_CODE_MAP, EDUCATION_NAME_MAP } from '@/constants/educations'
export { MARITAL_STATUSES, MARITAL_CODE_MAP } from '@/constants/maritalStatuses'
export { BLOOD_TYPES } from '@/constants/bloodTypes'

/** 国标常量组合式函数 */
export function useGbConstants() {
  function nationName(code: string): string { return NATION_CODE_MAP[code] ?? code }
  function nationCode(name: string): string { return NATION_NAME_MAP[name] ?? '' }
  function educationName(code: string): string { return EDUCATION_CODE_MAP[code] ?? code }
  function educationCode(name: string): string { return EDUCATION_NAME_MAP[name] ?? '' }
  function maritalName(code: string): string { return MARITAL_CODE_MAP[code] ?? code }

  return {
    NATIONS, EDUCATIONS, MARITAL_STATUSES, BLOOD_TYPES,
    nationName, nationCode, educationName, educationCode, maritalName,
  }
}
