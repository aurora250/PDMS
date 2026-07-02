<template>
  <div>
    <div class="page-header">
      <h3>迁移管理</h3>
      <el-button v-if="hasPermission('household:approve')" type="primary" @click="openCreate">新增迁移</el-button>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable @change="load">
            <el-option label="准迁证审批中" value="准迁证审批中" />
            <el-option label="准迁证已批准" value="准迁证已批准" />
            <el-option label="准迁证审批驳回" value="准迁证审批驳回" />
            <el-option label="迁移证已批准" value="迁移证已批准" />
            <el-option label="迁移审批通过" value="迁移审批通过" />
            <el-option label="迁移审批驳回" value="迁移审批驳回" />
          </el-select>
        </el-form-item>
        <el-form-item label="迁出省">
          <el-select v-model="fromFilter" placeholder="全部" clearable filterable style="width:140px" @change="load">
            <el-option v-for="p in PROVINCES" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item label="迁入省">
          <el-select v-model="toFilter" placeholder="全部" clearable filterable style="width:140px" @change="load">
            <el-option v-for="p in PROVINCES" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button @click="load">刷新</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="rid" label="ID" width="60" />
        <el-table-column label="人员UUID" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button v-if="row.applicantUuid" text size="small" type="primary" @click="$router.push(`/resident/${row.applicantUuid}`)">{{ row.applicantUuid }}</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="outgoingAddress" label="迁出地址" min-width="180" />
        <el-table-column prop="incomingAddress" label="迁入地址" min-width="180" />
        <el-table-column prop="businessType" label="类型" width="80" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }"><ApprovalBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button text size="small" @click="$router.push(`/resident/${row.applicantUuid}?tab=migration`)">轨迹</el-button>
            <el-button v-if="hasPermission('household:approve') && (row.status === '准迁证审批中' || row.status === '准迁证已批准' || row.status === '迁移证已批准')" text size="small" type="success" @click="showApprove(row, '通过')">通过</el-button>
            <el-button v-if="hasPermission('household:approve') && !row.status?.includes('驳回') && row.status !== '迁移审批通过'" text size="small" type="danger" @click="showApprove(row, '驳回')">驳回</el-button>
            <el-button v-if="hasPermission('household:material:attach')" text size="small" @click="openAttach(row)">附加材料</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50,100]" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 新增迁移对话框 -->
    <el-dialog v-model="dialogVisible" title="新增迁移申请" width="650px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="申请人UUID" prop="applicantUuid">
          <ResidentPicker v-model="form.applicantUuid" placeholder="搜索姓名或身份证号选择申请人"
            @pick="onApplicantPicked" />
        </el-form-item>

        <!-- 迁出地址: 级联菜单 + 详细地址 + 预览 -->
        <el-form-item label="迁出地区" required>
          <AreaCascader v-model="form.outgoingAreaId" placeholder="选择迁出省市区" />
        </el-form-item>
        <el-form-item label="迁出详址">
          <el-input v-model="form.outgoingDetail" placeholder="街道/路/号/楼/室" />
          <span class="form-tip">选择申请人后自动填充现居住地，可手动修改</span>
        </el-form-item>
        <div v-if="outgoingPreview" class="address-preview">
          <el-text type="info" size="small">迁出预览: {{ outgoingPreview }}</el-text>
        </div>

        <!-- 迁入地址: 级联菜单 + 详细地址 + 预览 -->
        <el-form-item label="迁入地区" required>
          <AreaCascader v-model="form.incomingAreaId" placeholder="选择迁入省市区" />
        </el-form-item>
        <el-form-item label="迁入详址">
          <el-input v-model="form.incomingDetail" placeholder="街道/路/号/楼/室" />
        </el-form-item>
        <div v-if="incomingPreview" class="address-preview">
          <el-text type="info" size="small">迁入预览: {{ incomingPreview }}</el-text>
        </div>

        <el-form-item label="迁移类型" prop="businessType">
          <el-select v-model="form.businessType" style="width:100%" disabled>
            <el-option label="市内" value="市内" /><el-option label="省内" value="省内" />
            <el-option label="跨省" value="跨省" />
          </el-select>
          <span class="form-tip">根据迁出/迁入地区自动判定</span>
        </el-form-item>
        <el-form-item label="办理日期" prop="handleDate">
          <el-date-picker v-model="form.handleDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="办理依据">
          <div style="display:flex;gap:6px;width:100%">
            <el-select v-model="form.handleBasis" style="flex:1" clearable filterable allow-create
              placeholder="选择或自行输入法规依据">
              <el-option v-for="r in MIGRATION_REGULATIONS" :key="r.name" :label="r.name" :value="r.name" />
            </el-select>
            <el-button v-if="selectedRegulationUrl" type="primary" link size="small" @click="openRegulationUrl">
              <el-icon><Link /></el-icon> 查看原文
            </el-button>
          </div>
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
          <p>确认通过该迁移申请？</p>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApproveDialog = false">取消</el-button>
        <el-button :type="approveAction === '通过' ? 'success' : 'danger'" @click="handleApprove" :loading="approving">确认</el-button>
      </template>
    </el-dialog>

    <!-- 附加材料对话框 (街道办) -->
    <el-dialog v-model="showAttachDialog" title="附加审核材料" width="450px">
      <el-form label-width="80px">
        <el-form-item label="上传文件">
          <AttachmentUploader v-model="attachFile" />
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { householdApi } from '@/api/household'
import { residentApi } from '@/api/resident'
import { areaApi } from '@/api/area'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import AreaCascader from '@/components/AreaCascader.vue'
import ApprovalBadge from '@/components/ApprovalBadge.vue'
import AttachmentUploader from '@/components/AttachmentUploader.vue'
import ResidentPicker from '@/components/ResidentPicker.vue'
import { PROVINCES } from '@/utils/constants'
import { Link } from '@element-plus/icons-vue'

