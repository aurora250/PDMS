<template>
  <div class="resident-list">
    <div class="page-header">
      <h3>常住人口管理</h3>
      <div class="actions">
        <el-button v-if="hasPermission('resident:import')" @click="importVisible = true">导入Excel</el-button>
        <el-button v-if="hasPermission('resident:export')" @click="handleExport">导出Excel</el-button>
        <el-button v-if="hasPermission('resident:write')" type="primary" @click="openCreate">新增人口</el-button>
      </div>
    </div>

    <!-- 搜索表单 -->
    <el-card style="margin-bottom:16px">
      <el-form :model="search" inline>
        <el-form-item label="姓名"><el-input v-model="search.name" placeholder="姓名" clearable /></el-form-item>
        <el-form-item label="性别">
          <el-select v-model="search.gender" placeholder="全部" clearable>
            <el-option label="男" value="男" /><el-option label="女" value="女" />
          </el-select>
        </el-form-item>
        <el-form-item label="民族">
          <el-select v-model="search.nation" placeholder="全部" clearable @change="onNationChange">
            <el-option v-for="n in NATIONS" :key="n.code" :label="n.name" :value="n.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="学历">
          <el-select v-model="search.educationLevel" placeholder="全部" clearable @change="onEducationChange">
            <el-option v-for="e in EDUCATIONS" :key="e.code" :label="e.name" :value="e.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="婚姻状况">
          <el-select v-model="search.maritalStatus" placeholder="全部" clearable>
            <el-option v-for="m in MARITAL_STATUSES" :key="m.code" :label="m.name" :value="m.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="户口状态">
          <el-select v-model="search.householdStatus" placeholder="全部" clearable>
            <el-option label="正常" value="正常" /><el-option label="迁出注销" value="迁出注销" />
            <el-option label="死亡注销" value="死亡注销" /><el-option label="失踪注销" value="失踪注销" />
            <el-option label="恢复" value="恢复" />
          </el-select>
        </el-form-item>
        <el-form-item label="省份">
          <el-select v-model="search.province" placeholder="全部" clearable filterable @change="doSearch">
            <el-option v-for="p in PROVINCES" :key="p" :label="p" :value="p" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="doSearch">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card>
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="idCardNo" label="身份证号" width="180" />
        <el-table-column prop="nation" label="民族" width="80" />
        <el-table-column label="民族代码" width="80">
          <template #default="{ row }">{{ row.nationCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="educationLevel" label="学历" width="120" />
        <el-table-column prop="maritalStatus" label="婚姻" width="80" />
        <el-table-column prop="phone" label="电话" width="120" />
        <el-table-column prop="householdStatus" label="户口状态" width="100" />
        <el-table-column prop="residence" label="居住地址" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="210">
          <template #default="{ row }">
            <el-button v-if="hasPermission('resident:read')" text size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="hasPermission('resident:write')" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('resident:write')" text size="small" type="warning" @click="$router.push(`/resident/${row.uuid}/relations`)">关系</el-button>
            <el-button v-if="hasPermission('resident:delete')" text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size"
          :total="pagination.total" :page-sizes="[10,20,50,100]" layout="total,sizes,prev,pager,next"
          @current-change="doSearch" @size-change="doSearch" />
      </div>
    </el-card>

    <!-- 新增/编辑抽屉 -->
    <ResidentDetail ref="detailRef" @saved="doSearch" />

    <!-- 导入Excel对话框 -->
    <el-dialog v-model="importVisible" title="导入Excel" width="400px">
      <el-upload
        ref="uploadRef"
        :auto-upload="false"
        :limit="1"
        accept=".xlsx,.xls"
        :on-change="onFileChange"
        :file-list="fileList"
        drag
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽Excel文件到此处或点击上传</div>
      </el-upload>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImport" :loading="importing" :disabled="!uploadFile">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { UploadFilled } from '@element-plus/icons-vue'
import { usePermission } from '@/composables/usePermission'
import { useGbConstants } from '@/composables/useGbConstants'
import { residentApi } from '@/api/resident'
import { ElMessageBox } from 'element-plus'
import { showError, showSuccess } from '@/utils/auth'
import type { Resident } from '@/types/resident'
import ResidentDetail from './ResidentDetail.vue'
import { PROVINCES } from '@/utils/constants'

const router = useRouter()
const route = useRoute()
const { hasPermission } = usePermission()
const { NATIONS, EDUCATIONS, MARITAL_STATUSES } = useGbConstants()

const list = ref<Resident[]>([])
const loading = ref(false)
const detailRef = ref()
const importVisible = ref(false)
const uploading = ref(false)
const importing = ref(false)
const uploadFile = ref<File | null>(null)
const fileList = ref<any[]>([])

function onFileChange(file: any) {
  uploadFile.value = file.raw
  fileList.value = [file]
}

async function handleImport() {
  if (!uploadFile.value) return
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', uploadFile.value)
    await residentApi.import(fd)
    showSuccess('导入成功')
    importVisible.value = false
    fileList.value = []
    uploadFile.value = null
    doSearch()
  } catch (e: any) { showError(e.message || '导入失败') }
  finally { importing.value = false }
}

const search = reactive<any>({
  name: '', gender: '', nation: '', nationCode: '',
  educationLevel: '', educationCode: '', maritalStatus: '',
  householdStatus: '', province: '',
})

const pagination = reactive({ current: 1, size: 20, total: 0 })

function onNationChange() {
  search.nationCode = search.nation ? NATION_CODE_MAP[search.nation] || '' : ''
}
function onEducationChange() {
  search.educationCode = search.educationLevel ? EDUCATION_NAME_MAP[search.educationLevel] || '' : ''
}

import { NATION_CODE_MAP, EDUCATION_NAME_MAP } from '@/composables/useGbConstants'

async function doSearch() {
  loading.value = true
  try {
    const res = await residentApi.search({
      ...search,
      page: pagination.current,
      size: pagination.size,
    })
    list.value = res.records || []
    pagination.total = res.total || 0
  } catch (e: any) {
    showError(e.message || '搜索失败')
  } finally { loading.value = false }
}

function resetSearch() {
  Object.keys(search).forEach(k => search[k] = '')
  doSearch()
}

function openCreate() { detailRef.value?.open() }
function openDetail(row: Resident) { router.push(`/resident/${row.uuid}`) }
function openEdit(row: Resident) { detailRef.value?.open(row) }

async function handleDelete(row: Resident) {
  try {
    await ElMessageBox.confirm('确认删除该常住人口记录？此操作不可恢复。', '确认删除', { type: 'warning' })
    await residentApi.delete(row.uuid!)
    showSuccess('删除成功')
    doSearch()
  } catch (e: any) { if (e !== 'cancel') showError(e.message || '删除失败') }
}

async function handleExport() {
  try {
    const blob = await residentApi.export(search)
    const url = URL.createObjectURL(blob as any)
    const a = document.createElement('a')
    a.href = url; a.download = 'resident-export.xlsx'; a.click()
    URL.revokeObjectURL(url)
  } catch (e: any) { showError('导出失败') }
}

onMounted(() => {
  if (route.query.province) search.province = route.query.province as string
  doSearch()
})
watch(() => route.query.province, (val) => {
  if (val) { search.province = val as string; doSearch() }
})
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.actions { display: flex; gap: 8px; }

/* 搜索表单下拉框最小宽度，确保选中后文字可见 */
.el-select {
  min-width: 120px;
}
/* 省份下拉框需要更宽 */
.el-form-item:last-of-type .el-select,
.el-form-item [class*="province"] ~ .el-select {
  min-width: 140px;
}
</style>
