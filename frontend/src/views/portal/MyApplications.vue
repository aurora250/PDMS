<template>
  <div>
    <h3>我的申请</h3>
    <el-tabs v-model="tab" @tab-change="load">
      <el-tab-pane label="户籍业务" name="household" />
      <el-tab-pane label="居住证" name="permit" />
    </el-tabs>
    <el-card>
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="statusFilter" placeholder="全部" clearable @change="load">
            <el-option label="待受理" value="待受理" />
            <el-option label="审批中" value="审批中" />
            <el-option label="已批准" value="已批准" />
            <el-option label="已驳回" value="已驳回" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button @click="load">刷新</el-button></el-form-item>
      </el-form>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column prop="businessType" label="类型" width="120">
          <template #default="{ row }">{{ row.businessType || '居住证' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }"><ApprovalBadge :status="row.status || '未知'" /></template>
        </el-table-column>
        <el-table-column prop="handleDate" label="时间" width="120" />
        <el-table-column v-if="tab === 'permit'" prop="permitNo" label="居住证号" width="200" />
        <el-table-column v-if="tab === 'permit'" prop="expiryDate" label="到期日" width="120" />
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无申请" />
      <div style="margin-top:16px;text-align:right">
        <el-pagination v-model:current-page="page.current" v-model:page-size="page.size" :total="page.total"
          layout="total,sizes,prev,pager,next" :page-sizes="[10,20,50,100]" @current-change="load" @size-change="load" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { householdApi } from '@/api/household'
import { floatingApi } from '@/api/floating'
import ApprovalBadge from '@/components/ApprovalBadge.vue'

const tab = ref('household')
const list = ref<any[]>([])
const loading = ref(false)
const statusFilter = ref('')
const page = reactive({ current: 1, size: 20, total: 0 })

async function load() {
  loading.value = true
  try {
    let res
    if (tab.value === 'household') {
      res = await householdApi.listBusiness({
        status: statusFilter.value || undefined,
        page: page.current, size: page.size,
      }, { silent: true })
    } else {
      res = await floatingApi.listPermit({
        status: statusFilter.value || undefined,
        page: page.current, size: page.size,
      }, { silent: true })
    }
    list.value = Array.isArray(res) ? res : (res.records || [])
    page.total = res.total || 0
  } catch { list.value = [] }
  finally { loading.value = false }
}

watch(tab, () => { statusFilter.value = ''; load() })
onMounted(load)
</script>
