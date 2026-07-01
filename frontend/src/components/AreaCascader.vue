<template>
  <el-cascader
    :model-value="selectedPath"
    @update:model-value="onPathChange"
    :options="treeOptions"
    :props="cascaderProps"
    :placeholder="placeholder"
    clearable
    filterable
    style="width: 100%"
  />
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { areaApi } from '@/api/area'

interface AreaNode {
  value: number
  label: string
  leaf?: boolean
  children?: AreaNode[]
}

const props = withDefaults(
  defineProps<{
    modelValue?: number | null
    placeholder?: string
  }>(),
  {
    modelValue: null,
    placeholder: '请选择省市区',
  }
)

const emit = defineEmits<{
  'update:modelValue': [value: number | null]
}>()

const treeOptions = ref<AreaNode[]>([])
const selectedPath = ref<number[]>([])
let resolving = false
let pendingAreaId: number | null = null  // 树加载前挂起的初始值

/** 扁平 area 列表 → 级联树 */
function buildTree(flatList: any[]): AreaNode[] {
  // 按 area_level 分组构建索引
  const byCode = new Map<string, any>()
  const roots: AreaNode[] = []
  const childrenMap = new Map<string, AreaNode[]>()

  // 第一遍：创建所有节点
  for (const a of flatList) {
    const code = String(a.areaCode || '').trim()
    const node: AreaNode = {
      value: a.areaId,
      label: a.areaName,
      leaf: a.areaLevel === '区县',
    }
    byCode.set(code, { ...a, _node: node })

    // 按 parent_id（area_code）分组
    const pCode = a.parentId != null ? String(a.parentId) : '__root__'
    if (!childrenMap.has(pCode)) childrenMap.set(pCode, [])
    childrenMap.get(pCode)!.push(node)
  }

  // 第二遍：构建层级关系
  for (const a of flatList) {
    const code = String(a.areaCode || '').trim()
    const node = byCode.get(code)?._node as AreaNode
    if (!node) continue

    const children = childrenMap.get(code)
    if (children && children.length > 0) {
      node.children = children
      node.leaf = false
    } else if (!node.leaf) {
      // 没有子节点的非区县级节点也标记为叶子，避免显示"no data"
      node.leaf = true
    }
  }

  // 收集根节点（parent_id 为 NULL 的记录）
  const rootChildren = childrenMap.get('__root__') || []
  return rootChildren
}

onMounted(async () => {
  try {
    const all: any[] = await areaApi.tree()
    if (all && all.length > 0) {
      treeOptions.value = buildTree(all)
      // 树加载完成后，解析之前挂起的初始值
      if (pendingAreaId != null) {
        await resolveAndSet(pendingAreaId)
        pendingAreaId = null
      }
    }
  } catch {
    // fallback: empty tree
  }
})

const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  emitPath: true,
  checkStrictly: true,  // 允许选择任意级别（省/市/区），不强制选到叶子
}

function onPathChange(path: number[]) {
  if (resolving) return
  selectedPath.value = path || []
  const areaId = path && path.length > 0 ? path[path.length - 1] : null
  emit('update:modelValue', areaId ?? null)
}

async function resolveAndSet(areaId: number) {
  resolving = true
  try {
    const ancestors: any[] = await areaApi.getAncestors(areaId)
    if (ancestors && ancestors.length > 0) {
      selectedPath.value = ancestors.map((a: any) => a.areaId)
    } else {
      selectedPath.value = [areaId]
    }
  } catch {
    selectedPath.value = [areaId]
  }
  resolving = false
}

/** 外部 modelValue 变化 → 解析并显示完整路径 */
watch(
  () => props.modelValue,
  async (newVal) => {
    if (newVal == null) {
      selectedPath.value = []
      pendingAreaId = null
      return
    }
    if (treeOptions.value.length === 0) {
      pendingAreaId = newVal  // 树还没加载完，挂起等待
      return
    }
    await resolveAndSet(newVal)
  },
  { immediate: true }
)
</script>
