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
        <el-tag v-if="identities.keyPerson" type="danger" size="small" effect="dark" style="cursor:pointer"
          @click="$router.push('/keyperson/list')">
          重点人员: {{ identities.keyPerson.controlLevel }}
        </el-tag>
        <el-tag v-if="identities.missingPerson" type="warning" size="small" effect="dark" style="cursor:pointer"
          @click="$router.push('/missing/list')">
          失踪: {{ identities.missingPerson.status }}
        </el-tag>
        <el-tag v-if="identities.police" type="primary" size="small" effect="dark" style="cursor:pointer"
          @click="$router.push('/system/police')">
          警员: {{ identities.police.policeNumber }} {{ identities.police.policeRank || '' }}
        </el-tag>
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
      <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
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

            <el-divider content-position="left">同户成员</el-divider>
            <el-table v-if="householdBook.members && householdBook.members.length > 0"
              :data="householdBook.members" stripe size="small">
              <el-table-column prop="name" label="姓名" width="120" />
              <el-table-column label="UUID" min-width="200" show-overflow-tooltip>
                <template #default="{ row }">
                  <el-button text size="small" type="primary" @click="$router.push(`/resident/${row.uuid}`)">{{ row.uuid }}</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-else description="暂未登记同户成员" />
          </div>
          <el-empty v-else description="该居民暂未关联户口簿" />
        </el-tab-pane>

        <!-- Tab 3: 家庭关系（树形） -->
        <el-tab-pane label="家庭关系" name="relations">
          <div style="margin-bottom:12px;display:flex;align-items:center;gap:8px">
            <span style="font-size:13px;color:#606266">显示代际：</span>
            <el-radio-group v-model="maxGenerations" size="small" @change="loadExtendedRelations">
              <el-radio-button :value="1">1代</el-radio-button>
              <el-radio-button :value="2">2代</el-radio-button>
              <el-radio-button :value="3">3代</el-radio-button>
            </el-radio-group>
          </div>
          <FamilyTree
            :person="{ name: resident!.name || '', gender: resident!.gender || '', uuid: resident!.uuid || '' }"
            :father="relations.fatherUuid ? { uuid: relations.fatherUuid, name: relations.fatherName || '父亲' } : null"
            :mother="relations.motherUuid ? { uuid: relations.motherUuid, name: relations.motherName || '母亲' } : null"
            :spouse="relations.spouseUuid ? { uuid: relations.spouseUuid, name: relations.spouseName || '配偶' } : null"
            :children="relations.children || []"
            :max-generations="maxGenerations"
            :paternal-grandfather="extendedRelations.paternalGrandfather"
            :paternal-grandmother="extendedRelations.paternalGrandmother"
            :maternal-grandfather="extendedRelations.maternalGrandfather"
            :maternal-grandmother="extendedRelations.maternalGrandmother"
            :grandchildren="extendedRelations.grandchildren"
            height="450px"
          />
        </el-tab-pane>

        <!-- Tab 4: 迁移轨迹（左侧列表 + 右侧地图） -->
        <el-tab-pane label="迁移轨迹" name="migration">
          <div v-if="migrationRoutes.length > 0" class="split-panel">
            <div class="split-left">
              <el-timeline>
                <el-timeline-item
                  v-for="(m, i) in migrationRoutes" :key="i"
                  :timestamp="m.date" placement="top"
                  :type="migrationTimelineType(m.status)"
                  :class="{ 'trace-hovered': hoveredMigrationIdx === i }"
                  @mouseenter="hoveredMigrationIdx = i"
                  @mouseleave="hoveredMigrationIdx = null"
                >
                  <el-card shadow="hover" size="small" class="trace-card">
                    <div class="trace-route">
                      <span class="trace-from">{{ m.fromAddress }}</span>
                      <el-icon><Right /></el-icon>
                      <span class="trace-to">{{ m.toAddress }}</span>
                    </div>
                    <div class="trace-meta">
                      <el-tag size="small">{{ m.type }}</el-tag>
                      <ApprovalBadge :status="m.status" />
                    </div>
                    <el-button text size="small" type="primary" style="margin-top:4px"
                      @click="$router.push(`/household/migration?fromAddress=${encodeURIComponent(m.fromAddress)}&toAddress=${encodeURIComponent(m.toAddress)}`)">
                      查看迁移记录
                    </el-button>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </div>
            <div class="split-right">
              <MigrationRouteMap :routes="migrationRoutes" height="100%"
                :hovered-index="hoveredMigrationIdx"
                @hover="hoveredMigrationIdx = $event"
                @click="onMigrationRouteClick" />
            </div>
          </div>
          <el-empty v-else description="暂无迁移记录" />
        </el-tab-pane>

        <!-- Tab 5: 持有证件 -->
        <el-tab-pane label="持有证件" name="permits">
          <div v-if="permits.all.length > 0" class="permits-gallery">
            <PermitCard
              v-for="(p, i) in permits.all" :key="i"
              :permit-type="p.permitType"
              :permit-no="p.permitNo"
              :issue-date="p.issueDate"
              :expiry-date="p.expiryDate"
              :status="p.status"
              :holder="resident?.name"
              :authority="p.issuingAuthority || p.outgoingPoliceStation || ''"
              @click="navigateToPermit(p._type)"
            />
          </div>
          <el-empty v-else description="暂未持有证件" />
        </el-tab-pane>

        <!-- Tab 6: 流动记录（时间线+地图，与迁移轨迹布局一致） -->
        <el-tab-pane label="流动记录" name="floating">
          <div v-if="floatingRoutes.length > 0" class="split-panel">
            <div class="split-left">
              <el-timeline>
                <el-timeline-item
                  v-for="(r, i) in floatingRoutes" :key="i"
                  :timestamp="r.date" placement="top"
                  type="primary"
                  :class="{ 'trace-hovered': hoveredFloatingIdx === i }"
                  @mouseenter="hoveredFloatingIdx = i"
                  @mouseleave="hoveredFloatingIdx = null"
                >
                  <el-card shadow="hover" size="small" class="trace-card">
                    <div class="trace-route">
                      <span class="trace-from">{{ r.fromAddress || '初始登记' }}</span>
                      <el-icon v-if="r.fromAddress"><Right /></el-icon>
                      <span class="trace-to">{{ r.toAddress }}</span>
                    </div>
                    <div class="trace-meta">
                      <el-tag size="small">{{ r.addressType }}</el-tag>
                      <el-tag size="small" type="warning">{{ r.purpose }}</el-tag>
                      <span v-if="r.workUnit" style="font-size:12px;color:#909399">{{ r.workUnit }}</span>
                    </div>
                    <el-button text size="small" type="primary" style="margin-top:4px"
                      @click="$router.push(`/floating/residence?keyword=${encodeURIComponent(r.toAddress)}`)">
                      查看居住地记录
                    </el-button>
                  </el-card>
                </el-timeline-item>
              </el-timeline>
            </div>
            <div class="split-right">
              <MigrationRouteMap :routes="floatingRoutes" height="100%"
                :hovered-index="hoveredFloatingIdx"
                @hover="hoveredFloatingIdx = $event"
                @click="onFloatingRouteClick" />
            </div>
          </div>
          <el-empty v-else description="暂无流动记录" />
        </el-tab-pane>

        <!-- Tab 7: 信访记录 -->
        <el-tab-pane label="信访记录" name="petition">
          <div v-if="petitionRecords.length > 0">
            <el-table :data="petitionRecords" stripe size="small">
              <el-table-column prop="petitionTime" label="信访时间" width="170" />
              <el-table-column prop="address" label="信访地点" min-width="150" />
              <el-table-column prop="evaluation" label="评估" width="80" />
              <el-table-column prop="remark" label="备注" min-width="200" />
              <el-table-column prop="handlerPoliceNo" label="处理民警" width="140" />
              <el-table-column label="操作" width="100">
                <template #default>
                  <el-button text size="small" type="primary"
                    @click="$router.push(`/keyperson/petition?uuid=${uuid}`)">
                    查看全部
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else description="暂无信访记录" />
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- 编辑抽屉 -->
    <ResidentDetail ref="detailRef" @saved="onEdited" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, Right } from '@element-plus/icons-vue'
