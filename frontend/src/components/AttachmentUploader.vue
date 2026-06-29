<template>
  <div class="attachment-uploader">
    <el-upload
      :headers="headers"
      :action="uploadUrl"
      :on-success="handleSuccess"
      :on-error="handleError"
      :before-upload="beforeUpload"
      :file-list="fileList"
      list-type="text"
      :on-remove="handleRemove"
    >
      <el-button type="primary" :disabled="disabled">
        <el-icon><Upload /></el-icon>
        {{ buttonText }}
      </el-button>
      <template #tip>
        <div class="upload-tip">{{ tip }}</div>
      </template>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { showError, showSuccess } from '@/utils/auth'

const props = withDefaults(defineProps<{
  modelValue?: string[]
  uploadUrl?: string
  buttonText?: string
  tip?: string
  disabled?: boolean
}>(), {
  modelValue: () => [],
  uploadUrl: '/api/file/upload',
  buttonText: '上传附件',
  tip: '支持 jpg、png、pdf 格式，单文件不超过 10MB',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const auth = useAuthStore()
const headers = computed(() => ({
  Authorization: `Bearer ${auth.token}`,
}))

const fileList = computed(() =>
  props.modelValue.map((url, i) => ({
    name: `附件${i + 1}`,
    url,
  }))
)

function beforeUpload(file: File) {
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    showError('文件大小不能超过 10MB')
    return false
  }
  return true
}

function handleSuccess(response: any) {
  const url = response?.data?.url || response?.url || ''
  if (url) {
    const newList = [...props.modelValue, url]
    emit('update:modelValue', newList)
    showSuccess('上传成功')
  }
}

function handleError() {
  showError('上传失败')
}

function handleRemove(_file: any) {
  const newList = props.modelValue.filter((u) => u !== _file.url)
  emit('update:modelValue', newList)
}
</script>

<style scoped>
.upload-tip { color: #909399; font-size: 12px; margin-top: 4px; }
</style>
