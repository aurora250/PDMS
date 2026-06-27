/** 重点人员 */
export interface KeyPerson {
  id?: number
  uuid: string
  controlLevel: string
  controlType: string
  designatedAt?: string
  revokedAt?: string
  responsiblePoliceNo: string
}

/** 走访计划 */
export interface VisitPlan {
  rid?: number
  keyPersonUuid: string
  planDate: string
  visitDate?: string
  status: string
  remark?: string
}

/** 信访记录 */
export interface PetitionRecord {
  rid?: number
  keyPersonUuid: string
  petitionDate: string
  content: string
  result?: string
}
