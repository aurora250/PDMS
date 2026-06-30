<template>
  <div>
    <div class="page-header">
      <h3>居住地管理</h3>
      <el-button v-if="hasPermission('fp:residence:write')" type="primary" @click="openCreate">新增登记</el-button>
    </div>
    <el-card>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="uuid" label="UUID" width="200" show-overflow-tooltip />
        <el-table-column prop="currentAddress" label="现地址" min-width="200" />
        <el-table-column prop="addressType" label="类型" width="100" />
        <el-table-column prop="purpose" label="目的" width="80" />
        <el-table-column prop="registerDate" label="登记日期" width="120" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="hasPermission('fp:residence:write')" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑居住地' : '登记居住地'" width="550px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="居民UUID" prop="uuid">
          <ResidentPicker v-model="form.uuid" placeholder="搜索姓名或身份证号选择居民" />
        </el-form-item>
        <el-form-item label="原始地址" prop="originalAddress">
          <el-input v-model="form.originalAddress" placeholder="户籍地址" />
        </el-form-item>
        <el-form-item label="现居住地址" prop="currentAddress">
          <el-input v-model="form.currentAddress" placeholder="现居住详细地址" />
        </el-form-item>
        <el-form-item label="所属区域" prop="areaId">
          <AreaCascader v-model="form.areaId" />
        </el-form-item>
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
          <el-input v-model="form.expectedDuration" placeholder="如: 长租" />
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
import { ref, reactive, onMounted } from 'vue'
import { floatingApi } from '@/api/floating'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import AreaCascader from '@/components/AreaCascader.vue'
import ResidentPicker from '@/components/ResidentPicker.vue'

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
  uuid: '', originalAddress: '', currentAddress: '', areaId: undefined as number | undefined,
  addressType: '租赁房屋', purpose: '务工', workUnit: '', expectedDuration: '',
  registerDate: new Date().toISOString().slice(0, 10),
})
const form = reactive(defaultForm())

const rules = {
  uuid: [{ required: true, message: '请输入居民UUID', trigger: 'blur' }],
  currentAddress: [{ required: true, message: '请输入现居住地址', trigger: 'blur' }],
  addressType: [{ required: true, message: '请选择居住类型', trigger: 'change' }],
  purpose: [{ required: true, message: '请选择居住目的', trigger: 'change' }],
  registerDate: [{ required: true, message: '请选择登记日期', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const res = await floatingApi.listResidence({ page: page.current, size: page.size })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function del(row: any) {
  try { await floatingApi.deleteResidence(row.rid); showSuccess('已注销'); load() } catch { /* ignore */ }
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
      await floatingApi.updateResidence(editRid, { ...form })
      showSuccess('更新成功')
    } else {
      await floatingApi.createResidence({ ...form })
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
