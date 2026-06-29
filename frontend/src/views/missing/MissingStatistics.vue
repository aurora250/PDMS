<template>
  <div>
    <div class="page-header"><h3>失踪人口统计</h3></div>

    <!-- 概览卡片 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <StatCard title="累计登记" :value="stats.totalCount" icon="Warning" color="danger" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="当前失踪" :value="stats.missingCount" icon="Search" color="warning" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="已寻回" :value="stats.recoveredCount" icon="CircleCheck" color="success" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="寻回率" :value="stats.recoveryRate" icon="TrendCharts" color="primary" :loading="loading" />
      </el-col>
    </el-row>

    <!-- 图表行1：失踪状态 + 管控级别 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="12">
        <el-card header="失踪人员状态分布">
          <v-chart v-if="!loading" :option="statusOption" style="height:350px" autoresize />
          <el-empty v-else-if="!loading" description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="重点人员管控级别分布">
          <v-chart v-if="!loading" :option="kpLevelOption" style="height:350px" autoresize />
          <el-empty v-else-if="!loading" description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表行2：管控类型 + 失踪地点 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="12">
        <el-card header="重点人员管控类型分布">
          <v-chart v-if="!loading && kpTypeData.length" :option="kpTypeOption" style="height:350px" autoresize />
          <el-empty v-else-if="!loading" description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="失踪地点分布 (TOP 10)">
          <v-chart v-if="!loading && placeData.length" :option="placeOption" style="height:350px" autoresize />
          <el-empty v-else-if="!loading" description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 汇总表格 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="失踪人员 — 最近登记">
          <el-table :data="recentMissing" v-loading="loading" stripe size="small" max-height="300">
            <el-table-column prop="missingDate" label="失踪日期" width="110" />
            <el-table-column prop="missingPlace" label="失踪地点" min-width="180" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }"><ApprovalBadge :status="row.status" /></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="重点人员 — 按类型统计">
          <el-table :data="kpTypeData" v-loading="loading" stripe size="small" max-height="300">
            <el-table-column prop="name" label="管控类型" min-width="150" />
            <el-table-column prop="value" label="人数" width="80" align="right" sortable />
            <el-table-column label="占比" width="80" align="right">
              <template #default="{ row }">
                {{ kpTotal ? Math.round(row.value / kpTotal * 100) + '%' : '-' }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { missingApi } from '@/api/missing'
import { keypersonApi } from '@/api/keyperson'
import StatCard from '@/components/StatCard.vue'
import ApprovalBadge from '@/components/ApprovalBadge.vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, BarChart, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const loading = ref(false)

const stats = reactive({
  totalCount: 0, missingCount: 0, recoveredCount: 0, recoveryRate: 0,
})

const recentMissing = ref<any[]>([])

// Missing person chart data
const statusOption = ref({})
const placeData = ref<{ name: string; value: number }[]>([])
const placeOption = ref({})

// KeyPerson chart data
const kpLevelOption = ref({})
const kpTypeData = ref<{ name: string; value: number }[]>([])
const kpTotal = ref(0)
const kpTypeOption = ref({})

onMounted(async () => {
  loading.value = true
  try {
    const [statsRes, missingRes, kpRes]: any[] = await Promise.all([
      missingApi.statistics(),
      missingApi.search({ page: 1, size: 200 }),
      keypersonApi.search({ page: 1, size: 2000 }),
    ])

    // --- Stats ---
    if (statsRes) {
      stats.totalCount = statsRes.totalCount || 0
      stats.missingCount = statsRes.missingCount || 0
      stats.recoveredCount = statsRes.recoveredCount || 0
      stats.recoveryRate = stats.totalCount > 0 ? Math.round((stats.recoveredCount / stats.totalCount) * 100) : 0

      statusOption.value = {
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        legend: { bottom: 0 },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          center: ['50%', '45%'],
          label: { show: true, formatter: '{b}\n{d}%' },
          data: [
            { value: stats.missingCount, name: '失踪中', itemStyle: { color: '#f56c6c' } },
            { value: stats.recoveredCount, name: '已寻回', itemStyle: { color: '#67c23a' } },
          ],
        }],
      }
    }

    // --- Missing person places ---
    const missingRecords = missingRes?.records || []
    recentMissing.value = missingRecords.slice(0, 20)
    const placeCounts: Record<string, number> = {}
    missingRecords.forEach((m: any) => {
      const place = m.missingPlace || '未知'
      // Simplify long addresses
      const short = place.length > 15 ? place.substring(0, 15) + '...' : place
      placeCounts[short] = (placeCounts[short] || 0) + 1
    })
    placeData.value = Object.entries(placeCounts)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 10)
      .map(([name, value]) => ({ name, value }))

    placeOption.value = {
      tooltip: { trigger: 'axis' },
      grid: { left: 20, right: 20, bottom: 20, top: 10, containLabel: true },
      xAxis: { type: 'value' },
      yAxis: {
        type: 'category',
        data: placeData.value.map(p => p.name).reverse(),
        axisLabel: { fontSize: 11 },
      },
      series: [{
        type: 'bar',
        data: placeData.value.map(p => p.value).reverse(),
        itemStyle: { color: '#e6a23c' },
      }],
    }

    // --- KeyPerson stats ---
    const kpRecords = kpRes?.records || []
    kpTotal.value = kpRes?.total || kpRecords.length

    // Control level distribution
    const levelCounts: Record<string, number> = {}
    kpRecords.forEach((k: any) => {
      const lv = k.controlLevel || '未知'
      levelCounts[lv] = (levelCounts[lv] || 0) + 1
    })
    const levelColors: Record<string, string> = { '一级': '#e74c3c', '二级': '#e6a23c', '三级': '#409eff' }
    kpLevelOption.value = {
      tooltip: { trigger: 'item', formatter: '{b}: {c}人 ({d}%)' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: '60%',
        center: ['50%', '45%'],
        label: { show: true, formatter: '{b}\n{d}%' },
        data: Object.entries(levelCounts).map(([name, value]) => ({
          name, value, itemStyle: { color: levelColors[name] || '#909399' },
        })),
      }],
    }

    // Control type distribution
    const typeCounts: Record<string, number> = {}
    kpRecords.forEach((k: any) => {
      const t = k.controlType || '未知'
      typeCounts[t] = (typeCounts[t] || 0) + 1
    })
    kpTypeData.value = Object.entries(typeCounts)
      .sort(([, a], [, b]) => b - a)
      .map(([name, value]) => ({ name, value }))

    kpTypeOption.value = {
      tooltip: { trigger: 'axis' },
      grid: { left: 20, right: 20, bottom: 20, top: 10, containLabel: true },
      xAxis: { type: 'value' },
      yAxis: {
        type: 'category',
        data: kpTypeData.value.map(t => t.name).reverse(),
        axisLabel: { fontSize: 11 },
      },
      series: [{
        type: 'bar',
        data: kpTypeData.value.map(t => t.value).reverse(),
        itemStyle: { color: '#409eff' },
      }],
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
