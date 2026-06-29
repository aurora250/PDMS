<template>
  <div class="trend-chart" v-loading="loading">
    <v-chart v-if="!loading" :option="chartOption" :style="{ height }" autoresize />
    <el-empty v-else-if="!loading && isEmpty" description="暂无数据" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { TooltipComponent, GridComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, TooltipComponent, GridComponent, LegendComponent, CanvasRenderer])

const props = withDefaults(defineProps<{
  data: { dates: string[]; values: number[] }
  title?: string
  height?: string
  loading?: boolean
  color?: string
  seriesLabel?: string
}>(), {
  height: '400px',
  loading: false,
  seriesLabel: '数量',
  color: '#409eff',
})

const isEmpty = computed(() => !props.data.values.length)

const chartOption = computed(() => ({
  title: props.title ? { text: props.title, left: 'center' } : undefined,
  tooltip: { trigger: 'axis' },
  xAxis: {
    type: 'category',
    data: props.data.dates,
    axisLabel: { rotate: 30 },
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: props.seriesLabel,
      type: 'line',
      data: props.data.values,
      smooth: true,
      itemStyle: { color: props.color },
      areaStyle: { color: props.color + '20' },
    },
  ],
}))
</script>
