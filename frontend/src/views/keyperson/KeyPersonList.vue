<template>
  <div>
    <div class="page-header"><h3>重点人员管理</h3><el-button v-if="hasPermission('keyperson:write')" type="primary" @click="dialogVisible=true">新增人员</el-button></div>
    <el-card>
      <el-form inline><el-form-item label="管控级别">
        <el-select v-model="filter.level" placeholder="全部" clearable @change="load"><el-option v-for="l in ['一级','二级','三级']" :key="l" :label="l" :value="l" /></el-select>
      </el-form-item></el-form>
      <el-table :data="list" stripe>
        <el-table-column prop="uuid" label="UUID" width="200" /><el-table-column prop="controlLevel" label="管控级别" />
        <el-table-column prop="controlType" label="管控类型" /><el-table-column prop="responsiblePoliceNo" label="责任民警" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button v-if="hasPermission('keyperson:write')" text size="small" type="primary" @click="edit(row)">编辑</el-button>
            <el-button v-if="hasPermission('keyperson:delete')" text size="small" type="danger" @click="del(row)">撤销</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { keypersonApi } from '@/api/keyperson'
import { usePermission } from '@/composables/usePermission'
import { showSuccess } from '@/utils/auth'
const { hasPermission } = usePermission()
const list = ref<any[]>([])
const dialogVisible = ref(false)
const filter = reactive({ level: '' })
async function load() { try { list.value = await keypersonApi.search({ controlLevel: filter.level || undefined }) } catch { /* */ } }
async function del(row: any) { try { await keypersonApi.delete(row.uuid); showSuccess('已撤销'); load() } catch { /* */ } }
function edit(_row: any) { /* */ }
onMounted(load)
</script>