const { hasPermission } = usePermission()
const route = useRoute()
const list = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref('')
const fromFilter = ref('')
const toFilter = ref('')
const page = reactive({ current: 1, size: 20, total: 0 })

/** 户籍迁移办理依据法规列表（含官方链接） */
const MIGRATION_REGULATIONS = [
  { name: '《中华人民共和国户口登记条例》第十条（迁出登记）',
    url: 'https://flk.npc.gov.cn/detail?id=2c909fdd678bf17901678bf8a7250b77&fileId=&type=&title=%E4%B8%AD%E5%8D%8E%E4%BA%BA%E6%B0%91%E5%85-%B1%E5%92%8C%E5%9B%BD%E6%88%B7%E5%8F%A3%E7%99%BB%E8%AE%B0%E6%9D%A1%E4%BE%8B' },
  { name: '《中华人民共和国户口登记条例》第十三条（迁入登记）',
    url: 'https://flk.npc.gov.cn/detail?id=2c909fdd678bf17901678bf8a7250b77&fileId=&type=&title=%E4%B8%AD%E5%8D%8E%E4%BA%BA%E6%B0%91%E5%85-%B1%E5%92%8C%E5%9B%BD%E6%88%B7%E5%8F%A3%E7%99%BB%E8%AE%B0%E6%9D%A1%E4%BE%8B' },
  { name: '《公安部关于解决当前户口管理工作中几个突出问题意见的通知》（国发〔1998〕24号）',
    url: 'https://www.gov.cn/gongbao/content/1998/content_61770.htm' },
  { name: '《关于进一步深化户籍制度改革的意见》（国发〔2014〕25号）',
    url: 'https://www.gov.cn/zhengce/content/2014-07/30/content_8944.htm' },
  { name: '《关于解决无户口人员登记户口问题的意见》（国办发〔2015〕96号）',
    url: 'https://www.gov.cn/gongbao/content/2016/content_5036272.htm' },
]
const selectedRegulationUrl = computed(() => {
  const found = MIGRATION_REGULATIONS.find(r => r.name === form.handleBasis)
  return found ? found.url : null
})
function openRegulationUrl() {
  if (selectedRegulationUrl.value) window.open(selectedRegulationUrl.value, '_blank')
}