import { residentApi } from '@/api/resident'
import { householdApi } from '@/api/household'
import { keypersonApi } from '@/api/keyperson'
import { missingApi } from '@/api/missing'
import { policeApi } from '@/api/auth'
import { floatingApi } from '@/api/floating'
import { usePermission } from '@/composables/usePermission'
import { showError } from '@/utils/auth'
import type { Resident } from '@/types/resident'
import ResidentDetail from './ResidentDetail.vue'
import FamilyTree from '@/components/charts/FamilyTree.vue'
import MigrationRouteMap from '@/components/charts/MigrationRouteMap.vue'
import PermitCard from '@/components/PermitCard.vue'
import ApprovalBadge from '@/components/ApprovalBadge.vue'

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
const maxGenerations = ref(2)
const extendedRelations = reactive({
  paternalGrandfather: null as { uuid: string; name: string } | null,
  paternalGrandmother: null as { uuid: string; name: string } | null,
  maternalGrandfather: null as { uuid: string; name: string } | null,
  maternalGrandmother: null as { uuid: string; name: string } | null,
  grandchildren: [] as { uuid: string; name: string; gender: string; parentName: string }[],
})
const floatingRecords = ref<any[]>([])
const hoveredMigrationIdx = ref<number | null>(null)
const hoveredFloatingIdx = ref<number | null>(null)

