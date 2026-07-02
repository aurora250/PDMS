<template>
  <div>
    <h3>个人信息</h3>
    <el-card style="margin-top:16px; max-width:800px" v-loading="loading">
      <template v-if="resident">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">{{ resident.name }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ resident.gender }}</el-descriptions-item>
          <el-descriptions-item label="身份证号">{{ resident.idCardNo }}</el-descriptions-item>
          <el-descriptions-item label="民族">{{ resident.nation }}</el-descriptions-item>
          <el-descriptions-item label="出生日期">{{ resident.birthDate }}</el-descriptions-item>
          <el-descriptions-item label="学历">{{ resident.educationLevel }}</el-descriptions-item>
          <el-descriptions-item label="血型">{{ resident.bloodType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="婚姻状况">{{ resident.maritalStatus }}</el-descriptions-item>
          <el-descriptions-item label="职业">{{ resident.occupation || '-' }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ resident.phone }}</el-descriptions-item>
          <el-descriptions-item label="户籍地址">{{ resident.householdAddress }}</el-descriptions-item>
          <el-descriptions-item label="户籍状态">
            <el-tag :type="householdStatusType(resident.householdStatus)" size="small">{{ resident.householdStatus }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="居住地址">{{ resident.residence }}</el-descriptions-item>
          <el-descriptions-item label="户口类型">{{ resident.householdType }}</el-descriptions-item>
        </el-descriptions>
        <div style="margin-top:16px">
          <el-button type="primary" @click="$router.push('/portal/edit-profile')">申请信息变更</el-button>
        </div>
      </template>
      <el-empty v-else-if="!loading" description="未找到个人信息" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { residentApi } from '@/api/resident'

const auth = useAuthStore()
const resident = ref<any>(null)
const loading = ref(false)

function householdStatusType(status: string): string {
  if (status === '正常') return 'success'
  if (status?.includes('注销')) return 'danger'
  return 'warning'
}

onMounted(async () => {
  if (!auth.residentUuid) return
  loading.value = true
  try { resident.value = await residentApi.getByUuid(auth.residentUuid) } catch { /* */ }
  finally { loading.value = false }
})
</script>
