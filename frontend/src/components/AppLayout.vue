<template>
  <el-container class="app-layout">
    <el-aside :width="app.sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">PDM</div>
      <el-menu :default-active="route.path" :collapse="app.sidebarCollapsed" router>
        <template v-for="item in visibleMenus" :key="item.path">
          <el-sub-menu v-if="item.children" :index="item.path">
            <template #title>
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.title }}</span>
            </template>
            <el-menu-item v-for="c in item.children" :key="c.path" :index="c.path">
              {{ c.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <el-icon class="collapse-btn" @click="app.toggleSidebar()" :size="20">
          <Expand v-if="app.sidebarCollapsed" /><Fold v-else />
        </el-icon>
        <div class="flex-1" />
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            {{ auth.username }} ({{ auth.role }})
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="changePwd">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main>
        <router-view />
      </el-main>
    </el-container>
    <ChangePasswordDialog ref="changePwdDialogRef" />
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { usePermission } from '@/composables/usePermission'
import ChangePasswordDialog from '@/components/ChangePasswordDialog.vue'

const auth = useAuthStore()
const app = useAppStore()
const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()

interface MenuItem {
  path: string
  title: string
  icon: string
  perm?: string
  children?: { path: string; title: string }[]
}

const MENU_ITEMS: MenuItem[] = [
  { path: '/dashboard', title: '仪表盘', icon: 'DataBoard', perm: 'statistics:read' },
  {
    path: '/resident', title: '常住人口', icon: 'User', perm: 'resident:read',
    children: [
      { path: '/resident', title: '人口列表' },
      { path: '/resident/change-request', title: '变更审批' },
    ],
  },
  {
    path: '/household', title: '户籍管理', icon: 'Notebook', perm: 'household:read',
    children: [
      { path: '/household/book', title: '户口簿管理' },
      { path: '/household/business', title: '户籍业务' },
      { path: '/household/migration', title: '迁移管理' },
      { path: '/household/permit', title: '证件管理' },
    ],
  },
  {
    path: '/floating', title: '流动人口', icon: 'Ship', perm: 'fp:read',
    children: [
      { path: '/floating/register', title: '流动登记' },
      { path: '/floating/residence', title: '居住地' },
      { path: '/floating/permit', title: '居住证' },
      { path: '/floating/statistics', title: '统计图表' },
    ],
  },
  {
    path: '/keyperson', title: '重点人员', icon: 'Warning', perm: 'keyperson:read',
    children: [
      { path: '/keyperson/list', title: '人员列表' },
      { path: '/keyperson/visit', title: '走访计划' },
      { path: '/keyperson/petition', title: '信访记录' },
      { path: '/keyperson/gis', title: 'GIS地图' },
    ],
  },
  {
    path: '/missing', title: '失踪人口', icon: 'Search', perm: 'missing:read',
    children: [
      { path: '/missing/list', title: '失踪列表' },
      { path: '/missing/statistics', title: '统计' },
    ],
  },
  { path: '/alert', title: '预警中心', icon: 'Bell', perm: 'alert:read' },
  {
    path: '/log', title: '日志审计', icon: 'Document', perm: 'log:audit:read',
    children: [
      { path: '/log/audit', title: '审计日志' },
      { path: '/log/login', title: '登录日志' },
    ],
  },
  { path: '/system', title: '系统管理', icon: 'Setting',
    children: [
      { path: '/system/users', title: '用户管理' },
      { path: '/system/police', title: '民警管理' },
      { path: '/system/permissions', title: '权限组' },
    ],
  },
]

const visibleMenus = computed(() => {
  return MENU_ITEMS.filter(item => {
    if (!item.perm) return true
    return hasPermission(item.perm)
  })
})

const changePwdDialogRef = ref()

function handleCommand(cmd: string) {
  if (cmd === 'logout') {
    auth.logout()
    router.push('/login')
  } else if (cmd === 'changePwd') {
    changePwdDialogRef.value?.open()
  }
}
</script>

<style scoped>
.app-layout { height: 100vh; }
.sidebar { background: #1d1e2c; overflow-y: auto; }
.sidebar .logo { color: #fff; text-align: center; padding: 16px; font-size: 20px; font-weight: bold; }
.topbar { display: flex; align-items: center; background: #fff; border-bottom: 1px solid #e4e7ed; padding: 0 16px; }
.collapse-btn { cursor: pointer; }
.flex-1 { flex: 1; }
.user-info { cursor: pointer; display: flex; align-items: center; gap: 4px; }
</style>