// Area code cache: area_id → area_code (from tree data)
const areaCodeMap = ref<Map<number, string>>(new Map())

/** 构建 area_id → area_code 缓存 */
async function loadAreaCodeMap() {
  try {
    const all: any[] = await areaApi.tree()
    for (const a of all) {
      if (a.areaId && a.areaCode) {
        areaCodeMap.value.set(a.areaId, String(a.areaCode).trim())
      }
    }
  } catch { /* ignore */ }
}

/** 获取区域路径前缀 */
async function getAreaPath(areaId: number | null | undefined): Promise<string> {
  if (!areaId) return ''
  try { return await areaApi.getPath(areaId) } catch { return '' }
}

// Create dialog
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  applicantUuid: '', outgoingDetail: '', outgoingAreaId: undefined as number | undefined,
  incomingDetail: '', incomingAreaId: undefined as number | undefined,
  businessType: '市内', handleDate: new Date().toISOString().slice(0, 10),
  handleBasis: '', fee: 0, remark: '', attachment: [] as string[],
})
const rules = {
  applicantUuid: [{ required: true, message: '请选择申请人', trigger: 'blur' }],
  businessType: [{ required: true, message: '请选择迁移类型', trigger: 'change' }],
  handleDate: [{ required: true, message: '请选择办理日期', trigger: 'change' }],
}

// 地址预览
const outgoingPreview = ref('')
const incomingPreview = ref('')

watch(
  () => [form.outgoingAreaId, form.outgoingDetail],
  async () => { outgoingPreview.value = await getAreaPath(form.outgoingAreaId) + (form.outgoingDetail || '') }
)
watch(
  () => [form.incomingAreaId, form.incomingDetail],
  async () => { incomingPreview.value = await getAreaPath(form.incomingAreaId) + (form.incomingDetail || '') }
)

/** 根据迁出/迁入 area_code 自动判定迁移类型 */
watch(
  () => [form.outgoingAreaId, form.incomingAreaId],
  async () => {
    const outId = form.outgoingAreaId
    const inId = form.incomingAreaId
    if (!outId || !inId || areaCodeMap.value.size === 0) return
    const outCode = areaCodeMap.value.get(outId) || ''
    const inCode = areaCodeMap.value.get(inId) || ''
    if (outCode.length < 4 || inCode.length < 4) return
    if (outCode.substring(0, 4) === inCode.substring(0, 4)) {
      form.businessType = '市内'
    } else if (outCode.substring(0, 2) === inCode.substring(0, 2)) {
      form.businessType = '省内'
    } else {
      form.businessType = '跨省'
    }
  }
)

/** 选择申请人后自动填充迁出地址为现居住地 */
async function onApplicantPicked(_resident: { uuid: string }) {
  try {
    const detail = await residentApi.getByUuid(_resident.uuid)
    if (detail) {
      if (detail.areaId != null) {
        form.outgoingAreaId = detail.areaId
      }
      if (detail.residence) {
        form.outgoingDetail = await stripAreaPrefix(detail.residence, detail.areaId)
      }
    }
  } catch { /* ignore */ }
}

async function stripAreaPrefix(fullAddress: string, areaId: number | null | undefined): Promise<string> {
  if (!fullAddress || !areaId) return fullAddress
  try {
    const path: string = await areaApi.getPath(areaId)
    if (path && fullAddress.startsWith(path)) {
      const stripped = fullAddress.substring(path.length)
      return stripped.trim() || fullAddress
    }
  } catch { /* ignore */ }
  return fullAddress
}

