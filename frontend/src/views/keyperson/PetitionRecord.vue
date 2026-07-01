<template>
  <div>
    <div class="page-header">
      <h3>信访记录</h3>
      <el-button v-if="hasPermission('keyperson:petition:write')" type="primary" @click="openCreate">登记信访</el-button>
    </div>
    <el-card>
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column label="人员UUID" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-button text size="small" type="primary" @click="$router.push(`/resident/${row.keyPersonUuid}`)">{{ row.keyPersonUuid }}</el-button>
          </template>
        </el-table-column>
        <el-table-column prop="petitionTime" label="信访时间" width="170" />
        <el-table-column prop="address" label="信访地点" min-width="150" />
        <el-table-column prop="evaluation" label="评估" width="80" />
        <el-table-column prop="remark" label="备注" min-width="200" />
        <el-table-column prop="handlerPoliceNo" label="处理民警" width="140" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button text size="small" @click="$router.push(`/keyperson/visit?uuid=${row.keyPersonUuid}`)">走访计划</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50,100]" @current-change="load" @size-change="load" />
      </div>
    </el-card>

    <!-- 登记信访对话框 -->
    <el-dialog v-model="dialogVisible" title="登记信访" width="550px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="人员UUID" prop="keyPersonUuid">
          <el-input v-model="form.keyPersonUuid" placeholder="请输入重点人员UUID" />
        </el-form-item>
        <el-form-item label="信访时间" prop="petitionTime">
          <el-date-picker v-model="form.petitionTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="信访地点" prop="address">
          <el-input v-model="form.address" placeholder="如: 区政府门口" />
        </el-form-item>
        <el-form-item label="处理民警" prop="handlerPoliceNo">
          <el-select v-model="form.handlerPoliceNo" placeholder="搜索民警姓名或警号选择" filterable remote
            :remote-method="searchPolice" :loading="policeSearching" clearable style="width:100%"
            @focus="searchPolice('')">
            <el-option v-for="p in policeOptions" :key="p.policeNumber" :label="`${p.residentName || p.policeNumber} (${p.policeNumber})`" :value="p.policeNumber" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估" prop="evaluation">
          <el-select v-model="form.evaluation" style="width:100%">
            <el-option label="一般" value="一般" /><el-option label="严重" value="严重" />
            <el-option label="重大" value="重大" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="详细描述信访内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">确认登记</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { keypersonApi } from '@/api/keyperson'
import { policeApi } from '@/api/auth'
import { usePermission } from '@/composables/usePermission'
import { showError, showSuccess } from '@/utils/auth'
import { uuidRule, policeNoRule } from '@/utils/validators'

const route = useRoute()
const { hasPermission } = usePermission()
const list = ref<any[]>([])
const loading = ref(false)
const page = reactive({ current: 1, size: 20, total: 0 })

// 民警搜索
const policeOptions = ref<any[]>([])
const policeSearching = ref(false)
async function searchPolice(query: string) {
  policeSearching.value = true
  try {
    const res = await policeApi.list({ keyword: query || undefined, page: 1, size: 50 })
    policeOptions.value = (res.records || []).map((p: any) => ({ ...p, label: `${p.residentName || p.policeNumber} (${p.policeNumber})`, value: p.policeNumber }))
  } catch { policeOptions.value = [] }
  finally { policeSearching.value = false }
}

const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const form = reactive({
  keyPersonUuid: '', petitionTime: new Date().toISOString().slice(0, 19),
  address: '', handlerPoliceNo: '', evaluation: '一般', remark: '',
})
const rules = {
  keyPersonUuid: [{ required: true, message: '请输入人员UUID', trigger: 'blur' }, uuidRule],
  petitionTime: [{ required: true, message: '请选择信访时间', trigger: 'change' }],
  address: [{ required: true, message: '请输入信访地点', trigger: 'blur' }],
  handlerPoliceNo: [{ required: true, message: '请输入处理民警编号', trigger: 'blur' }, policeNoRule],
  remark: [{ required: true, message: '请输入备注', trigger: 'blur' }],
}

async function load() {
  loading.value = true
  try {
    const res = await keypersonApi.listPetition({ keyPersonUuid: (route.query.uuid as string) || undefined, page: page.current, size: page.size })
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { /* ignore */ }
  finally { loading.value = false }
}

function openCreate() {
  form.keyPersonUuid = ''
  form.petitionTime = new Date().toISOString().slice(0, 19)
  form.address = ''
  form.handlerPoliceNo = ''
  form.evaluation = '一般'
  form.remark = ''
  dialogVisible.value = true
}

function resetForm() { formRef.value?.resetFields() }

async function handleCreate() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await keypersonApi.createPetition({ ...form })
    showSuccess('登记成功')
    dialogVisible.value = false
    load()
  } catch (e: any) { showError(e.message || '登记失败') }
  finally { submitting.value = false }
}

onMounted(load)
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
</style>
