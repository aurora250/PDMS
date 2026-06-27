/** 失踪人员 */
export interface MissingPerson {
  id?: number
  uuid: string
  name: string
  gender: string
  idCardNo?: string
  missingDate: string
  missingAddress: string
  status: string
}

/** 寻回记录 */
export interface MissingRecovery {
  rid?: number
  missingPersonUuid: string
  recoveryDate: string
  recoveryAddress: string
  status: string
}
