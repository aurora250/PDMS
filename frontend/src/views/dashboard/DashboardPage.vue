<template>
  <div class="dashboard">
    <!-- 顶部筛选栏 -->
    <div class="top-filter-bar">
      <div class="filter-controls">
        <span class="filter-label">数据维度</span>
        <el-select v-model="dataDimension" size="small" style="width:120px">
          <el-option label="常住人口" value="resident" />
          <el-option label="流动人口" value="floating" />
          <el-option label="重点人员" value="keyperson" />
        </el-select>
        <el-divider direction="vertical" />
        <el-button size="small" @click="resetFilters">重置</el-button>
      </div>
      <div class="filter-title"><h3>仪表盘</h3></div>
    </div>

    <!-- 主体三栏 -->
    <div class="dashboard-body">
      <!-- 左侧筛选面板 -->
      <div class="left-panel" :class="{ collapsed: leftCollapsed }">
        <div class="panel-toggle" @click="leftCollapsed = !leftCollapsed">
          <el-icon><component :is="leftCollapsed ? 'DArrowRight' : 'DArrowLeft'" /></el-icon>
        </div>
        <template v-if="!leftCollapsed">
          <div class="filter-group">
            <h4>显示模式</h4>
            <el-radio-group v-model="displayMode" size="small">
              <el-radio label="heatmap">热力图</el-radio>
              <el-radio label="flow">流向图</el-radio>
              <el-radio label="mixed">混合模式</el-radio>
            </el-radio-group>
          </div>
          <div class="filter-group">
            <h4>颜色方案</h4>
            <div class="color-scheme-picker">
              <span v-for="c in colorSchemes" :key="c.key"
                :class="['color-dot', c.key, { active: colorScheme === c.key }]"
                :title="c.label" @click="colorScheme = c.key" />
            </div>
          </div>
          <div class="filter-group">
            <h4>流线阈值</h4>
            <el-slider v-model="flowThreshold" :min="0" :max="2000" :step="50" size="small" />
            <span class="hint">≥ {{ flowThreshold }} 人</span>
          </div>
          <div class="filter-group">
            <h4>预警叠加</h4>
            <el-checkbox v-model="overlayKeyperson" label="重点人员" size="small" />
            <el-checkbox v-model="overlayMissing" label="失踪人员" size="small" />
          </div>
        </template>
      </div>

      <!-- 中央地图 -->
      <div class="map-area">
        <ChinaMapChart
          :data="mapData"
          :flows="flowData"
          :city-data="cityData"
          :display-mode="displayMode"
          :color-scheme="mapColorScheme"
          :selected-province="selectedProvince"
          :loading="mapLoading"
          :overlay-keyperson="overlayKeyperson"
          :overlay-missing="overlayMissing"
          :keyperson-data="kpMapData"
          :missing-data="missingMapData"
          :flow-threshold="flowThreshold"
          @province-click="onProvinceClick"
          @province-dblclick="onProvinceDblClick"
          @drill-down="onDrillDown"
          @back-national="onBackNational"
          @flow-click="onFlowClick"
          @kp-click="onKpClick"
          @missing-click="onMissingClick"
        />
      </div>

      <!-- 右侧详情面板 -->
      <div class="right-panel" :class="{ collapsed: rightCollapsed }">
        <div class="panel-toggle right-toggle" @click="rightCollapsed = !rightCollapsed">
          <el-icon><component :is="rightCollapsed ? 'DArrowLeft' : 'DArrowRight'" /></el-icon>
        </div>
        <template v-if="!rightCollapsed">
          <div class="panel-scroll">
            <!-- 模式标签 -->
            <el-tabs v-model="panelMode" size="small">
              <el-tab-pane label="概览" name="overview" />
              <el-tab-pane v-if="selectedProvince" label="省份" name="province" />
              <el-tab-pane v-if="selectedFlow" label="流线" name="flow" />
            </el-tabs>

            <!-- 概览模式 -->
            <div v-if="panelMode === 'overview'">
              <div class="panel-section">
                <h5>概览统计</h5>
                <div class="stat-grid">
                  <div @click="goToResidentList()" style="cursor:pointer">
                    <StatCard title="常住人口" :value="stats.resident" icon="User" color="primary" :loading="loading" mini />
                  </div>
                  <div @click="router.push('/floating/register')" style="cursor:pointer">
                    <StatCard title="流动人口" :value="stats.floating" icon="Ship" color="success" :loading="loading" mini />
                  </div>
                  <div @click="router.push('/keyperson/list')" style="cursor:pointer">
                    <StatCard title="重点人员" :value="stats.keyperson" icon="Warning" color="warning" :loading="loading" mini />
                  </div>
                  <div @click="router.push('/alert')" style="cursor:pointer">
                    <StatCard title="待处理预警" :value="stats.alerts" icon="Bell" color="danger" :loading="loading" mini />
                  </div>
                </div>
              </div>

              <div class="panel-section">
                <h5 style="cursor:pointer" @click="trendExpanded = !trendExpanded">
                  人口趋势 <el-icon><component :is="trendExpanded ? 'ArrowDown' : 'ArrowRight'" /></el-icon>
                </h5>
                <div v-show="trendExpanded">
                  <v-chart v-if="!trendEmpty" :option="trendOption" style="height:180px" autoresize />
                  <el-empty v-else description="暂无趋势数据" :image-size="40" />
                </div>
              </div>

              <div class="panel-section">
                <h5 style="cursor:pointer" @click="alertsExpanded = !alertsExpanded">
                  最近预警 <el-icon><component :is="alertsExpanded ? 'ArrowDown' : 'ArrowRight'" /></el-icon>
                </h5>
                <div v-show="alertsExpanded">
                  <el-empty v-if="alertList.length === 0" description="暂无预警" :image-size="40" />
                  <el-timeline v-else>
                    <el-timeline-item
                      v-for="a in alertList" :key="a.alertId || a.id"
                      :timestamp="a.createdAt"
                      :type="a.severity === '高' ? 'danger' : 'warning'"
                      size="small"
                    >
                      <a @click="router.push('/alert')" style="cursor:pointer;color:#409eff">
                        {{ a.alertContent || a.message }}
                      </a>
                    </el-timeline-item>
                  </el-timeline>
                </div>
              </div>
            </div>

            <!-- 省份模式 -->
            <div v-if="panelMode === 'province' && selectedProvince">
              <div class="panel-section">
                <h5>{{ selectedProvince }}</h5>
                <div v-if="cityData.length > 0">
                  <el-statistic title="城市数" :value="cityData.length" suffix="个" />
                  <div class="city-list" style="margin-top:8px;max-height:200px;overflow-y:auto">
                    <div v-for="c in cityData.slice(0, 15)" :key="c.name" class="city-row">
                      <el-text size="small">{{ c.name }}</el-text>
                      <el-text size="small" type="primary">{{ c.value?.toLocaleString() }}人</el-text>
                    </div>
                  </div>
                </div>
                <div v-else-if="provinceDetail">
                  <el-statistic title="常住人口" :value="provinceDetail.value || 0" suffix="人" />
                  <el-row :gutter="8" style="margin-top:8px">
                    <el-col :span="12"><el-statistic title="男性" :value="provinceDetail.male || 0" suffix="人" /></el-col>
                    <el-col :span="12"><el-statistic title="女性" :value="provinceDetail.female || 0" suffix="人" /></el-col>
                  </el-row>
                </div>
                <div class="detail-actions" style="margin-top:10px">
                  <el-button type="primary" size="small" @click="goToResidentList()">常住人口</el-button>
                  <el-button size="small" @click="goToMigrationList()">迁移记录</el-button>
                  <el-button size="small" @click="goToBookList()">户口簿</el-button>
                </div>
                <div v-if="flowData.length > 0" style="margin-top:8px">
                  <el-text size="small" type="info">相关迁移:</el-text>
                  <div v-for="f in relatedFlows" :key="f.from_name+f.to_name" class="flow-row"
                    style="cursor:pointer" @click="onFlowClick({ fromName: f.from_name, toName: f.to_name, value: f.value })">
                    <el-text size="small">{{ f.from_name }} → {{ f.to_name }}: {{ f.value }}人</el-text>
                  </div>
                </div>
              </div>
            </div>

            <!-- 流线模式 -->
            <div v-if="panelMode === 'flow' && selectedFlow">
              <div class="panel-section">
                <h5>迁移流详情</h5>
                <p><strong>{{ selectedFlow.fromName }}</strong> → <strong>{{ selectedFlow.toName }}</strong></p>
                <el-statistic title="迁移人数" :value="selectedFlow.value || 0" suffix="人" />
                <div class="detail-actions" style="margin-top:10px">
                  <el-button type="primary" size="small"
                    @click="router.push({path:'/household/migration',query:{from:selectedFlow.fromName,to:selectedFlow.toName}})">
                    查看迁移记录
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { floatingApi } from '@/api/floating'
import { alertApi } from '@/api/alert'
import { missingApi } from '@/api/missing'
import { residentApi } from '@/api/resident'
import { statisticsApi } from '@/api/statistics'
import StatCard from '@/components/StatCard.vue'
import ChinaMapChart from '@/components/charts/ChinaMapChart.vue'
import { resolveCoord, extractProvince } from '@/utils/geo'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const router = useRouter()
const loading = ref(false)
const mapLoading = ref(false)
const trendEmpty = ref(false)

