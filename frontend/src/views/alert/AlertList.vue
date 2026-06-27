<template>
  <div>
    <div class="page-header"><h3>预警中心</h3></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="alertType" label="预警类型" /><el-table-column prop="message" label="内容" /><el-table-column prop="createdAt" label="时间" /><el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="hasPermission('alert:handle') && row.status === '待处理'" text size="small" type="primary" @click="handle(row)">处理</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { alertApi } from '@/api/alert'
import { useAuthStore } from '@/stores/auth'
import { usePermission } from '@/composables/usePermission'
const auth = useAuthStore()
const { hasPermission } = usePermission()
const list = ref<any[]>([])
async function handle(row: any) { try { await alertApi.handle(row.id, auth.username) } catch { /* */ } }
onMounted(async () => { try { list.value = await alertApi.pending() } catch { /* */ } })
</script>
