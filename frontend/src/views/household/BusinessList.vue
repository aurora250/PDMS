<template>
  <div>
    <div class="page-header">
      <h3>户籍业务</h3>
      <el-button v-if="hasPermission('household:approve')" type="primary" @click="openCreate">新增业务</el-button>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable @change="load">
            <el-option label="审批中" value="审批中" />
            <el-option label="市局审批中" value="市局审批中" />
            <el-option label="已批准" value="已批准" /><el-option label="已驳回" value="已驳回" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="typeFilter" placeholder="全部" clearable @change="load">
            <el-option label="出生登记" value="出生登记" /><el-option label="死亡注销" value="死亡注销" />
            <el-option label="户口迁移" value="户口迁移" /><el-option label="登记项目变更" value="登记项目变更" />
            <el-option label="分户立户" value="分户立户" />
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
        <el-table-column prop="businessType" label="业务类型" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><ApprovalBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column prop="handleDate" label="办理日期" width="120" />
        <el-table-column label="操作" width="320">
          <template #default="{ row }">
            <!-- 民警：审批中可直接通过或提交市局 -->
            <el-button v-if="hasPermission('household:approve') && row.status === '审批中'" text size="small" type="success" @click="showApprove(row, '通过')">通过</el-button>
            <el-button v-if="hasPermission('household:approve') && row.status === '审批中'" text size="small" type="warning" @click="showApprove(row, '提交市局')">提交市局</el-button>
            <el-button v-if="hasPermission('household:approve') && (row.status === '审批中' || row.status === '市局审批中')" text size="small" type="danger" @click="showApprove(row, '驳回')">驳回</el-button>
            <!-- 市局：市局审批中可最终通过/驳回 -->
            <el-button v-if="hasPermission('household:second-approve') && row.status === '市局审批中'" text size="small" type="success" @click="showApprove(row, '通过')">市局通过</el-button>
            <el-button v-if="hasPermission('household:second-approve') && row.status === '市局审批中'" text size="small" type="danger" @click="showApprove(row, '驳回')">市局驳回</el-button>
            <!-- 街道办：附加材料（不审批） -->
            <el-button v-if="hasPermission('household:material:attach')" text size="small" @click="openAttach(row)">附加材料</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50,100]" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 新增业务对话框 -->
    <el-dialog v-model="dialogVisible" title="新增户籍业务" width="550px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="申请人UUID" prop="applicantUuid">
          <ResidentPicker v-model="form.applicantUuid" placeholder="搜索姓名或身份证号选择申请人" />
        </el-form-item>
        <el-form-item label="业务类型" prop="businessType">
          <el-select v-model="form.businessType" style="width:100%">
            <el-option label="出生登记" value="出生登记" /><el-option label="死亡注销" value="死亡注销" />
            <el-option label="户口迁移" value="户口迁移" /><el-option label="登记项目变更" value="登记项目变更" />
            <el-option label="分户立户" value="分户立户" />
          </el-select>
        </el-form-item>
        <el-form-item label="办理日期" prop="handleDate">
          <el-date-picker v-model="form.handleDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="办理依据">
          <el-input v-model="form.handleBasis" placeholder="法规依据" />
        </el-form-item>
        <el-form-item label="费用">
          <el-input-number v-model="form.fee" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="备注信息" />
        </el-form-item>
        <el-form-item label="附件">
          <AttachmentUploader v-model="form.attachment" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">确认提交</el-button>
      </template>
    </el-dialog>

    <!-- 审批对话框 -->
    <el-dialog v-model="showApproveDialog" :title="approveAction === '通过' ? '审批通过' : '驳回申请'" width="450px">
      <el-form label-width="80px">
        <el-form-item v-if="approveAction !== '通过'" label="驳回原因">
          <el-input v-model="rejectReason" type="textarea" placeholder="请输入驳回原因" />
        </el-form-item>
        <el-form-item v-else>
          <p>确认通过该业务申请？</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApproveDialog = false">取消</el-button>
        <el-button :type="approveAction === '通过' ? 'success' : 'danger'" @click="handleApprove" :loading="approving">确认</el-button>
      </template>
    </el-dialog>

    <!-- 附件上传对话框 -->
    <el-dialog v-model="showAttachDialog" title="附加审核材料" width="450px">
      <el-form label-width="80px">
        <el-form-item label="上传文件">
          <AttachmentUploader v-model="attachFiles" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="attachRemark" type="textarea" placeholder="审核材料说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAttachDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAttach" :loading="attaching">确认附加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { householdApi } from '@/api/household'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import ApprovalBadge from '@/components/ApprovalBadge.vue'
