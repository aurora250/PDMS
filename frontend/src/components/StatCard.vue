<template>
  <el-card shadow="hover" :class="['stat-card', color]">
    <div class="stat-content">
      <div class="stat-label">{{ title }}</div>
      <div class="stat-value">
        <el-statistic v-if="!loading" :value="value" />
        <el-skeleton v-else :rows="1" animated />
      </div>
      <div v-if="icon" class="stat-icon">
        <el-icon :size="32"><component :is="icon" /></el-icon>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  value: number
  icon?: string
  color?: string
  loading?: boolean
}>()
</script>

<style scoped>
.stat-card {
  min-width: 200px;
}
.stat-card.primary { border-left: 4px solid #409eff; }
.stat-card.success { border-left: 4px solid #67c23a; }
.stat-card.warning { border-left: 4px solid #e6a23c; }
.stat-card.danger  { border-left: 4px solid #f56c6c; }
.stat-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.stat-label { color: #909399; font-size: 14px; }
.stat-value { font-size: 28px; font-weight: bold; color: #303133; }
.stat-icon { color: #c0c4cc; }
</style>
