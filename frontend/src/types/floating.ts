/** 流动人口登记 */
export interface FpRegister {
  rid?: number
  uuid: string
  agentUuid?: string
  registerDate: string
  residencePermitNo?: string
  attachment?: string
}

/** 居住地 */
export interface FpResidence {
  rid?: number
  uuid: string
  currentAddress: string
  areaId?: number
  addressType?: string
  purpose: string
  expectedDuration: string
  workUnit?: string
  registerDate: string
}

/** 居住证 */
export interface ResidentPermit {
  id?: number
  permitNo?: string
  uuid: string
  issueDate?: string
  expiryDate?: string
  status: string
}
