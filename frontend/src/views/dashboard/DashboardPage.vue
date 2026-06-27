<template>
  <div class="dashboard">
    <h3>仪表盘</h3>
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover"><el-statistic title="常住人口" :value="stats.resident" /></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><el-statistic title="流动人口" :value="stats.floating" /></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover"><el-statistic title="重点人员" :value="stats.keyperson" /></el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <el-statistic title="待处理预警" :value="stats.alerts">
            <template #suffix>
              <el-tag v-if="stats.alerts > 0" type="danger" size="small">{{ stats.alerts }}条</el-tag>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="12">
        <el-card>
          <template #header>流动人口趋势</template>
          <v-chart :option="trendOption" style="height:300px" autoresize />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>近期预警</template>
          <el-empty v-if="alertList.length === 0" description="暂无预警，系统运行正常" />
          <el-timeline v-else>
            <el-timeline-item v-for="a in alertList" :key="a.id" :timestamp="a.createdAt" :type="a.level === '严重' ? 'danger' : 'warning'">
              {{ a.message }}
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { floatingApi } from '@/api/floating'
import { alertApi } from '@/api/alert'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

const stats = reactive({ resident: 0, floating: 0, keyperson: 0, alerts: 0 })
const alertList = ref<any[]>([])

const trendOption = ref({
  tooltip: { trigger: 'axis' },
  legend: { data: ['流动人口'] },
  xAxis: { type: 'category', data: [] as string[] },
  yAxis: { type: 'value' },
  series: [{ name: '流动人口', type: 'line', data: [] as number[], smooth: true }],
})

onMounted(async () => {
  try {
    const trend = await floatingApi.trend()
    if (trend) {
      trendOption.value.xAxis.data = trend.dates || []
      trendOption.value.series[0].data = trend.values || []
    }
  } catch { /* 数据暂不可用，显示空图表 */ }

  try {
    const pending = await alertApi.pending()
    if (Array.isArray(pending)) {
      alertList.value = pending.slice(0, 5)
      stats.alerts = pending.length
    }
  } catch { /* */ }
})
</script>