// 面板
const leftCollapsed = ref(false)
const rightCollapsed = ref(false)
const trendExpanded = ref(true)
const alertsExpanded = ref(true)
const panelMode = ref('overview')

// 筛选
const dataDimension = ref<'resident' | 'floating' | 'keyperson'>('resident')
const displayMode = ref<'heatmap' | 'flow' | 'mixed'>('heatmap')
const colorScheme = ref<'blue' | 'red' | 'green' | 'orange' | 'purple'>('blue')
const overlayKeyperson = ref(false)
const overlayMissing = ref(false)
const flowThreshold = ref(0)

const colorSchemes = [
  { key: 'blue' as const, label: '蓝白' }, { key: 'red' as const, label: '红白' },
  { key: 'green' as const, label: '绿白' }, { key: 'orange' as const, label: '橙白' },
  { key: 'purple' as const, label: '紫白' },
]

const mapColorScheme = computed(() => {
  return dataDimension.value === 'floating' ? 'purple' : colorScheme.value
})

// 统计数据
const stats = reactive({ resident: 0, floating: 0, keyperson: 0, alerts: 0 })
const alertList = ref<any[]>([])

// 地图数据
const provinceData = ref<Array<{ name: string; value: number; male?: number; female?: number }>>([])
const flowData = ref<Array<{ from_name: string; to_name: string; value: number }>>([])
const cityData = ref<Array<{ name: string; value: number; area_id?: number }>>([])
const selectedProvince = ref('')
const selectedFlow = ref<{ fromName: string; toName: string; value: number } | null>(null)
const kpMapData = ref<Array<{ name: string; value: [number, number]; level?: number | string }>>([])
const missingMapData = ref<Array<{ name: string; value: [number, number] }>>([])

