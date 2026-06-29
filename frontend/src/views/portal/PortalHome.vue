<template>
  <div class="portal-home">
    <h3>欢迎，{{ auth.username }}</h3>
    <p class="subtitle">您可以通过本平台办理户籍业务、申报流动人口登记等。</p>

    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="6">
        <StatCard title="我的申请" :value="stats.total" icon="Document" color="primary" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="办理中" :value="stats.pending" icon="Clock" color="warning" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="已完成" :value="stats.done" icon="CircleCheck" color="success" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="已驳回" :value="stats.rejected" icon="CircleClose" color="danger" :loading="loading" />
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="8">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/portal/self-fp')">
          <el-icon :size="32"><Plus /></el-icon>
          <div class="quick-title">流动人口申报</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/portal/self-recovery')">
          <el-icon :size="32"><Search /></el-icon>
          <div class="quick-title">寻回线索登记</div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="quick-card" @click="$router.push('/portal/applications')">
          <el-icon :size="32"><List /></el-icon>
          <div class="quick-title">查看全部申请</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近申请 -->
    <el-card style="margin-top:20px">
      <template #header>最近申请记录</template>
      <el-table :data="myApps.slice(0, 5)" stripe v-loading="loading">
        <el-table-column prop="businessType" label="类型" width="120">
          <template #default="{ row }">{{ row.businessType || row.permitNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="handleDate" label="申请时间" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><ApprovalBadge :status="row.status || '未知'" /></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && myApps.length === 0" description="暂无申请记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { usePermission } from '@/composables/usePermission'
import { householdApi } from '@/api/household'
import { floatingApi } from '@/api/floating'
import StatCard from '@/components/StatCard.vue'
import ApprovalBadge from '@/components/ApprovalBadge.vue'

const auth = useAuthStore()
const { hasPermission } = usePermission()
const myApps = ref<any[]>([])
const loading = ref(false)

const stats = reactive({ total: 0, pending: 0, done: 0, rejected: 0 })

onMounted(async () => {
  loading.value = true
  try {
    const [biz, fp] = await Promise.all([
      householdApi.listBusiness().catch(() => []),
      floatingApi.listPermit().catch(() => []),
    ])
    const bizArr = Array.isArray(biz) ? biz : (biz?.records || [])
    const fpArr = Array.isArray(fp) ? fp : (fp?.records || [])
    myApps.value = [...bizArr, ...fpArr]

    stats.total = myApps.value.length
    stats.pending = myApps.value.filter(a => a.status && !['已批准', '已驳回', '已制发'].includes(a.status)).length
    stats.done = myApps.value.filter(a => a.status && ['已批准', '已制发'].includes(a.status)).length
    stats.rejected = myApps.value.filter(a => a.status === '已驳回').length
  } catch {
    myApps.value = []
  }
  finally { loading.value = false }
})
</script>

<style scoped>
.subtitle { color: #909399; margin: 4px 0 0 0; }
.quick-card { text-align: center; cursor: pointer; padding: 24px; transition: transform 0.2s; }
.quick-card:hover { transform: translateY(-2px); }
.quick-card .quick-title { margin-top: 12px; font-size: 16px; font-weight: 500; }
</style>
