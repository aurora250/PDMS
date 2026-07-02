<template>
  <div>
    <div class="page-header">
      <h3>重点人员管理</h3>
      <el-button v-if="hasPermission('keyperson:write')" type="primary" @click="openCreate">新增人员</el-button>
    </div>
    <el-card>
      <el-form inline>
        <el-form-item label="搜索">
          <el-input v-model="filter.keyword" placeholder="姓名/UUID" clearable @keyup.enter="load" style="width:200px" />
        </el-form-item>
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
        <el-table-column label="UUID" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="$router.push(`/resident/${row.uuid}`)">{{ row.uuid }}</el-button>
          </template>
        </el-table-column>
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
          <ResidentPicker v-model="form.uuid" placeholder="搜索姓名或身份证号选择居民" :nonPoliceOnly="true" />
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
          <el-select v-model="form.responsiblePoliceNo" placeholder="搜索民警姓名或警号选择" filterable remote
            :remote-method="searchPolice" :loading="policeSearching" clearable style="width:100%"
            :disabled="!!autoFilledPoliceNo"
            @focus="searchPolice('')">
            <el-option v-for="p in policeOptions" :key="p.policeNumber" :label="`${p.residentName || p.policeNumber} (${p.policeNumber})`" :value="p.policeNumber" />
          </el-select>
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
import { policeApi } from '@/api/auth'
import { usePermission } from '@/composables/usePermission'
import { useAuthStore } from '@/stores/auth'
import { showError, showSuccess } from '@/utils/auth'
import { policeNoRule } from '@/utils/validators'
import ResidentPicker from '@/components/ResidentPicker.vue'

const { hasPermission } = usePermission()
const auth = useAuthStore()
const route = useRoute()
const list = ref<any[]>([])
const loading = ref(false)
const filter = reactive({ level: '', type: '', keyword: '' })
const page = reactive({ current: 1, size: 20, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
let editUuid = ''

// 民警搜索
const policeOptions = ref<any[]>([])
const policeSearching = ref(false)
/** 创建模式时自动填充的民警警号（非空表示已自动填充，应禁用选择器） */
const autoFilledPoliceNo = ref('')

async function searchPolice(query: string) {
  policeSearching.value = true
  try {
    const res = await policeApi.list({ keyword: query || undefined, page: 1, size: 50 }, { silent: true } as any)
    policeOptions.value = (res.records || []).map((p: any) => ({
      ...p,
      label: `${p.residentName || p.policeNumber} (${p.policeNumber})`,
      value: p.policeNumber,
    }))
  } catch { policeOptions.value = [] }
  finally { policeSearching.value = false }
}

const defaultForm = () => ({
  uuid: '', controlLevel: '一级', controlType: '刑满释放人员',
  responsiblePoliceNo: '', designatedAt: new Date().toISOString().slice(0, 19),
})
const form = reactive(defaultForm())

const rules = {
  uuid: [{ required: true, message: '请输入居民UUID', trigger: 'blur' }],
  controlLevel: [{ required: true, message: '请选择管控级别', trigger: 'change' }],
  controlType: [{ required: true, message: '请选择管控类型', trigger: 'change' }],
  responsiblePoliceNo: [{ required: true, message: '请输入责任民警编号', trigger: 'blur' }, policeNoRule],
  designatedAt: [{ required: true, message: '请选择列管日期', trigger: 'change' }],
}

async function load() {
  loading.value = true
  try {
    const res = await keypersonApi.search({
      controlLevel: filter.level || undefined,
      controlType: filter.type || undefined,
      keyword: filter.keyword || undefined,
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

async function openCreate() {
  Object.assign(form, defaultForm())
  isEdit.value = false
  autoFilledPoliceNo.value = ''
  // 尝试获取当前登录用户的民警信息并自动填充
  try {
    const me = await policeApi.getMe({ silent: true } as any)
    if (me && me.policeNumber) {
      form.responsiblePoliceNo = me.policeNumber
      autoFilledPoliceNo.value = me.policeNumber
    }
  } catch { /* 非民警用户，保持手动选择 */ }
  dialogVisible.value = true
}

function openEdit(row: any) {
  Object.assign(form, defaultForm())
  Object.assign(form, row)
  isEdit.value = true
  editUuid = row.uuid
  autoFilledPoliceNo.value = ''
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
