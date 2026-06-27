<template>
  <div>
    <div class="page-header"><h3>失踪人口管理</h3><el-button v-if="hasPermission('missing:write')" type="primary" @click="dialogVisible=true">登记失踪</el-button></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="name" label="姓名" /><el-table-column prop="gender" label="性别" /><el-table-column prop="missingDate" label="失踪日期" /><el-table-column prop="missingAddress" label="失踪地址" /><el-table-column prop="status" label="状态" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="hasPermission('missing:recovery:write')" text size="small" type="success" @click="recover(row)">寻回</el-button>
            <el-button v-if="hasPermission('missing:delete')" text size="small" type="danger" @click="del(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { missingApi } from '@/api/missing'
import { usePermission } from '@/composables/usePermission'
import { showSuccess } from '@/utils/auth'
const { hasPermission } = usePermission()
const list = ref<any[]>([])
const dialogVisible = ref(false)
async function load() { try { list.value = await missingApi.search() } catch { /* */ } }
async function del(row: any) { try { await missingApi.delete(row.rid ?? row.id); showSuccess('已撤销'); load() } catch { /* */ } }
async function recover(_row: any) { /* */ }
onMounted(load)
</script>
