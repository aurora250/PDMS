<template>
  <div>
    <div class="page-header"><h3>迁移管理</h3></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="rid" label="ID" width="60" />
        <el-table-column prop="uuid" label="人员UUID" width="200" />
        <el-table-column prop="fromAddress" label="原地址" />
        <el-table-column prop="toAddress" label="迁入地址" />
        <el-table-column prop="status" label="状态" width="120" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button text size="small" @click="$router.push(`/household/migration/${row.uuid}/trace`)">轨迹</el-button>
            <el-button v-if="hasPermission('household:approve')" text size="small" type="success" @click="approve(row, '通过')">通过</el-button>
            <el-button v-if="hasPermission('household:second-approve')" text size="small" type="primary" @click="approve(row, '通过')">二审通过</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { householdApi } from '@/api/household'
import { usePermission } from '@/composables/usePermission'
import { showSuccess } from '@/utils/auth'
const { hasPermission } = usePermission()
const list = ref<any[]>([])
async function load() { try { list.value = await householdApi.listMigration() } catch { /* */ } }
async function approve(row: any, status: string) { try { await householdApi.approveMigration(row.rid, status); showSuccess('已' + status); load() } catch { /* */ } }
onMounted(load)
</script>
