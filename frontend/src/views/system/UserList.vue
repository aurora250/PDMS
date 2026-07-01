<template>
  <div class="user-page">
    <div class="page-header">
      <h3>用户管理</h3>
      <el-button v-if="hasPermission('auth:user:write') && auth.role !== '用户管理员'" type="primary" @click="openCreate">创建用户</el-button>
    </div>
    <el-card>
      <el-form inline style="margin-bottom:12px">
        <el-form-item>
          <el-input v-model="userKeyword" placeholder="搜索用户名" clearable @keyup.enter="load" style="width:200px" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userRoleFilter" placeholder="全部" clearable @change="load">
            <el-option v-for="r in availableRoles" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="load">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe border>
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
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button v-if="hasPermission('auth:user:write') && auth.role !== '用户管理员'" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('auth:user:status')" text size="small" @click="toggleStatus(row)">
              {{ row.accountStatus === '有效' ? '禁用' : '启用' }}
            </el-button>
            <el-button v-if="hasPermission('auth:user:write')" text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button v-if="hasPermission('auth:user:write') && auth.role !== '用户管理员'" text size="small" type="warning" @click="openResetPassword(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50,100]" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑用户' : '创建用户'" width="500px">
      <el-form ref="formRef" :model="form" :rules="uRules" label-width="100px">
        <el-form-item v-if="!editing" label="用户名" prop="username"><el-input v-model="form.username" placeholder="请输入用户名" maxlength="50" /></el-form-item>
        <el-form-item v-if="!editing" label="密码" prop="password"><el-input v-model="form.password" type="password" placeholder="8-16位密码" maxlength="16" show-password /></el-form-item>
        <el-form-item v-if="!editing" label="实名认证" prop="residentUuid">
          <ResidentPicker v-model="form.residentUuid" placeholder="搜索姓名或身份证号绑定实名信息" />
        </el-form-item>
        <el-form-item label="角色" prop="userRole">
          <el-select v-model="form.userRole">
            <el-option v-for="r in availableRoles" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限组" prop="permissionGroupId">
          <el-select v-model="form.permissionGroupId">
            <el-option v-for="g in groups" :key="g.groupId" :label="g.groupName" :value="g.groupId" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话" prop="phone"><el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdDialogVisible" title="重置密码" width="400px">
      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
        <el-form-item label="新密码" prop="password">
          <el-input v-model="pwdForm.password" type="password" placeholder="8-16位密码" maxlength="16" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { userApi, permissionGroupApi } from '@/api/auth'
import { usePermission } from '@/composables/usePermission'
import { useAuthStore } from '@/stores/auth'
import { showError, showSuccess } from '@/utils/auth'
import { phoneRule } from '@/utils/validators'
import ResidentPicker from '@/components/ResidentPicker.vue'

const { hasPermission } = usePermission()
const auth = useAuthStore()

const ALL_ROLES = ['系统管理员','市局负责人','数据审查员','采集员','街道办','民警','用户管理员','普通用户']
const USER_ADMIN_ROLES = ['普通用户', '采集员', '街道办']

/** 用户管理员只能看到/管理这三种角色 */
const availableRoles = computed(() =>
  auth.role === '用户管理员' ? USER_ADMIN_ROLES : ALL_ROLES
)

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
  username: '', password: '', residentUuid: '', userRole: '普通用户', permissionGroupId: null as any, phone: '',
})
const form = reactive(defaultForm())

const uRules = computed(() => ({
  username: [{ required: true, message: '请输入用户名' }],
  password: editing.value ? [] : [{ required: true, message: '请输入密码' }, { min: 8, max: 16, message: '密码长度需在8-16位之间' }],
  residentUuid: editing.value ? [] : [{ required: true, message: '请选择关联居民完成实名认证', trigger: 'change' }],
  userRole: [{ required: true, message: '请选择角色', trigger: 'change' }],
  permissionGroupId: [{ required: true, message: '请选择权限组', trigger: 'change' }],
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
  // silent 抑制 403 弹窗：非管理员角色无 auth:permission:write 权限
  try { groups.value = await permissionGroupApi.list({ silent: true } as any) } catch { /* */ }
}

function openCreate() {
  Object.assign(form, defaultForm())
  editing.value = false
  dialogVisible.value = true
}
function openEdit(row: any) {
  editUuid = row.userUuid
  Object.assign(form, row)
  editing.value = true
  dialogVisible.value = true
}
async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (valid === false) return
  try {
    if (editing.value) {
      await userApi.update(editUuid, { userRole: form.userRole, permissionGroupId: form.permissionGroupId, phone: form.phone })
    } else {
      await userApi.create({
        username: form.username, password: form.password,
        residentUuid: form.residentUuid, userRole: form.userRole,
        permissionGroupId: form.permissionGroupId, phone: form.phone,
      })
    }
    showSuccess(editing.value ? '修改成功' : '创建成功')
    dialogVisible.value = false; load()
  } catch (e: any) { showError(e.message || '操作失败') }
}
async function toggleStatus(row: any) {
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

// ====== 重置密码 ======
const pwdDialogVisible = ref(false)
const pwdFormRef = ref()
const pwdForm = reactive({ password: '' })
let resetUuid = ''
const pwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 16, message: '密码长度需在8-16位之间', trigger: 'blur' },
  ],
}
function openResetPassword(row: any) {
  resetUuid = row.userUuid
  pwdForm.password = ''
  pwdDialogVisible.value = true
}
async function handleResetPassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (valid === false) return
  try {
    await userApi.resetPassword(resetUuid, pwdForm.password)
    showSuccess('密码重置成功')
    pwdDialogVisible.value = false
  } catch (e: any) { showError(e.message || '重置失败') }
}

onMounted(() => { load(); loadGroups() })
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
