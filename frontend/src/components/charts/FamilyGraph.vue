<template>
  <div class="family-graph-container" :style="{ height: height }">
    <v-chart v-if="option" :option="option" autoresize />
    <el-empty v-else description="暂未录入家庭关系" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { GraphChart } from 'echarts/charts'
import { TooltipComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([GraphChart, TooltipComponent, TitleComponent, CanvasRenderer])

const props = withDefaults(defineProps<{
  person: { uuid?: string; name: string; gender: string }
  father?: { uuid?: string; name?: string } | null
  mother?: { uuid?: string; name?: string } | null
  spouse?: { uuid?: string; name?: string } | null
  children?: Array<{ uuid?: string; name?: string; gender?: string }>
  height?: string
}>(), {
  height: '400px',
  children: () => [],
})

const option = computed(() => {
  const nodes: any[] = []
  const links: any[] = []
  const hasData = props.father || props.mother || props.spouse || (props.children && props.children.length > 0)

  if (!hasData) return null

  // 中心节点 — 本人
  nodes.push({
    name: props.person.name,
    symbolSize: 40,
    itemStyle: { color: '#409EFF' },
    label: { fontSize: 14, fontWeight: 'bold' },
    category: 0,
  })

  // 父亲
  if (props.father?.name) {
    nodes.push({
      name: props.father.name,
      symbolSize: 30,
      itemStyle: { color: '#F56C6C' },
      category: 1,
    })
    links.push({ source: props.person.name, target: props.father.name, label: { show: true, formatter: '父' } })
  }

  // 母亲
  if (props.mother?.name) {
    nodes.push({
      name: props.mother.name,
      symbolSize: 30,
      itemStyle: { color: '#E6A23C' },
      category: 2,
    })
    links.push({ source: props.person.name, target: props.mother.name, label: { show: true, formatter: '母' } })
  }

  // 配偶
  if (props.spouse?.name) {
    nodes.push({
      name: props.spouse.name,
      symbolSize: 30,
      itemStyle: { color: '#F56C6C' },
      category: 3,
    })
    links.push({ source: props.person.name, target: props.spouse.name, label: { show: true, formatter: '配偶' }, lineStyle: { type: 'dashed' } })
  }

  // 子女
  if (props.children && props.children.length > 0) {
    for (const child of props.children) {
      nodes.push({
        name: child.name,
        symbolSize: 25,
        itemStyle: { color: '#67C23A' },
        category: 4,
      })
      links.push({ source: props.person.name, target: child.name, label: { show: true, formatter: '子女' } })
    }
  }

  return {
    tooltip: {
      trigger: 'item',
      formatter: (p: any) => {
        if (p.dataType === 'node') return `${p.name}`
        return `${p.data.source} → ${p.data.target}`
      },
    },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      force: { repulsion: 200, edgeLength: [100, 200] },
      data: nodes,
      links: links,
      label: { show: true, fontSize: 12 },
      categories: [
        { name: '本人' },
        { name: '父亲' },
        { name: '母亲' },
        { name: '配偶' },
        { name: '子女' },
      ],
    }],
  }
})
</script>

<style scoped>
.family-graph-container { width: 100%; }
</style>
