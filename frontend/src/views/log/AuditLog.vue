<template>
  <div>
    <div class="page-header">
      <h3>审计日志</h3>
      <el-button v-if="hasPermission('log:export')" @click="handleExport">导出</el-button>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="操作人">
          <el-input v-model="filters.operatorUuid" placeholder="操作人UUID" clearable />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="filters.operationType" placeholder="全部" clearable>
            <el-option label="新增" value="新增" /><el-option label="修改" value="修改" /><el-option label="删除" value="删除" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="dateRange" type="datetimerange" range-separator="至"
            start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="load">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="operatorUuid" label="操作人" width="200" show-overflow-tooltip />
        <el-table-column prop="operationType" label="操作类型" width="80" />
        <el-table-column prop="targetType" label="目标类型" width="120" />
        <el-table-column prop="targetId" label="目标ID" width="120" />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column prop="operationTime" label="操作时间" width="170" />
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
import { logApi } from '@/api/log'
import { usePermission } from '@/composables/usePermission'
import { useExport } from '@/composables/useExport'

const { hasPermission } = usePermission()
const { doExport } = useExport()
const list = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20, total: 0 })
const dateRange = ref<[string, string] | null>(null)

const filters = reactive({
  operatorUuid: '',
  operationType: '',
  startTime: '',
  endTime: '',
})

async function load() {
  loading.value = true
  try {
    const res = await logApi.audit({
      operatorUuid: filters.operatorUuid || undefined,
      operationType: filters.operationType || undefined,
      startTime: dateRange.value?.[0] || undefined,
      endTime: dateRange.value?.[1] || undefined,
      page: page.current, size: page.size,
    })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function resetFilters() {
  filters.operatorUuid = ''
  filters.operationType = ''
  dateRange.value = null
  page.current = 1
  load()
}

async function handleExport() {
  try {
    await doExport(
      () => logApi.export({
        operatorUuid: filters.operatorUuid || undefined,
        operationType: filters.operationType || undefined,
        startTime: dateRange.value?.[0] || undefined,
        endTime: dateRange.value?.[1] || undefined,
      }),
      'audit-logs.xlsx'
    )
  } catch { /* ignore */ }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
</style>
