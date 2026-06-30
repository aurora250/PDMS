<template>
  <div class="perm-page">
    <div class="page-header">
      <h3>权限组管理</h3>
      <el-button v-if="hasPermission('auth:permission:write')" type="primary" @click="openCreate">新建权限组</el-button>
    </div>
    <el-card>
      <el-table :data="pageList" v-loading="loading" stripe border>
        <el-table-column prop="groupId" label="ID" width="60" />
        <el-table-column prop="groupName" label="名称" width="150" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="权限" min-width="300">
          <template #default="{ row }">
            <template v-if="row.permissions">
              <el-tag v-if="parsePerms(row.permissions).includes('*')" type="danger">全部权限</el-tag>
              <template v-else>
                <el-tag v-for="p in parsePerms(row.permissions).slice(0, 6)" :key="p" size="small" style="margin:2px">{{ p }}</el-tag>
                <el-tag v-if="parsePerms(row.permissions).length > 6" size="small" type="info" style="margin:2px">
                  +{{ parsePerms(row.permissions).length - 6 }} 更多
                </el-tag>
              </template>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="hasPermission('auth:permission:write') && row.groupName !== '系统管理员组'" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('auth:permission:write') && row.groupName !== '系统管理员组'" text size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="list.length"
          layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50,100]" background small />
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="editing ? '编辑权限组' : '新建权限组'" width="750px" top="5vh">
      <el-form ref="permFormRef" :model="form" :rules="permRules" label-width="80px">
        <el-form-item label="名称" prop="groupName"><el-input v-model="form.groupName" placeholder="权限组名称" maxlength="50" /></el-form-item>
        <el-form-item label="描述" prop="description"><el-input v-model="form.description" placeholder="权限组描述" maxlength="200" /></el-form-item>
        <el-form-item label="权限">
          <div class="perm-panel">
            <div class="perm-toolbar">
              <el-checkbox v-model="checkAll" :indeterminate="isIndeterminate" @change="toggleAll">全选</el-checkbox>
              <el-button text size="small" type="danger" @click="clearAll">清空</el-button>
            </div>
            <el-collapse v-model="activeModules">
              <el-collapse-item v-for="mod in PERM_MODULES" :key="mod.key" :name="mod.key">
                <template #title>
                  <div class="module-title">
                    <el-checkbox
                      :model-value="moduleChecked(mod.key)"
                      :indeterminate="moduleIndeterminate(mod.key)"
                      @click.stop
                      @change="toggleModule(mod.key, $event)"
                    />
                    <span style="margin-left:8px;font-weight:500">{{ mod.label }}</span>
                    <span class="module-desc">({{ mod.desc }})</span>
                  </div>
                </template>
                <el-checkbox-group v-model="checkedPerms" class="perm-grid">
                  <el-checkbox v-for="perm in mod.perms" :key="perm" :value="perm" :label="perm">
                    {{ perm }}
                  </el-checkbox>
                </el-checkbox-group>
              </el-collapse-item>
            </el-collapse>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">{{ editing ? '保存修改' : '确认创建' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { permissionGroupApi } from '@/api/auth'
import { usePermission } from '@/composables/usePermission'
import { ElMessageBox } from 'element-plus'
import { showError, showSuccess } from '@/utils/auth'

const { hasPermission } = usePermission()

const PERM_MODULES = [
  {
    key: 'resident', label: '常住人口', desc: '人口信息管理',
    perms: ['resident:read', 'resident:write', 'resident:delete', 'resident:export', 'resident:import'],
  },
  {
    key: 'household', label: '户籍管理', desc: '户口簿/业务/迁移/证件',
    perms: ['household:read', 'household:write', 'household:approve', 'household:second-approve', 'household:material:attach'],
  },
  {
    key: 'fp', label: '流动人口', desc: '登记/居住地/居住证',
    perms: ['fp:read', 'fp:write', 'fp:delete', 'fp:review', 'fp:permit:approve', 'fp:permit:issue', 'fp:residence:write'],
  },
  {
    key: 'keyperson', label: '重点人员', desc: '列管/走访/信访',
    perms: ['keyperson:read', 'keyperson:write', 'keyperson:delete', 'keyperson:review', 'keyperson:visit-plan:write', 'keyperson:petition:write'],
  },
  {
    key: 'missing', label: '失踪人口', desc: '登记/寻回/统计',
    perms: ['missing:read', 'missing:write', 'missing:delete', 'missing:review', 'missing:recovery:write'],
  },
  {
    key: 'alert', label: '预警中心', desc: '预警查看/处理',
    perms: ['alert:read', 'alert:handle'],
  },
  {
    key: 'log', label: '日志审计', desc: '审计/登录日志',
    perms: ['log:audit:read', 'log:login:read', 'log:export'],
  },
  {
    key: 'auth', label: '系统管理', desc: '用户/民警/权限组',
    perms: ['auth:user:read', 'auth:user:write', 'auth:user:status', 'auth:police:read', 'auth:police:write', 'auth:permission:write'],
  },
  {
    key: 'statistics', label: '仪表盘', desc: '统计查看',
    perms: ['statistics:read'],
  },
]

