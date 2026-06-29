<template>
  <div class="dashboard">
    <h3>仪表盘</h3>

    <!-- 概览卡片 -->
    <el-row :gutter="16">
      <el-col :span="6">
        <StatCard title="常住人口" :value="stats.resident" icon="User" color="primary" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="流动人口" :value="stats.floating" icon="Ship" color="success" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="重点人员" :value="stats.keyperson" icon="Warning" color="warning" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="待处理预警" :value="stats.alerts" icon="Bell" color="danger" :loading="loading" />
      </el-col>
    </el-row>

    <!-- 中国地图 -->
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="detailVisible ? 16 : 24">
        <el-card>
          <template #header>
            <div class="map-header">
              <span>人口地理分布</span>
              <div class="map-controls">
                <el-radio-group v-model="mapFilter" size="small" @change="onFilterChange">
                  <el-radio-button value="resident">户籍人口</el-radio-button>
                  <el-radio-button value="migration">人口迁移</el-radio-button>
                </el-radio-group>
                <el-tag v-if="selectedProvince" closable size="small" type="warning" @close="clearSelection">
                  {{ selectedProvince }}
                </el-tag>
              </div>
            </div>
          </template>
          <ChinaMapChart
            :data="mapFilter === 'resident' ? provinceData : []"
            :flows="mapFilter === 'migration' ? flowData : []"
            :city-data="cityData"
            :filter-mode="mapFilter"
            :selected-province="selectedProvince"
            :loading="mapLoading"
            height="620px"
            @province-click="onProvinceClick"
            @drill-down="onDrillDown"
            @back-national="onBackNational"
          />
        </el-card>
      </el-col>

      <!-- 右侧详情栏 -->
      <el-col v-if="detailVisible" :span="8">
        <el-card class="detail-panel">
          <template #header>
            <div class="detail-header">
              <span>{{ selectedProvince }}</span>
              <el-button text size="small" @click="clearSelection">✕</el-button>
            </div>
          </template>
          <div v-if="cityData.length > 0" class="detail-body">
            <el-statistic title="城市数" :value="cityData.length" suffix="个" />
            <el-divider />
            <div style="max-height:360px;overflow-y:auto">
              <div v-for="c in cityData.slice(0, 20)" :key="c.name" class="flow-item" style="display:flex;justify-content:space-between">
                <el-text size="small">{{ c.name }}</el-text>
                <el-text size="small" type="primary">{{ c.value?.toLocaleString() }}人</el-text>
              </div>
            </div>
            <el-divider />
            <div class="detail-actions">
              <el-button type="primary" size="small" @click="goToResidentList">
                查看常住人口
              </el-button>
            </div>
          </div>
          <div v-else-if="provinceDetail" class="detail-body">
            <el-statistic title="常住人口" :value="provinceDetail.value || 0" suffix="人" />
            <el-divider />
            <el-row :gutter="12">
              <el-col :span="12">
                <el-statistic title="男性" :value="provinceDetail.male || 0" suffix="人" />
              </el-col>
              <el-col :span="12">
                <el-statistic title="女性" :value="provinceDetail.female || 0" suffix="人" />
              </el-col>
            </el-row>
            <el-divider />
            <div class="detail-actions">
              <el-button type="primary" size="small" @click="goToResidentList">
                查看常住人口
              </el-button>
              <el-button size="small" @click="goToMigrationList">
                查看迁移记录
              </el-button>
            </div>
            <!-- 迁入/迁出 Top5 -->
            <div v-if="flowData.length > 0" style="margin-top:12px">
              <el-text size="small" type="info">相关迁移:</el-text>
              <div v-for="f in relatedFlows" :key="f.from_name + f.to_name" class="flow-item">
                <el-text size="small">
                  {{ f.from_name }} → {{ f.to_name }}: {{ f.value }}人
                </el-text>
              </div>
            </div>
          </div>
          <el-empty v-else description="点击省份查看详情" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 流动人口趋势 & 近期预警 -->
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header>流动人口趋势</template>
          <v-chart v-if="!trendEmpty" :option="trendOption" style="height:300px" autoresize />
          <el-empty v-else description="暂无趋势数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>近期预警</template>
          <el-empty v-if="alertList.length === 0" description="暂无预警，系统运行正常" />
          <el-timeline v-else>
            <el-timeline-item
              v-for="a in alertList"
              :key="a.alertId || a.id"
              :timestamp="a.createdAt"
              :type="a.severity === '高' ? 'danger' : 'warning'"
            >
              {{ a.alertContent || a.message }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { floatingApi } from '@/api/floating'
import { alertApi } from '@/api/alert'
import { statisticsApi } from '@/api/statistics'
import StatCard from '@/components/StatCard.vue'
import ChinaMapChart from '@/components/charts/ChinaMapChart.vue'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const router = useRouter()
const loading = ref(false)
const mapLoading = ref(false)
const trendEmpty = ref(false)

const stats = reactive({ resident: 0, floating: 0, keyperson: 0, alerts: 0 })
const alertList = ref<any[]>([])

// 地图数据
const mapFilter = ref<'resident' | 'migration'>('resident')
const provinceData = ref<Array<{ name: string; value: number; male?: number; female?: number }>>([])
const flowData = ref<Array<{ from_name: string; to_name: string; value: number }>>([])
const cityData = ref<Array<{ name: string; value: number; area_id?: number }>>([])
const selectedProvince = ref('')
const detailVisible = ref(false)

const provinceDetail = computed(() => {
  if (!selectedProvince.value) return null
  return provinceData.value.find(d => d.name === selectedProvince.value) || null
})

const relatedFlows = computed(() => {
  if (!selectedProvince.value || !flowData.value.length) return []
  return flowData.value
    .filter(f => f.from_name === selectedProvince.value || f.to_name === selectedProvince.value)
    .sort((a, b) => b.value - a.value)
    .slice(0, 5)
})

const trendOption = ref({
  tooltip: { trigger: 'axis' },
  legend: { data: ['流动人口'] },
  xAxis: { type: 'category', data: [] as string[] },
  yAxis: { type: 'value' },
  series: [{ name: '流动人口', type: 'line', data: [] as number[], smooth: true }],
})

onMounted(async () => {
  loading.value = true
  mapLoading.value = true

  // 加载省级人口分布
  try {
    const provData: any = await statisticsApi.getProvincePopulation()
    if (Array.isArray(provData)) {
      provinceData.value = provData.map((d: any) => ({
        name: d.name || '未知',
        value: Number(d.value) || 0,
        male: Number(d.male) || 0,
        female: Number(d.female) || 0,
      }))
      stats.resident = provinceData.value.reduce((sum, d) => sum + d.value, 0)
    }
  } catch { /* */ }
  finally { mapLoading.value = false }

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
        const date = t.registerDate || ''
        const month = date.substring(0, 7)
        if (month) monthCounts[month] = (monthCounts[month] || 0) + 1
      })
      const sorted = Object.entries(monthCounts).sort()
      trendOption.value.xAxis.data = sorted.map(([k]) => k)
      trendOption.value.series[0].data = sorted.map(([, v]) => v)
      stats.floating = sorted.reduce((sum, [, v]) => sum + v, 0)
    } else {
      trendEmpty.value = true
    }
  } catch { trendEmpty.value = true }

  // 预警
  try {
    const pending: any = await alertApi.pending()
    if (Array.isArray(pending)) {
      alertList.value = pending.slice(0, 5)
      stats.alerts = pending.length
    }
  } catch { /* */ }

  // 重点人员
  try {
    const { keypersonApi } = await import('@/api/keyperson')
    const kpRes: any = await keypersonApi.search({ page: 1, size: 1 })
    stats.keyperson = kpRes?.total || 0
  } catch { /* */ }

  loading.value = false
})

