<template>
  <div>
    <div class="page-header"><h3>GIS地图</h3></div>
    <el-card>
      <GisMap
        :markers="markers"
        :center="[39.9042, 116.4074]"
        :zoom="10"
        height="600px"
      />
      <div class="gis-legend" v-if="markers.length">
        <el-tag v-for="item in legendItems" :key="item.label" :color="item.color" size="small" effect="dark">
          {{ item.label }}
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { keypersonApi } from '@/api/keyperson'
import GisMap from '@/components/charts/GisMap.vue'

interface Marker {
  lat: number
  lng: number
  popup?: string
  color?: string
}

const markers = ref<Marker[]>([])
const loading = ref(false)

const levelColors: Record<string, string> = {
  '一级': '#e74c3c',
  '二级': '#e6a23c',
  '三级': '#409eff',
}

const legendItems = [
  { label: '一级管控', color: '#e74c3c' },
  { label: '二级管控', color: '#e6a23c' },
  { label: '三级管控', color: '#409eff' },
]

onMounted(async () => {
  loading.value = true
  try {
    const data = await keypersonApi.getGis()
    if (Array.isArray(data)) {
      markers.value = data
        .filter((d: any) => d.latitude && d.longitude)
        .map((d: any) => ({
          lat: d.latitude,
          lng: d.longitude,
          popup: `<b>${d.name || '未知'}</b><br/>管控级别: ${d.controlLevel || '-'}<br/>类型: ${d.controlType || '-'}`,
          color: levelColors[d.controlLevel] || '#909399',
        }))
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { margin: 0; }
.gis-legend { margin-top: 12px; display: flex; gap: 12px; }
</style>
