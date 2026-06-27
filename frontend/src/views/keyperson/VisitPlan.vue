<template>
  <div>
    <div class="page-header"><h3>走访计划</h3><el-button v-if="hasPermission('keyperson:visit-plan:write')" type="primary">新增计划</el-button></div>
    <el-card><el-table :data="list" stripe>
      <el-table-column prop="keyPersonUuid" label="人员UUID" width="200" /><el-table-column prop="planDate" label="计划日期" /><el-table-column prop="status" label="状态" />
    </el-table></el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { keypersonApi } from '@/api/keyperson'
import { usePermission } from '@/composables/usePermission'
const { hasPermission } = usePermission()
const list = ref<any[]>([])
onMounted(async () => { try { list.value = await keypersonApi.listVisitPlan() } catch { /* */ } })
</script>