// 将流动记录转换为迁移轨迹格式（每条记录：原地址 → 现地址）
const floatingRoutes = computed(() => {
  const sorted = [...floatingRecords.value].sort((a, b) => (a.registerDate || '').localeCompare(b.registerDate || ''))
  return sorted.map((r) => ({
    fromAddress: r.originalAddress || '',
    toAddress: r.currentAddress || '',
    date: r.registerDate || '',
    type: '居住登记',
    status: '有效',
    addressType: r.addressType || '',
    purpose: r.purpose || '',
    workUnit: r.workUnit || '',
  }))
})
const petitionRecords = ref<any[]>([])
const permits = reactive({ all: [] as any[] })

const identities = reactive<Record<string, any>>({
  keyPerson: null,
  missingPerson: null,
  police: null,
})

const uuid = computed(() => route.params.uuid as string)

async function loadIdentities(residentUuid: string) {
  try {
    const [kpRes, missRes, policeRes] = await Promise.all([
      keypersonApi.search({ keyword: residentUuid, page: 1, size: 1 }, { silent: true }).catch(() => null),
      missingApi.search({ residentUuid, page: 1, size: 1 }, { silent: true }).catch(() => null),
      policeApi.list({ residentUuid, page: 1, size: 1 }, { silent: true }).catch(() => null),
    ])
    identities.keyPerson = kpRes?.records?.[0] || null
    identities.missingPerson = missRes?.records?.[0] || null
    identities.police = policeRes?.records?.[0] || null
  } catch { /* ignore */ }
}