import AttachmentUploader from '@/components/AttachmentUploader.vue'
import ResidentPicker from '@/components/ResidentPicker.vue'

const { hasPermission } = usePermission()
const list = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref('')
const typeFilter = ref('')
const page = reactive({ current: 1, size: 20, total: 0 })

// Create dialog
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  applicantUuid: '', businessType: '出生登记', handleDate: new Date().toISOString().slice(0, 10),
  handleBasis: '', fee: 0, remark: '', attachment: [] as string[],
})
const rules = {
  applicantUuid: [{ required: true, message: '请输入申请人UUID', trigger: 'blur' }],
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  handleDate: [{ required: true, message: '请选择办理日期', trigger: 'change' }],
}

// Approve dialog
const showApproveDialog = ref(false)
const approving = ref(false)
const approveAction = ref('')
const rejectReason = ref('')
let approveRid = 0

// Attach dialog
const showAttachDialog = ref(false)
const attaching = ref(false)
const attachFiles = ref<string[]>([])
const attachRemark = ref('')
let attachRid = 0

async function load() {
  loading.value = true
  try {
    const res = await householdApi.listBusiness({
      status: statusFilter.value || undefined,
      businessType: typeFilter.value || undefined,
      page: page.current, size: page.size,
    })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, {
    applicantUuid: '', businessType: '出生登记', handleDate: new Date().toISOString().slice(0, 10),
    handleBasis: '', fee: 0, remark: '', attachment: [],
  })
  dialogVisible.value = true
}

function resetForm() { formRef.value?.resetFields() }

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await householdApi.createBusiness({ ...form })
    showSuccess('提交成功')
    dialogVisible.value = false
    load()
  } catch (e: any) { showError(e.message || '提交失败') }
  finally { submitting.value = false }
}

function showApprove(row: any, action: string) {
  approveRid = row.rid
  approveAction.value = action
  rejectReason.value = ''
  showApproveDialog.value = true
}

async function handleApprove() {
  try {
    const actionText = approveAction.value === '驳回' ? '确认驳回该业务申请？'
      : approveAction.value === '提交市局' ? '确认提交市局审批（特殊事项）？'
      : '确认通过该业务申请？'
    await ElMessageBox.confirm(actionText, '确认操作', { type: 'warning' })
  } catch { showApproveDialog.value = false; return }
  approving.value = true
  try {
    // 直接发送审批动作（通过/驳回/提交市局），后端状态机决定下一状态
    await householdApi.approveBusiness(approveRid, approveAction.value, rejectReason.value || undefined)
    showSuccess(approveAction.value === '驳回' ? '已驳回' : approveAction.value === '提交市局' ? '已提交市局' : '已通过')
    showApproveDialog.value = false
    load()
  } catch (e: any) { showError(e.message || '操作失败') }
  finally { approving.value = false }
}

function openAttach(row: any) {
  attachRid = row.rid
  attachFiles.value = []
  attachRemark.value = ''
  showAttachDialog.value = true
}

async function handleAttach() {
  attaching.value = true
  try {
    const fd = new FormData()
    if (attachFiles.value.length > 0) {
      fd.append('attachmentPath', attachFiles.value.join(','))
    }
    if (attachRemark.value) {
      fd.append('remark', attachRemark.value)
    }
    await householdApi.attachBusinessMaterial(attachRid, fd)
    showSuccess('材料已附加')
    showAttachDialog.value = false
    load()
  } catch (e: any) { showError(e.message || '附加失败') }
  finally { attaching.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
</style>
