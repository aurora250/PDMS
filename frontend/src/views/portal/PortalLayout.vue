<template>
  <el-container class="portal-layout">
    <el-header class="portal-topbar">
      <h3>人口管理系统 · 群众服务平台</h3>
      <div class="flex-1" />
      <span>{{ auth.username }}</span>
      <el-button text @click="handleLogout">退出</el-button>
    </el-header>
    <el-main>
      <el-menu mode="horizontal" :default-active="route.path" router>
        <el-menu-item index="/portal">首页</el-menu-item>
        <el-menu-item index="/portal/applications">我的申请</el-menu-item>
        <el-menu-item index="/portal/self-fp">流动人口申报</el-menu-item>
        <el-menu-item index="/portal/self-recovery">寻回线索登记</el-menu-item>
      </el-menu>
      <div class="portal-content">
        <router-view />
      </div>
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.portal-layout { min-height: 100vh; }
.portal-topbar { display: flex; align-items: center; background: #1a365d; color: #fff; padding: 0 24px; }
.portal-topbar h3 { margin: 0; }
.flex-1 { flex: 1; }
.portal-content { padding: 24px; }
</style>
