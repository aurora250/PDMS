<template>
  <div>
    <div class="page-header">
      <h3>证件管理</h3>
      <el-button v-if="hasPermission('household:write')" type="primary" @click="openIssue">{{ tab === 'approval' ? '签发准迁证' : '签发迁移证' }}</el-button>
    </div>
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="准迁证" name="approval" />
      <el-tab-pane label="迁移证" name="migration" />
    </el-tabs>
    <el-card>
      <el-form inline style="margin-bottom:12px">
        <el-form-item>
          <el-input v-model="permitKeyword" placeholder="搜索证件编号" clearable @keyup.enter="load" style="width:220px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="permitStatusFilter" placeholder="全部" clearable @change="load">
            <el-option label="有效" value="有效" /><el-option label="作废" value="作废" />
            <el-option label="审批中" value="审批中" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button @click="load">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="permitNo" label="证件编号" width="200" />
        <el-table-column prop="issueDate" label="签发日期" width="120" />
        <el-table-column v-if="tab === 'approval'" prop="expiryDate" label="有效期至" width="120" />
        <el-table-column v-if="tab === 'approval'" prop="issuingAuthority" label="签发机关" min-width="180" />
        <el-table-column v-if="tab === 'migration'" prop="outgoingPoliceStation" label="迁出派出所" min-width="180" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><ApprovalBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '有效'" text size="small" type="danger" @click="handleVoid(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,prev,pager,next" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 签发对话框 -->
    <el-dialog v-model="dialogVisible" :title="tab === 'approval' ? '签发准迁证' : '签发迁移证'" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="证件编号">
          <el-input v-model="form.permitNo" :placeholder="tab === 'approval' ? '如: AP-2026-00001' : '如: MP-2026-00001'" />
        </el-form-item>
        <el-form-item label="签发日期" prop="issueDate">
          <el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item v-if="tab === 'approval'" label="有效期至" prop="expiryDate">
          <el-date-picker v-model="form.expiryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item v-if="tab === 'approval'" label="签发机关" prop="issuingAuthority">
          <el-input v-model="form.issuingAuthority" placeholder="如: 北京市公安局东城分局" />
        </el-form-item>
        <el-form-item v-if="tab === 'migration'" label="迁出派出所" prop="outgoingPoliceStation">
          <el-input v-model="form.outgoingPoliceStation" placeholder="如: 某某派出所" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleIssue" :loading="issuing">确认签发</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { householdApi } from '@/api/household'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import ApprovalBadge from '@/components/ApprovalBadge.vue'

const { hasPermission } = usePermission()
const tab = ref('approval')
const list = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20, total: 0 })
const permitKeyword = ref('')
const permitStatusFilter = ref('')

const dialogVisible = ref(false)
const issuing = ref(false)
const formRef = ref()
const form = reactive({
  permitNo: '', issueDate: new Date().toISOString().slice(0, 10),
  expiryDate: '', issuingAuthority: '', outgoingPoliceStation: '',
})

const rules = {
  issueDate: [{ required: true, message: '请选择签发日期', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const params: any = { page: page.current, size: page.size }
    if (permitKeyword.value) params.keyword = permitKeyword.value
    if (permitStatusFilter.value) params.status = permitStatusFilter.value
    let res
    if (tab.value === 'approval') {
      res = await householdApi.listApprovalPermit(params)
    } else {
      res = await householdApi.listMigrationPermit(params)
    }
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleVoid(row: any) {
  try {
    await ElMessageBox.confirm('确认作废该证件？', '确认作废', { type: 'warning' })
    // Update status to 作废
    if (tab.value === 'approval') {
      await householdApi.approveBusiness(row.rid, '已驳回')
    }
    showSuccess('已作废')
    load()
  } catch { /* ignore */ }
}

function openIssue() {
  form.permitNo = ''
  form.issueDate = new Date().toISOString().slice(0, 10)
  form.expiryDate = ''
  form.issuingAuthority = ''
  form.outgoingPoliceStation = ''
  dialogVisible.value = true
}

function resetForm() { formRef.value?.resetFields() }

async function handleIssue() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  issuing.value = true
  try {
    if (tab.value === 'approval') {
      await householdApi.createApprovalPermit({
        permitNo: form.permitNo, issueDate: form.issueDate,
        expiryDate: form.expiryDate, issuingAuthority: form.issuingAuthority,
        status: '有效',
      })
    } else {
      await householdApi.createMigrationPermit({
        permitNo: form.permitNo, issueDate: form.issueDate,
        outgoingPoliceStation: form.outgoingPoliceStation, status: '有效',
      })
    }
    showSuccess('签发成功')
    dialogVisible.value = false
    load()
  } catch (e: any) { showError(e.message || '签发失败') }
  finally { issuing.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
</style>
