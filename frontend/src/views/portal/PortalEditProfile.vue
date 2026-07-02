<template>
  <div>
    <h3>个人信息变更申请</h3>
    <p class="subtitle">修改您的个人信息，提交后需民警审核通过方可生效。</p>
    <el-card style="margin-top:16px; max-width:700px" v-loading="preloading">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="曾用名"><el-input v-model="form.formerName" /></el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender"><el-radio label="男" /><el-radio label="女" /></el-radio-group>
        </el-form-item>
        <el-form-item label="民族" prop="nation">
          <el-select v-model="form.nation" @change="onNationChange">
            <el-option v-for="n in NATIONS" :key="n.code" :label="n.name" :value="n.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="出生日期" prop="birthDate">
          <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="学历" prop="educationLevel">
          <el-select v-model="form.educationLevel" @change="onEducationChange">
            <el-option v-for="e in EDUCATIONS" :key="e.code" :label="e.name" :value="e.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="血型">
          <el-select v-model="form.bloodType" clearable>
            <el-option v-for="b in BLOOD_TYPES" :key="b" :label="b" :value="b" />
          </el-select>
        </el-form-item>
        <el-form-item label="婚姻状况" prop="maritalStatus">
          <el-select v-model="form.maritalStatus">
            <el-option v-for="m in MARITAL_STATUSES" :key="m.code" :label="m.name" :value="m.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="职业"><el-input v-model="form.occupation" /></el-form-item>
        <el-form-item label="电话" prop="phone"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="居住地区">
          <AreaCascader v-model="addressForm.areaId" />
        </el-form-item>
        <el-form-item label="居住详址">
          <el-input v-model="addressForm.detail" placeholder="街道/路/号/楼/室" />
          <span class="form-tip">请填写与所选地区对应的街道门牌号</span>
        </el-form-item>
        <div v-if="addressForm.preview" class="address-preview">
          <el-text type="info" size="small">居住地址预览: {{ addressForm.preview }}</el-text>
        </div>
        <el-form-item label="户籍地区">
          <AreaCascader v-model="addressForm.householdAreaId" />
        </el-form-item>
        <el-form-item label="户籍详址">
          <el-input v-model="addressForm.householdDetail" placeholder="街道/路/号/楼/室" />
          <span class="form-tip">请填写与所选地区对应的详细门牌号</span>
        </el-form-item>
        <div v-if="addressForm.householdPreview" class="address-preview">
          <el-text type="info" size="small">户籍地址预览: {{ addressForm.householdPreview }}</el-text>
        </div>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">提交变更申请</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { residentApi } from '@/api/resident'
import { areaApi } from '@/api/area'
import { useGbConstants } from '@/composables/useGbConstants'
import { showError, showSuccess } from '@/utils/auth'
import AreaCascader from '@/components/AreaCascader.vue'

const auth = useAuthStore()
const { NATIONS, EDUCATIONS, MARITAL_STATUSES, BLOOD_TYPES, nationCode, educationCode } = useGbConstants()

const formRef = ref()
const saving = ref(false)
const preloading = ref(false)
let originalData: any = null

const form = reactive<any>({
  name: '', formerName: '', gender: '男',
  nation: '汉族', nationCode: '01', birthDate: '1990-01-01',
  educationLevel: '大学本科', educationCode: '20', bloodType: '',
  maritalStatus: '未婚', occupation: '', phone: '',
})

const addressForm = reactive({
  areaId: null as number | null, detail: '', preview: '',
  householdAreaId: null as number | null, householdDetail: '', householdPreview: '',
})

const rules = {
  name: [{ required: true, message: '请输入姓名' }],
  gender: [{ required: true, message: '请选择性别' }],
  nation: [{ required: true, message: '请选择民族' }],
  birthDate: [{ required: true, message: '请选择出生日期' }],
  educationLevel: [{ required: true, message: '请选择学历' }],
  maritalStatus: [{ required: true, message: '请选择婚姻状况' }],
  phone: [{ required: true, message: '请输入电话' }],
}

function onNationChange() { form.nationCode = nationCode(form.nation) }
function onEducationChange() { form.educationCode = educationCode(form.educationLevel) }

async function buildPreview(areaId: number | null, detail: string): Promise<string> {
  if (!areaId) return detail || ''
  try { return (await areaApi.getPath(areaId)) + (detail || '') } catch { return detail || '' }
}

watch(() => [addressForm.areaId, addressForm.detail], async () => {
  addressForm.preview = await buildPreview(addressForm.areaId, addressForm.detail)
})
watch(() => [addressForm.householdAreaId, addressForm.householdDetail], async () => {
  addressForm.householdPreview = await buildPreview(addressForm.householdAreaId, addressForm.householdDetail)
})

async function stripAreaPrefix(full: string, aid: number | null | undefined): Promise<string> {
  if (!full || !aid) return full
  try {
    const path = await areaApi.getPath(aid)
    if (path && full.startsWith(path)) return full.substring(path.length).trim() || full
  } catch { /* */ }
  return full
}

onMounted(async () => {
  if (!auth.residentUuid) return
  preloading.value = true
  try {
    const r = await residentApi.getByUuid(auth.residentUuid)
    if (r) {
      originalData = { ...r }
      Object.assign(form, r)
      form.nationCode = r.nationCode || nationCode(r.nation)
      form.educationCode = r.educationCode || educationCode(r.educationLevel)
      addressForm.areaId = r.areaId ?? null
      addressForm.householdAreaId = r.householdAreaId ?? null
      addressForm.detail = await stripAreaPrefix(r.residence || '', r.areaId)
      addressForm.householdDetail = await stripAreaPrefix(r.householdAddress || '', r.householdAreaId)
    }
  } catch { /* */ }
  finally { preloading.value = false }
})

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload: Record<string, any> = { ...form }
    payload.residence = (await buildPreview(addressForm.areaId, addressForm.detail)) || addressForm.detail
    payload.areaId = addressForm.areaId
    payload.householdAddress = (await buildPreview(addressForm.householdAreaId, addressForm.householdDetail)) || addressForm.householdDetail
    payload.householdAreaId = addressForm.householdAreaId

    const changed: string[] = []
    const orig: Record<string, any> = {}
    const mod: Record<string, any> = {}
    for (const k of Object.keys(payload)) {
      if (payload[k] !== (originalData?.[k] ?? undefined) && payload[k] !== '' && originalData?.[k] !== undefined) {
        changed.push(k); orig[k] = originalData[k]; mod[k] = payload[k]
      }
    }
    if (changed.length === 0) { showError('未检测到任何变更'); saving.value = false; return }

    await residentApi.submitChangeRequest({
      applicantUuid: auth.residentUuid,
      changeField: changed.join('、'),
      originalData: JSON.stringify(orig),
      modifiedData: JSON.stringify(mod),
      status: '请求',
    })
    showSuccess('变更申请已提交，请等待审核')
  } catch (e: any) { showError(e.message || '提交失败') }
  finally { saving.value = false }
}
</script>

<style scoped>
.subtitle { color: #909399; margin: 4px 0 16px 0; }
.form-tip { display: block; font-size: 11px; color: #909399; line-height: 1.5; }
.address-preview { margin: -8px 0 12px 110px; padding: 4px 8px; background: #f5f7fa; border-radius: 4px; }
</style>
