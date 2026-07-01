import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/login/LoginPage.vue'),
      meta: { public: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/login/RegisterPage.vue'),
      meta: { public: true },
    },
    {
      path: '/portal',
      component: () => import('@/views/portal/PortalLayout.vue'),
      children: [
        { path: '', name: 'PortalHome', component: () => import('@/views/portal/PortalHome.vue') },
        { path: 'applications', name: 'MyApplications', component: () => import('@/views/portal/MyApplications.vue') },
        { path: 'self-fp', name: 'SelfRegisterFp', component: () => import('@/views/portal/SelfRegisterFp.vue') },
        { path: 'self-recovery', name: 'SelfReportRecovery', component: () => import('@/views/portal/SelfReportRecovery.vue') },
      ],
    },
    {
      path: '/',
      component: () => import('@/components/AppLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'Dashboard', component: () => import('@/views/dashboard/DashboardPage.vue') },
        { path: 'resident', name: 'ResidentList', component: () => import('@/views/resident/ResidentList.vue') },
        { path: 'resident/:uuid', name: 'ResidentDetail', component: () => import('@/views/resident/ResidentDetailPage.vue') },
        { path: 'resident/:uuid/relations', name: 'ResidentRelations', component: () => import('@/views/resident/ResidentRelations.vue') },
        { path: 'resident/change-request', name: 'ChangeRequestList', component: () => import('@/views/resident/ChangeRequestList.vue') },
        { path: 'household', redirect: '/household/book' },
        { path: 'household/book', name: 'BookManagement', component: () => import('@/views/household/BookManagement.vue') },
        { path: 'household/business', name: 'BusinessList', component: () => import('@/views/household/BusinessList.vue') },
        { path: 'household/migration', name: 'MigrationList', component: () => import('@/views/household/MigrationList.vue') },
        { path: 'household/migration/:uuid/trace', name: 'MigrationTrace', component: () => import('@/views/household/MigrationTrace.vue') },
        { path: 'household/permit', name: 'PermitManagement', component: () => import('@/views/household/PermitManagement.vue') },
        { path: 'floating', redirect: '/floating/register' },
        { path: 'floating/register', name: 'FpRegisterList', component: () => import('@/views/floating/FpRegisterList.vue') },
        { path: 'floating/residence', name: 'ResidenceList', component: () => import('@/views/floating/ResidenceList.vue') },
        { path: 'floating/permit', name: 'FpPermitList', component: () => import('@/views/floating/PermitList.vue') },
        { path: 'floating/statistics', name: 'FpStatistics', component: () => import('@/views/floating/FpStatistics.vue') },
        { path: 'keyperson', redirect: '/keyperson/list' },
        { path: 'keyperson/list', name: 'KeyPersonList', component: () => import('@/views/keyperson/KeyPersonList.vue') },
        { path: 'keyperson/visit', name: 'VisitPlan', component: () => import('@/views/keyperson/VisitPlan.vue') },
        { path: 'keyperson/petition', name: 'PetitionRecord', component: () => import('@/views/keyperson/PetitionRecord.vue') },
        { path: 'missing', redirect: '/missing/list' },
        { path: 'missing/list', name: 'MissingList', component: () => import('@/views/missing/MissingList.vue') },
        { path: 'missing/statistics', name: 'MissingStatistics', component: () => import('@/views/missing/MissingStatistics.vue') },
        { path: 'log', redirect: '/log/audit' },
        { path: 'log/audit', name: 'AuditLog', component: () => import('@/views/log/AuditLog.vue') },
        { path: 'log/login', name: 'LoginLog', component: () => import('@/views/log/LoginLog.vue') },
        { path: 'alert', name: 'AlertList', component: () => import('@/views/alert/AlertList.vue') },
        { path: 'system/users', name: 'UserList', component: () => import('@/views/system/UserList.vue') },
        { path: 'system/police', name: 'PoliceList', component: () => import('@/views/system/PoliceList.vue') },
        { path: 'system/permissions', name: 'PermissionGroup', component: () => import('@/views/system/PermissionGroup.vue') },
      ],
    },
  ],
})

router.beforeEach((to, _from, next) => {
  const auth = useAuthStore()

  // 公开页面
  if (to.meta.public) return next()

  // 未登录
  if (!auth.token && !auth.restore()) return next('/login')
  auth.persist()

  // 强制改密
  if (auth.mustChangePassword && to.path !== '/login') {
    // 允许 pass through for now, login page handles the dialog
  }

  // 群众 → 强制门户
  if (auth.role === '普通用户' && !to.path.startsWith('/portal')) {
    return next('/portal')
  }

  // 非群众 → 不能进门户
  if (auth.role !== '普通用户' && to.path.startsWith('/portal')) {
    return next('/dashboard')
  }

  next()
})

export default router
