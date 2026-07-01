<template>
  <div class="permit-card" :class="cardClass" @click="$emit('click')">
    <div class="permit-card-header">
      <span class="permit-card-title">{{ title }}</span>
      <el-tag :type="statusType" size="small">{{ status }}</el-tag>
    </div>
    <div class="permit-card-body">
      <div class="permit-card-no">
        <span class="label">证件编号</span>
        <span class="value">{{ permitNo }}</span>
      </div>
      <div class="permit-card-dates">
        <div class="date-item">
          <span class="label">签发日期</span>
          <span class="value">{{ issueDate }}</span>
        </div>
        <div class="date-item">
          <span class="label">到期日期</span>
          <span class="value">{{ expiryDate }}</span>
        </div>
      </div>
      <div v-if="holder" class="permit-card-holder">
        <span class="label">持有人</span>
        <span class="value holder-name">{{ holder }}</span>
      </div>
    </div>
    <div class="permit-card-footer">
      <div class="hologram" />
      <span class="authority">{{ authority }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  permitType: string
  permitNo: string
  issueDate: string
  expiryDate: string
  status: string
  holder?: string
  authority?: string
}>()

defineEmits<{ click: [] }>()

const cardClass = computed(() => ({
  'permit-approval': props.permitType === '准迁证',
  'permit-migration': props.permitType === '迁移证',
  'permit-residence': props.permitType === '居住证',
}))

const title = computed(() => {
  if (props.permitType === '准迁证') return '准予迁入证明'
  if (props.permitType === '迁移证') return '户口迁移证件'
  return '居住证'
})

const statusType = computed(() => {
  const s = props.status
  if (s === '有效' || s === '已批准') return 'success'
  if (s === '审批中' || s === '申领') return 'warning'
  if (s === '作废' || s === '过期' || s === '注销') return 'danger'
  return 'info'
})
</script>

<style scoped>
.permit-card {
  width: 280px; border-radius: 10px; overflow: hidden;
  cursor: pointer; transition: all 0.25s;
  box-shadow: 0 2px 8px rgba(0,0,0,.08);
  flex-shrink: 0;
}
.permit-card:hover { transform: translateY(-4px); box-shadow: 0 6px 20px rgba(0,0,0,.12); }

.permit-approval { background: linear-gradient(135deg, #fff9e6, #fff3cd); border: 1px solid #f0d060; }
.permit-migration { background: linear-gradient(135deg, #e8f4fd, #d4e9fc); border: 1px solid #90c0e8; }
.permit-residence { background: linear-gradient(135deg, #eaf7ea, #d4edd4); border: 1px solid #90c890; }

.permit-card-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; border-bottom: 1px dashed rgba(0,0,0,.1);
}
.permit-card-title { font-weight: 700; font-size: 14px; color: #303133; }
.permit-card-body { padding: 12px 14px; }
.permit-card-no { margin-bottom: 10px; }
.permit-card-no .value { font-family: monospace; font-size: 15px; font-weight: 600; color: #303133; letter-spacing: 1px; }
.permit-card-dates { display: flex; gap: 24px; margin-bottom: 8px; }
.date-item { display: flex; flex-direction: column; }
.label { font-size: 11px; color: #909399; margin-bottom: 2px; }
.date-item .value { font-size: 13px; color: #606266; }
.permit-card-holder { margin-top: 4px; }
.holder-name { font-weight: 600; color: #409EFF; }
.permit-card-footer {
  display: flex; justify-content: space-between; align-items: center;
  padding: 8px 14px; background: rgba(255,255,255,.5);
  border-top: 1px solid rgba(0,0,0,.06);
}
.hologram {
  width: 32px; height: 32px; border-radius: 50%;
  background: conic-gradient(from 0deg, #c0c0c0, #e0e0e0, #fff, #c0c0c0);
  opacity: 0.5;
}
.authority { font-size: 11px; color: #909399; }
</style>
