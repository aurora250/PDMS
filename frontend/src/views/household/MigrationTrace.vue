<template>
  <div>
    <div class="page-header">
      <h3>迁移轨迹 — {{ uuid }}</h3>
      <el-button @click="$router.back()">返回</el-button>
    </div>
    <el-card v-loading="loading">
      <el-empty v-if="!loading && !traces.length" description="暂无迁移记录" />
      <el-timeline v-else>
        <el-timeline-item
          v-for="(t, i) in traces"
          :key="i"
          :timestamp="t.handleDate || t.requestTime || ''"
          :color="statusColor(t.status)"
          placement="top"
        >
          <el-card shadow="hover">
            <div class="trace-item">
              <div class="trace-status">
                <ApprovalBadge :status="t.status || '未知'" />
              </div>
              <div class="trace-address">
                <p><el-icon><LocationFilled /></el-icon> <strong>迁出：</strong>{{ t.outgoingAddress || '未知' }}</p>
                <p><el-icon><LocationFilled /></el-icon> <strong>迁入：</strong>{{ t.incomingAddress || '未知' }}</p>
              </div>
              <div class="trace-meta" v-if="t.businessType || t.handleBasis">
                <el-tag size="small" v-if="t.businessType">{{ t.businessType }}</el-tag>
                <span v-if="t.handleBasis" style="margin-left:8px;color:#909399">{{ t.handleBasis }}</span>
              </div>
              <div v-if="t.rejectReason" class="trace-reason">
                <el-text type="danger">驳回原因：{{ t.rejectReason }}</el-text>
              </div>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { householdApi } from '@/api/household'
import ApprovalBadge from '@/components/ApprovalBadge.vue'

const route = useRoute()
const uuid = route.params.uuid as string
const traces = ref<any[]>([])
const loading = ref(false)

function statusColor(status: string): string {
  if (!status) return '#909399'
  if (status.includes('通过') || status.includes('批准')) return '#67c23a'
  if (status.includes('驳回')) return '#f56c6c'
  if (status.includes('审批')) return '#e6a23c'
  return '#409eff'
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await householdApi.getMigrationTrace(uuid)
    traces.value = Array.isArray(res) ? res : [res].filter(Boolean)
  } catch { /* ignore */ }
  finally { loading.value = false }
})
</script>

<style scoped>
.page-header { margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }
.page-header h3 { margin: 0; }
.trace-item { display: flex; flex-direction: column; gap: 8px; }
.trace-address p { margin: 4px 0; display: flex; align-items: center; gap: 4px; }
.trace-meta { display: flex; align-items: center; }
.trace-reason { margin-top: 4px; }
</style>
