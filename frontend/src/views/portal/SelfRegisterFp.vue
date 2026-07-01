<template>
  <div class="self-register-fp">
    <h3>流动人口自主申报</h3>
    <p class="subtitle">群众可通过此页面直接申报流动人口登记，无需经过采集员和街道办环节。</p>

    <el-card style="margin-top:20px; max-width: 800px;">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-divider content-position="left">个人信息</el-divider>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio label="男" /><el-radio label="女" />
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="身份证号" prop="idCardNo">
          <IdCardInput v-model="form.idCardNo" @parsed="onIdParsed" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="民族" prop="nation">
              <GbSelect dict="nation" v-model="form.nation" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期" prop="birthDate">
              <el-date-picker v-model="form.birthDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="学历" prop="educationLevel">
              <GbSelect dict="education" v-model="form.educationLevel" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="婚姻状况">
              <GbSelect dict="maritalStatus" v-model="form.maritalStatus" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="电话号码" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入联系电话" />
        </el-form-item>

        <el-divider content-position="left">居住信息</el-divider>
        <el-form-item label="居住区域" prop="areaId">
          <AreaCascader v-model="form.areaId" placeholder="请选择省市区" />
        </el-form-item>
        <el-form-item label="详细地址" prop="currentAddress">
          <el-input v-model="form.currentAddress" placeholder="请输入门牌号、街道、小区等详细居住地址" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="居住类型" prop="addressType">
              <el-select v-model="form.addressType" style="width:100%">
                <el-option label="租赁房屋" value="租赁房屋" />
                <el-option label="自有住房" value="自有住房" />
                <el-option label="单位宿舍" value="单位宿舍" />
                <el-option label="学校宿舍" value="学校宿舍" />
                <el-option label="亲友借住" value="亲友借住" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="居住目的" prop="purpose">
              <el-select v-model="form.purpose" style="width:100%">
                <el-option label="务工" value="务工" />
                <el-option label="经商" value="经商" />
                <el-option label="投靠亲属" value="投靠亲属" />
                <el-option label="求学" value="求学" />
                <el-option label="其他" value="其他" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="工作单位">
              <el-input v-model="form.workUnit" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计时长">
              <el-select v-model="form.expectedDuration" style="width:100%">
                <el-option label="短租" value="短租" /><el-option label="中租" value="中租" />
                <el-option label="长租" value="长租" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="原户籍地址">
          <el-input v-model="form.originalAddress" placeholder="原户籍详细地址" />
        </el-form-item>

        <el-divider content-position="left">附件材料</el-divider>
        <el-form-item label="附件">
          <AttachmentUploader v-model="form.attachment" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" @click="handleSubmit" :loading="submitting">提交申报</el-button>
          <el-button size="large" @click="$router.push('/portal')">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { floatingApi } from '@/api/floating'
import { residentApi } from '@/api/resident'
import { useAuthStore } from '@/stores/auth'
import { useGbConstants } from '@/composables/useGbConstants'
import { showError, showSuccess } from '@/utils/auth'
import { idCardRule, phoneRule } from '@/utils/validators'
import IdCardInput from '@/components/IdCardInput.vue'
import GbSelect from '@/components/GbSelect.vue'
import AttachmentUploader from '@/components/AttachmentUploader.vue'
import AreaCascader from '@/components/AreaCascader.vue'

const auth = useAuthStore()
const { nationCode, educationCode } = useGbConstants()

const formRef = ref()
const submitting = ref(false)
const preloading = ref(false)

const form = reactive({
  name: '', gender: '男', idCardNo: '', nation: '汉族', nationCode: '01',
  birthDate: '1990-01-01', educationLevel: '初中', educationCode: '70',
  maritalStatus: '未婚', phone: '',
  areaId: undefined as number | undefined, currentAddress: '',
  addressType: '租赁房屋', purpose: '务工',
  workUnit: '', expectedDuration: '', originalAddress: '',
  attachment: [] as string[],
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
  idCardNo: [{ required: true, message: '请输入身份证号', trigger: 'blur' }, idCardRule],
  nation: [{ required: true, message: '请选择民族', trigger: 'change' }],
  birthDate: [{ required: true, message: '请选择出生日期', trigger: 'change' }],
  educationLevel: [{ required: true, message: '请选择学历', trigger: 'change' }],
  phone: [{ required: true, message: '请输入电话号码', trigger: 'blur' }, phoneRule],
  areaId: [{ required: true, message: '请选择居住区域', trigger: 'change' }],
  currentAddress: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
  addressType: [{ required: true, message: '请选择居住类型', trigger: 'change' }],
  purpose: [{ required: true, message: '请选择居住目的', trigger: 'change' }],
}

function onIdParsed(data: { birthDate: string; gender: string }) {
  form.birthDate = data.birthDate
  form.gender = data.gender
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    form.nationCode = nationCode(form.nation)
    form.educationCode = educationCode(form.educationLevel)
    await floatingApi.createRegister({
      ...form,
      registerDate: new Date().toISOString().slice(0, 10),
    }, { silent: true })
    showSuccess('申报已提交，请等待审核')
    formRef.value?.resetFields()
  } catch (e: any) {
    showError(e.message || '提交失败')
  } finally { submitting.value = false }
}

/** 从已绑定的居民实名信息自动预填 */
onMounted(async () => {
  if (!auth.residentUuid) return
  preloading.value = true
  try {
    const resident = await residentApi.getByUuid(auth.residentUuid, { silent: true })
    if (resident) {
      form.name = resident.name || form.name
      form.gender = resident.gender || form.gender
      form.idCardNo = resident.idCardNo || form.idCardNo
      form.nation = resident.nation || form.nation
      form.birthDate = resident.birthDate || form.birthDate
      form.educationLevel = resident.educationLevel || form.educationLevel
      form.maritalStatus = resident.maritalStatus || form.maritalStatus
      form.phone = resident.phone || form.phone
      form.originalAddress = resident.householdAddress || form.originalAddress
      form.areaId = resident.areaId || form.areaId
      form.currentAddress = resident.currentAddress || form.currentAddress
    }
  } catch { /* ignore if resident lookup fails */ }
  finally { preloading.value = false }
})
</script>

<style scoped>
.subtitle { color: #909399; margin: 4px 0 0 0; }
</style>
