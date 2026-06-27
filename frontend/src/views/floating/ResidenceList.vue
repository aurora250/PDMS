<template>
  <div>
    <div class="page-header"><h3>居住地管理</h3><el-button v-if="hasPermission('fp:residence:write')" type="primary" @click="dialogVisible=true">新增登记</el-button></div>
    <el-card>
      <el-table :data="list" stripe>
        <el-table-column prop="uuid" label="UUID" width="200" /><el-table-column prop="currentAddress" label="现地址" />
        <el-table-column prop="purpose" label="目的" /><el-table-column prop="registerDate" label="登记日期" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { floatingApi } from '@/api/floating'
import { usePermission } from '@/composables/usePermission'
const { hasPermission } = usePermission()
const list = ref<any[]>([])
const dialogVisible = ref(false)
onMounted(async () => { try { list.value = await floatingApi.listResidence() } catch { /* */ } })
</script>
