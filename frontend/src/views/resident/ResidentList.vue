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
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="hasPermission('resident:read')" text size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="hasPermission('resident:write')" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePermission } from '@/composables/usePermission'
import { useGbConstants } from '@/composables/useGbConstants'
import { residentApi } from '@/api/resident'
import { showError, showSuccess } from '@/utils/auth'
import type { Resident } from '@/types/resident'
import ResidentDetail from './ResidentDetail.vue'

const router = useRouter()
const { hasPermission } = usePermission()
const { NATIONS, EDUCATIONS, MARITAL_STATUSES } = useGbConstants()

const list = ref<Resident[]>([])
const loading = ref(false)
const detailRef = ref()
const importVisible = ref(false)

const search = reactive<any>({
  name: '', gender: '', nation: '', nationCode: '',
  educationLevel: '', educationCode: '', maritalStatus: '', householdStatus: '',
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
    await residentApi.delete(row.uuid!)
    showSuccess('删除成功')
    doSearch()
  } catch (e: any) { showError(e.message || '删除失败') }
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

onMounted(doSearch)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.actions { display: flex; gap: 8px; }
</style>
