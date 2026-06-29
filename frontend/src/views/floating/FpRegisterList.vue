<template>
  <div>
    <div class="page-header">
      <h3>流动人口登记</h3>
      <el-button v-if="hasPermission('fp:write')" type="primary" @click="openCreate">新增登记</el-button>
    </div>
    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="rid" label="ID" width="60" />
        <el-table-column prop="uuid" label="UUID" width="200" show-overflow-tooltip />
        <el-table-column prop="registerDate" label="登记日期" width="120" />
        <el-table-column prop="residencePermitNo" label="居住证号" width="180" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="hasPermission('fp:write')" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('fp:delete')" text size="small" type="danger" @click="del(row)">注销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,prev,pager,next" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 登记/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑登记' : '新增登记'" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="居民UUID" prop="uuid">
          <el-input v-model="form.uuid" placeholder="请输入居民UUID" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="登记日期" prop="registerDate">
          <el-date-picker v-model="form.registerDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="居住证号">
          <el-input v-model="form.residencePermitNo" placeholder="可选" />
        </el-form-item>
        <el-form-item label="附件">
          <AttachmentUploader v-model="form.attachment" />
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
import { ref, reactive, onMounted } from 'vue'
import { floatingApi } from '@/api/floating'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import AttachmentUploader from '@/components/AttachmentUploader.vue'

const { hasPermission } = usePermission()
const list = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
let editRid = 0

const defaultForm = () => ({
  uuid: '', registerDate: new Date().toISOString().slice(0, 10), residencePermitNo: '', attachment: [] as string[],
})
const form = reactive(defaultForm())

const rules = {
  uuid: [{ required: true, message: '请输入居民UUID', trigger: 'blur' }],
  registerDate: [{ required: true, message: '请选择登记日期', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const res = await floatingApi.listRegister({ page: page.current, size: page.size })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function del(row: any) {
  try { await floatingApi.deleteRegister(row.rid); showSuccess('已注销'); load() } catch { /* ignore */ }
}

function openCreate() {
  Object.assign(form, defaultForm())
  isEdit.value = false
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, defaultForm())
  Object.assign(form, row)
  isEdit.value = true
  editRid = row.rid
  dialogVisible.value = true
}

function resetForm() { formRef.value?.resetFields() }

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await floatingApi.updateRegister(editRid, { ...form })
      showSuccess('更新成功')
    } else {
      await floatingApi.createRegister({ ...form })
      showSuccess('登记成功')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) { showError(e.message || '保存失败') }
  finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
</style>
