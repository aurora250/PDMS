/** 系统用户 */
export interface SysUser {
  userUuid: string
  username: string
  residentUuid: string
  userRole: string
  permissionGroupId: number
  phone: string
  accountStatus: string
  mustChangePassword: boolean
  registerMaterials: string
  createTime?: string
}

/** 民警 */
export interface Police {
  policeNo: string
  name: string
  policeRank: string
  station: string
  dutyStatus: string
  phone: string
  createTime?: string
}

/** 权限组 */
export interface PermissionGroup {
  groupId: number
  groupName: string
  description: string
  permissions: string
}
