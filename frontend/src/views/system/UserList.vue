<template>
  <div class="user-page">
    <div class="page-header">
      <h3>用户管理</h3>
      <el-button v-if="hasPermission('auth:user:write')" type="primary" @click="openCreate">创建用户</el-button>
    </div>
    <el-card>
      <el-form inline style="margin-bottom:12px">
        <el-form-item>
          <el-input v-model="userKeyword" placeholder="搜索用户名" clearable @keyup.enter="load" style="width:200px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userRoleFilter" placeholder="全部" clearable @change="load">
            <el-option v-for="r in ROLES" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="userRole" label="角色" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="accountStatus" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.accountStatus === '有效' ? 'success' : 'danger'">{{ row.accountStatus }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="mustChangePassword" label="须改密" width="80">
          <template #default="{ row }">{{ row.mustChangePassword ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="hasPermission('auth:user:write')" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('auth:user:status')" text size="small" @click="toggleStatus(row)">
              {{ row.accountStatus === '有效' ? '禁用' : '启用' }}
            </el-button>
            <el-button v-if="hasPermission('auth:user:write')" text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,prev,pager,next" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑用户' : '创建用户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="uRules" label-width="100px">
        <el-form-item v-if="!editing" label="用户名" prop="username"><el-input v-model="form.username" /></el-form-item>
        <el-form-item v-if="!editing" label="密码" prop="password"><el-input v-model="form.password" type="password" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.userRole">
            <el-option v-for="r in ROLES" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限组">
          <el-select v-model="form.permissionGroupId">
            <el-option v-for="g in groups" :key="g.groupId" :label="g.groupName" :value="g.groupId" />
          </el-select>
        </el-form-item>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { userApi, permissionGroupApi } from '@/api/auth'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import { phoneRule } from '@/utils/validators'

const { hasPermission } = usePermission()
const ROLES = ['系统管理员','市局负责人','数据审查员','采集员','街道办','民警','用户管理员','普通用户']

const list = ref<any[]>([])
const groups = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const formRef = ref()
const page = reactive({ current: 1, size: 20, total: 0 })
const userKeyword = ref('')
const userRoleFilter = ref('')
let editUuid = ''

const defaultForm = () => ({
  username: '', password: '', userRole: '普通用户', permissionGroupId: 8, phone: '',
})
const form = reactive(defaultForm())
const uRules = computed(() => ({
  username: [{ required: true, message: '请输入用户名' }],
  password: editing.value ? [] : [{ required: true, message: '请输入密码' }, { min: 6, message: '至少6位' }],
  phone: [phoneRule],
}))

async function load() {
  loading.value = true
  try {
    const res = await userApi.list({ keyword: userKeyword.value || undefined, userRole: userRoleFilter.value || undefined, page: page.current, size: page.size })
    list.value = res.records || []
    page.total = res.total || 0
  } catch { /* */ } finally { loading.value = false }
}
async function loadGroups() {
  try { groups.value = await permissionGroupApi.list() } catch { /* */ }
}

function openCreate() {
  Object.assign(form, defaultForm()); editing.value = false; dialogVisible.value = true
}
function openEdit(row: any) {
  editUuid = row.userUuid
  Object.assign(form, row); editing.value = true; dialogVisible.value = true
}
async function handleSave() {
  try {
    if (editing.value) {
      await userApi.update(editUuid, { userRole: form.userRole, permissionGroupId: form.permissionGroupId, phone: form.phone })
    } else {
      await userApi.create({
        username: form.username, password: form.password,
        userRole: form.userRole, permissionGroupId: form.permissionGroupId, phone: form.phone,
      })
    }
    showSuccess(editing.value ? '修改成功' : '创建成功')
    dialogVisible.value = false; load()
  } catch (e: any) { showError(e.message || '操作失败') }
}
async function toggleStatus(row: any) {
  // 后端 CHECK 约束: '审批中','有效','冻结','注销','锁定'
  const newStatus = row.accountStatus === '有效' ? '冻结' : '有效'
  const actionText = newStatus === '冻结' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确认${actionText}该用户？`, '确认操作', { type: 'warning' })
    await userApi.updateStatus(row.userUuid, newStatus)
    showSuccess('状态已更新')
    load()
  } catch (e: any) { if (e !== 'cancel') showError(e.message || '操作失败') }
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确认删除该用户？此操作不可恢复。', '确认删除', { type: 'warning' })
    await userApi.delete(row.userUuid)
    showSuccess('删除成功')
    load()
  } catch (e: any) { if (e !== 'cancel') showError(e.message || '删除失败') }
}

onMounted(() => { load(); loadGroups() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
