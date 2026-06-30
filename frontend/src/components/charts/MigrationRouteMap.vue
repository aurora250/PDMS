<template>
  <div class="migration-map-container">
    <div v-if="routes.length === 0" class="empty-state">
      <el-empty description="暂无迁移记录" />
    </div>
    <template v-else>
      <v-chart v-if="option" :option="option" :style="{ height: height }" autoresize />
      <!-- 迁移时间线 -->
      <div class="migration-timeline" style="margin-top: 16px">
        <el-timeline>
          <el-timeline-item
            v-for="(route, idx) in routes"
            :key="idx"
            :timestamp="route.date"
            :color="idx === 0 ? '#67C23A' : '#909399'"
            placement="top"
          >
            <el-card shadow="hover" size="small">
              <p><strong>{{ route.type || '户籍迁移' }}</strong></p>
              <p style="font-size: 12px; color: #909399">
                <el-tag size="small" type="danger">迁出</el-tag> {{ route.fromAddress }}
              </p>
              <p style="font-size: 12px; color: #909399">
                <el-tag size="small" type="success">迁入</el-tag> {{ route.toAddress }}
              </p>
              <p v-if="route.status" style="font-size: 12px">
                状态: <el-tag size="small">{{ route.status }}</el-tag>
              </p>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use, registerMap } from 'echarts/core'
import { LinesChart, ScatterChart, EffectScatterChart } from 'echarts/charts'
import { GeoComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { resolveCoord } from '@/utils/geo'

use([LinesChart, ScatterChart, EffectScatterChart, GeoComponent, TooltipComponent, CanvasRenderer])

// 动态加载中国地图GeoJSON
let mapRegistered = false
async function ensureMap() {
  if (mapRegistered) return
  try {
    const geoJson = await import('@/assets/china.json')
    registerMap('china', geoJson.default as any)
    mapRegistered = true
  } catch {
    // 地图数据不可用时降级为仅显示时间线
  }
}
ensureMap()

export interface MigrationRoute {
  fromAddress: string
  toAddress: string
  date: string
  type?: string
  status?: string
}

const props = withDefaults(defineProps<{
  routes: MigrationRoute[]
  height?: string
}>(), {
  height: '400px',
})

const option = computed(() => {
  if (props.routes.length === 0) return null

  const coords: Array<{ name: string; value: [number, number] }> = []
  const lines: Array<{ coords: [[number, number], [number, number]] }> = []

  for (const route of props.routes) {
    const fromCoord = resolveCoord(route.fromAddress)
    const toCoord = resolveCoord(route.toAddress)
    if (fromCoord && toCoord) {
      coords.push({ name: route.fromAddress, value: fromCoord })
      coords.push({ name: route.toAddress, value: toCoord })
      lines.push({ coords: [fromCoord, toCoord] })
    }
  }

  if (lines.length === 0) return null

  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => p.name || '',
    },
    geo: {
      map: 'china',
      roam: true,
      label: { show: false },
      itemStyle: { areaColor: '#f5f5f5', borderColor: '#ddd' },
    },
    series: [
      {
        type: 'effectScatter',
        coordinateSystem: 'geo',
        data: coords.filter((_, i) => i % 2 === 0), // 迁出点
        symbolSize: 10,
        rippleEffect: { brushType: 'stroke' },
        itemStyle: { color: '#F56C6C' },
      },
      {
        type: 'effectScatter',
        coordinateSystem: 'geo',
        data: coords.filter((_, i) => i % 2 === 1), // 迁入点
        symbolSize: 10,
        rippleEffect: { brushType: 'stroke' },
        itemStyle: { color: '#67C23A' },
      },
      {
        type: 'lines',
        coordinateSystem: 'geo',
        data: lines,
        lineStyle: { color: '#409EFF', width: 2, curveness: 0.2 },
        effect: { show: true, period: 6, trailLength: 0.3, symbol: 'arrow', symbolSize: 8 },
      },
    ],
  }
})
</script>

<style scoped>
.migration-map-container { width: 100%; }
.empty-state { display: flex; justify-content: center; padding: 40px 0; }
</style>
