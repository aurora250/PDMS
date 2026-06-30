<template>
  <div>
    <div class="page-header">
      <h3>户口簿管理</h3>
      <el-button v-if="hasPermission('household:write')" type="primary" @click="openApply">申领户口簿</el-button>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="搜索">
          <el-input v-model="kw" placeholder="户口簿号/户主/地址" clearable @keyup.enter="load" style="width:200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="bookStatus" placeholder="全部" clearable @change="load">
            <el-option label="有效" value="有效" /><el-option label="冻结" value="冻结" />
            <el-option label="无效" value="无效" /><el-option label="审批中" value="审批中" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="householdBookNo" label="户口簿号" width="200" />
        <el-table-column prop="householderName" label="户主" width="120" />
        <el-table-column prop="hukouAddress" label="户籍地址" min-width="200" />
        <el-table-column prop="establishDate" label="成立日期" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><ApprovalBadge :status="row.status" /></template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button text size="small" @click="handleReissue(row)">补办</el-button>
            <el-button text size="small" @click="handleRenew(row)">换发</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,prev,pager,next" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 申领对话框 -->
    <el-dialog v-model="showApply" title="申领户口簿" width="550px" @close="resetApplyForm">
      <el-form ref="applyFormRef" :model="applyForm" :rules="applyRules" label-width="110px">
        <el-form-item label="户口簿号">
          <el-input v-model="applyForm.householdBookNo" placeholder="自动生成或手动输入" />
        </el-form-item>
        <el-form-item label="户主" prop="householderUuid">
          <ResidentPicker v-model="applyForm.householderUuid" placeholder="搜索姓名或身份证号选择户主" />
        </el-form-item>
        <el-form-item label="户籍地址" prop="hukouAddress">
          <el-input v-model="applyForm.hukouAddress" placeholder="详细户籍地址" />
        </el-form-item>
        <el-form-item label="户籍区域" prop="hukouAreaId">
          <AreaCascader v-model="applyForm.hukouAreaId" />
        </el-form-item>
        <el-form-item label="成立日期" prop="establishDate">
          <el-date-picker v-model="applyForm.establishDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="成员UUID列表">
          <el-input v-model="applyForm.memberUuidList" type="textarea" placeholder="逗号分隔，如: uuid1,uuid2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApply = false">取消</el-button>
        <el-button type="primary" @click="handleApply" :loading="applying">确认申领</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { householdApi } from '@/api/household'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import AreaCascader from '@/components/AreaCascader.vue'
import ApprovalBadge from '@/components/ApprovalBadge.vue'
import ResidentPicker from '@/components/ResidentPicker.vue'

const { hasPermission } = usePermission()
const route = useRoute()
const list = ref<any[]>([])
const loading = ref(false)
const kw = ref('')
const bookStatus = ref('')
const page = reactive({ current: 1, size: 20, total: 0 })

const showApply = ref(false)
const applying = ref(false)
const applyFormRef = ref()
const applyForm = reactive({
  householdBookNo: '', householderUuid: '', hukouAddress: '',
  hukouAreaId: undefined as number | undefined, establishDate: new Date().toISOString().slice(0, 10),
  memberUuidList: '',
})

const applyRules = {
  householderUuid: [{ required: true, message: '请输入户主UUID', trigger: 'blur' }],
  hukouAddress: [{ required: true, message: '请输入户籍地址', trigger: 'blur' }],
  establishDate: [{ required: true, message: '请选择成立日期', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const res = await householdApi.searchBook({ keyword: kw.value || undefined, status: bookStatus.value || undefined, page: page.current, size: page.size })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function handleReissue(row: any) {
  try { await householdApi.reissueBook({ bookNo: row.householdBookNo }); showSuccess('补办申请已提交'); load() } catch { /* ignore */ }
}

async function handleRenew(row: any) {
  try { await householdApi.renewBook({ bookNo: row.householdBookNo }); showSuccess('换发申请已提交'); load() } catch { /* ignore */ }
}

function openApply() {
  Object.assign(applyForm, {
    householdBookNo: '', householderUuid: '', hukouAddress: '',
    hukouAreaId: undefined, establishDate: new Date().toISOString().slice(0, 10), memberUuidList: '',
  })
  showApply.value = true
}

function resetApplyForm() { applyFormRef.value?.resetFields() }

async function handleApply() {
  const valid = await applyFormRef.value?.validate().catch(() => false)
  if (!valid) return
  applying.value = true
  try {
    await householdApi.applyBook({ ...applyForm })
    showSuccess('申领成功')
    showApply.value = false
    load()
  } catch (e: any) { showError(e.message || '申领失败') }
  finally { applying.value = false }
}

onMounted(() => {
  if (route.query.province) kw.value = route.query.province as string
  load()
})
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
</style>
