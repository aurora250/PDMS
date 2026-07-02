<template>
  <div class="migration-map-container">
    <div v-if="routes.length === 0" class="empty-state">
      <el-empty description="暂无迁移记录" />
    </div>
    <template v-else>
      <v-chart v-if="option && mapReady" ref="chartRef" :option="option"
        :style="{ height: height }" autoresize
        @mousemove="onChartEvent" @click="onChartEvent" />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import VChart from 'vue-echarts'
import { use, registerMap } from 'echarts/core'
import { LinesChart, ScatterChart, EffectScatterChart } from 'echarts/charts'
import { GeoComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { resolveCoord } from '@/utils/geo'

use([LinesChart, ScatterChart, EffectScatterChart, GeoComponent, TooltipComponent, CanvasRenderer])

const chartRef = ref()

const mapReady = ref(false)
async function ensureMap() {
  try {
    const geoJson = await import('@/assets/china.json')
    registerMap('china', geoJson.default as any)
    mapReady.value = true
  } catch {
    mapReady.value = true
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
  hoveredIndex?: number | null
}>(), {
  height: '400px',
  hoveredIndex: null,
})

const emit = defineEmits<{
  (e: 'hover', index: number | null): void
  (e: 'click', index: number): void
}>()

// 状态 → 线颜色
function getLineColor(status?: string): string {
  if (!status) return '#409EFF'
  if (status.includes('驳回')) return '#F56C6C'
  if (status.includes('通过') || status === '有效') return '#409EFF'
  return '#E6A23C' // 中间状态
}

// 国家标准地图五色方案
const PROVINCE_COLORS: [string, string][] = [
  ['北京市', '#f5f0e0'], ['天津市', '#dcedc8'], ['河北省', '#f8e0e0'],
  ['山西省', '#f5f0e0'], ['内蒙古自治区', '#dcedc8'], ['辽宁省', '#f5f0e0'],
  ['吉林省', '#f8e0e0'], ['黑龙江省', '#f5f0e0'], ['上海市', '#f5f0e0'],
  ['江苏省', '#dcedc8'], ['浙江省', '#f8e0e0'], ['安徽省', '#f5f0e0'],
  ['福建省', '#f5f0e0'], ['江西省', '#dcedc8'], ['山东省', '#dae8fc'],
  ['河南省', '#dcedc8'], ['湖北省', '#f8e0e0'], ['湖南省', '#f5f0e0'],
  ['广东省', '#f8e0e0'], ['广西壮族自治区', '#dcedc8'], ['海南省', '#f5f0e0'],
  ['重庆市', '#dcedc8'], ['四川省', '#f5f0e0'], ['贵州省', '#f8e0e0'],
  ['云南省', '#dae8fc'], ['西藏自治区', '#dcedc8'], ['陕西省', '#dae8fc'],
  ['甘肃省', '#f8e0e0'], ['青海省', '#dae8fc'], ['宁夏回族自治区', '#f5f0e0'],
  ['新疆维吾尔自治区', '#f5f0e0'], ['台湾省', '#f5f0e0'],
  ['香港特别行政区', '#f5f0e0'], ['澳门特别行政区', '#f5f0e0'],
]

const option = computed(() => {
  if (props.routes.length === 0) return null

  const coords: Array<{ name: string; value: [number, number] }> = []
  const lines: Array<{
    coords: [[number, number], [number, number]]
    lineStyle: { color: string; width: number }
    name: string
  }> = []

  props.routes.forEach((route, i) => {
    const fromCoord = resolveCoord(route.fromAddress)
    const toCoord = resolveCoord(route.toAddress)
    if (fromCoord && toCoord) {
      coords.push({ name: route.fromAddress, value: fromCoord })
      coords.push({ name: route.toAddress, value: toCoord })
      const baseColor = getLineColor(route.status)
      const isHovered = props.hoveredIndex != null && props.hoveredIndex === i
      lines.push({
        coords: [fromCoord, toCoord],
        lineStyle: { color: isHovered ? brighten(baseColor) : baseColor, width: isHovered ? 4 : 2 },
        name: `${route.fromAddress || '?'} → ${route.toAddress || '?'}\n${route.type || ''}  ${route.status || ''}`,
      })
    }
  })

  if (lines.length === 0) return null

  let minLng = Infinity, maxLng = -Infinity, minLat = Infinity, maxLat = -Infinity
  for (const c of coords) {
    const [lng, lat] = c.value
    if (lng < minLng) minLng = lng
    if (lng > maxLng) maxLng = lng
    if (lat < minLat) minLat = lat
    if (lat > maxLat) maxLat = lat
  }
  const lngSpan = maxLng - minLng || 1
  const latSpan = maxLat - minLat || 1
  const zoom = Math.max(1, Math.min(50 / (lngSpan + 4), 35 / (latSpan + 4), 8))
  const center: [number, number] = [(minLng + maxLng) / 2, (minLat + maxLat) / 2]

  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => (p.name || '').replace(/\n/g, '<br/>'),
    },
    geo: {
      map: 'china', roam: true, center, zoom,
      label: { show: false },
      itemStyle: { borderColor: '#fff', borderWidth: 0.5 },
      regions: PROVINCE_COLORS.map(([name, color]) => ({ name, itemStyle: { areaColor: color } })),
    },
    series: [
      {
        type: 'effectScatter', coordinateSystem: 'geo',
        data: coords.filter((_, i) => i % 2 === 0),
        symbolSize: 10, rippleEffect: { brushType: 'stroke' },
        itemStyle: { color: '#F56C6C' },
      },
      {
        type: 'effectScatter', coordinateSystem: 'geo',
        data: coords.filter((_, i) => i % 2 === 1),
        symbolSize: 10, rippleEffect: { brushType: 'stroke' },
        itemStyle: { color: '#67C23A' },
      },
      {
        type: 'lines', coordinateSystem: 'geo', data: lines,
        lineStyle: { curveness: 0.2 },
        emphasis: { lineStyle: { width: 4 } },
        effect: { show: true, period: 6, trailLength: 0.3, symbol: 'arrow', symbolSize: 8 },
      },
    ],
  }
})

// brighten a hex color
function brighten(hex: string): string {
  const num = parseInt(hex.replace('#', ''), 16)
  const r = Math.min(255, ((num >> 16) & 0xff) + 40)
  const g = Math.min(255, ((num >> 8) & 0xff) + 40)
  const b = Math.min(255, (num & 0xff) + 40)
  return `#${((r << 16) | (g << 8) | b).toString(16).padStart(6, '0')}`
}

// chart events → detect lines series
function onChartEvent(event: any) {
  if (!event || event.seriesType !== 'lines') return
  const idx = event.dataIndex
  if (event.type === 'click') {
    emit('click', idx)
  } else if (event.type === 'mousemove') {
    emit('hover', idx)
  }
}

// when mouse leaves chart container, clear hover
function onChartMouseleave() {
  emit('hover', null)
}
</script>

<style scoped>
.migration-map-container { width: 100%; height: 100%; position: relative; }
.empty-state { display: flex; justify-content: center; padding: 40px 0; }
</style>
