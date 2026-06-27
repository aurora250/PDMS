<template>
  <div class="change-request-page">
    <div class="page-header"><h3>变更申请管理</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable @change="load">
            <el-option label="请求" value="请求" /><el-option label="一审" value="一审" />
            <el-option label="二审" value="二审" /><el-option label="通过" value="通过" />
            <el-option label="驳回" value="驳回" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button @click="load">刷新</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="rid" label="ID" width="60" />
        <el-table-column prop="applicantUuid" label="申请人" width="200" />
        <el-table-column prop="changeField" label="变更字段" width="120" />
        <el-table-column prop="originalData" label="原值" width="150" />
        <el-table-column prop="modifiedData" label="新值" width="150" />
        <el-table-column prop="status" label="状态" width="80" />
        <el-table-column prop="requestTime" label="申请时间" width="120" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="hasPermission('resident:change-request:approve') && row.status === '请求'" text size="small" type="success" @click="approve(row, '通过')">通过</el-button>
            <el-button v-if="hasPermission('resident:change-request:approve') && row.status === '请求'" text size="small" type="danger" @click="approve(row, '驳回')">驳回</el-button>
            <el-button v-if="hasPermission('resident:change-request:second-approve') && row.status === '一审'" text size="small" type="success" @click="approve(row, '通过')">二审通过</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,prev,pager,next" @current-change="load" @size-change="load" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { residentApi } from '@/api/resident'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'

const { hasPermission } = usePermission()
const list = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref('')
const page = reactive({ current: 1, size: 20, total: 0 })

async function load() {
  loading.value = true
  try {
    const res = await residentApi.listChangeRequests({
      status: statusFilter.value || undefined,
      page: page.current, size: page.size,
    })
    list.value = res.records || []
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function approve(row: any, status: string) {
  try {
    await residentApi.approveChangeRequest(row.rid, status)
    showSuccess(status === '通过' ? '已通过' : '已驳回')
    load()
  } catch (e: any) { showError(e.message || '操作失败') }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