const mapData = computed(() => provinceData.value)
const provinceDetail = computed(() =>
  selectedProvince.value ? provinceData.value.find(d => d.name === selectedProvince.value) || null : null
)
const relatedFlows = computed(() => {
  if (!selectedProvince.value || !flowData.value.length) return []
  return flowData.value.filter(f => f.from_name === selectedProvince.value || f.to_name === selectedProvince.value)
    .sort((a, b) => b.value - a.value).slice(0, 5)
})

const trendOption = ref({
  tooltip: { trigger: 'axis' as const },
  legend: { data: ['流动人口'] },
  xAxis: { type: 'category' as const, data: [] as string[] },
  yAxis: { type: 'value' as const },
  series: [{ name: '流动人口', type: 'line' as const, data: [] as number[], smooth: true }],
})

// ── 数据加载 ──
async function loadResidentData() {
  mapLoading.value = true
  try {
    const [prov, flows] = await Promise.all([
      statisticsApi.getProvincePopulation(),
      statisticsApi.getMigrationFlows(),
    ])
    if (Array.isArray(prov)) {
      provinceData.value = prov.map((d: any) => ({
        name: d.name || '未知', value: Number(d.value) || 0,
        male: Number(d.male) || 0, female: Number(d.female) || 0,
      }))
      stats.resident = provinceData.value.reduce((s, d) => s + d.value, 0)
    }
    if (Array.isArray(flows)) flowData.value = flows
  } catch { /* */ }
  finally { mapLoading.value = false }
}

