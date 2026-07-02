<template>
  <div>
    <h3>分户立户申请</h3>
    <p class="subtitle">以您为户主申请新的户口簿，提交后由民警审核。</p>
    <el-card style="margin-top:16px; max-width:600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="户籍地区" prop="hukouAreaId">
          <AreaCascader v-model="form.hukouAreaId" placeholder="选择新户籍地省市区" />
        </el-form-item>
        <el-form-item label="户籍详址" prop="hukouAddressDetail">
          <el-input v-model="form.hukouAddressDetail" placeholder="街道/路/号/楼/室" />
        </el-form-item>
        <el-form-item label="登记日期" prop="handleDate">
          <el-date-picker v-model="form.handleDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { householdApi } from '@/api/household'
import { showError, showSuccess } from '@/utils/auth'
import AreaCascader from '@/components/AreaCascader.vue'

const auth = useAuthStore()
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  hukouAreaId: undefined as number | undefined,
  hukouAddressDetail: '',
  handleDate: new Date().toISOString().slice(0, 10),
})

const rules = {
  hukouAreaId: [{ required: true, message: '请选择户籍地区' }],
  hukouAddressDetail: [{ required: true, message: '请输入详细地址' }],
  handleDate: [{ required: true, message: '请选择登记日期' }],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await householdApi.createBusiness({
      applicantUuid: auth.residentUuid,
      businessType: '分户立户',
      handleDate: form.handleDate,
      detailJson: JSON.stringify({
        newHouseholderUuid: auth.residentUuid,
        hukouAreaId: form.hukouAreaId,
        hukouAddressDetail: form.hukouAddressDetail,
      }),
    })
    showSuccess('分户立户申请已提交，请等待审核')
  } catch (e: any) { showError(e.message || '提交失败') }
  finally { submitting.value = false }
}
</script>

<style scoped>
.subtitle { color: #909399; margin: 4px 0 16px 0; }
</style>
