<template>
  <div>
    <div class="page-header"><h3>登录日志</h3></div>
    <el-card><el-table :data="list" stripe>
      <el-table-column prop="username" label="用户名" /><el-table-column prop="ip" label="IP" /><el-table-column prop="loginTime" label="登录时间" /><el-table-column prop="result" label="结果" />
    </el-table></el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { logApi } from '@/api/log'
const list = ref<any[]>([])
onMounted(async () => { try { list.value = await logApi.login() } catch { /* */ } })
</script>
