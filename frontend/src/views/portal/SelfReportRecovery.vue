<template>
  <div class="self-report-recovery">
    <h3>寻回线索登记</h3>
    <p class="subtitle">如果您有失踪人员线索或已寻回失踪人员，请在此登记。</p>

    <el-card style="margin-top:20px; max-width: 700px;">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="失踪人员姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入失踪人员姓名" />
        </el-form-item>
        <el-form-item label="身份证号">
          <IdCardInput v-model="form.idCardNo" @parsed="onIdParsed" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="寻回日期" prop="recoveryDate">
              <el-date-picker v-model="form.recoveryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="寻回地点" prop="recoveryPlace">
              <el-input v-model="form.recoveryPlace" placeholder="寻回地点" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="线索描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="5" placeholder="请详细描述寻回经过或线索信息..." />
        </el-form-item>
        <el-form-item label="您的姓名">
          <el-input v-model="form.reporterName" placeholder="可选" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="方便我们联系核实" />
        </el-form-item>
        <el-form-item label="附件材料">
          <AttachmentUploader v-model="form.attachment" tip="可上传照片、证明材料等" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">提交线索</el-button>
          <el-button size="large" @click="$router.push('/portal')">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { missingApi } from '@/api/missing'
import { showError, showSuccess } from '@/utils/auth'
import { phoneRule } from '@/utils/validators'
import IdCardInput from '@/components/IdCardInput.vue'
import AttachmentUploader from '@/components/AttachmentUploader.vue'

const formRef = ref()
const submitting = ref(false)

const form = reactive({
  name: '', idCardNo: '', recoveryDate: new Date().toISOString().slice(0, 10),
  recoveryPlace: '', description: '', reporterName: '', contactPhone: '',
  attachment: [] as string[],
})

const rules = {
  name: [{ required: true, message: '请输入失踪人员姓名', trigger: 'blur' }],
  recoveryDate: [{ required: true, message: '请选择寻回日期', trigger: 'change' }],
  description: [{ required: true, message: '请描述线索信息', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }, phoneRule],
}

function onIdParsed(_data: { birthDate: string; gender: string }) {
  /* optionally auto-fill */
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await missingApi.recovery({
      name: form.name,
      recoveryDate: form.recoveryDate,
      recoveryAddress: form.recoveryPlace,
      summary: form.description,
      contactPhone: form.contactPhone,
    })
    showSuccess('线索已提交，感谢您的帮助！')
  } catch (e: any) {
    showError(e.message || '提交失败')
  } finally { submitting.value = false }
}
</script>

<style scoped>
.subtitle { color: #909399; margin: 4px 0 0 0; }
</style>
