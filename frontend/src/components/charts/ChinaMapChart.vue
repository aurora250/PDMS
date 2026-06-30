<template>
  <div class="china-map-wrapper">
    <!-- 省级视图返回按钮 -->
    <div v-if="drillProvince" class="drill-bar">
      <el-button size="small" @click="backToNational" :icon="ArrowLeft">返回全国</el-button>
      <el-tag size="small" type="primary" style="margin-left:8px">{{ drillProvince }}</el-tag>
    </div>
    <div class="map-chart-container">
      <v-chart
        v-if="!loading && mapReady"
        :option="chartOption"
        autoresize
        @click="onMapClick"
      />
      <el-skeleton v-else :rows="14" animated />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { MapChart, LinesChart, EffectScatterChart, ScatterChart } from 'echarts/charts'
import { TooltipComponent, VisualMapComponent, GeoComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'

echarts.use([MapChart, LinesChart, EffectScatterChart, ScatterChart,
  TooltipComponent, VisualMapComponent, GeoComponent, CanvasRenderer])

interface ProvinceData { name: string; value: number; male?: number; female?: number }
interface FlowData { from_name: string; to_name: string; value: number }

const COLOR_SCHEMES: Record<string, string[]> = {
  blue:   ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695'],
  red:    ['#fee5d9', '#fcae91', '#fb6a4a', '#de2d26', '#a50f15'],
  green:  ['#edf8e9', '#bae4b3', '#74c476', '#31a354', '#006d2c'],
  orange: ['#feedde', '#fdbe85', '#fd8d3c', '#e6550d', '#a63603'],
  purple: ['#f3e8ff', '#d4b8f0', '#b088e0', '#8b5fd0', '#6b3fb8'],
}

// 公安系统红黄蓝三级管控
const KP_LEVEL_COLORS: Record<string, string> = {
  '红': '#F56C6C', '1': '#F56C6C', '一级': '#F56C6C',
  '黄': '#E6A23C', '2': '#E6A23C', '二级': '#E6A23C',
  '蓝': '#409EFF', '3': '#409EFF', '三级': '#409EFF',
}

const props = withDefaults(defineProps<{
  data?: ProvinceData[]
  flows?: FlowData[]
  cityData?: Array<{ name: string; value: number; area_id?: number }>
  loading?: boolean
  displayMode?: 'heatmap' | 'flow' | 'mixed'
  colorScheme?: 'blue' | 'red' | 'green' | 'orange' | 'purple'
  selectedProvince?: string
  provinceHighlight?: string
  keypersonData?: Array<{ name: string; value: [number, number]; level?: number | string }>
  missingData?: Array<{ name: string; value: [number, number] }>
  overlayKeyperson?: boolean
  overlayMissing?: boolean
  flowThreshold?: number
  genderFilter?: string
}>(), {
  data: () => [], flows: () => [], cityData: () => [],
  loading: false,
  displayMode: 'heatmap', colorScheme: 'blue',
  selectedProvince: '', provinceHighlight: '',
  keypersonData: () => [], missingData: () => [],
  overlayKeyperson: false, overlayMissing: false,
  flowThreshold: 0, genderFilter: '',
})

const emit = defineEmits<{
  'province-click': [provinceName: string]
  'province-dblclick': [provinceName: string]
  'drill-down': [provinceName: string]
  'back-national': []
  'flow-click': [data: { fromName: string; toName: string; value: number }]
  'kp-click': [data: { name: string }]
  'missing-click': [data: { name: string }]
}>()

const mapReady = ref(false)
const drillProvince = ref('')
const provinceGeo = ref<any>(null)
const geoCoordMap: Record<string, [number, number]> = {}
const adcodeMap: Record<string, number> = {}

async function loadMap() {
  if (mapReady.value) return
  try {
    const geoJson = await import('@/assets/china.json')
    const gj = (geoJson.default || geoJson) as any
    echarts.registerMap('china', gj)
    const features = gj.features || []
    for (const f of features) {
      const fp: any = f.properties || {}
      const name = fp.name; const cp = fp.centroid || fp.center
      if (name && cp && Array.isArray(cp) && cp.length >= 2) geoCoordMap[name] = [cp[0], cp[1]]
      if (name && fp.adcode) adcodeMap[name] = fp.adcode
    }
    mapReady.value = true
  } catch { /* */ }
}
loadMap()

const colors = computed(() => COLOR_SCHEMES[props.colorScheme] || COLOR_SCHEMES.blue)
const showHeat = computed(() => props.displayMode === 'heatmap' || props.displayMode === 'mixed')
const showFlows = computed(() => props.displayMode === 'flow' || props.displayMode === 'mixed')

const maxValue = computed(() => {
  if (!props.data?.length) return 100
  return Math.max(...props.data.map(d => d.value))
})

// 按阈值过滤的流向线
const flowLines = computed(() => {
  if (!props.flows?.length) return []
  return props.flows
    .filter(f => f.from_name !== f.to_name)
    .filter(f => f.value >= (props.flowThreshold || 0))
    .filter(f => geoCoordMap[f.from_name] && geoCoordMap[f.to_name])
    .map(f => ({
      fromName: f.from_name, toName: f.to_name,
      coords: [geoCoordMap[f.from_name], geoCoordMap[f.to_name]],
      value: f.value,
    }))
})

async function loadProvinceGeo(provinceName: string) {
  const adcode = adcodeMap[provinceName]
  if (!adcode) return
  try {
    const url = `https://geo.datav.aliyun.com/areas_v3/bound/${adcode}_full.json`
    const resp = await fetch(url)
    const geo = await resp.json()
    provinceGeo.value = geo
    echarts.registerMap('province', geo)
    for (const f of (geo.features || [])) {
      const p: any = f.properties || {}
      const n = p.name, cp = p.centroid || p.center
      if (n && cp && Array.isArray(cp) && cp.length >= 2 && !geoCoordMap[n]) {
        geoCoordMap[n] = [cp[0], cp[1]]
      }
    }
    drillProvince.value = provinceName
    emit('drill-down', provinceName)
  } catch { /* */ }
}

function backToNational() {
  drillProvince.value = ''
  provinceGeo.value = null
  emit('back-national')
}

const chartOption = computed(() => {
  const isDrill = !!drillProvince.value && !!provinceGeo.value
  const mapName = isDrill ? 'province' : 'china'

  const geoComponent: any = {
    map: mapName, roam: true,
    zoom: isDrill ? 2.2 : 1.35,
    scaleLimit: { min: 0.9, max: 8 },
    top: isDrill ? 5 : 10, bottom: isDrill ? 5 : 40,
    label: { show: true, fontSize: 9, color: '#303133' },
    emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' }, itemStyle: { areaColor: '#f0cf85' } },
    itemStyle: { borderColor: '#fff', borderWidth: 1, areaColor: showHeat.value ? undefined : '#f8f9fa' },
  }
  if (!isDrill) geoComponent.center = [104.5, 37.5]

  const mapSeries: any = {
    type: 'map', map: mapName, roam: true,
    zoom: isDrill ? 2.2 : 1.35,
    scaleLimit: { min: 0.9, max: 8 },
    top: isDrill ? 5 : 10, bottom: isDrill ? 5 : 40,
    label: { show: true, fontSize: 9, color: '#303133' },
    emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' }, itemStyle: { areaColor: '#f0cf85' } },
    itemStyle: { borderColor: '#fff', borderWidth: 1 },
    data: [],
  }
  if (!isDrill) mapSeries.center = [104.5, 37.5]

  if (showHeat.value && !isDrill) {
    mapSeries.data = props.data.map(d => ({
      name: d.name, value: d.value,
      selected: props.selectedProvince ? d.name === props.selectedProvince : false,
    }))
  }
  if (isDrill && showHeat.value) {
    mapSeries.data = (props.cityData || []).map(d => ({ name: d.name, value: d.value || 0 }))
  }

  const series: any[] = [mapSeries]

  // 流向线
  if (showFlows.value && !isDrill && flowLines.value.length > 0) {
    series.push({
      type: 'lines', coordinateSystem: 'geo', geoIndex: 0, zlevel: 2,
      polyline: false,
      effect: { show: true, period: 4, trailLength: 0.3, symbol: 'arrow', symbolSize: 8 },
      lineStyle: { color: '#ff8c00', width: 1.5, opacity: 0.35, curveness: 0.2 },
      data: flowLines.value.map(f => ({
        fromName: f.fromName, toName: f.toName, coords: f.coords, value: f.value,
      })),
      name: '人口迁移流',
    })
    const fromPoints = flowLines.value.map(f => ({ name: f.fromName, value: f.coords[0] }))
    series.push({
      type: 'effectScatter', coordinateSystem: 'geo', geoIndex: 0, zlevel: 3,
      rippleEffect: { brushType: 'stroke' }, symbolSize: 5,
      itemStyle: { color: '#ff8c00' },
      data: [...new Map(fromPoints.map(p => [JSON.stringify(p.value), p])).values()],
      name: '迁出点',
    })
  }

  // 重点人员 — 按红黄蓝等级分色
  if (props.overlayKeyperson && props.keypersonData?.length && !isDrill) {
    const byLevel: Record<string, any[]> = {}
    for (const kp of props.keypersonData) {
      const colorKey = resolveKpColor(kp.level)
      if (!byLevel[colorKey]) byLevel[colorKey] = []
      byLevel[colorKey].push(kp)
    }
    const levelNames: Record<string, string> = {
      '#F56C6C': '红色(一级严控)', '#E6A23C': '黄色(二级常规)', '#409EFF': '蓝色(三级关注)',
    }
    for (const [colorKey, items] of Object.entries(byLevel)) {
      series.push({
        type: 'scatter', coordinateSystem: 'geo', geoIndex: 0, zlevel: 5,
        symbol: 'pin', symbolSize: 28,
        silent: false,
        itemStyle: { color: colorKey },
        data: items,
        name: `重点人员-${levelNames[colorKey] || colorKey}`,
      })
    }
  }

  // 失踪人员 — 蓝色三角标记
  if (props.overlayMissing && props.missingData?.length && !isDrill) {
    series.push({
      type: 'scatter', coordinateSystem: 'geo', geoIndex: 0, zlevel: 5,
      symbol: 'triangle', symbolSize: 20,
      silent: false,
      itemStyle: { color: '#409eff' },
      data: props.missingData,
      name: '失踪人员',
    })
  }

  const needsGeo = showFlows.value || isDrill || props.overlayKeyperson || props.overlayMissing

  return {
    geo: needsGeo ? geoComponent : undefined,
    backgroundColor: '#fff',
    tooltip: {
      trigger: 'item' as const,
      formatter: (params: any) => {
        if (params.seriesType === 'map') {
          if (!params.data) return params.name
          const v = params.data.value
          if (v === undefined || v === null) return `${params.name}<br/>暂无数据`
          return `<strong>${params.name}</strong><br/>${v.toLocaleString()} 人`
        }
        if (params.seriesType === 'lines') {
          const d = params.data || {}
          return `${d.fromName || ''} → ${d.toName || ''}<br/>${(d.value || 0).toLocaleString()} 人<br/><small>点击查看详情</small>`
        }
        if (params.seriesType === 'scatter') {
          const lvl = params.data?.level
          const lvlText = lvl ? ` [${lvl}级管控]` : ''
          return `${params.name}${lvlText}<br/><small>点击查看详情</small>`
        }
        return ''
      },
    },
    visualMap: showHeat.value ? {
      min: 0, max: maxValue.value,
      left: 'left', bottom: 20, calculable: true, orient: 'horizontal' as const,
      inRange: { color: colors.value },
      text: ['高', '低'], textStyle: { color: '#606266' },
    } : undefined,
    series,
  }
})

function resolveKpColor(level?: number | string): string {
  if (level === undefined || level === null) return '#E6A23C'
  const key = String(level)
  return KP_LEVEL_COLORS[key] || '#E6A23C'
}

// ── 单/双击区分 ──
let clickTimer: ReturnType<typeof setTimeout> | null = null
const DBL_DELAY = 300

function onMapClick(params: any) {
  // lines 系列 — 流线点击
  if (params.seriesType === 'lines') {
    const d = params.data || {}
    if (d.fromName && d.toName) {
      emit('flow-click', { fromName: d.fromName, toName: d.toName, value: d.value || 0 })
    }
    return
  }

  // scatter 系列 — 重点人员/失踪人员点击
  if (params.seriesType === 'scatter' && params.name) {
    if (params.seriesName?.startsWith('重点人员')) {
      emit('kp-click', { name: params.name })
    } else if (params.seriesName === '失踪人员') {
      emit('missing-click', { name: params.name })
    }
    return
  }

  // map 系列 — 省份单/双击
  if (params.seriesType === 'map' && params.name) {
    if (clickTimer) {
      // 300ms内第二次点击 → 双击 → 下钻
      clearTimeout(clickTimer)
      clickTimer = null
      if (!drillProvince.value) {
        loadProvinceGeo(params.name)
      }
      emit('province-dblclick', params.name)
    } else {
      // 第一次点击，等待300ms判断
      clickTimer = setTimeout(() => {
        clickTimer = null
        emit('province-click', params.name)
      }, DBL_DELAY)
    }
  }
}
</script>

<style scoped>
.china-map-wrapper {
  width: 100%; height: 100%;
  display: flex; flex-direction: column;
}
.drill-bar { margin-bottom: 8px; display: flex; align-items: center; flex-shrink: 0; }
.map-chart-container {
  flex: 1; min-height: 300px;
  display: flex; align-items: stretch;
}
.map-chart-container > * {
  flex: 1; min-height: 0;
}
</style>
