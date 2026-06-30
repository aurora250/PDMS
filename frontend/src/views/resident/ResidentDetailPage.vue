<template>
  <div class="resident-detail-page" v-loading="loading">
    <!-- 顶部摘要栏 -->
    <div class="summary-bar">
      <div class="summary-left">
        <el-button text @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon> 返回列表
        </el-button>
        <h3 v-if="resident">{{ resident.name }}</h3>
        <el-tag v-if="resident" :type="resident.gender === '男' ? 'primary' : 'danger'" size="small">
          {{ resident.gender }}
        </el-tag>
        <span v-if="resident" class="id-card">{{ resident.idCardNo }}</span>
      </div>
      <div class="summary-meta" v-if="resident">
        <span>民族: {{ resident.nation }}</span>
        <el-divider direction="vertical" />
        <span>学历: {{ resident.educationLevel }}</span>
        <el-divider direction="vertical" />
        <el-tag :type="householdStatusType(resident.householdStatus)" size="small">
          {{ resident.householdStatus }}
        </el-tag>
      </div>
      <div class="summary-actions" v-if="resident">
        <el-button v-if="hasPermission('resident:write')" type="primary" size="small" @click="openEdit">
          编辑
        </el-button>
      </div>
    </div>

    <!-- 错误状态 -->
    <el-result v-if="error" icon="error" title="加载失败" :sub-title="error">
      <template #extra><el-button type="primary" @click="loadAll">重新加载</el-button></template>
    </el-result>

    <!-- 详情Tabs -->
    <template v-if="resident && !error">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- Tab 1: 基本信息 -->
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="姓名">{{ resident.name }}</el-descriptions-item>
            <el-descriptions-item label="曾用名">{{ resident.formerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ resident.gender }}</el-descriptions-item>
            <el-descriptions-item label="身份证号">{{ resident.idCardNo }}</el-descriptions-item>
            <el-descriptions-item label="民族">{{ resident.nation }}</el-descriptions-item>
            <el-descriptions-item label="出生日期">{{ resident.birthDate }}</el-descriptions-item>
            <el-descriptions-item label="学历">{{ resident.educationLevel }}</el-descriptions-item>
            <el-descriptions-item label="血型">{{ resident.bloodType || '-' }}</el-descriptions-item>
            <el-descriptions-item label="婚姻状况">{{ resident.maritalStatus }}</el-descriptions-item>
            <el-descriptions-item label="职业">{{ resident.occupation || '-' }}</el-descriptions-item>
            <el-descriptions-item label="电话">{{ resident.phone }}</el-descriptions-item>
            <el-descriptions-item label="户口类型">{{ resident.householdType }}</el-descriptions-item>
            <el-descriptions-item label="户口状态">
              <el-tag :type="householdStatusType(resident.householdStatus)" size="small">
                {{ resident.householdStatus }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="居住地址" :span="2">{{ resident.residence }}</el-descriptions-item>
            <el-descriptions-item label="户籍地址" :span="2">{{ resident.householdAddress }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- Tab 2: 户籍信息 -->
        <el-tab-pane label="户籍信息" name="household">
          <div v-if="householdBook" class="household-section">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="户口簿号">{{ householdBook.householdBookNo }}</el-descriptions-item>
              <el-descriptions-item label="户主姓名">{{ householdBook.householderName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="立户日期">{{ householdBook.establishDate }}</el-descriptions-item>
              <el-descriptions-item label="户口簿状态">
                <el-tag :type="bookStatusType(householdBook.status)" size="small">{{ householdBook.status }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="户籍地址" :span="2">{{ householdBook.hukouAddress }}</el-descriptions-item>
            </el-descriptions>

            <!-- 同户成员 -->
            <el-divider content-position="left">同户成员</el-divider>
            <el-table v-if="householdBook.members && householdBook.members.length > 0"
              :data="householdBook.members" stripe size="small">
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column prop="uuid" label="UUID" min-width="200" show-overflow-tooltip />
            </el-table>
            <el-empty v-else description="暂未登记同户成员" />
          </div>
          <el-empty v-else description="该居民暂未关联户口簿">
            <template #extra>
              <span style="font-size:12px;color:#909399">请先在户口簿管理中为该居民建立户口簿</span>
            </template>
          </el-empty>
        </el-tab-pane>

        <!-- Tab 3: 迁移轨迹 -->
        <el-tab-pane label="迁移轨迹" name="migration">
          <MigrationRouteMap :routes="migrationRoutes" height="450px" />
        </el-tab-pane>

        <!-- Tab 4: 家庭关系 -->
        <el-tab-pane label="家庭关系" name="relations">
          <FamilyGraph
            :person="{ name: resident.name, gender: resident.gender, uuid: resident.uuid }"
            :father="relations.fatherUuid ? { uuid: relations.fatherUuid, name: relations.fatherName } : null"
            :mother="relations.motherUuid ? { uuid: relations.motherUuid, name: relations.motherName } : null"
            :spouse="relations.spouseUuid ? { uuid: relations.spouseUuid, name: relations.spouseName } : null"
            :children="relations.children || []"
            height="450px"
          />
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 编辑抽屉 (复用ResidentDetail) -->
    <ResidentDetail ref="detailRef" @saved="onEdited" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { residentApi } from '@/api/resident'
import { householdApi } from '@/api/household'
import { usePermission } from '@/composables/usePermission'
import { showError } from '@/utils/auth'
import type { Resident } from '@/types/resident'
import ResidentDetail from './ResidentDetail.vue'
import FamilyGraph from '@/components/charts/FamilyGraph.vue'
import MigrationRouteMap from '@/components/charts/MigrationRouteMap.vue'

const route = useRoute()
const router = useRouter()
const { hasPermission } = usePermission()

const loading = ref(false)
const error = ref('')
const activeTab = ref('basic')
const detailRef = ref()

const resident = ref<Resident | null>(null)
const householdBook = ref<any>(null)
const migrationRoutes = ref<any[]>([])
const relations = reactive<any>({
  fatherUuid: null, fatherName: null,
  motherUuid: null, motherName: null,
  spouseUuid: null, spouseName: null,
  children: [],
})

const uuid = route.params.uuid as string

async function loadAll() {
  if (!uuid) return
  loading.value = true
  error.value = ''

  try {
    const [residentData, householdData, migrations, relationsData] = await Promise.all([
      residentApi.getByUuid(uuid).catch(() => null),
      householdApi.getBookByResident(uuid).catch(() => null),
      householdApi.getMigrationTrace(uuid).catch(() => []),
      residentApi.getRelationsDetail(uuid).catch(() => null),
    ])

    resident.value = residentData
    householdBook.value = householdData
    migrationRoutes.value = Array.isArray(migrations) ? migrations.map((m: any) => ({
      fromAddress: m.outgoingAddress || '',
      toAddress: m.incomingAddress || '',
      date: m.handleDate || m.createTime || '',
      type: m.businessType || '户籍迁移',
      status: m.status || '',
    })) : []

    if (relationsData) {
      Object.assign(relations, {
        fatherUuid: relationsData.fatherUuid,
        fatherName: relationsData.fatherName,
        motherUuid: relationsData.motherUuid,
        motherName: relationsData.motherName,
        spouseUuid: relationsData.spouseUuid,
        spouseName: relationsData.spouseName,
        children: relationsData.children || [],
      })
    }
  } catch (e: any) {
    error.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function openEdit() {
  if (resident.value) {
    detailRef.value?.open(resident.value)
  }
}

function onEdited() {
  // 重新加载居民数据（编辑保存后）
  loadAll()
}

function householdStatusType(status: string): string {
  const map: Record<string, string> = {
    '正常': 'success', '死亡注销': 'danger', '失踪注销': 'warning', '迁出注销': 'info',
  }
  return map[status] || 'info'
}

function bookStatusType(status: string): string {
  const map: Record<string, string> = {
    '有效': 'success', '审批中': 'warning', '冻结': 'info', '无效': 'danger',
  }
  return map[status] || 'info'
}

onMounted(loadAll)
</script>

<style scoped>
.resident-detail-page { padding: 0; }
.summary-bar {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; background: #fff; border-radius: 8px;
  margin-bottom: 16px; box-shadow: 0 1px 4px rgba(0,0,0,.06);
}
.summary-left { display: flex; align-items: center; gap: 12px; }
.summary-left h3 { margin: 0; font-size: 18px; }
.summary-left .id-card { font-size: 13px; color: #909399; font-family: monospace; }
.summary-meta { display: flex; align-items: center; gap: 4px; font-size: 13px; color: #606266; }
.summary-actions { display: flex; gap: 8px; }
.household-section { margin-top: 0; }
</style>
