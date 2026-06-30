/** 常住人口 */
export interface Resident {
  uuid?: string
  name: string
  formerName?: string
  gender: string
  idCardNo: string
  nation: string
  nationCode?: string
  birthDate: string
  educationLevel: string
  educationCode?: string
  bloodType?: string
  maritalStatus: string
  occupation?: string
  phone: string
  photo?: string
  residence: string
  areaId?: number
  householdType: string
  householdStatus: string
  householdAddress: string
  householdAreaId?: number
}

/** 搜索请求 */
export interface ResidentSearchRequest {
  page: number
  size: number
  name?: string
  gender?: string
  nation?: string
  nationCode?: string
  educationLevel?: string
  educationCode?: string
  maritalStatus?: string
  householdStatus?: string
  province?: string
  idCardNo?: string
  minAge?: number
  maxAge?: number
}

/** 家庭关系 */
export interface ResidentRelation {
  relationPersonUuid: string
  fatherUuid?: string
  motherUuid?: string
  spouseUuid?: string
}

/** 家庭关系（含姓名+子女） */
export interface ResidentRelationVO {
  relationPersonUuid: string
  fatherUuid?: string
  fatherName?: string
  motherUuid?: string
  motherName?: string
  spouseUuid?: string
  spouseName?: string
  children?: Array<{ uuid: string; name: string; gender?: string }>
}

/** 变更请求 */
export interface ResidentChangeRequest {
  rid?: number
  applicantUuid: string
  changeField: string
  originalData: string
  modifiedData: string
  status?: string
  requestTime?: string
  handlerIdList?: string
}
