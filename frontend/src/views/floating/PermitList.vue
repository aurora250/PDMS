<template>
  <div>
    <div class="page-header"><h3>居住证管理</h3><el-button v-if="hasPermission('fp:write')" type="primary" @click="showApply=true">申领</el-button></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="permitNo" label="居住证号" /><el-table-column prop="uuid" label="UUID" width="200" />
        <el-table-column prop="issueDate" label="签发日" /><el-table-column prop="expiryDate" label="到期日" /><el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button v-if="hasPermission('fp:permit:approve') && row.status === '申领'" text size="small" type="success" @click="approve(row)">审批通过</el-button>
            <el-button v-if="hasPermission('fp:permit:issue') && row.status === '已批准'" text size="small" @click="issue(row)">制发</el-button>
            <el-button v-if="hasPermission('fp:write')" text size="small" type="primary" @click="renew(row)">续期</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { floatingApi } from '@/api/floating'
import { usePermission } from '@/composables/usePermission'
import { showSuccess } from '@/utils/auth'
const { hasPermission } = usePermission()
const list = ref<any[]>([])
const showApply = ref(false)
async function load() { try { list.value = await floatingApi.listPermit() } catch { /* */ } }
async function approve(row: any) { try { await floatingApi.approvePermit(row.id); showSuccess('已批准'); load() } catch { /* */ } }
async function issue(row: any) { try { await floatingApi.issuePermit(row.id); showSuccess('已制发'); load() } catch { /* */ } }
async function renew(row: any) { try { await floatingApi.renewPermit(row.id, {}); showSuccess('已续期'); load() } catch { /* */ } }
onMounted(load)
</script>