async function loadPermits() {
  try {
    const [approvalRes, migrationRes, fpPermitRes] = await Promise.all([
      householdApi.listApprovalPermit({ keyword: uuid.value, page: 1, size: 50 }, { silent: true }).catch(() => ({ records: [] })),
      householdApi.listMigrationPermit({ keyword: uuid.value, page: 1, size: 50 }, { silent: true }).catch(() => ({ records: [] })),
      floatingApi.listPermit({ keyword: uuid.value, page: 1, size: 50 }, { silent: true }).catch(() => ({ records: [] })),
    ])
    const approvalPermits = (approvalRes.records || []).map((p: any) => ({ ...p, permitType: '准迁证', _type: 'approval', _uuid: p.uuid || uuid.value }))
    const migrationPermits = (migrationRes.records || []).map((p: any) => ({ ...p, permitType: '迁移证', _type: 'migration', _uuid: p.uuid || uuid.value }))
    const fpPermits = (fpPermitRes.records || []).map((p: any) => ({ ...p, permitType: '居住证', _type: 'fp', _uuid: p.uuid || uuid.value }))
    permits.all = [...approvalPermits, ...migrationPermits, ...fpPermits]
  } catch { /* ignore */ }
}

async function loadExtendedRelations() {
  extendedRelations.paternalGrandfather = null
  extendedRelations.paternalGrandmother = null
  extendedRelations.maternalGrandfather = null
  extendedRelations.maternalGrandmother = null
  extendedRelations.grandchildren = []
  const parentUuids = [relations.fatherUuid, relations.motherUuid].filter(Boolean)
  const childUuids = (relations.children || []).map((c: any) => c.uuid).filter(Boolean)

  try {
    // 获取祖父母（父母的父母）
    const parentRelationsList = await Promise.all(
      parentUuids.map((u: string) => residentApi.getRelationsDetail(u, { silent: true }).catch(() => null))
    )
    for (let i = 0; i < parentRelationsList.length; i++) {
      const rd = parentRelationsList[i]
      if (!rd) continue
      const isFather = parentUuids[i] === relations.fatherUuid
      if (isFather) {
        if (rd.fatherUuid) extendedRelations.paternalGrandfather = { uuid: rd.fatherUuid, name: rd.fatherName || '祖父' }
        if (rd.motherUuid) extendedRelations.paternalGrandmother = { uuid: rd.motherUuid, name: rd.motherName || '祖母' }
      } else {
        if (rd.fatherUuid) extendedRelations.maternalGrandfather = { uuid: rd.fatherUuid, name: rd.fatherName || '外祖父' }
        if (rd.motherUuid) extendedRelations.maternalGrandmother = { uuid: rd.motherUuid, name: rd.motherName || '外祖母' }
      }
    }

    // 获取孙子女（子女的子女）
    const childRelationsList = await Promise.all(
      childUuids.map((u: string) => residentApi.getRelationsDetail(u, { silent: true }).catch(() => null))
    )
    for (let i = 0; i < childRelationsList.length; i++) {
      const cr = childRelationsList[i]
      if (!cr || !cr.children) continue
      for (const gc of cr.children) {
        extendedRelations.grandchildren.push({
          uuid: gc.uuid,
          name: gc.name || '孙',
          gender: gc.gender || '',
          parentName: (relations.children || [])[i]?.name || '',
        })
      }
    }
  } catch { /* ignore */ }
}

function navigateToPermit(type: string) {
  if (type === 'fp') {
    router.push(`/floating/permit?keyword=${uuid.value}`)
  } else {
    router.push(`/household/permit?keyword=${uuid.value}`)
  }
}