async function loadFloatingData() {
  mapLoading.value = true
  try {
    const fpData: any = await floatingApi.listResidence({ page: 1, size: 500 })
    const records = fpData?.records || (Array.isArray(fpData) ? fpData : [])
    const provCount: Record<string, number> = {}
    for (const r of records) {
      const addr = r.currentAddress || r.residence || ''
      const province = extractProvince(addr)
      if (province) provCount[province] = (provCount[province] || 0) + 1
    }
    provinceData.value = Object.entries(provCount)
      .filter(([, v]) => v > 0)
      .map(([name, value]) => ({ name, value, male: 0, female: 0 }))
    stats.floating = provinceData.value.reduce((s, d) => s + d.value, 0)
  } catch { /* */ }
  finally { mapLoading.value = false }
}

async function loadKeypersonData() {
  mapLoading.value = true
  try {
    const { keypersonApi } = await import('@/api/keyperson')
    const kpData: any = await keypersonApi.search({ page: 1, size: 100 })
    const records = kpData?.records || (Array.isArray(kpData) ? kpData : [])
    // 批量查居民信息
    const residentMap: Record<string, any> = {}
    for (const r of records.slice(0, 50)) {
      try {
        const res = await residentApi.getByUuid(r.uuid)
        if (res) residentMap[r.uuid] = res
      } catch { /* */ }
    }
    const provCount: Record<string, number> = {}
    for (const r of records) {
      const res = residentMap[r.uuid]
      const addr = res?.residence || res?.householdAddress || ''
      const province = extractProvince(addr)
      if (province) provCount[province] = (provCount[province] || 0) + 1
    }
    provinceData.value = Object.entries(provCount)
      .filter(([, v]) => v > 0)
      .map(([name, value]) => ({ name, value, male: 0, female: 0 }))
  } catch { /* */ }
  finally { mapLoading.value = false }
}

async function loadKeypersonMapData() {
  try {
    const { keypersonApi } = await import('@/api/keyperson')
    const kpData: any = await keypersonApi.search({ page: 1, size: 100 })
    const records = kpData?.records || (Array.isArray(kpData) ? kpData : [])
    // 两阶段查询获取居民地址
    const residentMap: Record<string, any> = {}
    for (const r of records.slice(0, 50)) {
      try {
        const res = await residentApi.getByUuid(r.uuid)
        if (res) residentMap[r.uuid] = res
      } catch { /* */ }
    }
    kpMapData.value = records
      .filter((k: any) => residentMap[k.uuid])
      .map((k: any) => {
        const res = residentMap[k.uuid]
        const addr = res?.residence || res?.householdAddress || ''
        return {
          name: res.name || k.uuid,
          value: resolveCoord(addr) || [116.46, 39.92] as [number, number],
          level: k.controlLevel || k.controlType || '黄',
        }
      })
  } catch { /* */ }
}

