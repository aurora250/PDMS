<template>
  <div>
    <div class="page-header">
      <h3>居住地管理</h3>
      <el-button v-if="hasPermission('fp:residence:write')" type="primary" @click="openCreate">新增登记</el-button>
    </div>
    <el-card>
      <el-form inline style="margin-bottom:12px">
        <el-form-item>
          <el-input v-model="keyword" placeholder="搜索UUID/地址" clearable @keyup.enter="load" style="width:260px" />
        </el-form-item>
        <el-form-item><el-button @click="load">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column label="UUID" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button v-if="row.uuid" text size="small" type="primary" @click="$router.push(`/resident/${row.uuid}`)">{{ row.uuid }}</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="currentAddress" label="现地址" min-width="200" />
        <el-table-column prop="addressType" label="类型" width="100" />
        <el-table-column prop="purpose" label="目的" width="80" />
        <el-table-column prop="registerDate" label="登记日期" width="120" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button v-if="hasPermission('fp:delete')" text size="small" type="danger" @click="del(row)">注销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50,100]" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 登记/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑居住地' : '登记居住地'" width="650px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="居民UUID" prop="uuid">
          <ResidentPicker v-model="form.uuid" placeholder="搜索姓名或身份证号选择居民"
            @pick="onResidentPicked" :disabled="isEdit" />
        </el-form-item>

        <!-- 原始地址（自动填充选中居民的现居住地，只读） -->
        <el-form-item label="原始地区" required>
          <AreaCascader v-model="origAreaId" disabled placeholder="选择居民后自动填充" />
        </el-form-item>
        <el-form-item label="原始详址">
          <el-input v-model="origDetail" disabled placeholder="选择居民后自动填充" />
          <span v-if="!isEdit" class="form-tip">根据选中居民的现居住地自动填充</span>
        </el-form-item>
        <div v-if="origPreview" class="address-preview">
          <el-text type="info" size="small">原始地址预览: {{ origPreview }}</el-text>
        </div>

        <!-- 现居住地址（用户填写新的居住地） -->
        <el-form-item label="现住地区" required>
          <AreaCascader v-model="currAreaId" placeholder="选择现居住地省市区" />
        </el-form-item>
        <el-form-item label="现住详址">
          <el-input v-model="currDetail" placeholder="街道/路/号/楼/室" />
        </el-form-item>
        <div v-if="currPreview" class="address-preview">
          <el-text type="info" size="small">现住地址预览: {{ currPreview }}</el-text>
        </div>

        <el-form-item label="居住类型" prop="addressType">
          <el-select v-model="form.addressType" style="width:100%">
            <el-option label="租赁房屋" value="租赁房屋" />
            <el-option label="自有住房" value="自有住房" />
            <el-option label="单位宿舍" value="单位宿舍" />
            <el-option label="学校宿舍" value="学校宿舍" />
            <el-option label="亲友借住" value="亲友借住" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="居住目的" prop="purpose">
          <el-select v-model="form.purpose" style="width:100%">
            <el-option label="务工" value="务工" />
            <el-option label="经商" value="经商" />
            <el-option label="投靠亲属" value="投靠亲属" />
            <el-option label="求学" value="求学" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="工作单位">
          <el-input v-model="form.workUnit" placeholder="可选" />
        </el-form-item>
        <el-form-item label="预计时长">
          <el-select v-model="form.expectedDuration" style="width:100%">
            <el-option label="短租" value="短租" /><el-option label="中租" value="中租" />
            <el-option label="长租" value="长租" />
          </el-select>
        </el-form-item>
        <el-form-item label="登记日期" prop="registerDate">
          <el-date-picker v-model="form.registerDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">{{ isEdit ? '保存' : '确认登记' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { floatingApi } from '@/api/floating'
import { residentApi } from '@/api/resident'
import { areaApi } from '@/api/area'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import AreaCascader from '@/components/AreaCascader.vue'
import ResidentPicker from '@/components/ResidentPicker.vue'

const { hasPermission } = usePermission()
const route = useRoute()
const list = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
let editRid = 0

const origAreaId = ref<number | undefined>(undefined)
const origDetail = ref('')
const origPreview = ref('')
const currAreaId = ref<number | undefined>(undefined)
const currDetail = ref('')
const currPreview = ref('')

const defaultForm = () => ({
  uuid: '', addressType: '租赁房屋', purpose: '务工', workUnit: '', expectedDuration: '',
  registerDate: new Date().toISOString().slice(0, 10),
})
const form = reactive(defaultForm())

const rules = {
  uuid: [{ required: true, message: '请选择居民', trigger: 'change' }],
  addressType: [{ required: true, message: '请选择居住类型', trigger: 'change' }],
  purpose: [{ required: true, message: '请选择居住目的', trigger: 'change' }],
  registerDate: [{ required: true, message: '请选择登记日期', trigger: 'change' }],
}

const keyword = ref('')

// ─── 地址预览工具函数 ───
async function getAreaPath(areaId: number | null | undefined): Promise<string> {
  if (!areaId) return ''
  try { return await areaApi.getPath(areaId) } catch { return '' }
}

async function buildPreview(areaId: number | null | undefined, detail: string): Promise<string> {
  const path = await getAreaPath(areaId)
  return (path || '') + (detail || '')
}

async function stripAreaPrefix(fullAddress: string, areaId: number | null | undefined): Promise<string> {
  if (!fullAddress || !areaId) return fullAddress
  try {
    const path = await getAreaPath(areaId)
    if (path && fullAddress.startsWith(path)) {
      return fullAddress.substring(path.length).trim() || fullAddress
    }
  } catch { /* ignore */ }
  return fullAddress
}

// ─── 地址预览 watchers ───
watch([origAreaId, origDetail], async () => {
  origPreview.value = await buildPreview(origAreaId.value, origDetail.value)
})
watch([currAreaId, currDetail], async () => {
  currPreview.value = await buildPreview(currAreaId.value, currDetail.value)
})

// ─── 选择居民 → 自动填充原始地址 ───
async function onResidentPicked(_resident: { uuid: string }) {
  try {
    const detail = await residentApi.getByUuid(_resident.uuid)
    if (detail) {
      if (detail.areaId != null) origAreaId.value = detail.areaId
      origDetail.value = await stripAreaPrefix(detail.residence || '', detail.areaId)
    }
  } catch { /* ignore */ }
}

// ─── 重置 ───
function clearAddressState() {
  origAreaId.value = undefined; origDetail.value = ''; origPreview.value = ''
  currAreaId.value = undefined; currDetail.value = ''; currPreview.value = ''
}

async function load() {
  loading.value = true
  try {
    const res = await floatingApi.listResidence({ keyword: keyword.value || undefined, page: page.current, size: page.size })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function del(row: any) {
  try {
    await ElMessageBox.confirm('确认注销该居住地登记？', '确认注销', { type: 'warning' })
    await floatingApi.deleteResidence(row.rid); showSuccess('已注销'); load()
  } catch { /* ignore */ }
}

function openCreate() {
  Object.assign(form, defaultForm())
  clearAddressState()
  isEdit.value = false
  dialogVisible.value = true
}

async function openEdit(row: any) {
  clearAddressState()
  form.uuid = row.uuid || ''
  form.addressType = row.addressType || '租赁房屋'
  form.purpose = row.purpose || '务工'
  form.workUnit = row.workUnit || ''
  form.expectedDuration = row.expectedDuration || ''
  form.registerDate = row.registerDate || new Date().toISOString().slice(0, 10)
  isEdit.value = true
  editRid = row.rid

  // 原始地址：从 stored areaId 反拆分
  if (row.originalAddress && row.originalAreaId) {
    origAreaId.value = row.originalAreaId
    origDetail.value = await stripAreaPrefix(row.originalAddress, row.originalAreaId)
  } else if (row.originalAddress) {
    origDetail.value = row.originalAddress
  }
  // 现住地址：从 stored areaId 反拆分
  if (row.areaId) {
    currAreaId.value = row.areaId
    currDetail.value = await stripAreaPrefix(row.currentAddress || '', row.areaId)
  } else {
    currDetail.value = row.currentAddress || ''
  }

  dialogVisible.value = true
}

function resetForm() { formRef.value?.resetFields(); clearAddressState() }

async function handleSave() {
  if (!currAreaId.value) { showError('请选择现住地区'); return }
  if (!currDetail.value.trim()) { showError('请填写现住详址'); return }

  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const originalAddress = origPreview.value || origDetail.value || ''
    const currentAddress = currPreview.value || currDetail.value

    const payload = {
      ...form,
      originalAddress,
      currentAddress,
      areaId: currAreaId.value,
    }

    if (isEdit.value) {
      await floatingApi.updateResidence(editRid, payload)
      showSuccess('更新成功')
    } else {
      await floatingApi.createResidence(payload)
      showSuccess('登记成功')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) { showError(e.message || '保存失败') }
  finally { saving.value = false }
}

onMounted(() => {
  if (route.query.keyword) {
    keyword.value = route.query.keyword as string
  }
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
