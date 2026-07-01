<template>
  <div class="change-request-page">
    <div class="page-header">
      <h3>变更申请管理</h3>
      <el-button type="primary" @click="openCreate" v-if="hasPermission('resident:write')">提交变更申请</el-button>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable @change="load">
            <el-option label="请求" value="请求" />
            <el-option label="市局审批中" value="市局审批中" />
            <el-option label="通过" value="通过" />
            <el-option label="驳回" value="驳回" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button @click="load">刷新</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="rid" label="ID" width="60" />
        <el-table-column label="申请人" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button v-if="row.applicantUuid" text size="small" type="primary" @click="$router.push(`/resident/${row.applicantUuid}`)">{{ row.applicantUuid }}</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="changeField" label="变更字段" width="120" />
        <el-table-column label="原值" min-width="140">
          <template #default="{ row }">
            <span class="data-cell">{{ formatJson(row.originalData) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="新值" min-width="140">
          <template #default="{ row }">
            <span class="data-cell">{{ formatJson(row.modifiedData) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><ApprovalBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="requestTime" label="申请时间" width="120" />
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <!-- 民警：请求中可审批/驳回/提交市局 -->
            <el-button v-if="hasPermission('resident:change-request:approve') && row.status === '请求'" text size="small" type="success" @click="approve(row, '通过')">通过</el-button>
            <el-button v-if="hasPermission('resident:change-request:approve') && row.status === '请求'" text size="small" type="warning" @click="approve(row, '提交市局')">提交市局</el-button>
            <el-button v-if="hasPermission('resident:change-request:approve') && (row.status === '请求' || row.status === '市局审批中')" text size="small" type="danger" @click="approve(row, '驳回')">驳回</el-button>
            <!-- 市局：市局审批中可最终审批 -->
            <el-button v-if="hasPermission('resident:change-request:second-approve') && row.status === '市局审批中'" text size="small" type="success" @click="approve(row, '通过')">市局通过</el-button>
            <el-button v-if="hasPermission('resident:change-request:second-approve') && row.status === '市局审批中'" text size="small" type="danger" @click="approve(row, '驳回')">市局驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50,100]" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 提交变更申请对话框 -->
    <el-dialog v-model="dialogVisible" title="提交变更申请" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="申请人UUID" prop="applicantUuid">
          <ResidentPicker v-model="form.applicantUuid" placeholder="搜索姓名或身份证号选择申请人" />
        </el-form-item>
        <el-form-item label="变更字段" prop="changeField">
          <el-select v-model="form.changeField" placeholder="选择要变更的字段" style="width:100%">
            <el-option label="姓名" value="name" />
            <el-option label="性别" value="gender" />
            <el-option label="民族" value="nation" />
            <el-option label="出生日期" value="birthDate" />
            <el-option label="学历" value="educationLevel" />
            <el-option label="婚姻状况" value="maritalStatus" />
            <el-option label="职业" value="occupation" />
            <el-option label="电话" value="phone" />
            <el-option label="居住地址" value="residence" />
            <el-option label="户口地址" value="householdAddress" />
          </el-select>
        </el-form-item>
        <el-form-item label="原始值" prop="originalData">
          <el-input v-model="form.originalData" type="textarea" placeholder="请输入原始值" />
        </el-form-item>
        <el-form-item label="新值" prop="modifiedData">
          <el-input v-model="form.modifiedData" type="textarea" placeholder="请输入变更后的值" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { residentApi } from '@/api/resident'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import ApprovalBadge from '@/components/ApprovalBadge.vue'
import ResidentPicker from '@/components/ResidentPicker.vue'

const { hasPermission } = usePermission()

/** 将JSON字符串格式化为可读的 key: value 文本 */
function formatJson(raw: string): string {
  if (!raw) return '-'
  try {
    const obj = JSON.parse(raw)
    return Object.entries(obj)
      .map(([k, v]) => `${k}: ${v}`)
      .join('；')
  } catch {
    return raw
  }
}
const list = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref('')
const page = reactive({ current: 1, size: 20, total: 0 })

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  applicantUuid: '',
  changeField: '',
  originalData: '',
  modifiedData: '',
})

const rules = {
  applicantUuid: [{ required: true, message: '请输入申请人UUID', trigger: 'blur' }],
  changeField: [{ required: true, message: '请选择变更字段', trigger: 'change' }],
  originalData: [{ required: true, message: '请输入原始值', trigger: 'blur' }],
  modifiedData: [{ required: true, message: '请输入新值', trigger: 'blur' }],
}

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

async function approve(row: any, action: string) {
  try {
    const actionText = action === '驳回' ? '确认驳回该变更请求？'
      : action === '提交市局' ? '确认提交市局审批（特殊事项）？'
      : '确认通过该变更请求？'
    await ElMessageBox.confirm(actionText, '确认操作', { type: 'warning' })
    await residentApi.approveChangeRequest(row.rid, action)
    showSuccess(action === '驳回' ? '已驳回' : action === '提交市局' ? '已提交市局' : '已通过')
    load()
  } catch (e: any) { if (e !== 'cancel') showError(e.message || '操作失败') }
}

function openCreate() {
  form.applicantUuid = ''
  form.changeField = ''
  form.originalData = ''
  form.modifiedData = ''
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await residentApi.submitChangeRequest({ ...form, status: '请求' })
    showSuccess('提交成功')
    dialogVisible.value = false
    load()
  } catch (e: any) {
    showError(e.message || '提交失败')
  } finally { submitting.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
.data-cell { font-size: 12px; color: #606266; word-break: break-all; }
</style>