async function loadMissingMapData() {
  try {
    const data: any = await missingApi.search({ status: '失踪中', page: 1, size: 500 })
    const records = data?.records || (Array.isArray(data) ? data : [])
    missingMapData.value = records
      .filter((m: any) => m.missingPlace)
      .map((m: any) => ({
        name: m.name || '',
        value: resolveCoord(m.missingPlace) || [116.46, 39.92] as [number, number],
      }))
  } catch { /* */ }
}

// ── Watchers ──
watch(dataDimension, (dim) => {
  selectedProvince.value = ''; cityData.value = []; selectedFlow.value = null; panelMode.value = 'overview'
  if (dim === 'resident') loadResidentData()
  else if (dim === 'floating') loadFloatingData()
  else if (dim === 'keyperson') loadKeypersonData()
})

watch(overlayKeyperson, (on) => { if (on && kpMapData.value.length === 0) loadKeypersonMapData() })
watch(overlayMissing, (on) => { if (on && missingMapData.value.length === 0) loadMissingMapData() })

// ── 事件处理 ──
function onProvinceClick(name: string) {
  selectedProvince.value = name
  panelMode.value = 'province'
}

function onProvinceDblClick(name: string) {
  selectedProvince.value = name
  panelMode.value = 'province'
}

async function onDrillDown(provinceName: string) {
  selectedProvince.value = provinceName
  try {
    const cities: any = await statisticsApi.getCityPopulation(provinceName)
    if (Array.isArray(cities)) {
      cityData.value = cities.map((c: any) => ({
        name: c.name || '', value: Number(c.value) || 0,
      }))
    }
  } catch { cityData.value = [] }
}

function onBackNational() { cityData.value = [] }

function onFlowClick(flow: { fromName: string; toName: string; value: number }) {
  selectedFlow.value = flow
  panelMode.value = 'flow'
}

function onKpClick(data: { name: string }) {
  router.push('/keyperson/list')
}

function onMissingClick(data: { name: string }) {
  router.push('/missing/list')
}

function goToResidentList() {
  router.push({ path: '/resident', query: { province: selectedProvince.value } })
}

function goToMigrationList() { router.push({ path: '/household/migration', query: { from: selectedProvince.value } }) }

function goToBookList() { router.push({ path: '/household/book', query: { province: selectedProvince.value } }) }

function resetFilters() {
  dataDimension.value = 'resident'
  displayMode.value = 'heatmap'
  selectedProvince.value = ''
  selectedFlow.value = null
  cityData.value = []
  panelMode.value = 'overview'
}

// ── 初始加载 ──
onMounted(async () => {
  loading.value = true

  await loadResidentData()

  // 仪表盘概览
  try {
    const dashData: any = await statisticsApi.getDashboard()
    if (dashData) {
      if (dashData.residentCount) stats.resident = dashData.residentCount
      if (dashData.pendingAlerts) stats.alerts = dashData.pendingAlerts
    }
  } catch { /* */ }

  // 流动人口趋势
  try {
    const trend: any = await floatingApi.trend()
    if (trend && Array.isArray(trend) && trend.length > 0) {
      const monthCounts: Record<string, number> = {}
      trend.forEach((t: any) => {
        const date = t.registerDate || ''; const month = date.substring(0, 7)
        if (month) monthCounts[month] = (monthCounts[month] || 0) + 1
      })
      const sorted = Object.entries(monthCounts).sort()
      trendOption.value.xAxis.data = sorted.map(([k]) => k)
      trendOption.value.series[0].data = sorted.map(([, v]) => v)
      stats.floating = sorted.reduce((sum, [, v]) => sum + v, 0)
    } else { trendEmpty.value = true }
  } catch { trendEmpty.value = true }

  // 预警
  try {
    const pending: any = await alertApi.pending()
    if (Array.isArray(pending)) { alertList.value = pending.slice(0, 5); stats.alerts = pending.length }
  } catch { /* */ }

  // 重点人员
  try {
    const { keypersonApi } = await import('@/api/keyperson')
    const kpRes: any = await keypersonApi.search({ page: 1, size: 1 })
    stats.keyperson = kpRes?.total || 0
  } catch { /* */ }

  loading.value = false
})
</script>

