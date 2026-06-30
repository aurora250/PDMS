<template>
  <div>
    <div class="page-header">
      <h3>走访计划</h3>
      <el-button v-if="hasPermission('keyperson:visit-plan:write')" type="primary" @click="openCreate">新增计划</el-button>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable @change="load">
            <el-option label="待走访" value="待走访" /><el-option label="已完成" value="已完成" />
            <el-option label="已逾期" value="已逾期" /><el-option label="已取消" value="已取消" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button @click="load">刷新</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="keyPersonUuid" label="人员UUID" width="200" show-overflow-tooltip />
        <el-table-column prop="plannedDate" label="计划日期" width="120" />
        <el-table-column prop="actualDate" label="实际日期" width="120" />
        <el-table-column prop="visitType" label="走访类型" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><ApprovalBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="assignedPoliceNo" label="责任民警" width="140" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="hasPermission('keyperson:visit-plan:write') && row.status === '待走访'" text size="small" type="success" @click="openComplete(row)">完成走访</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,prev,pager,next" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 新增计划对话框 -->
    <el-dialog v-model="dialogVisible" title="制定走访计划" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="人员UUID" prop="keyPersonUuid">
          <el-input v-model="form.keyPersonUuid" placeholder="请输入重点人员UUID" />
        </el-form-item>
        <el-form-item label="计划日期" prop="plannedDate">
          <el-date-picker v-model="form.plannedDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="走访类型" prop="visitType">
          <el-select v-model="form.visitType" style="width:100%">
            <el-option label="入户走访" value="入户走访" /><el-option label="电话" value="电话" />
            <el-option label="视频" value="视频" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任民警" prop="assignedPoliceNo">
          <el-input v-model="form.assignedPoliceNo" placeholder="民警编号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 完成走访对话框 -->
    <el-dialog v-model="showComplete" title="完成走访" width="450px" @close="resetCompleteForm">
      <el-form ref="completeFormRef" :model="completeForm" :rules="completeRules" label-width="100px">
        <el-form-item label="实际日期" prop="actualDate">
          <el-date-picker v-model="completeForm.actualDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="走访记录">
          <el-input v-model="completeForm.petitionRecord" type="textarea" :rows="4" placeholder="走访情况记录" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showComplete = false">取消</el-button>
        <el-button type="primary" @click="handleComplete" :loading="completing">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { keypersonApi } from '@/api/keyperson'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import ApprovalBadge from '@/components/ApprovalBadge.vue'

const { hasPermission } = usePermission()
const list = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref('')
const page = reactive({ current: 1, size: 20, total: 0 })

// Create dialog
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  keyPersonUuid: '', plannedDate: '', visitType: '入户走访', assignedPoliceNo: '',
})
const rules = {
  keyPersonUuid: [{ required: true, message: '请输入人员UUID', trigger: 'blur' }],
  plannedDate: [{ required: true, message: '请选择计划日期', trigger: 'change' }],
  visitType: [{ required: true, message: '请选择走访类型', trigger: 'change' }],
  assignedPoliceNo: [{ required: true, message: '请指定责任民警', trigger: 'blur' }],
}

// Complete dialog
const showComplete = ref(false)
const completing = ref(false)
const completeFormRef = ref()
let completeVisitId = 0
const completeForm = reactive({ actualDate: new Date().toISOString().slice(0, 10), petitionRecord: '' })
const completeRules = {
  actualDate: [{ required: true, message: '请选择实际日期', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const res = await keypersonApi.listVisitPlan({
      status: statusFilter.value || undefined,
      page: page.current, size: page.size,
    })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openCreate() {
  form.keyPersonUuid = ''
  form.plannedDate = ''
  form.visitType = '入户走访'
  form.assignedPoliceNo = ''
  dialogVisible.value = true
}

function resetForm() { formRef.value?.resetFields() }

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await keypersonApi.createVisitPlan({ ...form, status: '待走访' })
    showSuccess('创建成功')
    dialogVisible.value = false
    load()
  } catch (e: any) { showError(e.message || '创建失败') }
  finally { submitting.value = false }
}

function openComplete(row: any) {
  completeVisitId = row.id
  completeForm.actualDate = new Date().toISOString().slice(0, 10)
  completeForm.petitionRecord = ''
  showComplete.value = true
}

function resetCompleteForm() { completeFormRef.value?.resetFields() }

async function handleComplete() {
  const valid = await completeFormRef.value?.validate().catch(() => false)
  if (!valid) return
  completing.value = true
  try {
    await keypersonApi.updateVisitPlan(completeVisitId, {
      actualDate: completeForm.actualDate,
      petitionRecord: completeForm.petitionRecord,
    })
    showSuccess('走访完成')
    showComplete.value = false
    load()
  } catch (e: any) { showError(e.message || '操作失败') }
  finally { completing.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
</style>
