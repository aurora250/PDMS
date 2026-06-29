<template>
  <div>
    <div class="page-header"><h3>预警中心</h3></div>
    <el-card>
      <el-form inline>
        <el-form-item label="预警类型">
          <el-select v-model="filters.alertType" placeholder="全部" clearable @change="load">
            <el-option label="居住证到期" value="居住证到期" />
            <el-option label="走访逾期" value="走访逾期" />
            <el-option label="重点人员匹配" value="重点人员匹配" />
            <el-option label="证件到期" value="证件到期" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="filters.severity" placeholder="全部" clearable @change="load">
            <el-option label="高" value="高" /><el-option label="中" value="中" /><el-option label="低" value="低" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="filters.isHandled" placeholder="全部" clearable @change="load">
            <el-option label="待处理" :value="0" /><el-option label="已处理" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="alertType" label="预警类型" width="120" />
        <el-table-column prop="alertContent" label="内容" min-width="200" show-overflow-tooltip />
        <el-table-column prop="severity" label="严重程度" width="80">
          <template #default="{ row }">
            <el-tag :type="row.severity === '高' ? 'danger' : row.severity === '中' ? 'warning' : 'info'" size="small">
              {{ row.severity }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column prop="isHandled" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isHandled ? 'success' : 'warning'" size="small">
              {{ row.isHandled ? '已处理' : '待处理' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="hasPermission('alert:handle') && !row.isHandled" text size="small" type="primary" @click="openHandle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,prev,pager,next" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 处理预警对话框 -->
    <el-dialog v-model="showHandle" title="处理预警" width="450px" @close="resetHandleForm">
      <el-form ref="handleFormRef" :model="handleForm" label-width="80px">
        <el-form-item label="预警类型">
          <el-input :model-value="currentAlert?.alertType" disabled />
        </el-form-item>
        <el-form-item label="预警内容">
          <el-input :model-value="currentAlert?.alertContent" type="textarea" disabled :rows="2" />
        </el-form-item>
        <el-form-item label="处理人">
          <el-input :model-value="auth.username" disabled />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="handleForm.remark" type="textarea" placeholder="处理备注（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showHandle = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="handling">确认处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { alertApi } from '@/api/alert'
import { useAuthStore } from '@/stores/auth'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'

const auth = useAuthStore()
const { hasPermission } = usePermission()
const list = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20, total: 0 })

const filters = reactive({
  alertType: '',
  severity: '',
  isHandled: undefined as number | undefined,
})

// Handle dialog
const showHandle = ref(false)
const handling = ref(false)
const handleFormRef = ref()
const currentAlert = ref<any>(null)
const handleForm = reactive({ remark: '' })

async function load() {
  loading.value = true
  try {
    const res = await alertApi.search({
      alertType: filters.alertType || undefined,
      severity: filters.severity || undefined,
      isHandled: filters.isHandled !== undefined ? filters.isHandled : undefined,
      page: page.current, size: page.size,
    })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openHandle(row: any) {
  currentAlert.value = row
  handleForm.remark = ''
  showHandle.value = true
}

function resetHandleForm() { handleFormRef.value?.resetFields() }

async function handleSubmit() {
  handling.value = true
  try {
    await alertApi.handle(currentAlert.value.alertId || currentAlert.value.id, auth.username)
    showSuccess('预警已处理')
    showHandle.value = false
    load()
  } catch (e: any) { showError(e.message || '处理失败') }
  finally { handling.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