async function onFilterChange(mode: string) {
  if (mode === 'migration' && flowData.value.length === 0) {
    try {
      const flows: any = await statisticsApi.getMigrationFlows()
      if (Array.isArray(flows)) {
        flowData.value = flows
      }
    } catch { /* */ }
  }
}

function onProvinceClick(name: string) {
  if (selectedProvince.value === name) {
    clearSelection()
  } else {
    selectedProvince.value = name
    detailVisible.value = true
  }
}

async function onDrillDown(provinceName: string) {
  selectedProvince.value = provinceName
  detailVisible.value = true
  try {
    const cities: any = await statisticsApi.getCityPopulation(provinceName)
    if (Array.isArray(cities)) {
      cityData.value = cities.map((c: any) => ({
        name: c.name || '', value: Number(c.value) || 0,
        area_id: c.area_id, male: c.male, female: c.female,
      }))
    }
  } catch { cityData.value = [] }
}

function onBackNational() {
  cityData.value = []
}

function clearSelection() {
  selectedProvince.value = ''
  detailVisible.value = false
  cityData.value = []
}

function goToResidentList() {
  router.push({ path: '/resident', query: { province: selectedProvince.value } })
}

function goToMigrationList() {
  router.push('/household/migration')
}
</script>

<style scoped>
.dashboard h3 { margin-bottom: 16px; }
.map-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.map-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}
.detail-panel {
  height: 100%;
  min-height: 520px;
}
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.detail-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.detail-actions {
  display: flex;
  gap: 8px;
}
.flow-item {
  padding: 2px 0;
  border-bottom: 1px dashed #ebeef5;
}
</style>
