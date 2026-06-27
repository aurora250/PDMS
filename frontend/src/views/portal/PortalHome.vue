<template>
  <div class="portal-home">
    <h3>欢迎，{{ auth.username }}</h3>
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="8"><el-card shadow="hover"><el-statistic title="我的申请" :value="myApps.length" /></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover"><el-statistic title="办理中" :value="myApps.filter(a=>a.status!=='已批准'&&a.status!=='驳回').length" /></el-card></el-col>
      <el-col :span="8"><el-card shadow="hover"><el-statistic title="已完成" :value="myApps.filter(a=>a.status==='已批准').length" /></el-card></el-col>
    </el-row>

    <el-card style="margin-top:20px">
      <template #header>我的申请记录</template>
      <el-table :data="myApps" stripe>
        <el-table-column prop="businessType" label="类型" width="120" />
        <el-table-column prop="createTime" label="申请时间" width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{row}"><el-tag :type="row.status==='已批准'?'success':row.status==='驳回'?'danger':'warning'">{{ row.status }}</el-tag></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="myApps.length===0" description="暂无申请记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { householdApi } from '@/api/household'
import { floatingApi } from '@/api/floating'

const auth = useAuthStore()
const myApps = ref<any[]>([])

onMounted(async () => {
  try {
    const [biz, fp] = await Promise.all([
      householdApi.listBusiness().catch(() => []),
      floatingApi.listPermit().catch(() => []),
    ])
    myApps.value = [...(Array.isArray(biz) ? biz : []), ...(Array.isArray(fp) ? fp : [])]
  } catch { myApps.value = [] }
})
</script>
