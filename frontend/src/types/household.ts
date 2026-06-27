/** 户籍业务 */
export interface HouseholdBusiness {
  rid?: number
  uuid: string
  businessType: string
  status: string
  applicantUuid?: string
  handlerIdList?: string
  attachment?: string
  createTime?: string
}

/** 户籍迁移 */
export interface HouseholdMigration {
  rid?: number
  uuid: string
  fromAddress: string
  toAddress: string
  status: string
  migrationType?: string
  createTime?: string
}

/** 户口簿 */
export interface HouseholdBook {
  bookNo: string
  householderUuid: string
  address: string
  memberCount?: number
}

/** 准迁证/迁移证 */
export interface HouseholdPermit {
  permitNo: string
  businessId?: number
  status: string
  issueDate?: string
}

/** 行政区划 */
export interface Area {
  areaId: number
  areaCode: string
  areaName: string
  parentId?: number
  areaLevel: string
}
