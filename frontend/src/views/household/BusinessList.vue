<template>
  <div>
    <div class="page-header"><h3>户籍业务</h3></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="rid" label="ID" width="60" />
        <el-table-column prop="businessType" label="业务类型" />
        <el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="hasPermission('household:approve') && row.status !== '已批准'" text size="small" type="success" @click="approve(row, '通过')">通过</el-button>
            <el-button v-if="hasPermission('household:approve') && row.status !== '已驳回'" text size="small" type="danger" @click="approve(row, '驳回')">驳回</el-button>
            <el-button v-if="hasPermission('household:second-approve') && row.status === '一审'" text size="small" type="primary" @click="approve(row, '通过')">二审通过</el-button>
            <el-button v-if="hasPermission('household:material:attach') && row.status === '待受理'" text size="small" @click="attach(row)">附加材料</el-button>
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
async function load() { try { list.value = await householdApi.listBusiness() } catch { /* */ } }
async function approve(row: any, status: string) { try { await householdApi.approveBusiness(row.rid, status); showSuccess('已' + status); load() } catch { /* */ } }
function attach(row: any) { /* upload attachment */ }
onMounted(load)
</script>
