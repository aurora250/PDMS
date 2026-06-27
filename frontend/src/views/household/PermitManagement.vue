<template>
  <div>
    <div class="page-header"><h3>证件管理</h3></div>
    <el-tabs v-model="tab">
      <el-tab-pane label="准迁证" name="approval" />
      <el-tab-pane label="迁移证" name="migration" />
    </el-tabs>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="permitNo" label="证件编号" />
        <el-table-column prop="status" label="状态" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { householdApi } from '@/api/household'
const tab = ref('approval')
const list = ref<any[]>([])
async function load() {
  try {
    if (tab.value === 'approval') list.value = await householdApi.listApprovalPermit()
    else list.value = await householdApi.listMigrationPermit()
  } catch { /* */ }
}
watch(tab, load)
onMounted(load)
</script>
