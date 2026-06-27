<template>
  <div>
    <h3>我的申请</h3>
    <el-tabs v-model="tab">
      <el-tab-pane label="户籍业务" name="household" />
      <el-tab-pane label="居住证" name="permit" />
    </el-tabs>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="businessType" label="类型" />
      <el-table-column prop="status" label="状态" />
      <el-table-column prop="createTime" label="时间" />
    </el-table>
    <el-empty v-if="!loading && list.length===0" description="暂无申请" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { householdApi } from '@/api/household'
import { floatingApi } from '@/api/floating'

const tab = ref('household')
const list = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = tab.value === 'household'
      ? await householdApi.listBusiness()
      : await floatingApi.listPermit()
  } catch { list.value = [] }
  finally { loading.value = false }
}

watch(tab, load)
onMounted(load)
</script>
