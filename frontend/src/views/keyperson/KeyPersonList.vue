<template>
  <div>
    <div class="page-header">
      <h3>重点人员管理</h3>
      <el-button v-if="hasPermission('keyperson:write')" type="primary" @click="openCreate">新增人员</el-button>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="管控级别">
          <el-select v-model="filter.level" placeholder="全部" clearable @change="load">
            <el-option v-for="l in ['一级','二级','三级']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="管控类型">
          <el-select v-model="filter.type" placeholder="全部" clearable @change="load">
            <el-option label="刑满释放人员" value="刑满释放人员" />
            <el-option label="社区矫正人员" value="社区矫正人员" />
            <el-option label="涉毒人员" value="涉毒人员" />
            <el-option label="信访重点人员" value="信访重点人员" />
            <el-option label="涉稳人员" value="涉稳人员" />
            <el-option label="精神障碍患者(肇事肇祸风险)" value="精神障碍患者(肇事肇祸风险)" />
            <el-option label="其他重点人员" value="其他重点人员" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button @click="load">刷新</el-button></el-form-item>
      </el-form>
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="uuid" label="UUID" width="200" show-overflow-tooltip />
        <el-table-column prop="controlLevel" label="管控级别" width="100" />
        <el-table-column prop="controlType" label="管控类型" min-width="150" />
        <el-table-column prop="responsiblePoliceNo" label="责任民警" width="140" />
        <el-table-column prop="designatedAt" label="列管日期" width="120" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button v-if="hasPermission('keyperson:write')" text size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="hasPermission('keyperson:delete')" text size="small" type="danger" @click="del(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50,100]" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑重点人员' : '新增列管'" width="500px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="居民UUID" prop="uuid">
          <ResidentPicker v-model="form.uuid" placeholder="搜索姓名或身份证号选择居民" />
        </el-form-item>
        <el-form-item label="管控级别" prop="controlLevel">
          <el-select v-model="form.controlLevel" style="width:100%">
            <el-option label="一级" value="一级" /><el-option label="二级" value="二级" /><el-option label="三级" value="三级" />
          </el-select>
        </el-form-item>
        <el-form-item label="管控类型" prop="controlType">
          <el-select v-model="form.controlType" style="width:100%">
            <el-option label="刑满释放人员" value="刑满释放人员" />
            <el-option label="社区矫正人员" value="社区矫正人员" />
            <el-option label="涉毒人员" value="涉毒人员" />
            <el-option label="信访重点人员" value="信访重点人员" />
            <el-option label="涉稳人员" value="涉稳人员" />
            <el-option label="精神障碍患者(肇事肇祸风险)" value="精神障碍患者(肇事肇祸风险)" />
            <el-option label="其他重点人员" value="其他重点人员" />
          </el-select>
        </el-form-item>
        <el-form-item label="责任民警" prop="responsiblePoliceNo">
          <el-input v-model="form.responsiblePoliceNo" placeholder="输入民警编号，如 P20260001" />
        </el-form-item>
        <el-form-item label="列管日期" prop="designatedAt">
          <el-date-picker v-model="form.designatedAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">{{ isEdit ? '保存' : '确认列管' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { keypersonApi } from '@/api/keyperson'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import ResidentPicker from '@/components/ResidentPicker.vue'

const { hasPermission } = usePermission()
const route = useRoute()
const list = ref<any[]>([])
const loading = ref(false)
const filter = reactive({ level: '', type: '' })
const page = reactive({ current: 1, size: 20, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
let editUuid = ''

const defaultForm = () => ({
  uuid: '', controlLevel: '一级', controlType: '刑满释放人员',
  responsiblePoliceNo: '', designatedAt: new Date().toISOString().slice(0, 19),
})
const form = reactive(defaultForm())

const rules = {
  uuid: [{ required: true, message: '请输入居民UUID', trigger: 'blur' }],
  controlLevel: [{ required: true, message: '请选择管控级别', trigger: 'change' }],
  controlType: [{ required: true, message: '请选择管控类型', trigger: 'change' }],
  responsiblePoliceNo: [{ required: true, message: '请输入责任民警编号', trigger: 'blur' }],
  designatedAt: [{ required: true, message: '请选择列管日期', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const res = await keypersonApi.search({
      controlLevel: filter.level || undefined,
      controlType: filter.type || undefined,
      page: page.current, size: page.size,
    })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

async function del(row: any) {
  try {
    await ElMessageBox.confirm('确认撤销该重点人员列管记录？', '确认撤销', { type: 'warning' })
    await keypersonApi.delete(row.uuid); showSuccess('已撤销'); load()
  } catch { /* ignore */ }
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
  editUuid = row.uuid
  dialogVisible.value = true
}

function resetForm() { formRef.value?.resetFields() }

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await keypersonApi.update(editUuid, { controlLevel: form.controlLevel, controlType: form.controlType })
      showSuccess('更新成功')
    } else {
      await keypersonApi.create({ ...form })
      showSuccess('列管成功')
    }
    dialogVisible.value = false
    load()
  } catch (e: any) { showError(e.message || '保存失败') }
  finally { saving.value = false }
}

onMounted(() => {
  if (route.query.level) filter.level = route.query.level as string
  if (route.query.type) filter.type = route.query.type as string
  load()
})
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
</style>