<style scoped>
.dashboard {
  display: flex; flex-direction: column;
  height: calc(100vh - 120px); min-height: 600px; overflow: hidden;
}
.top-filter-bar {
  display: flex; align-items: center; justify-content: space-between;
  padding: 8px 16px; background: var(--pdm-header-bg); border-radius: 8px;
  margin-bottom: 12px; box-shadow: var(--pdm-card-shadow); flex-shrink: 0;
}
.filter-controls { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.filter-label { font-size: 13px; color: #606266; white-space: nowrap; }
.filter-title h3 { margin: 0; font-size: 16px; color: var(--pdm-primary); }
.dashboard-body { display: flex; flex: 1; gap: 12px; min-height: 0; overflow: hidden; }

/* 左侧面板 */
.left-panel {
  position: relative; width: 190px; flex-shrink: 0;
  background: var(--pdm-header-bg); border-radius: 8px; padding: 12px;
  box-shadow: var(--pdm-card-shadow); overflow-y: auto; transition: width 0.2s;
  border-top: 3px solid var(--pdm-primary);
}
.left-panel.collapsed { width: 36px; padding: 8px; }
.panel-toggle { position: absolute; top: 8px; right: 8px; cursor: pointer; color: #909399; z-index: 1; }
.panel-toggle:hover { color: var(--pdm-primary-light); }
.filter-group { margin-bottom: 16px; }
.filter-group h4 { font-size: 13px; color: #303133; margin: 0 0 8px; }
.hint { font-size: 11px; color: #909399; }
.color-scheme-picker { display: flex; gap: 8px; }
.color-dot {
  width: 22px; height: 22px; border-radius: 50%; cursor: pointer;
  border: 2px solid transparent; transition: border-color 0.2s;
}
.color-dot.blue { background: linear-gradient(135deg, #313695, #e0f3f8); }
.color-dot.red { background: linear-gradient(135deg, #a50f15, #fee5d9); }
.color-dot.green { background: linear-gradient(135deg, #006d2c, #edf8e9); }
.color-dot.orange { background: linear-gradient(135deg, #a63603, #feedde); }
.color-dot.purple { background: linear-gradient(135deg, #6b3fb8, #f3e8ff); }
.color-dot.active { border-color: var(--pdm-primary); }

/* 中央地图 */
.map-area {
  flex: 1; min-width: 500px; background: var(--pdm-header-bg); border-radius: 8px;
  box-shadow: var(--pdm-card-shadow); overflow: hidden;
  display: flex; min-height: 0;
}
.map-area > :deep(.china-map-wrapper) { flex: 1; display: flex; flex-direction: column; min-height: 0; }
.map-area > :deep(.china-map-wrapper) > div { flex: 1; min-height: 0; }

/* 右侧面板 */
.right-panel {
  position: relative; width: 340px; flex-shrink: 0;
  background: var(--pdm-header-bg); border-radius: 8px;
  box-shadow: var(--pdm-card-shadow);
  transition: width 0.2s; overflow: hidden; display: flex; flex-direction: column;
  border-top: 3px solid var(--pdm-primary);
}
.right-panel.collapsed { width: 36px; }
.right-toggle { left: 8px; right: auto; }
.panel-scroll { flex: 1; overflow-y: auto; padding: 12px; min-height: 0; }
.panel-section { margin-bottom: 14px; }
.panel-section h5 { font-size: 13px; color: #303133; margin: 0 0 8px; display: flex; align-items: center; gap: 4px; }
.stat-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.detail-actions { display: flex; gap: 6px; flex-wrap: wrap; }
.city-row { display: flex; justify-content: space-between; padding: 2px 0; border-bottom: 1px dashed #ebeef5; }
.flow-row { padding: 2px 0; border-bottom: 1px dashed #ebeef5; }
</style>
