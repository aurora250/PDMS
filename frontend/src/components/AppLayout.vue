<template>
  <el-container class="app-layout">
    <el-aside :width="app.sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="logo">
        <svg class="logo-shield" viewBox="0 0 32 32" width="28" height="28" v-show="!app.sidebarCollapsed">
          <path d="M16 2 L28 6 L28 16 C28 22 16 30 16 30 C16 30 4 22 4 16 L4 6 Z" fill="#c9a84c" stroke="#d4b55a" stroke-width="1.2"/>
          <polygon points="16,8 18.5,13.5 24.5,14 20,18 21.5,23.5 16,20 10.5,23.5 12,18 7.5,14 13.5,13.5" fill="#0f1f38"/>
        </svg>
        <svg class="logo-shield-mini" viewBox="0 0 32 32" width="24" height="24" v-show="app.sidebarCollapsed">
          <path d="M16 2 L28 6 L28 16 C28 22 16 30 16 30 C16 30 4 22 4 16 L4 6 Z" fill="#c9a84c" stroke="#d4b55a" stroke-width="1.2"/>
          <polygon points="16,8 18.5,13.5 24.5,14 20,18 21.5,23.5 16,20 10.5,23.5 12,18 7.5,14 13.5,13.5" fill="#0f1f38"/>
        </svg>
        <div class="logo-text" v-show="!app.sidebarCollapsed">
          <span class="logo-title">人口数据库管理</span>
          <span class="logo-subtitle">人口数据库管理信息平台</span>
        </div>
      </div>
      <el-menu :default-active="route.path" :collapse="app.sidebarCollapsed" router background-color="#0f1f38" text-color="#b0c4da" active-text-color="#ffffff" class="sidebar-menu">
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
        <span class="topbar-title">人口数据库管理系统</span>
        <div class="flex-1" />
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            <svg class="police-badge" viewBox="0 0 16 16" width="16" height="16">
              <path d="M8 1 L14 3 L14 8 C14 11 8 15 8 15 C8 15 2 11 2 8 L2 3 Z" fill="#c9a84c" stroke="#d4b55a" stroke-width=".6"/>
            </svg>
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
import {ArrowDown, Expand, Fold} from "@element-plus/icons-vue";

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
  children?: { path: string; title: string; perm?: string }[]
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
  { path: '/system', title: '系统管理', icon: 'Setting', perm: 'auth:user:read',
    children: [
      { path: '/system/users', title: '用户管理', perm: 'auth:user:read' },
      { path: '/system/police', title: '民警管理', perm: 'police:read' },
      { path: '/system/permissions', title: '权限组', perm: 'auth:user:write' },
    ],
  },
]

const visibleMenus = computed(() => {
  return MENU_ITEMS
    .filter(item => {
      if (!item.perm) return true
      return hasPermission(item.perm)
    })
    .map(item => {
      if (item.children) {
        return {
          ...item,
          children: item.children.filter(child => {
            if (!child.perm) return true
            return hasPermission(child.perm)
          }),
        }
      }
      return item
    })
    .filter(item => !item.children || item.children.length > 0)
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

/* ── 侧栏 ── */
.sidebar {
  background: var(--pdm-sidebar);
  overflow-y: auto;
  overflow-x: hidden;
}
.sidebar .logo {
  color: #fff;
  text-align: center;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid rgba(255,255,255,.08);
}
.logo-shield, .logo-shield-mini {
  flex-shrink: 0;
}
.logo-text {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}
.logo-title {
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
  color: #ffffff;
}
.logo-subtitle {
  font-size: 11px;
  color: var(--pdm-accent);
  letter-spacing: 2px;
  font-weight: 500;
}

/* 侧栏菜单覆盖 */
.sidebar-menu {
  border-right: none !important;
}
.sidebar-menu .el-menu-item.is-active {
  background-color: var(--pdm-sidebar-active) !important;
}

/* ── 顶栏 ── */
.topbar {
  display: flex;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid var(--pdm-header-border);
  padding: 0 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,.06);
  z-index: 10;
}
.topbar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--pdm-primary);
  margin-left: 12px;
  letter-spacing: 1px;
}
.collapse-btn { cursor: pointer; color: #606266; }
.collapse-btn:hover { color: var(--pdm-primary); }
.flex-1 { flex: 1; }
.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  color: #303133;
  font-size: 13px;
}
.police-badge { vertical-align: middle; }
</style>
