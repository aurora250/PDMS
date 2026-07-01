<template>
  <div>
    <div class="page-header">
      <h3>失踪人口管理</h3>
      <el-button v-if="hasPermission('missing:write')" type="primary" @click="openCreate">登记失踪</el-button>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable @change="load">
            <el-option label="失踪中" value="失踪中" /><el-option label="已经寻回" value="已经寻回" />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="nameFilter" placeholder="姓名" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item label="省份">
          <el-select v-model="provinceFilter" placeholder="全部" clearable filterable style="width:130px" @change="load">
            <el-option v-for="p in PROVINCES" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button @click="load">刷新</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="missingDate" label="失踪日期" width="120" />
        <el-table-column prop="missingPlace" label="失踪地点" min-width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><ApprovalBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="hasPermission('missing:recovery:write') && row.status !== '已经寻回'" text size="small" type="success" @click="openRecover(row)">寻回</el-button>
            <el-button v-if="hasPermission('missing:delete')" text size="small" type="danger" @click="del(row)">撤销</el-button>
            <el-button text size="small" @click="goDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50,100]" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 登记失踪对话框 -->
    <el-dialog v-model="dialogVisible" title="登记失踪" width="550px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="居民UUID" prop="residentUuid">
          <ResidentPicker v-model="form.residentUuid" placeholder="搜索姓名或身份证号选择失踪人员" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCardNo">
          <IdCardInput v-model="form.idCardNo" @parsed="onIdParsed" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="自动填充或手动输入" />
        </el-form-item>
        <el-form-item label="失踪日期" prop="missingDate">
          <el-date-picker v-model="form.missingDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="失踪地点" prop="missingPlace">
          <el-input v-model="form.missingPlace" placeholder="最后出现地点" />
        </el-form-item>
        <el-form-item label="体貌特征">
          <el-input v-model="form.appearance" type="textarea" :rows="2" placeholder="身高、体型、发型等" />
        </el-form-item>
        <el-form-item label="可能去向">
          <el-input v-model="form.possibleWay" placeholder="如: 疑似被拐卖" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="家属联系电话" maxlength="11" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">确认登记</el-button>
      </template>
    </el-dialog>

    <!-- 寻回对话框 -->
    <el-dialog v-model="showRecover" title="登记寻回" width="450px" @close="resetRecoverForm">
      <el-form ref="recoverFormRef" :model="recoverForm" :rules="recoverRules" label-width="100px">
        <el-form-item label="寻回日期" prop="recoveryDate">
          <el-date-picker v-model="recoverForm.recoveryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="寻回说明" prop="summary">
          <el-input v-model="recoverForm.summary" type="textarea" :rows="3" placeholder="寻回经过..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRecover = false">取消</el-button>
        <el-button type="primary" @click="handleRecover" :loading="recovering">确认寻回</el-button>
      </template>
    </el-dialog>

    <!-- 失踪详情对话框 -->
    <el-dialog v-model="showDetail" title="失踪记录详情" width="500px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="姓名">{{ detailForm.name }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ detailForm.gender }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ detailForm.idCardNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="失踪日期">{{ detailForm.missingDate }}</el-descriptions-item>
        <el-descriptions-item label="失踪地点" :span="2">{{ detailForm.missingPlace }}</el-descriptions-item>
        <el-descriptions-item label="体貌特征" :span="2">{{ detailForm.appearance || '-' }}</el-descriptions-item>
        <el-descriptions-item label="可能去向" :span="2">{{ detailForm.possibleWay || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ detailForm.contactPhone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <ApprovalBadge :status="detailForm.status" />
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 居民详情对话框 -->
    <ResidentDetail ref="detailRef" @saved="load" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { missingApi } from '@/api/missing'
import ResidentDetail from '@/views/resident/ResidentDetail.vue'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import { phoneRule, idCardRule } from '@/utils/validators'
import IdCardInput from '@/components/IdCardInput.vue'
import ApprovalBadge from '@/components/ApprovalBadge.vue'
import ResidentPicker from '@/components/ResidentPicker.vue'
import { PROVINCES } from '@/utils/constants'

const detailRef = ref()
const { hasPermission } = usePermission()
const route = useRoute()
const list = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref('')
const nameFilter = ref('')
const provinceFilter = ref('')
const page = reactive({ current: 1, size: 20, total: 0 })

// Create dialog
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  residentUuid: '', idCardNo: '', name: '', missingDate: new Date().toISOString().slice(0, 10),
  missingPlace: '', appearance: '', possibleWay: '', contactPhone: '', medicalHistory: '', status: '失踪中',
})
const rules = {
  residentUuid: [{ required: true, message: '请输入居民UUID', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  idCardNo: [idCardRule],
  missingDate: [{ required: true, message: '请选择失踪日期', trigger: 'change' }],
  missingPlace: [{ required: true, message: '请输入失踪地点', trigger: 'blur' }],
  contactPhone: [phoneRule],
}

// Recover dialog
const showRecover = ref(false)
const recovering = ref(false)
const recoverFormRef = ref()
let recoverRid = 0
const recoverForm = reactive({ recoveryDate: new Date().toISOString().slice(0, 10), summary: '' })
const recoverRules = {
  recoveryDate: [{ required: true, message: '请选择寻回日期', trigger: 'change' }],
  summary: [{ required: true, message: '请输入寻回说明', trigger: 'blur' }],
}

// Detail dialog
const showDetail = ref(false)
const detailForm = reactive({
  name: '', gender: '', idCardNo: '', missingDate: '', missingPlace: '',
  appearance: '', possibleWay: '', contactPhone: '', status: '',
})

async function goDetail(row: any) {
  Object.assign(detailForm, {
    name: row.name || '',
    gender: row.gender || '',
    idCardNo: row.idCardNo || '',
    missingDate: row.missingDate || '',
    missingPlace: row.missingPlace || '',
    appearance: row.appearance || '',
    possibleWay: row.possibleWay || '',
    contactPhone: row.contactPhone || '',
    status: row.status || '',
  })
  showDetail.value = true
}

function onIdParsed(data: { birthDate: string; gender: string }) {
  /* auto-fill handled by IdCardInput */
}

async function load() {
  loading.value = true
  try {
    const res = await missingApi.search({
      status: statusFilter.value || undefined,
      name: nameFilter.value || undefined,
      province: provinceFilter.value || undefined,
      page: page.current, size: page.size,
    })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function del(row: any) {
  try {
    await ElMessageBox.confirm('确认撤销该失踪记录？', '确认撤销', { type: 'warning' })
    await missingApi.delete(row.rid ?? row.id); showSuccess('已撤销'); load()
  } catch { /* ignore */ }
}

function openCreate() {
  Object.assign(form, {
    residentUuid: '', idCardNo: '', name: '', missingDate: new Date().toISOString().slice(0, 10),
    missingPlace: '', appearance: '', possibleWay: '', contactPhone: '', medicalHistory: '', status: '失踪中',
  })
  dialogVisible.value = true
}

function resetForm() { formRef.value?.resetFields() }

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await missingApi.create({ ...form })
    showSuccess('登记成功')
    dialogVisible.value = false
    load()
  } catch (e: any) { showError(e.message || '登记失败') }
  finally { submitting.value = false }
}

function openRecover(row: any) {
  ElMessageBox.confirm('确认该失踪人员已被寻回？', '确认寻回', { type: 'warning' }).then(() => {
    recoverRid = row.rid ?? row.id
    recoverForm.recoveryDate = new Date().toISOString().slice(0, 10)
    recoverForm.summary = ''
    showRecover.value = true
  }).catch(() => {})
}

function resetRecoverForm() { recoverFormRef.value?.resetFields() }

async function handleRecover() {
  const valid = await recoverFormRef.value?.validate().catch(() => false)
  if (!valid) return
  recovering.value = true
  try {
    await missingApi.recovery({
      missingRecordRid: recoverRid,
      recoveryDate: recoverForm.recoveryDate,
      summary: recoverForm.summary,
    })
    showSuccess('寻回登记成功')
    showRecover.value = false
    load()
  } catch (e: any) { showError(e.message || '寻回登记失败') }
  finally { recovering.value = false }
}

onMounted(() => {
  if (route.query.province) provinceFilter.value = route.query.province as string
  load()
})
watch(() => route.query.province, (val) => {
  if (val) { provinceFilter.value = val as string; load() }
})
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
</style>
