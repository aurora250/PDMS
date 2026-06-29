<template>
  <div>
    <div class="page-header">
      <h3>居住证管理</h3>
      <el-button v-if="hasPermission('fp:write')" type="primary" @click="openApply">申领</el-button>
    </div>
    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="permitNo" label="居住证号" width="180" />
        <el-table-column prop="uuid" label="UUID" width="200" show-overflow-tooltip />
        <el-table-column prop="issueDate" label="签发日" width="120" />
        <el-table-column prop="expiryDate" label="到期日" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><ApprovalBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button v-if="hasPermission('fp:permit:approve') && row.status === '申领'" text size="small" type="success" @click="approve(row)">审批通过</el-button>
            <el-button v-if="hasPermission('fp:permit:issue') && row.status === '已批准'" text size="small" @click="issue(row)">制发</el-button>
            <el-button v-if="hasPermission('fp:write')" text size="small" type="primary" @click="openRenew(row)">续期</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,prev,pager,next" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 申领对话框 -->
    <el-dialog v-model="showApply" title="申领居住证" width="450px" @close="resetApplyForm">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="100px">
        <el-form-item label="居民UUID" prop="uuid">
          <el-input v-model="applyForm.uuid" placeholder="请输入居民UUID" />
        </el-form-item>
        <el-form-item label="有效期至" prop="expiryDate">
          <el-date-picker v-model="applyForm.expiryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApply = false">取消</el-button>
        <el-button type="primary" @click="handleApply" :loading="applying">确认申领</el-button>
      </template>
    </el-dialog>

    <!-- 续期对话框 -->
    <el-dialog v-model="showRenew" title="续期居住证" width="450px" @close="resetRenewForm">
      <el-form ref="renewFormRef" :model="renewForm" :rules="renewRules" label-width="110px">
        <el-form-item label="居住证号">
          <el-input :model-value="renewForm.permitNo" disabled />
        </el-form-item>
        <el-form-item label="原到期日">
          <el-input :model-value="renewForm.oldExpiryDate" disabled />
        </el-form-item>
        <el-form-item label="新到期日" prop="newExpiryDate">
          <el-date-picker v-model="renewForm.newExpiryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="续期日期" prop="renewalDate">
          <el-date-picker v-model="renewForm.renewalDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="renewForm.remark" type="textarea" placeholder="续期原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRenew = false">取消</el-button>
        <el-button type="primary" @click="handleRenew" :loading="renewing">确认续期</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { floatingApi } from '@/api/floating'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import ApprovalBadge from '@/components/ApprovalBadge.vue'

const { hasPermission } = usePermission()
const list = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20, total: 0 })

// Apply dialog
const showApply = ref(false)
const applying = ref(false)
const applyFormRef = ref()
const applyForm = reactive({ uuid: '', expiryDate: '' })
const applyRules = {
  uuid: [{ required: true, message: '请输入居民UUID', trigger: 'blur' }],
  expiryDate: [{ required: true, message: '请选择有效期', trigger: 'change' }],
}

// Renew dialog
const showRenew = ref(false)
const renewing = ref(false)
const renewFormRef = ref()
let renewPermitId = 0
const renewForm = reactive({ permitNo: '', oldExpiryDate: '', newExpiryDate: '', renewalDate: new Date().toISOString().slice(0, 10), remark: '' })
const renewRules = {
  newExpiryDate: [{ required: true, message: '请选择新到期日', trigger: 'change' }],
  renewalDate: [{ required: true, message: '请选择续期日期', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const res = await floatingApi.listPermit({ page: page.current, size: page.size })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function approve(row: any) {
  try { await floatingApi.approvePermit(row.id); showSuccess('已批准'); load() } catch { /* ignore */ }
}

async function issue(row: any) {
  try { await floatingApi.issuePermit(row.id); showSuccess('已制发'); load() } catch { /* ignore */ }
}

function openApply() {
  applyForm.uuid = ''
  applyForm.expiryDate = ''
  showApply.value = true
}

function resetApplyForm() { applyFormRef.value?.resetFields() }

async function handleApply() {
  const valid = await applyFormRef.value?.validate().catch(() => false)
  if (!valid) return
  applying.value = true
  try {
    await floatingApi.applyPermit({ ...applyForm })
    showSuccess('申领成功')
    showApply.value = false
    load()
  } catch (e: any) { showError(e.message || '申领失败') }
  finally { applying.value = false }
}

function openRenew(row: any) {
  renewPermitId = row.id
  renewForm.permitNo = row.permitNo || ''
  renewForm.oldExpiryDate = row.expiryDate || ''
  renewForm.newExpiryDate = ''
  renewForm.renewalDate = new Date().toISOString().slice(0, 10)
  renewForm.remark = ''
  showRenew.value = true
}

function resetRenewForm() { renewFormRef.value?.resetFields() }

async function handleRenew() {
  const valid = await renewFormRef.value?.validate().catch(() => false)
  if (!valid) return
  renewing.value = true
  try {
    await floatingApi.renewPermit(renewPermitId, {
      permitNo: renewForm.permitNo,
      oldExpiryDate: renewForm.oldExpiryDate,
      newExpiryDate: renewForm.newExpiryDate,
      renewalDate: renewForm.renewalDate,
      remark: renewForm.remark,
    })
    showSuccess('续期成功')
    showRenew.value = false
    load()
  } catch (e: any) { showError(e.message || '续期失败') }
  finally { renewing.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
</style>
