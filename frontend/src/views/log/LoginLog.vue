<template>
  <div>
    <div class="page-header"><h3>登录日志</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="用户UUID">
          <el-input v-model="filters.userUuid" placeholder="用户UUID" clearable />
        </el-form-item>
        <el-form-item label="结果">
          <el-select v-model="filters.isSuccess" placeholder="全部" clearable>
            <el-option label="成功" :value="1" /><el-option label="失败" :value="0" />
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
        <el-table-column prop="userUuid" label="用户UUID" width="200" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" width="170" />
        <el-table-column prop="ipAddress" label="IP地址" width="140" />
        <el-table-column prop="isSuccess" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isSuccess === 1 ? 'success' : 'danger'" size="small">
              {{ row.isSuccess === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="150" />
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

const list = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20, total: 0 })
const dateRange = ref<[string, string] | null>(null)

const filters = reactive({
  userUuid: '',
  isSuccess: undefined as number | undefined,
})

async function load() {
  loading.value = true
  try {
    const res = await logApi.login({
      userUuid: filters.userUuid || undefined,
      isSuccess: filters.isSuccess !== undefined ? filters.isSuccess : undefined,
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
  filters.userUuid = ''
  filters.isSuccess = undefined
  dateRange.value = null
  page.current = 1
  load()
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
