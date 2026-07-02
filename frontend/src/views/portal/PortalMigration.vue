<template>
  <div>
    <h3>户口迁移申请</h3>
    <p class="subtitle">申请将户籍迁移到新的地区，提交后由民警审核。</p>
    <el-card style="margin-top:16px; max-width:650px" v-loading="preloading">
      <el-form ref="formRef" :model="form" label-width="110px">
        <el-form-item label="迁出地区" required>
          <AreaCascader v-model="form.outgoingAreaId" disabled placeholder="自动填入您的户籍地区" />
        </el-form-item>
        <el-form-item label="迁出详址">
          <el-input v-model="form.outgoingDetail" disabled placeholder="自动填入您的户籍地址" />
          <span class="form-tip">自动填入您的户籍地址，可手动修改</span>
        </el-form-item>
        <div v-if="outgoingPreview" class="address-preview">
          <el-text type="info" size="small">迁出地址预览: {{ outgoingPreview }}</el-text>
        </div>

        <el-form-item label="迁入地区" required>
          <AreaCascader v-model="form.incomingAreaId" placeholder="选择迁入地省市区" />
        </el-form-item>
        <el-form-item label="迁入详址">
          <el-input v-model="form.incomingDetail" placeholder="街道/路/号/楼/室" />
        </el-form-item>
        <div v-if="incomingPreview" class="address-preview">
          <el-text type="info" size="small">迁入地址预览: {{ incomingPreview }}</el-text>
        </div>

        <el-form-item label="办理日期" prop="handleDate">
          <el-date-picker v-model="form.handleDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="迁移类型" v-if="form.businessType">
          <el-tag>{{ form.businessType }}</el-tag>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { householdApi } from '@/api/household'
import { residentApi } from '@/api/resident'
import { areaApi } from '@/api/area'
import { showError, showSuccess } from '@/utils/auth'
import AreaCascader from '@/components/AreaCascader.vue'

const auth = useAuthStore()
const formRef = ref()
const submitting = ref(false)
const preloading = ref(false)
const outgoingPreview = ref('')
const incomingPreview = ref('')

const form = reactive({
  outgoingAreaId: undefined as number | undefined,
  outgoingDetail: '',
  incomingAreaId: undefined as number | undefined,
  incomingDetail: '',
  handleDate: new Date().toISOString().slice(0, 10),
  businessType: '',
})

// 加载区域编码映射用于自动判断迁移类型
let areaCodeMap: Record<number, string> = {}
async function loadAreaCodes() {
  try {
    const tree = await areaApi.tree()
    const walk = (nodes: any[]) => {
      for (const n of nodes) {
        if (n.areaId) areaCodeMap[n.areaId] = n.areaCode || ''
        if (n.children) walk(n.children)
      }
    }
    walk(Array.isArray(tree) ? tree : [])
  } catch { /* */ }
}

watch([() => form.outgoingAreaId, () => form.incomingAreaId], () => {
  const o = form.outgoingAreaId, i = form.incomingAreaId
  if (o && i && areaCodeMap[o] && areaCodeMap[i]) {
    const oc = areaCodeMap[o], ic = areaCodeMap[i]
    if (oc.slice(0, 4) === ic.slice(0, 4)) form.businessType = '市内'
    else if (oc.slice(0, 2) === ic.slice(0, 2)) form.businessType = '省内'
    else form.businessType = '跨省'
  } else {
    form.businessType = ''
  }
})

async function buildPreview(areaId: number | undefined, detail: string): Promise<string> {
  if (!areaId) return detail
  try { return (await areaApi.getPath(areaId)) + detail } catch { return detail }
}
watch(() => [form.outgoingAreaId, form.outgoingDetail], async () => {
  outgoingPreview.value = await buildPreview(form.outgoingAreaId, form.outgoingDetail)
})
watch(() => [form.incomingAreaId, form.incomingDetail], async () => {
  incomingPreview.value = await buildPreview(form.incomingAreaId, form.incomingDetail)
})

async function stripAreaPrefix(full: string, aid: number | null | undefined): Promise<string> {
  if (!full || !aid) return full
  try { const path = await areaApi.getPath(aid); if (path && full.startsWith(path)) return full.substring(path.length).trim() || full } catch { /* */ }
  return full
}

onMounted(async () => {
  await loadAreaCodes()
  if (!auth.residentUuid) return
  preloading.value = true
  try {
    const detail = await residentApi.getByUuid(auth.residentUuid)
    if (detail) {
      if (detail.householdAreaId != null) form.outgoingAreaId = detail.householdAreaId
      form.outgoingDetail = await stripAreaPrefix(detail.householdAddress || '', detail.householdAreaId)
    }
  } catch { /* */ }
  finally { preloading.value = false }
})

async function handleSubmit() {
  if (!form.outgoingAreaId) { showError('请确认迁出地区（系统已自动填入您的户籍地）'); return }
  if (!form.incomingAreaId) { showError('请选择迁入地区'); return }
  if (!form.incomingDetail.trim()) { showError('请填写迁入详址'); return }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const outAddr = outgoingPreview.value || form.outgoingDetail
    const inAddr = incomingPreview.value || form.incomingDetail
    await householdApi.createMigration({
      applicantUuid: auth.residentUuid,
      outgoingAddress: outAddr,
      outgoingAreaId: form.outgoingAreaId,
      incomingAddress: inAddr,
      incomingAreaId: form.incomingAreaId,
      businessType: form.businessType || '市内',
      handleDate: form.handleDate,
    })
    showSuccess('迁移申请已提交，请等待审核')
  } catch (e: any) { showError(e.message || '提交失败') }
  finally { submitting.value = false }
}
</script>

<style scoped>
.subtitle { color: #909399; margin: 4px 0 16px 0; }
.form-tip { display: block; font-size: 11px; color: #909399; line-height: 1.5; }
.address-preview { margin: -8px 0 12px 110px; padding: 4px 8px; background: #f5f7fa; border-radius: 4px; }
</style>
