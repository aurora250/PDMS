<template>
  <div>
    <div class="page-header"><h3>流动人口登记</h3><el-button v-if="hasPermission('fp:write')" type="primary" @click="dialogVisible=true">新增登记</el-button></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="rid" label="ID" width="60" /><el-table-column prop="uuid" label="UUID" width="200" />
        <el-table-column prop="registerDate" label="登记日期" /><el-table-column prop="residencePermitNo" label="居住证号" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button v-if="hasPermission('fp:write')" text size="small" type="primary" @click="edit(row)">编辑</el-button>
            <el-button v-if="hasPermission('fp:delete')" text size="small" type="danger" @click="del(row)">注销</el-button>
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
const dialogVisible = ref(false)
async function load() { try { list.value = await floatingApi.listRegister() } catch { /* */ } }
async function del(row: any) { try { await floatingApi.deleteRegister(row.rid); showSuccess('已注销'); load() } catch { /* */ } }
function edit(_row: any) { /* */ }
onMounted(load)
</script>
