<template>
  <div>
    <h3>居住证申领</h3>
    <p class="subtitle">申请办理居住证，提交后由民警审核制发。</p>
    <el-card style="margin-top:16px; max-width:500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="签发日期" prop="issueDate">
          <el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="有效期至" prop="expiryDate">
          <el-date-picker v-model="form.expiryDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申领</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { floatingApi } from '@/api/floating'
import { showError, showSuccess } from '@/utils/auth'

const auth = useAuthStore()
const formRef = ref()
const submitting = ref(false)

const today = new Date().toISOString().slice(0, 10)
const nextYear = new Date(Date.now() + 365 * 86400000).toISOString().slice(0, 10)

const form = reactive({
  issueDate: today,
  expiryDate: nextYear,
})

const rules = {
  issueDate: [{ required: true, message: '请选择签发日期' }],
  expiryDate: [{ required: true, message: '请选择有效期' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await floatingApi.applyPermit({
      uuid: auth.residentUuid,
      issueDate: form.issueDate,
      expiryDate: form.expiryDate,
    })
    showSuccess('居住证申领已提交，请等待审核')
  } catch (e: any) { showError(e.message || '提交失败') }
  finally { submitting.value = false }
}
</script>

<style scoped>
.subtitle { color: #909399; margin: 4px 0 16px 0; }
</style>
