<template>
  <div class="police-page">
    <div class="page-header">
      <h3>民警管理</h3>
      <el-button v-if="hasPermission('auth:police:write')" type="primary" @click="openCreate">新增民警</el-button>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="搜索">
          <el-input v-model="keyword" placeholder="姓名/警号" clearable @keyup.enter="load" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="policeNumber" label="警号" width="140" />
        <el-table-column label="姓名" width="100">
          <template #default="{ row }">{{ row.name || row.policeNumber }}</template>
        </el-table-column>
        <el-table-column prop="policeRank" label="警衔" width="80" />
        <el-table-column prop="policeStation" label="派出所" min-width="150" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="dutyStatus" label="执勤状态" width="100" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="hasPermission('auth:police:write')" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button text size="small" @click="showDetail(row)">详情</el-button>
            <el-button v-if="hasPermission('auth:police:write')" text size="small" @click="toggleStatus(row)">
              {{ row.dutyStatus === '在岗' ? '离职' : '在岗' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,prev,pager,next" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑民警' : '新增民警'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item v-if="!editing" label="警号" required>
          <el-input v-model="form.policeNumber" placeholder="如: P20260001" />
        </el-form-item>
        <el-form-item label="居民UUID">
          <ResidentPicker v-model="form.residentUuid" placeholder="搜索姓名或身份证号选择关联居民" />
        </el-form-item>
        <el-form-item label="警衔">
          <el-select v-model="form.policeRank" style="width:100%">
            <el-option v-for="r in ['警员','警司','警督','警监']" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="派出所">
          <el-input v-model="form.policeStation" />
        </el-form-item>
        <el-form-item label="部门">
          <el-input v-model="form.department" placeholder="如: 治安大队" />
        </el-form-item>
        <el-form-item label="辖区">
          <AreaCascader v-model="form.areaId" />
          <el-input v-model="form.jurisdiction" placeholder="详细地址，如: 某某社区" style="margin-top:8px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { policeApi } from '@/api/auth'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import ResidentPicker from '@/components/ResidentPicker.vue'
import AreaCascader from '@/components/AreaCascader.vue'

const { hasPermission } = usePermission()
const list = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')
const page = reactive({ current: 1, size: 20, total: 0 })
const dialogVisible = ref(false)
const editing = ref(false)
let editNo = ''

const form = reactive({
  policeNumber: '', residentUuid: '', policeRank: '警员',
  policeStation: '', department: '', jurisdiction: '', areaId: undefined as number | undefined,
})

async function load() {
  loading.value = true
  try {
    const res = await policeApi.list({ keyword: keyword.value || undefined, page: page.current, size: page.size })
    list.value = res.records || []
    page.total = res.total || 0
  } catch { /* */ }
  finally { loading.value = false }
}

function openCreate() {
  Object.assign(form, { policeNumber: '', residentUuid: '', policeRank: '警员', policeStation: '', department: '', jurisdiction: '', areaId: undefined })
  editing.value = false; dialogVisible.value = true
}

function openEdit(row: any) {
  editNo = row.policeNumber
  Object.assign(form, row)
  editing.value = true; dialogVisible.value = true
}

function showDetail(row: any) {
  editNo = row.policeNumber
  Object.assign(form, row)
  editing.value = true; dialogVisible.value = true
}

async function handleSave() {
  try {
    if (editing.value) await policeApi.update(editNo, { ...form })
    else await policeApi.create({ ...form })
    showSuccess(editing.value ? '修改成功' : '新增成功')
    dialogVisible.value = false; load()
  } catch (e: any) { showError(e.message || '操作失败') }
}

async function toggleStatus(row: any) {
  const s = row.dutyStatus === '在岗' ? '离职' : '在岗'
  const actionText = s === '离职' ? '确认将该民警标记为离职？' : '确认将该民警恢复为在岗？'
  try {
    await ElMessageBox.confirm(actionText, '确认操作', { type: 'warning' })
    await policeApi.updateStatus(row.policeNumber, s); showSuccess('状态已更新'); load()
  } catch (e: any) { if (e !== 'cancel') showError(e.message || '操作失败') }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
