<template>
  <div>
    <div class="page-header"><h3>迁移轨迹 — {{ uuid }}</h3></div>
    <el-card><el-timeline><el-timeline-item v-for="(t,i) in traces" :key="i" :timestamp="t.time">{{ t.description }}</el-timeline-item></el-timeline></el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { householdApi } from '@/api/household'
const route = useRoute()
const uuid = route.params.uuid as string
const traces = ref<any[]>([])
onMounted(async () => { try { traces.value = await householdApi.getMigrationTrace(uuid) } catch { /* */ } })
</script>
