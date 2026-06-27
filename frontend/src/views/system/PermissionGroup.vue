<template>
  <div class="perm-page">
    <div class="page-header">
      <h3>权限组管理</h3>
      <el-button v-if="hasPermission('auth:permission:write')" type="primary" @click="openCreate">新建权限组</el-button>
    </div>
    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="groupId" label="ID" width="60" />
        <el-table-column prop="groupName" label="名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="权限" min-width="300">
          <template #default="{ row }">
            <template v-if="row.permissions">
              <el-tag v-if="row.permissions.includes('*')" type="danger">全部权限</el-tag>
              <el-tag v-else v-for="p in parsePerms(row.permissions)" :key="p" size="small" style="margin:2px">{{ p }}</el-tag>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="hasPermission('auth:permission:write')" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('auth:permission:write')" text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑权限组' : '新建权限组'" width="600px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.groupName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
        <el-form-item label="权限">
          <el-input v-model="form.permissions" type="textarea" :rows="4" placeholder='JSON数组: ["resident:read","fp:write"] 或 ["*"]' />
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
import { permissionGroupApi } from '@/api/auth'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'

const { hasPermission } = usePermission()
const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
let editId = 0

const form = reactive({ groupName: '', description: '', permissions: '[]' })

function parsePerms(s: string): string[] {
  try { return JSON.parse(s) } catch { return [s] }
}

async function load() {
  loading.value = true
  try { list.value = await permissionGroupApi.list() } catch { /* */ }
  finally { loading.value = false }
}
function openCreate() { Object.assign(form, { groupName: '', description: '', permissions: '[]' }); editing.value = false; dialogVisible.value = true }
function openEdit(row: any) { editId = row.groupId; Object.assign(form, row); editing.value = true; dialogVisible.value = true }
async function handleSave() {
  try {
    if (editing.value) await permissionGroupApi.update(editId, { groupName: form.groupName, description: form.description, permissions: form.permissions })
    else await permissionGroupApi.create({ groupName: form.groupName, description: form.description, permissions: form.permissions })
    showSuccess(editing.value ? '修改成功' : '创建成功')
    dialogVisible.value = false; load()
  } catch (e: any) { showError(e.message || '操作失败') }
}
async function handleDelete(row: any) {
  try { await permissionGroupApi.delete(row.groupId); showSuccess('删除成功'); load() }
  catch (e: any) { showError(e.message || '删除失败') }
}
onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
