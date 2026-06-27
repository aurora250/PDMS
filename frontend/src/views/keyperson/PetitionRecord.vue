<template>
  <div>
    <div class="page-header"><h3>信访记录</h3><el-button v-if="hasPermission('keyperson:petition:write')" type="primary">登记信访</el-button></div>
    <el-card><el-table :data="list" stripe>
      <el-table-column prop="keyPersonUuid" label="人员UUID" width="200" /><el-table-column prop="petitionDate" label="日期" /><el-table-column prop="content" label="内容" />
    </el-table></el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { keypersonApi } from '@/api/keyperson'
import { usePermission } from '@/composables/usePermission'
const { hasPermission } = usePermission()
const list = ref<any[]>([])
onMounted(async () => { try { list.value = await keypersonApi.listPetition() } catch { /* */ } })
</script>
