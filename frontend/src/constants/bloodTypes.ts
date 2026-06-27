export const BLOOD_TYPES = ['A', 'B', 'AB', 'O', '未知'] as const
export type BloodType = (typeof BLOOD_TYPES)[number]
