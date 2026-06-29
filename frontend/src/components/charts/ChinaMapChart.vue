<template>
  <div class="china-map-wrapper">
    <!-- 省级视图返回按钮 -->
    <div v-if="drillProvince" style="margin-bottom:8px">
      <el-button size="small" @click="backToNational" icon="ArrowLeft">
        返回全国
      </el-button>
      <el-tag size="small" type="primary" style="margin-left:8px">{{ drillProvince }}</el-tag>
    </div>
    <v-chart
      v-if="!loading && mapReady"
      :option="chartOption"
      :style="{ height: height }"
      autoresize
      @click="onMapClick"
    />
    <el-skeleton v-else :rows="14" animated :style="{ height: height }" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import * as echarts from 'echarts/core'
import { MapChart, LinesChart, EffectScatterChart, ScatterChart } from 'echarts/charts'
import {
  TooltipComponent, VisualMapComponent, GeoComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'

echarts.use([MapChart, LinesChart, EffectScatterChart, ScatterChart,
  TooltipComponent, VisualMapComponent, GeoComponent, CanvasRenderer])

interface ProvinceData { name: string; value: number; male?: number; female?: number }
interface FlowData { from_name: string; to_name: string; value: number }

const props = withDefaults(
  defineProps<{
    data?: ProvinceData[]
    flows?: FlowData[]
    cityData?: Array<{ name: string; value: number; area_id?: number }>
    height?: string
    loading?: boolean
    filterMode?: 'resident' | 'migration'
    selectedProvince?: string
  }>(),
  {
    data: () => [], flows: () => [], cityData: () => [],
    height: '620px', loading: false, filterMode: 'resident', selectedProvince: '',
  }
)

const emit = defineEmits<{
  'province-click': [provinceName: string]
  'drill-down': [provinceName: string]
  'back-national': []
}>()

const mapReady = ref(false)
const drillProvince = ref('')
const provinceGeo = ref<any>(null)
const geoCoordMap: Record<string, [number, number]> = {}
const adcodeMap: Record<string, number> = {}

// 加载中国地图
async function loadMap() {
  if (mapReady.value) return
  try {
    const geoJson = await import('@/assets/china.json')
    const gj = (geoJson.default || geoJson) as any
    echarts.registerMap('china', gj)
    const features = gj.features || []
    for (const f of features) {
      const props: any = f.properties || {}
      const name = props.name
      const cp = props.centroid || props.center
      if (name && cp && Array.isArray(cp) && cp.length >= 2) {
        geoCoordMap[name] = [cp[0], cp[1]]
      }
      if (name && props.adcode) adcodeMap[name] = props.adcode
    }
    mapReady.value = true
  } catch { /* */ }
}
loadMap()

const maxValue = computed(() => {
  if (!props.data?.length) return 100
  return Math.max(...props.data.map(d => d.value))
})

// 流向线
const flowLines = computed(() => {
  if (!props.flows?.length) return []
  const maxFlow = Math.max(...props.flows.map(f => f.value))
  return props.flows
    .filter(f => f.from_name !== f.to_name)  // 过滤同省自迁移（零长度线不可见）
    .filter(f => geoCoordMap[f.from_name] && geoCoordMap[f.to_name])
    .map(f => ({
      fromName: f.from_name, toName: f.to_name,
      coords: [geoCoordMap[f.from_name], geoCoordMap[f.to_name]],
      value: f.value,
    }))
})

// 加载省份地图并注册
async function loadProvinceGeo(provinceName: string) {
  const adcode = adcodeMap[provinceName]
  if (!adcode) return
  try {
    const url = `https://geo.datav.aliyun.com/areas_v3/bound/${adcode}_full.json`
    const resp = await fetch(url)
    const geo = await resp.json()
    provinceGeo.value = geo
    echarts.registerMap('province', geo)
    // 提取城市级中心点
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

watch(() => props.selectedProvince, (name) => {
  if (name) loadProvinceGeo(name)
})

const chartOption = computed(() => {
  const isFlow = props.filterMode === 'migration'
  const isDrill = !!drillProvince.value && !!provinceGeo.value

  const mapName = isDrill ? 'province' : 'china'

  const geoComponent: any = {
    map: mapName, roam: true,
    zoom: isDrill ? 1.2 : 1.35,
    scaleLimit: { min: 0.9, max: 6 },
    top: 10, bottom: 40,
    label: { show: true, fontSize: 9, color: '#303133' },
    emphasis: {
      label: { show: true, fontSize: 14, fontWeight: 'bold' },
      itemStyle: { areaColor: '#f0cf85' },
    },
    itemStyle: { borderColor: '#fff', borderWidth: 1 },
  }
  if (!isDrill) geoComponent.center = [104.5, 37.5]

  const mapSeries: any = {
    type: 'map', map: mapName, roam: true,
    zoom: isDrill ? 1.2 : 1.35,
    scaleLimit: { min: 0.9, max: 6 },
    top: 10, bottom: 40,
    label: { show: true, fontSize: 9, color: '#303133' },
    emphasis: {
      label: { show: true, fontSize: 14, fontWeight: 'bold' },
      itemStyle: { areaColor: '#f0cf85' },
    },
    itemStyle: { borderColor: '#fff', borderWidth: 1 },
    data: [],
  }
  if (!isDrill) mapSeries.center = [104.5, 37.5]

  if (!isFlow && !isDrill) {
    mapSeries.data = props.data.map(d => ({
      name: d.name, value: d.value,
      selected: props.selectedProvince ? d.name === props.selectedProvince : false,
    }))
  }

  // 省级视图下显示城市数据点
  if (isDrill && !isFlow) {
    mapSeries.data = (props.cityData || []).map(d => ({
      name: d.name, value: d.value || 0,
    }))
  }

  const series: any[] = [mapSeries]

  // 流向线
  if (isFlow && !isDrill && flowLines.value.length > 0) {
    series.push({
      type: 'lines', coordinateSystem: 'geo', geoIndex: 0, zlevel: 1,
      polyline: false,
      effect: { show: true, period: 4, trailLength: 0.3, symbol: 'arrow', symbolSize: 8 },
      lineStyle: { color: '#e74c3c', width: 1, opacity: 0.4, curveness: 0.2 },
      data: flowLines.value.map(f => ({
        fromName: f.fromName, toName: f.toName, coords: f.coords, value: f.value,
      })),
    })
    const fromPoints = flowLines.value.map(f => ({ name: f.fromName, value: f.coords[0] }))
    series.push({
      type: 'effectScatter', coordinateSystem: 'geo', geoIndex: 0, zlevel: 2,
      rippleEffect: { brushType: 'stroke' }, symbolSize: 6,
      itemStyle: { color: '#e74c3c' },
      data: [...new Map(fromPoints.map(p => [JSON.stringify(p.value), p])).values()],
    })
  }

  const needsGeo = isFlow || isDrill
  return {
    geo: needsGeo ? geoComponent : undefined,
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
          return `${d.fromName || ''} → ${d.toName || ''}<br/>${(d.value || 0).toLocaleString()} 人`
        }
        return ''
      },
    },
    visualMap: isFlow
      ? undefined
      : {
          min: 0, max: maxValue.value,
          left: 'left', bottom: 20, calculable: true, orient: 'horizontal' as const,
          inRange: { color: ['#e0f3f8', '#abd9e9', '#74add1', '#4575b4', '#313695'] },
          text: ['高', '低'], textStyle: { color: '#606266' },
        },
    series,
  }
})

let lastClickTime = 0
function onMapClick(params: any) {
  if (params.componentType === 'series' && params.seriesType === 'map' && params.name) {
    const now = Date.now()
    if (now - lastClickTime < 400) return
    lastClickTime = now
    // 全国视图点击省份 → 下钻
    if (!drillProvince.value) {
      loadProvinceGeo(params.name)
    }
    emit('province-click', params.name)
  }
}
</script>

<style scoped>
.china-map-wrapper { width: 100%; }
</style>
