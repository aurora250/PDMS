<template>
  <div>
    <div class="page-header"><h3>GIS地图</h3></div>
    <el-card><div ref="mapContainer" style="height:600px" /></el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import L from 'leaflet'
const mapContainer = ref()
onMounted(() => {
  const map = L.map(mapContainer.value).setView([39.9, 116.4], 10)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', { maxZoom: 19 }).addTo(map)
})
</script>
