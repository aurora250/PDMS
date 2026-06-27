<template>
  <el-drawer v-model="visible" :title="isEdit ? '编辑人口信息' : '新增人口'" size="600px" @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
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
      <el-form-item label="居住地址" prop="residence"><el-input v-model="form.residence" /></el-form-item>
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
        </el-select>
      </el-form-item>
      <el-form-item label="户口地址" prop="householdAddress"><el-input v-model="form.householdAddress" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">{{ isEdit ? '保存修改' : '确认新增' }}</el-button>
    </template>
  </el-drawer>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { residentApi } from '@/api/resident'
import { useGbConstants } from '@/composables/useGbConstants'
import { showError, showSuccess } from '@/utils/auth'
import type { Resident } from '@/types/resident'

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

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true }],
  idCardNo: [{ required: true, message: '请输入身份证号' }, { len: 18, message: '身份证号为18位' }],
  nation: [{ required: true }],
  birthDate: [{ required: true }],
  educationLevel: [{ required: true }],
  maritalStatus: [{ required: true }],
  phone: [{ required: true }],
  residence: [{ required: true }],
  householdType: [{ required: true }],
  householdAddress: [{ required: true }],
}

function onNationChange() { form.nationCode = nationCode(form.nation) }
function onEducationChange() { form.educationCode = educationCode(form.educationLevel) }
function onIdCardBlur() {
  const v = form.idCardNo
  if (v.length === 18) {
    const d = v.substring(6, 14)
    form.birthDate = `${d.substring(0,4)}-${d.substring(4,6)}-${d.substring(6,8)}`
    form.gender = parseInt(v.charAt(16)) % 2 === 1 ? '男' : '女'
  }
}

function open(row?: Resident) {
  Object.assign(form, defaultForm())
  isEdit.value = !!row
  visible.value = true
  if (row) {
    editUuid = row.uuid!
    Object.assign(form, row)
  }
}

function resetForm() {
  formRef.value?.resetFields()
}

async function handleSave() {
  saving.value = true
  try {
    if (isEdit.value) {
      await residentApi.update(editUuid, { ...form })
      showSuccess('修改成功')
    } else {
      await residentApi.create({ ...form })
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
