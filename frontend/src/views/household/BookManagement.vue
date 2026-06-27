<template>
  <div>
    <div class="page-header">
      <h3>户口簿管理</h3>
      <el-button v-if="hasPermission('household:write')" type="primary" @click="showApply = true">申领户口簿</el-button>
    </div>
    <el-card>
      <el-form inline><el-form-item label="搜索"><el-input v-model="kw" placeholder="户口簿号/户主" clearable /></el-form-item>
        <el-form-item><el-button type="primary" @click="load">搜索</el-button></el-form-item>
      </el-form>
      <el-table :data="list" stripe><el-table-column prop="bookNo" label="户口簿号" /><el-table-column prop="address" label="地址" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button text size="small" @click="handleReissue(row)">补办</el-button>
            <el-button text size="small" @click="handleRenew(row)">换发</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { householdApi } from '@/api/household'
import { usePermission } from '@/composables/usePermission'
const { hasPermission } = usePermission()
const list = ref<any[]>([])
const kw = ref('')
const showApply = ref(false)
async function load() { try { list.value = await householdApi.searchBook({ kw: kw.value }) } catch { /* */ } }
async function handleReissue(row: any) { try { await householdApi.reissueBook({ bookNo: row.bookNo }) } catch { /* */ } }
async function handleRenew(row: any) { try { await householdApi.renewBook({ bookNo: row.bookNo }) } catch { /* */ } }
</script>