const ALL_PERMS = PERM_MODULES.flatMap(m => m.perms)

const list = ref<any[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const editing = ref(false)
const saving = ref(false)
let editId = 0

const page = reactive({ current: 1, size: 20 })
const activeModules = ref<string[]>([])

const pageList = computed(() => {
  const start = (page.current - 1) * page.size
  return list.value.slice(start, start + page.size)
})

const form = reactive({ groupName: '', description: '', permissions: '[]' })
const checkedPerms = ref<string[]>([])
const permFormRef = ref()
const permRules = {
  groupName: [{ required: true, message: '请输入权限组名称', trigger: 'blur' }, { max: 50, message: '名称不超过50字', trigger: 'blur' }],
  description: [{ max: 200, message: '描述不超过200字', trigger: 'blur' }],
}

const checkAll = computed(() => checkedPerms.value.length === ALL_PERMS.length)
const isIndeterminate = computed(() => checkedPerms.value.length > 0 && checkedPerms.value.length < ALL_PERMS.length)

function parsePerms(s: string): string[] {
  try { return JSON.parse(s) } catch { return [s] }
}

function moduleChecked(key: string): boolean {
  const modPerms = PERM_MODULES.find(m => m.key === key)!.perms
  return modPerms.every(p => checkedPerms.value.includes(p))
}

function moduleIndeterminate(key: string): boolean {
  const modPerms = PERM_MODULES.find(m => m.key === key)!.perms
  const checked = modPerms.filter(p => checkedPerms.value.includes(p)).length
  return checked > 0 && checked < modPerms.length
}

function toggleModule(key: string, val: boolean) {
  const modPerms = PERM_MODULES.find(m => m.key === key)!.perms
  if (val) {
    modPerms.forEach(p => { if (!checkedPerms.value.includes(p)) checkedPerms.value.push(p) })
  } else {
    checkedPerms.value = checkedPerms.value.filter(p => !modPerms.includes(p))
  }
}

function toggleAll(val: boolean) {
  checkedPerms.value = val ? [...ALL_PERMS] : []
}

function clearAll() {
  checkedPerms.value = []
}

async function load() {
  loading.value = true
  try { list.value = await permissionGroupApi.list() } catch { /* */ }
  finally { loading.value = false }
}

function openCreate() {
  form.groupName = ''; form.description = ''; form.permissions = '[]'
  checkedPerms.value = []
  activeModules.value = []
  editing.value = false; dialogVisible.value = true
}

function openEdit(row: any) {
  editId = row.groupId
  form.groupName = row.groupName || ''
  form.description = row.description || ''
  editId = row.groupId
  const perms = parsePerms(row.permissions || '[]')
  checkedPerms.value = perms.filter((p: string) => p !== '*')
  activeModules.value = PERM_MODULES
    .filter(m => m.perms.some(p => checkedPerms.value.includes(p)))
    .map(m => m.key)
  editing.value = true; dialogVisible.value = true
}

async function handleSave() {
  const valid = await permFormRef.value?.validate().catch(() => false)
  if (valid === false) return
  if (checkedPerms.value.length === 0) { showError('请至少选择一个权限'); return }
  saving.value = true
  try {
    const data = {
      groupName: form.groupName,
      description: form.description,
      permissions: JSON.stringify(checkedPerms.value),
    }
    if (editing.value) {
      await permissionGroupApi.update(editId, data)
    } else {
      await permissionGroupApi.create(data)
    }
    showSuccess(editing.value ? '修改成功' : '创建成功')
    dialogVisible.value = false; load()
  } catch (e: any) { showError(e.message || '操作失败') }
  finally { saving.value = false }
}

async function handleDelete(row: any) {
  try { await ElMessageBox.confirm('确认删除该权限组？', '确认删除', { type: 'warning' }); await permissionGroupApi.delete(row.groupId); showSuccess('删除成功'); load() }
  catch (e: any) { if (e !== 'cancel') showError(e.message || '删除失败') }
}

onMounted(load)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.perm-panel { width: 100%; }
.perm-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.module-title { display: flex; align-items: center; }
.module-desc { color: #909399; font-size: 12px; margin-left: 6px; }
.perm-grid { display: flex; flex-wrap: wrap; gap: 8px 16px; padding: 8px 0 8px 24px; }
</style>
