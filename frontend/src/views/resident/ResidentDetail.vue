<template>
  <el-drawer v-model="visible" :title="isEdit ? '编辑人口信息' : '新增人口'" size="650px" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-form-item label="姓名" prop="name"><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="曾用名"><el-input v-model="form.formerName" /></el-form-item>
      <el-form-item label="性别" prop="gender">
        <el-radio-group v-model="form.gender"><el-radio label="男" /><el-radio label="女" /></el-radio-group>
      </el-form-item>
      <el-form-item label="身份证号" prop="idCardNo">
        <el-input v-model="form.idCardNo" maxlength="18" @blur="onIdCardBlur" />
      </el-form-item>
      <el-form-item label="民族" prop="nation">
        <el-select v-model="form.nation" @change="onNationChange" placeholder="选择民族">
          <el-option v-for="n in NATIONS" :key="n.code" :label="n.name" :value="n.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="出生日期" prop="birthDate">
        <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="学历" prop="educationLevel">
        <el-select v-model="form.educationLevel" @change="onEducationChange" placeholder="选择学历">
          <el-option v-for="e in EDUCATIONS" :key="e.code" :label="`${e.name} (${e.code})`" :value="e.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="血型">
        <el-select v-model="form.bloodType" placeholder="选择血型" clearable>
          <el-option v-for="b in BLOOD_TYPES" :key="b" :label="b" :value="b" />
        </el-select>
      </el-form-item>
      <el-form-item label="婚姻状况" prop="maritalStatus">
        <el-select v-model="form.maritalStatus" placeholder="选择婚姻状况">
          <el-option v-for="m in MARITAL_STATUSES" :key="m.code" :label="`${m.name} (${m.code})`" :value="m.name" />
        </el-select>
      </el-form-item>
      <el-form-item label="职业"><el-input v-model="form.occupation" /></el-form-item>
      <el-form-item label="电话" prop="phone"><el-input v-model="form.phone" /></el-form-item>

      <!-- 居住地址: 省市区级联 + 详细地址 -->
      <el-form-item label="居住地区" prop="areaId">
        <AreaCascader v-model="addressForm.areaId" placeholder="选择居住地省市区" />
      </el-form-item>
      <el-form-item label="居住详址" prop="addressDetail">
        <el-input v-model="addressForm.detail" placeholder="街道/路/号/楼/室" />
        <span class="form-tip">请填写与所选地区对应的街道门牌号等详细地址</span>
      </el-form-item>
      <div v-if="addressForm.preview" class="address-preview">
        <el-text type="info" size="small">预览: {{ addressForm.preview }}</el-text>
      </div>

      <el-form-item label="户口类型" prop="householdType">
        <el-select v-model="form.householdType">
          <el-option label="农业户口" value="农业户口" />
          <el-option label="非农业户口" value="非农业户口" />
          <el-option label="居民户口" value="居民户口" />
        </el-select>
      </el-form-item>
      <el-form-item label="户口状态">
        <el-select v-model="form.householdStatus">
          <el-option label="正常" value="正常" />
          <el-option label="死亡注销" value="死亡注销" />
          <el-option label="失踪注销" value="失踪注销" />
          <el-option label="迁出注销" value="迁出注销" />
          <el-option label="恢复" value="恢复" />
        </el-select>
      </el-form-item>

      <!-- 户籍地址: 省市区级联 + 详细地址 -->
      <el-form-item label="户籍地区" prop="householdAreaId">
        <AreaCascader v-model="addressForm.householdAreaId" placeholder="选择户籍地省市区" />
      </el-form-item>
      <el-form-item label="户籍详址" prop="householdDetail">
        <el-input v-model="addressForm.householdDetail" placeholder="街道/路/号/楼/室" />
        <span class="form-tip">请填写与所选地区对应的详细门牌号</span>
      </el-form-item>
      <div v-if="addressForm.householdPreview" class="address-preview">
        <el-text type="info" size="small">预览: {{ addressForm.householdPreview }}</el-text>
      </div>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">{{ isEdit ? '保存修改' : '确认新增' }}</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { residentApi } from '@/api/resident'
import { areaApi } from '@/api/area'
import { useGbConstants } from '@/composables/useGbConstants'
import { showError, showSuccess } from '@/utils/auth'
import type { Resident } from '@/types/resident'
import AreaCascader from '@/components/AreaCascader.vue'
import { idCardRule, phoneRule } from '@/utils/validators'

const { NATIONS, EDUCATIONS, MARITAL_STATUSES, BLOOD_TYPES, nationCode, educationCode } = useGbConstants()

const emit = defineEmits(['saved'])
const visible = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
let editUuid = ''

const defaultForm = () => ({
  name: '', formerName: '', gender: '男', idCardNo: '',
  nation: '汉族', nationCode: '01', birthDate: '1990-01-01',
  educationLevel: '大学本科', educationCode: '20', bloodType: '',
  maritalStatus: '未婚', occupation: '', phone: '',
  residence: '', householdType: '居民户口',
  householdStatus: '正常', householdAddress: '',
})

const form = reactive<Resident>(defaultForm())

