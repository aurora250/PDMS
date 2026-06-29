<template>
  <div>
    <div class="page-header"><h3>流动人口统计</h3></div>
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="6">
        <StatCard title="居住登记数" :value="stats.residenceCount" icon="User" color="primary" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="流动登记数" :value="stats.registerCount" icon="Ship" color="success" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="区域数" :value="stats.areaCount" icon="Location" color="warning" :loading="loading" />
      </el-col>
      <el-col :span="6">
        <StatCard title="本月新增" :value="stats.currentMonth" icon="TrendCharts" color="primary" :loading="loading" />
      </el-col>
    </el-row>
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="居住类型分布">
          <v-chart v-if="!loading && addressTypeOption" :option="addressTypeOption" style="height:350px" autoresize />
          <el-empty v-else-if="!loading" description="暂无数据" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="月度流动趋势">
          <TrendChart :data="trendData" height="350px" :loading="loading" series-label="登记数" color="#409eff" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { floatingApi } from '@/api/floating'
import StatCard from '@/components/StatCard.vue'
import TrendChart from '@/components/charts/TrendChart.vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, TooltipComponent, LegendComponent, CanvasRenderer])

const loading = ref(false)
const addressTypeOption = ref<any>(null)

const stats = reactive({
  residenceCount: 0, registerCount: 0, areaCount: 0, currentMonth: 0,
})

const trendData = reactive<{ dates: string[]; values: number[] }>({
  dates: [], values: [],
})

onMounted(async () => {
  loading.value = true
  try {
    const [heatmapRes, trendRes]: any[] = await Promise.all([
      floatingApi.heatmap(),
      floatingApi.trend(),
    ])

    // Heatmap: aggregate by addressType
    if (Array.isArray(heatmapRes) && heatmapRes.length) {
      stats.residenceCount = heatmapRes.length
      const areaIds = new Set(heatmapRes.map((h: any) => h.areaId).filter(Boolean))
      stats.areaCount = areaIds.size

      const typeCounts: Record<string, number> = {}
      heatmapRes.forEach((h: any) => {
        const t = h.addressType || '未知'
        typeCounts[t] = (typeCounts[t] || 0) + 1
      })
      addressTypeOption.value = {
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        series: [{
          type: 'pie',
          radius: '60%',
          data: Object.entries(typeCounts).map(([name, value]) => ({ name, value })),
        }],
      }
    }

    // Trend: aggregate by month
    if (Array.isArray(trendRes) && trendRes.length) {
      stats.registerCount = trendRes.length
      const monthCounts: Record<string, number> = {}
      const now = new Date()
      const thisMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
      trendRes.forEach((t: any) => {
        const date = t.registerDate || ''
        const month = date.substring(0, 7)
        if (month) {
          monthCounts[month] = (monthCounts[month] || 0) + 1
          if (month === thisMonth) stats.currentMonth++
        }
      })
      const sorted = Object.entries(monthCounts).sort()
      trendData.dates = sorted.map(([k]) => k)
      trendData.values = sorted.map(([, v]) => v)
    }
  } catch { /* ignore */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.page-header { margin-bottom: 16px; }
.page-header h3 { margin: 0; }
</style>
