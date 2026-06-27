<template>
  <div>
    <div class="page-header"><h3>审计日志</h3><el-button v-if="hasPermission('log:export')" @click="handleExport">导出</el-button></div>
    <el-card><el-table :data="list" stripe>
      <el-table-column prop="operator" label="操作人" /><el-table-column prop="operation" label="操作" /><el-table-column prop="target" label="目标" /><el-table-column prop="time" label="时间" />
    </el-table></el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { logApi } from '@/api/log'
import { usePermission } from '@/composables/usePermission'
const { hasPermission } = usePermission()
const list = ref<any[]>([])
async function handleExport() { try { await logApi.export() } catch { /* */ } }
onMounted(async () => { try { list.value = await logApi.audit() } catch { /* */ } })
</script>