// 地址拆分表单
const addressForm = reactive({
  areaId: null as number | null,
  detail: '',
  preview: '',
  householdAreaId: null as number | null,
  householdDetail: '',
  householdPreview: '',
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  idCardNo: [{ required: true, message: '请输入身份证号', trigger: 'blur' }, { len: 18, message: '身份证号为18位' }, idCardRule],
  nation: [{ required: true, message: '请选择民族', trigger: 'change' }],
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  educationLevel: [{ required: true, message: '请选择学历', trigger: 'change' }],
  maritalStatus: [{ required: true, message: '请选择婚姻状况', trigger: 'change' }],
  phone: [{ required: true, message: '请输入电话', trigger: 'blur' }, phoneRule],
  areaId: [{ required: true, message: '请选择居住地区', trigger: 'change' }],
  addressDetail: [{ required: true, message: '请填写详细地址', trigger: 'blur' }],
  householdType: [{ required: true, message: '请选择户口类型', trigger: 'change' }],
  householdAreaId: [{ required: true, message: '请选择户籍地区', trigger: 'change' }],
  householdDetail: [{ required: true, message: '请填写详细地址', trigger: 'blur' }],
}

function onNationChange() { form.nationCode = nationCode(form.nation) }
function onEducationChange() { form.educationCode = educationCode(form.educationLevel) }
function onIdCardBlur() {
  const v = form.idCardNo
  if (v.length === 18) {
    const d = v.substring(6, 14)
    form.birthDate = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`
    form.gender = parseInt(v.charAt(16)) % 2 === 1 ? '男' : '女'
  }
}

/** 拼接地区路径 + 详细地址 → 完整地址预览 */
async function buildPreview(areaId: number | null, detail: string): Promise<string> {
  if (!areaId) return detail || ''
  try {
    const path: string = await areaApi.getPath(areaId)
    return (path || '') + (detail || '')
  } catch {
    return detail || ''
  }
}

// 居住地址预览
watch(
  () => [addressForm.areaId, addressForm.detail],
  async () => {
    addressForm.preview = await buildPreview(addressForm.areaId, addressForm.detail)
  }
)

// 户籍地址预览
watch(
  () => [addressForm.householdAreaId, addressForm.householdDetail],
  async () => {
    addressForm.householdPreview = await buildPreview(addressForm.householdAreaId, addressForm.householdDetail)
  }
)

async function open(row?: Resident) {
  Object.assign(form, defaultForm())
  addressForm.areaId = null
  addressForm.detail = ''
  addressForm.preview = ''
  addressForm.householdAreaId = null
  addressForm.householdDetail = ''
  addressForm.householdPreview = ''
  isEdit.value = !!row
  visible.value = true
  if (row) {
    editUuid = row.uuid!
    Object.assign(form, row)
    // 地区ID来自 row，级联菜单据此显示默认值
    addressForm.areaId = row.areaId ?? null
    addressForm.householdAreaId = row.householdAreaId ?? null
    // 从完整地址中剥离级联菜单已覆盖的省市区部分，仅保留街道门牌号
    addressForm.detail = await stripAreaPrefix(row.residence || '', row.areaId)
    addressForm.householdDetail = await stripAreaPrefix(row.householdAddress || '', row.householdAreaId)
  }
}

/** 从完整地址中剥离地区前缀，仅保留街道门牌号部分 */
async function stripAreaPrefix(fullAddress: string, areaId: number | null | undefined): Promise<string> {
  if (!fullAddress || !areaId) return fullAddress
  try {
    const path: string = await areaApi.getPath(areaId)
    if (path && fullAddress.startsWith(path)) {
      return fullAddress.substring(path.length)
    }
  } catch { /* ignore */ }
  return fullAddress
}

function resetForm() {
  formRef.value?.resetFields()
}

async function handleSave() {
  // 先验证 Element Plus 表单规则
  try {
    await formRef.value?.validate()
  } catch {
    return // 验证未通过
  }

  // 检查自定义必填项
  if (!addressForm.areaId) { showError('请选择居住地区'); return }
  if (!addressForm.detail.trim()) { showError('请填写居住详址'); return }
  if (!addressForm.householdAreaId) { showError('请选择户籍地区'); return }
  if (!addressForm.householdDetail.trim()) { showError('请填写户籍详址'); return }

  saving.value = true
  try {
    // 拼接完整地址: 地区路径 + 详细地址
    const residencePath = await buildPreview(addressForm.areaId, addressForm.detail)
    const householdPath = await buildPreview(addressForm.householdAreaId, addressForm.householdDetail)

    const payload = {
      ...form,
      residence: residencePath || addressForm.detail,
      areaId: addressForm.areaId,
      householdAddress: householdPath || addressForm.householdDetail,
      householdAreaId: addressForm.householdAreaId,
    }

    if (isEdit.value) {
      await residentApi.update(editUuid, payload)
      showSuccess('修改成功')
    } else {
      await residentApi.create(payload)
      showSuccess('新增成功')
    }
    visible.value = false
    emit('saved')
  } catch (e: any) {
    showError(e.message || '保存失败')
  } finally { saving.value = false }
}

defineExpose({ open })
</script>

<style scoped>
.form-tip {
  display: block;
  font-size: 11px;
  color: #909399;
  line-height: 1.5;
}
.address-preview {
  margin: -8px 0 12px 110px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
}
</style>
