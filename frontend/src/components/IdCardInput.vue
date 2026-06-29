<template>
  <el-input
    :model-value="modelValue"
    @update:model-value="$emit('update:modelValue', $event)"
    :placeholder="placeholder"
    maxlength="18"
    show-word-limit
    @blur="onBlur"
    clearable
  />
</template>

<script setup lang="ts">
const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
}>(), {
  placeholder: '请输入18位身份证号',
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'parsed': [data: { birthDate: string; gender: string }]
}>()

function onBlur() {
  const v = props.modelValue
  if (v && v.length === 18) {
    const d = v.substring(6, 14)
    const birthDate = `${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}`
    const gender = parseInt(v.charAt(16)) % 2 === 1 ? '男' : '女'
    emit('parsed', { birthDate, gender })
  }
}
</script>
