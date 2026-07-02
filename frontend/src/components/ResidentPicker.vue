<template>
  <el-select
    v-model="selectedUuid"
    filterable
    remote
    reserve-keyword
    clearable
    :placeholder="placeholder"
    :remote-method="remoteSearch"
    :loading="loading"
    value-key="uuid"
    style="width:100%"
    @change="onChange"
  >
    <el-option
      v-for="r in options"
      :key="r.uuid"
      :label="`${r.name} - ${r.idCardNo}`"
      :value="r.uuid"
    >
      <div class="picker-option">
        <span class="picker-name">{{ r.name }}</span>
        <span class="picker-id">{{ r.idCardNo }}</span>
        <span class="picker-gender" :style="{ color: r.gender === '男' ? '#409EFF' : '#E6A23C' }">{{ r.gender }}</span>
      </div>
    </el-option>
  </el-select>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { residentApi } from '@/api/resident'
import { policeApi } from '@/api/auth'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    placeholder?: string
    nonPoliceOnly?: boolean
    gender?: string
  }>(),
  { placeholder: '输入姓名或身份证号搜索居民', nonPoliceOnly: false }
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'pick': [resident: { uuid: string; name: string; idCardNo: string; gender: string }]
}>()

const selectedUuid = ref(props.modelValue || '')
const options = ref<Array<{ uuid: string; name: string; idCardNo: string; gender: string }>>([])
const loading = ref(false)
let timer: ReturnType<typeof setTimeout> | null = null

/** 缓存民警 UUID 列表，避免每次搜索都请求 */
let policeUuidsCache: string[] | null = null
async function getPoliceUuids(): Promise<string[]> {
  if (policeUuidsCache !== null) return policeUuidsCache
  try {
    policeUuidsCache = await policeApi.getAllResidentUuids({ silent: true } as any)
  } catch {
    policeUuidsCache = []
  }
  return policeUuidsCache
}

/** 清除缓存（可在需要刷新时调用） */
function clearPoliceUuidsCache() {
  policeUuidsCache = null
}

watch(() => props.modelValue, (v) => { selectedUuid.value = v || '' })

async function remoteSearch(query: string) {
  if (!query || query.trim().length < 1) { options.value = []; return }
  if (timer) clearTimeout(timer)
  timer = setTimeout(async () => {
    loading.value = true
    try {
      const res = await residentApi.search({ page: 1, size: 10, name: query.trim(), gender: props.gender || undefined })
      const data = Array.isArray(res) ? res : (res.records || [])
      let mapped = (data as any[]).map((r: any) => ({
        uuid: r.uuid || r.userUuid || '',
        name: r.name || '未知',
        idCardNo: r.idCardNo || r.id_card_no || '',
        gender: r.gender || '',
      }))
      // 排除民警身份的居民
      if (props.nonPoliceOnly) {
        const excludeSet = new Set(await getPoliceUuids())
        mapped = mapped.filter(r => !excludeSet.has(r.uuid))
      }
      options.value = mapped
    } catch { options.value = [] }
    finally { loading.value = false }
  }, 300)
}

function onChange(uuid: string) {
  emit('update:modelValue', uuid || '')
  const found = options.value.find(r => r.uuid === uuid)
  if (found) emit('pick', found)
}
</script>

<style scoped>
.picker-option { display: flex; gap: 12px; align-items: center; }
.picker-name { font-weight: 600; min-width: 60px; }
.picker-id { color: #909399; font-size: 13px; }
.picker-gender { font-size: 12px; margin-left: auto; }
</style>
