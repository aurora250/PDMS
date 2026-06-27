<template>
  <div class="police-page">
    <div class="page-header">
      <h3>民警管理</h3>
      <el-button v-if="hasPermission('auth:police:write')" type="primary" @click="openCreate">新增民警</el-button>
    </div>
    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="policeNo" label="警号" width="120" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="policeRank" label="警衔" width="80" />
        <el-table-column prop="station" label="派出所" width="150" />
        <el-table-column prop="dutyStatus" label="执勤状态" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="hasPermission('auth:police:write')" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('auth:police:write')" text size="small" @click="toggleStatus(row)">
              {{ row.dutyStatus === '在岗' ? '离岗' : '在岗' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑民警' : '新增民警'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item v-if="!editing" label="警号" required><el-input v-model="form.policeNo" /></el-form-item>
        <el-form-item label="姓名" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="警衔"><el-select v-model="form.policeRank">
          <el-option v-for="r in ['警员','警司','警督','警监']" :key="r" :label="r" :value="r" />
        </el-select></el-form-item>
        <el-form-item label="派出所"><el-input v-model="form.station" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
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
import { policeApi } from '@/api/auth'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'

const { hasPermission } = usePermission()
const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
let editNo = ''

const form = reactive({ policeNo: '', name: '', policeRank: '警员', station: '', phone: '' })

async function load() {
  loading.value = true
  try { list.value = await policeApi.list() } catch { /* */ }
  finally { loading.value = false }
}
function openCreate() { Object.assign(form, { policeNo: '', name: '', policeRank: '警员', station: '', phone: '' }); editing.value = false; dialogVisible.value = true }
function openEdit(row: any) { editNo = row.policeNo; Object.assign(form, row); editing.value = true; dialogVisible.value = true }
async function handleSave() {
  try {
    if (editing.value) await policeApi.update(editNo, { ...form })
    else await policeApi.create({ ...form })
    showSuccess(editing.value ? '修改成功' : '新增成功')
    dialogVisible.value = false; load()
  } catch (e: any) { showError(e.message || '操作失败') }
}
async function toggleStatus(row: any) {
  const s = row.dutyStatus === '在岗' ? '离岗' : '在岗'
  try { await policeApi.updateStatus(row.policeNo, s); showSuccess('状态已更新'); load() }
  catch (e: any) { showError(e.message || '操作失败') }
}
onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
