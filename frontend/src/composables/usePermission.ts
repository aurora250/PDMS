import { useAuthStore } from '@/stores/auth'

export function usePermission() {
  const auth = useAuthStore()

  function hasPermission(perm: string): boolean {
    return auth.permissions.includes('*') || auth.permissions.includes(perm)
  }

  function hasAnyPermission(...perms: string[]): boolean {
    return perms.some(p => hasPermission(p))
  }

  function hasAllPermissions(...perms: string[]): boolean {
    return perms.every(p => hasPermission(p))
  }

  const canEdit   = (module: string) => hasPermission(`${module}:write`)
  const canRead   = (module: string) => hasPermission(`${module}:read`)
  const canDelete = (module: string) => hasPermission(`${module}:delete`)
  const canApprove = () => hasPermission('household:approve')
  const canSecondApprove = () => hasPermission('household:second-approve')
  const canAttachMaterial = () => hasPermission('household:material:attach')
  const canReview = () => hasAnyPermission('fp:review', 'keyperson:review', 'missing:review')

  return {
    hasPermission, hasAnyPermission, hasAllPermissions,
    canEdit, canRead, canDelete,
    canApprove, canSecondApprove, canAttachMaterial, canReview,
  }
}
