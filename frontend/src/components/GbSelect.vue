<template>
  <el-select
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :placeholder="placeholder"
    clearable
    @change="$emit('change', $event)"
  >
    <el-option
      v-for="item in options"
      :key="item.code"
      :label="item.name"
      :value="valueBy === 'name' ? item.name : item.code"
    />
  </el-select>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useGbConstants } from '@/composables/useGbConstants'

const props = withDefaults(defineProps<{
  dict: 'nation' | 'education' | 'maritalStatus' | 'bloodType'
  modelValue: string
  placeholder?: string
  valueBy?: 'name' | 'code'
}>(), {
  placeholder: '请选择',
  valueBy: 'name',
})

defineEmits<{
  'update:modelValue': [value: string]
  'change': [value: string]
}>()

const { NATIONS, EDUCATIONS, MARITAL_STATUSES, BLOOD_TYPES } = useGbConstants()

const options = computed(() => {
  switch (props.dict) {
    case 'nation': return NATIONS
    case 'education': return EDUCATIONS
    case 'maritalStatus': return MARITAL_STATUSES
    case 'bloodType': return BLOOD_TYPES.map((b: string, i: number) => ({ code: String(i), name: b }))
    default: return []
  }
})
</script>