// Approve dialog
const showApproveDialog = ref(false)
const approving = ref(false)
const approveAction = ref('')
const rejectReason = ref('')
let approveRid = 0

async function load() {
  loading.value = true
  try {
    const res = await householdApi.listMigration({
      status: statusFilter.value || undefined,
      fromAddress: fromFilter.value || undefined,
      toAddress: toFilter.value || undefined,
      page: page.current, size: page.size,
    })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, {
    applicantUuid: '', outgoingDetail: '', outgoingAreaId: undefined,
    incomingDetail: '', incomingAreaId: undefined,
    businessType: '市内', handleDate: new Date().toISOString().slice(0, 10),
    handleBasis: '', fee: 0, remark: '', attachment: [],
  })
  outgoingPreview.value = ''
  incomingPreview.value = ''
  dialogVisible.value = true
}

function resetForm() { formRef.value?.resetFields(); outgoingPreview.value = ''; incomingPreview.value = '' }

async function handleCreate() {
  if (!form.outgoingAreaId) { showError('请选择迁出地区'); return }
  if (!form.outgoingDetail.trim()) { showError('请填写迁出详址'); return }
  if (!form.incomingAreaId) { showError('请选择迁入地区'); return }
  if (!form.incomingDetail.trim()) { showError('请填写迁入详址'); return }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await householdApi.createMigration({
      applicantUuid: form.applicantUuid,
      outgoingAddress: outgoingPreview.value || form.outgoingDetail,
      outgoingAreaId: form.outgoingAreaId,
      incomingAddress: incomingPreview.value || form.incomingDetail,
      incomingAreaId: form.incomingAreaId,
      businessType: form.businessType,
      handleDate: form.handleDate,
      handleBasis: form.handleBasis,
      fee: form.fee,
      remark: form.remark,
      attachment: form.attachment?.length ? form.attachment.join(',') : '',
    })
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
    const actionText = approveAction.value === '驳回' ? '确认驳回该迁移申请？' : '确认通过该迁移申请？'
    await ElMessageBox.confirm(actionText, '确认操作', { type: 'warning' })
  } catch { showApproveDialog.value = false; return }
  approving.value = true
  try {
    await householdApi.approveMigration(approveRid, approveAction.value, rejectReason.value || undefined)
    showSuccess(approveAction.value === '驳回' ? '已驳回' : '已通过')
    showApproveDialog.value = false
    load()
  } catch (e: any) { showError(e.message || '操作失败') }
  finally { approving.value = false }
}

// Attach material
const showAttachDialog = ref(false)
const attaching = ref(false)
const attachFile = ref<string[]>([])
const attachRemark = ref('')
let attachRid = 0

function openAttach(row: any) {
  attachRid = row.rid
  attachFile.value = []
  attachRemark.value = ''
  showAttachDialog.value = true
}

async function handleAttach() {
  attaching.value = true
  try {
    const fd = new FormData()
    if (attachFile.value.length > 0) fd.append('attachmentPath', attachFile.value.join(','))
    if (attachRemark.value) fd.append('remark', attachRemark.value)
    await householdApi.attachMigrationMaterial(attachRid, fd)
    showSuccess('材料已附加')
    showAttachDialog.value = false
    load()
  } catch (e: any) { showError(e.message || '附加失败') }
  finally { attaching.value = false }
}

onMounted(async () => {
  await loadAreaCodeMap()
  if (route.query.fromAddress) fromFilter.value = route.query.fromAddress as string
  if (route.query.toAddress) toFilter.value = route.query.toAddress as string
  if (route.query.from) fromFilter.value = route.query.from as string
  if (route.query.to) toFilter.value = route.query.to as string
  load()
})
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
.form-tip { display: block; font-size: 11px; color: #909399; line-height: 1.5; }
.address-preview {
  margin: -8px 0 12px 110px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