async function loadAll() {
  if (!uuid.value) return
  loading.value = true
  error.value = ''

  try {
    const [residentData, householdData, migrations, relationsData] = await Promise.all([
      residentApi.getByUuid(uuid.value, { silent: true }).catch(() => null),
      householdApi.getBookByResident(uuid.value, { silent: true }).catch(() => null),
      householdApi.getMigrationTrace(uuid.value, { silent: true }).catch(() => []),
      residentApi.getRelationsDetail(uuid.value, { silent: true }).catch(() => null),
    ])

    if (!residentData) {
      error.value = '户籍人员不存在'
      loading.value = false
      return
    }
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

  await loadIdentities(uuid.value)

  // 支持 ?tab=xxx 参数自动切换到指定标签，切换后清理URL
  const tabParam = route.query.tab as string
  if (tabParam) {
    activeTab.value = tabParam
    router.replace({ query: {} })
  }
}

async function loadTabData(tab: string) {
  if (tab === 'relations' && maxGenerations.value >= 2 && !extendedRelations.paternalGrandfather && !extendedRelations.maternalGrandfather && extendedRelations.grandchildren.length === 0) {
    await loadExtendedRelations()
  }
  if (tab === 'permits' && permits.all.length === 0) await loadPermits()
  if (tab === 'floating' && floatingRecords.value.length === 0) {
    try {
      const res = await floatingApi.listResidence({ keyword: uuid.value, page: 1, size: 50 }, { silent: true })
      floatingRecords.value = res?.records || []
    } catch { /* ignore */ }
  }
  if (tab === 'petition' && petitionRecords.value.length === 0) {
    try {
      const res = await keypersonApi.listPetition({ keyPersonUuid: uuid.value, page: 1, size: 50 })
      petitionRecords.value = res?.records || []
    } catch { /* ignore */ }
  }
}

function onTabChange(tab: any) {
  loadTabData(tab as string)
}

function openEdit() {
  if (resident.value) detailRef.value?.open(resident.value)
}

function onEdited() { loadAll() }

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

/** 迁移轨迹 timeline 节点颜色：驳回=红，通过=绿，中间=蓝 */
function migrationTimelineType(status?: string): 'danger' | 'success' | 'primary' {
  if (!status) return 'primary'
  if (status.includes('驳回')) return 'danger'
  if (status.includes('通过')) return 'success'
  return 'primary'
}

function onMigrationRouteClick(idx: number) {
  const m = migrationRoutes.value[idx]
  if (m) {
    router.push(`/household/migration?fromAddress=${encodeURIComponent(m.fromAddress)}&toAddress=${encodeURIComponent(m.toAddress)}`)
  }
}

function onFloatingRouteClick(idx: number) {
  const r = floatingRoutes.value[idx]
  if (r) {
    router.push(`/floating/residence?keyword=${encodeURIComponent(r.toAddress)}`)
  }
}

onMounted(loadAll)

// 点击家庭关系成员跳转时，路由参数变化需重新加载
watch(() => route.params.uuid, (newUuid) => {
  if (newUuid && newUuid !== resident.value?.uuid) {
    Object.assign(relations, { fatherUuid: null, fatherName: null, motherUuid: null, motherName: null, spouseUuid: null, spouseName: null, children: [] })
    extendedRelations.paternalGrandfather = null; extendedRelations.paternalGrandmother = null
    extendedRelations.maternalGrandfather = null; extendedRelations.maternalGrandmother = null
    extendedRelations.grandchildren = []
    floatingRecords.value = []
    petitionRecords.value = []
    permits.all = []
    activeTab.value = (route.query.tab as string) || 'basic'
    loadAll()
  }
})
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
.split-panel { display: flex; gap: 16px; height: calc(100vh - 200px); }
.split-left { flex: 1; overflow-y: auto; padding-right: 8px; }
.split-right { flex: 1; height: 100%; min-height: 450px; }
.trace-card { margin-bottom: 4px; }
.trace-route { display: flex; align-items: center; gap: 8px; font-size: 13px; margin-bottom: 6px; }
.trace-from, .trace-to { color: #303133; }
.permits-gallery {
  display: flex; flex-wrap: wrap; gap: 16px;
  padding: 8px 0; justify-content: flex-start;
}
.trace-meta { display: flex; gap: 8px; align-items: center; }
.split-left :deep(.el-timeline-item.trace-hovered) .el-timeline-item__node {
  animation: pulse-node 0.6s ease-in-out infinite alternate;
}
.split-left :deep(.el-timeline-item.trace-hovered) .el-card {
  box-shadow: 0 2px 12px rgba(64,158,255,0.3) !important;
  border-color: #409EFF;
}
@keyframes pulse-node {
  from { transform: scale(1); }
  to { transform: scale(1.3); }
}
</style>
