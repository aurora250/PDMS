<template>
  <el-tag :type="tagType" :size="size" :effect="effect">{{ status }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  status: string
  size?: '' | 'small' | 'large'
  effect?: 'light' | 'dark' | 'plain'
}>(), {
  size: 'small',
  effect: 'light',
})

const tagType = computed(() => {
  const s = props.status
  if (/通过|批准|已制发|已续期|已处理|有效|成功|正常|寻回/.test(s)) return 'success'
  if (/驳回|拒绝|失效|过期|异常|失败|注销|撤销/.test(s)) return 'danger'
  if (/待|审批中|申领|受理中|请求|待走访|待处理|迁移审批/.test(s)) return 'warning'
  if (/一审|二审|审核/.test(s)) return 'info'
  if (/失踪/.test(s)) return 'danger'
  return 'info'
})
</script>
