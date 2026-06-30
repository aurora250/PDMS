<template>
  <div class="relations-page">
    <div class="page-header"><h3>家庭关系</h3></div>
    <el-card v-if="relation">
      <el-descriptions title="关系成员" :column="3" border>
        <el-descriptions-item label="本人UUID">{{ relation.relationPersonUuid }}</el-descriptions-item>
        <el-descriptions-item label="父亲">{{ relation.fatherUuid || '未登记' }}</el-descriptions-item>
        <el-descriptions-item label="母亲">{{ relation.motherUuid || '未登记' }}</el-descriptions-item>
        <el-descriptions-item label="配偶">{{ relation.spouseUuid || '未登记' }}</el-descriptions-item>
      </el-descriptions>
      <el-divider />
      <el-form ref="relationsFormRef" :model="editForm" inline>
        <el-form-item label="父亲UUID" prop="fatherUuid"><ResidentPicker v-model="editForm.fatherUuid" placeholder="搜索姓名或身份证号选择父亲" /></el-form-item>
        <el-form-item label="母亲UUID" prop="motherUuid"><ResidentPicker v-model="editForm.motherUuid" placeholder="搜索姓名或身份证号选择母亲" /></el-form-item>
        <el-form-item label="配偶UUID" prop="spouseUuid"><ResidentPicker v-model="editForm.spouseUuid" placeholder="搜索姓名或身份证号选择配偶" /></el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave">保存关系</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-empty v-else description="未找到家庭关系" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { residentApi } from '@/api/resident'
import { showError, showSuccess } from '@/utils/auth'
import { uuidRule } from '@/utils/validators'
import ResidentPicker from '@/components/ResidentPicker.vue'
import type { ResidentRelation } from '@/types/resident'

const route = useRoute()
const uuid = route.params.uuid as string
const relation = ref<ResidentRelation | null>(null)
const editForm = reactive<ResidentRelation>({ relationPersonUuid: uuid })
const relationsFormRef = ref()

const relationsRules = {
  fatherUuid: [uuidRule],
  motherUuid: [uuidRule],
  spouseUuid: [uuidRule],
}

async function load() {
  try { relation.value = await residentApi.getRelations(uuid) }
  catch { /* empty */ }
}
async function handleSave() {
  const valid = await relationsFormRef.value?.validate().catch(() => false)
  if (valid === false) return
  try {
    await residentApi.setRelations(uuid, { ...editForm })
    showSuccess('保存成功')
    load()
  } catch (e: any) { showError(e.message || '保存失败') }
}

onMounted(load)
</script>

<style scoped>
.relations-page { padding: 0; }
.page-header { margin-bottom: 16px; }
</style>
