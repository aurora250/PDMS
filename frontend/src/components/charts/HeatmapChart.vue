<template>
  <div class="heatmap-chart" v-loading="loading">
    <v-chart v-if="!loading" :option="chartOption" :style="{ height }" autoresize />
    <el-empty v-else-if="!loading && isEmpty" description="暂无数据" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { HeatmapChart } from 'echarts/charts'
import { TooltipComponent, VisualMapComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([HeatmapChart, TooltipComponent, VisualMapComponent, GridComponent, CanvasRenderer])

const props = withDefaults(defineProps<{
  data: { dates: string[]; regions: string[]; values: number[][] }
  height?: string
  loading?: boolean
}>(), {
  height: '400px',
  loading: false,
})

const isEmpty = computed(() => !props.data.values.length)

const chartOption = computed(() => ({
  tooltip: {
    formatter: (params: any) =>
      `${params.name}<br/>日期: ${params.value[0]}<br/>区域: ${params.value[1]}<br/>数量: ${params.value[2]}`,
  },
  visualMap: {
    min: 0,
    max: Math.max(...(props.data.values.flat()) || [1]),
    calculable: true,
    orient: 'horizontal',
    left: 'center',
    bottom: 0,
    inRange: { color: ['#50a3ba', '#eac736', '#d94e5d'] },
  },
  grid: { left: 80, right: 20, top: 20, bottom: 60 },
  xAxis: {
    type: 'category',
    data: props.data.dates,
    splitArea: { show: true },
    axisLabel: { rotate: 45 },
  },
  yAxis: {
    type: 'category',
    data: props.data.regions,
    splitArea: { show: true },
  },
  series: [
    {
      type: 'heatmap',
      data: props.data.values,
      label: { show: false },
      emphasis: {
        itemStyle: { shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.5)' },
      },
    },
  ],
}))
</script>
