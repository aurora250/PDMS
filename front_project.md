# PDM 人口数据库管理系统 — 前端开发计划

## 一、权限组分析

### 1.1 权限组定义（`sql/init-schema.sql:585-619`）

| id | 组名 | 权限数 | 核心特征 |
|----|------|--------|---------|
| 1 | **系统管理员组** | `["*"]` | 通配符，全模块所有权限 |
| 2 | **民警组** | 16项 | 核心审批人：一审审批、居住证制发、常住人口CRUD |
| 3 | **采集员组** | 11项 | 一线采集：流动/重点/失踪数据录入+走访+信访，无审批 |
| 4 | **街道办组** | 7项 | 辅助角色：只读+上传附件，无审批/编辑/删除 |
| 5 | **数据审查员组** | 6项 | 全局独立：只读+review标记，不参与业务流程 |
| 6 | **市局负责人组** | 11项 | 全市查看+极少二审+仪表盘+预警处理 |
| 7 | **用户管理员组** | 3项 | 仅用户账号CRUD+状态管理 |
| 8 | **普通用户组** | 5项 | 群众自助：`self:*`限定操作自己数据 |

### 1.2 权限矩阵

```
模块\角色        | 系统管理员 | 民警 | 采集员 | 街道办 | 审查员 | 市局 | 用户管理员 | 群众
─────────────────┼───────────┼─────┼───────┼───────┼───────┼─────┼──────────┼─────
auth:user        |    RW     |  -  |   -   |   -   |   -   |  -  |    RW    |  -
auth:police      |    RW     |  RW |   -   |   -   |   -   |  R  |    -     |  -
auth:permission  |    RW     |  -  |   -   |   -   |   -   |  -  |    -     |  -
resident         |   RW+I+E+A1+A2|RW+I+E+A1| - |  R  |   -   | R+A2|    -     |self:R
household        |   RW+A1+A2+M |RW+A1|  -  |  R+M |   -   | R+A2|    -     |self:apply
fp               |  全部     |R+A(permit)|RWD|  R  | R+Review| R  |    -     |self:W
keyperson        |  全部     |  RW |RW+GIS+Visit| R | R+Review| R  |    -     |  -
missing          |  全部     |  RW |RW+Recovery| R | R+Review| R  |    -     |self:W
log              |  全部     |  -  |   -   |   -   |   -   |  -  |    -     |  -
alert            |    RH     |  RH |   -   |   R   |   -   |  RH |    -     |  -
statistics       |    R      |  -  |   -   |   -   |   -   |  R  |    -     |  R
```

**符号说明**：R=读, W=写, D=删除, A1=一审, A2=二审, M=附加材料, I=导入, E=导出

### 1.3 详细权限清单

```
系统管理员组 (id=1) ["*"]
  └─ 通配符，匹配所有权限字符串

民警组 (id=2)
  ├─ resident:read, resident:write, resident:import, resident:export
  ├─ resident:change-request:approve
  ├─ household:read, household:write, household:approve
  ├─ keyperson:read, keyperson:write
  ├─ missing:read, missing:write
  ├─ fp:read, fp:permit:approve, fp:permit:issue
  ├─ alert:read, alert:handle
  └─ police:read, police:write

采集员组 (id=3)
  ├─ fp:read, fp:write, fp:delete, fp:residence:write
  ├─ keyperson:read, keyperson:write, keyperson:gis:read
  ├─ keyperson:visit-plan:write, keyperson:petition:write
  └─ missing:read, missing:write, missing:recovery:write

街道办组 (id=4)
  ├─ resident:read
  ├─ household:read, household:material:attach
  ├─ fp:read, keyperson:read, missing:read
  └─ alert:read

数据审查员组 (id=5)
  ├─ fp:read, fp:review
  ├─ keyperson:read, keyperson:review
  └─ missing:read, missing:review

市局负责人组 (id=6)
  ├─ resident:read, resident:change-request:second-approve
  ├─ household:read, household:second-approve
  ├─ fp:read, keyperson:read, missing:read
  ├─ alert:read, alert:handle
  ├─ police:read
  └─ statistics:read

用户管理员组 (id=7)
  └─ auth:user:read, auth:user:write, auth:user:status

普通用户组 (id=8)
  ├─ self:resident:read, self:fp:write
  ├─ self:household:apply, self:missing:recovery:write
  └─ statistics:read
```

---

## 二、业务审批流程

```
普通流程:  采集员(数据采集) → 街道办(附加材料) → 民警(一审审批) → 市局(二审，极少)
群众自助:  群众 ──────────────────────────────→ 民警(一审审批) → 市局(二审，极少)
                                              跳过采集员+街道办
数据审查员: 独立于流程外，全局事后审查数据质量
```

### 各角色审批权

| 角色 | 审批权 | 说明 |
|------|--------|------|
| 群众 | 无 | 仅自助申报 |
| 采集员 | 无 | 仅数据采集 |
| 街道办 | **无** | 仅附加材料，不可通过/驳回 |
| 民警 | **一审（最终）** | 大部分业务的最终审批人 |
| 市局 | **二审（极少）** | 仅重要/疑难业务（跨省迁移等） |
| 数据审查员 | 无 | 独立审查标记，不参与流程 |
| 用户管理员 | 无 | 仅用户账号管理 |
| 系统管理员 | **全权** | 拥有一审+二审+所有权限 |

---

## 三、技术选型

| 层 | 选型 | 理由 |
|----|------|------|
| 框架 | **Vue 3.4** + Composition API + TypeScript | 响应式、类型安全 |
| 构建 | **Vite 5** | 秒级 HMR |
| UI | **Element Plus 2.x** | 中文文档完善，公安后台常用 |
| 图表 | **ECharts 5** + vue-echarts | 热力图、趋势图 |
| 地图 | **Leaflet** (OSM) | 重点人员 GIS，开源无 API Key |
| 状态 | **Pinia** | Vue 3 官方状态管理 |
| 路由 | **Vue Router 4** | 动态权限路由 |
| HTTP | **Axios** | 拦截器统一 Token/错误处理 |
| 表格 | **vxe-table** | 虚拟滚动，万级数据流畅 |
| CSS | **UnoCSS** | 原子化 CSS |

---

## 四、项目结构

```
frontend/
├── index.html
├── vite.config.ts
├── package.json
├── tsconfig.json
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── router/index.ts                  # 路由 + 权限守卫
│   ├── stores/
│   │   ├── auth.ts                      # Token/user/permissions
│   │   └── app.ts                       # 全局状态（侧栏折叠等）
│   ├── api/
│   │   ├── request.ts                   # Axios 实例（拦截器）
│   │   ├── auth.ts                      # 登录/用户/民警/权限组
│   │   ├── resident.ts
│   │   ├── household.ts
│   │   ├── keyperson.ts
│   │   ├── floating.ts
│   │   ├── missing.ts
│   │   ├── log.ts
│   │   ├── alert.ts
│   │   └── area.ts
│   ├── composables/
│   │   ├── useAuth.ts                   # 登录/登出/刷新Token
│   │   ├── usePermission.ts             # hasPermission('resident:write')
│   │   ├── useGbConstants.ts            # 国标常量（民族/学历/婚姻代码↔名称）
│   │   ├── usePagination.ts             # 分页状态封装
│   │   └── useExport.ts                 # 文件下载
│   ├── constants/
│   │   ├── nations.ts                   # 56民族 GB 3304-1991
│   │   ├── educations.ts                # 10种学历 GB/T 4658-2006
│   │   ├── maritalStatuses.ts           # 8种婚姻状况 GB/T 2261.2-2003
│   │   └── bloodTypes.ts               # 血型枚举
│   ├── utils/
│   │   ├── validate.ts                  # 身份证 GB 11643-1999 / 手机号 / 密码
│   │   └── format.ts                    # 日期/国标代码→名称格式化
│   ├── types/
│   │   ├── user.ts
│   │   ├── resident.ts
│   │   ├── household.ts
│   │   ├── keyperson.ts
│   │   ├── floating.ts
│   │   ├── missing.ts
│   │   └── common.ts
│   ├── views/
│   │   ├── login/                       # 登录 + 强制改密
│   │   │   ├── LoginPage.vue
│   │   │   └── ChangePasswordDialog.vue
│   │   ├── portal/                      # 群众门户
│   │   │   ├── PortalLayout.vue
│   │   │   ├── PortalHome.vue
│   │   │   ├── MyApplications.vue
│   │   │   ├── SelfRegisterFp.vue
│   │   │   └── SelfReportRecovery.vue
│   │   ├── dashboard/                   # 仪表盘
│   │   │   └── DashboardPage.vue
│   │   ├── resident/                    # 常住人口
│   │   │   ├── ResidentList.vue
│   │   │   ├── ResidentDetail.vue
│   │   │   ├── ResidentRelations.vue
│   │   │   ├── ChangeRequestList.vue
│   │   │   └── ChangeApproval.vue
│   │   ├── household/                   # 户籍管理
│   │   │   ├── BookManagement.vue
│   │   │   ├── BusinessList.vue
│   │   │   ├── BusinessApproval.vue
│   │   │   ├── MigrationList.vue
│   │   │   ├── MigrationApproval.vue
│   │   │   ├── MigrationTrace.vue
│   │   │   ├── PermitManagement.vue
│   │   │   └── SupplementaryMaterial.vue
│   │   ├── keyperson/                   # 重点人员
│   │   │   ├── KeyPersonList.vue
│   │   │   ├── VisitPlan.vue
│   │   │   ├── PetitionRecord.vue
│   │   │   └── KeyPersonGis.vue
│   │   ├── floating/                    # 流动人口
│   │   │   ├── FpRegisterList.vue
│   │   │   ├── ResidenceList.vue
│   │   │   ├── PermitList.vue
│   │   │   └── FpStatistics.vue
│   │   ├── missing/                     # 失踪人口
│   │   │   ├── MissingList.vue
│   │   │   └── MissingStatistics.vue
│   │   ├── log/                         # 日志审计
│   │   │   ├── AuditLog.vue
│   │   │   └── LoginLog.vue
│   │   ├── alert/                       # 预警中心
│   │   │   └── AlertList.vue
│   │   └── system/                      # 系统管理
│   │       ├── UserList.vue
│   │       ├── PoliceList.vue
│   │       └── PermissionGroup.vue
│   └── components/                      # 共用组件
│       ├── AppLayout.vue                # 主布局（侧栏+顶栏+内容）
│       ├── PortalLayout.vue             # 群众门户简化布局
│       ├── SearchForm.vue               # 通用搜索表单
│       ├── DataTable.vue                # 通用分页表格
│       ├── StatCard.vue                 # 统计卡片
│       ├── ApprovalFlow.vue             # 审批流组件
│       ├── ApprovalBadge.vue            # 审批状态标签
│       ├── AreaCascader.vue             # 省市区三级联动 (GB/T 2260-2007)
│       ├── IdCardInput.vue              # 身份证输入 (GB 11643-1999)
│       ├── GbSelect.vue                 # 国标代码下拉（民族/学历/婚姻联动）
│       ├── AttachmentUploader.vue       # 附件材料上传
│       ├── PermissionGuard.vue          # 权限包裹组件
│       └── charts/
│           ├── HeatmapChart.vue
│           ├── TrendChart.vue
│           └── GisMap.vue
└── public/
    └── favicon.ico
```

---

## 五、权限系统

### 5.1 登录流程

```
POST /api/auth/login {username, password}
→ LoginResponse { accessToken, refreshToken, role, permissions, mustChangePassword }
→ auth store { token, role, permissions: ["resident:read","fp:write",...] }
```

### 5.2 权限判断 (`composables/usePermission.ts`)

```typescript
export function usePermission() {
  const auth = useAuthStore()

  function hasPermission(perm: string): boolean {
    return auth.permissions.includes('*') || auth.permissions.includes(perm)
  }
  function hasAnyPermission(...perms: string[]): boolean {
    return perms.some(p => hasPermission(p))
  }

  const canEdit   = (m: string) => hasPermission(`${m}:write`)
  const canRead   = (m: string) => hasPermission(`${m}:read`)
  const canDelete = (m: string) => hasPermission(`${m}:delete`)
  const canApprove = () => hasPermission('household:approve')
  const canSecondApprove = () => hasPermission('household:second-approve')
  const canAttachMaterial = () => hasPermission('household:material:attach')
  const canReview = () => hasAnyPermission('fp:review','keyperson:review','missing:review')

  return { hasPermission, hasAnyPermission, canEdit, canRead, canDelete,
           canApprove, canSecondApprove, canAttachMaterial, canReview }
}
```

### 5.3 路由守卫

```typescript
router.beforeEach((to, from, next) => {
  const auth = useAuthStore()
  if (to.path !== '/login' && !auth.token) return next('/login')
  if (auth.mustChangePassword && to.path !== '/change-password') return next('/change-password')
  if (auth.role === '普通用户' && !to.path.startsWith('/portal')) return next('/portal')
  if (auth.role !== '普通用户' && to.path.startsWith('/portal')) return next('/dashboard')
  next()
})
```

### 5.4 动态菜单（权限驱动）

```typescript
export const MENU_ITEMS = [
  { path: '/dashboard',  title: '仪表盘',   icon: 'DataBoard', perm: 'statistics:read' },
  { path: '/resident',   title: '常住人口', icon: 'User',      perm: 'resident:read' },
  { path: '/household',  title: '户籍管理', icon: 'Notebook',  perm: 'household:read' },
  { path: '/floating',   title: '流动人口', icon: 'Ship',      perm: 'fp:read' },
  { path: '/keyperson',  title: '重点人员', icon: 'Warning',   perm: 'keyperson:read' },
  { path: '/missing',    title: '失踪人口', icon: 'Search',    perm: 'missing:read' },
  { path: '/alert',      title: '预警中心', icon: 'Bell',      perm: 'alert:read' },
  { path: '/log',        title: '日志审计', icon: 'Document',  perm: 'log:audit:read' },
  { path: '/system/users',       title: '用户管理', icon: 'Avatar',  perm: 'auth:user:read' },
  { path: '/system/police',      title: '民警管理', icon: 'Police',  perm: 'auth:police:read' },
  { path: '/system/permissions', title: '权限组',   icon: 'Lock',    perm: 'auth:permission:write' },
]
// 侧栏: MENU_ITEMS.filter(item => hasPermission(item.perm))
```

### 5.5 按钮级权限

```html
<el-button v-if="hasPermission('resident:write')" @click="edit">编辑</el-button>
<el-button v-if="hasPermission('household:approve')" type="success">一审通过</el-button>
<el-button v-if="hasPermission('household:second-approve')" type="primary">二审通过</el-button>
<el-upload v-if="hasPermission('household:material:attach')" />
```

---

## 六、国标代码前端方案

### 6.1 数据来源

后端资源文件 (`pdm-common-core/src/main/resources/`) → 前端 `src/constants/` 编译时内联：

| 常量文件 | 标准 | 条目数 |
|---------|------|--------|
| `nations.ts` | GB 3304-1991 民族代码 | 56民族 + 97(其他) + 98(外籍) |
| `educations.ts` | GB/T 4658-2006 学历代码 | 10种 (10~99) |
| `maritalStatuses.ts` | GB/T 2261.2-2003 婚姻状况 | 8种 (10~90) |
| `bloodTypes.ts` | - | A/B/AB/O/未知 |

### 6.2 GbSelect 联动

```html
<el-select v-model="form.nation" @change="onNationChange">
  <el-option v-for="n in NATIONS" :key="n.code" :label="n.name" :value="n.name" />
</el-select>
<!-- onNationChange: form.nationCode = nationCode(form.nation) -->
```

### 6.3 教育程度对照（旧值→国标）

| 旧值 | 国标值 (GB/T 4658-2006) | 代码 |
|------|------------------------|------|
| 博士研究生 | **研究生** | 10 |
| 硕士研究生 | **研究生** | 10 |
| 本科 | **大学本科** | 20 |
| 大专 | **大学专科** | 30 |
| 中专 | **中等职业教育** | 40 |
| - | **技工学校** (新增) | 50 |
| 高中 | **高中** | 60 |
| 初中 | **初中** | 70 |
| 小学及以下 | **小学** / **文盲或半文盲** | 80/90 |
| - | **未知** (新增) | 99 |

### 6.4 婚姻状况扩展 (GB/T 2261.2-2003)

| 旧值 | 新值 |
|------|------|
| 未婚 | 未婚 |
| 已婚 | 已婚 / 初婚 / 再婚 / 复婚 |
| 离异 | **离婚** |
| 丧偶 | 丧偶 |
| - | **未说明的婚姻状况** (新增) |

---

## 七、页面与 API 映射

### 0. 登录 (`/login`) — 任何用户
- `POST /api/auth/login` — 登录
- `PUT /api/auth/change-password` — 改密
- `POST /api/auth/refresh` — 刷新 Token

### 0b. 群众门户 (`/portal`) — `self:*`
- PortalHome: 我的申请概览
- MyApplications: 户籍业务/迁移/居住证申请列表
- SelfRegisterFp: `POST /api/fp/register` 流动人口自主申报
- SelfReportRecovery: `POST /api/missing/recovery` 寻回线索登记

### 1. 仪表盘 (`/dashboard`) — `statistics:read`
- `GET /api/fp/statistics/heatmap` / `/trend`
- `GET /api/missing/statistics`
- `GET /api/alert/pending`

### 2. 常住人口 (`/resident`) — 9 API

| 页面 | API | 权限 |
|------|-----|------|
| ResidentList | `POST /api/resident/search` | `resident:read` |
| ResidentList 导出 | `GET /api/resident/export` | `resident:export` |
| ResidentList 导入 | `POST /api/resident/import` | `resident:import` |
| ResidentDetail | `GET /api/resident/{uuid}` | `resident:read` |
| ResidentDetail 新增 | `POST /api/resident` | `resident:write` |
| ResidentDetail 编辑 | `PUT /api/resident/{uuid}` | `resident:write` |
| ResidentDetail 删除 | `DELETE /api/resident/{uuid}` | `resident:delete` |
| ResidentRelations | `GET/POST /api/resident/{uuid}/relations` | `resident:read`/`write` |
| ChangeRequestList | `GET /api/resident/change-request` | `resident:read` |
| ChangeApproval | `PUT /api/resident/change-request/{rid}/approve` | `resident:change-request:approve` |

### 3. 户籍管理 (`/household`) — 16 API

| 页面 | API | 权限 |
|------|-----|------|
| BookManagement | `GET /api/household/book/search` | `household:read` |
| | `POST /api/household/book/{apply,reissue,renew}` | `household:write` |
| BusinessList | `GET /api/household/business` | `household:read` |
| BusinessApproval | `POST/PUT /api/household/business` | `household:approve` |
| MigrationList | `GET /api/household/migration` | `household:read` |
| MigrationApproval | `POST/PUT /api/household/migration` | `household:approve` |
| MigrationTrace | `GET /api/household/migration/trace/{uuid}` | `household:read` |
| PermitList | `GET /api/household/{approval,migration}-permit` | `household:read` |
| PermitManagement | `POST /api/household/{approval,migration}-permit` | `household:write` |
| SupplementaryMaterial | 复用 BusinessList/MigrationList | `household:material:attach` |
| AreaCascader | `GET /api/area?parentId=` | 公开 |

### 4. 重点人员 (`/keyperson`) — 10 API

| 页面 | API | 权限 |
|------|-----|------|
| KeyPersonList | `GET /api/keyperson/search` | `keyperson:read` |
| | `POST /api/keyperson` | `keyperson:write` |
| | `PUT /api/keyperson/{uuid}` | `keyperson:write` |
| | `DELETE /api/keyperson/{uuid}` | `keyperson:delete` |
| VisitPlan | `GET /api/keyperson/visit-plan` | `keyperson:read` |
| | `POST/PUT /api/keyperson/visit-plan` | `keyperson:visit-plan:write` |
| PetitionRecord | `GET /api/keyperson/petition` | `keyperson:read` |
| | `POST /api/keyperson/petition` | `keyperson:petition:write` |
| KeyPersonGis | `GET /api/keyperson/gis` | `keyperson:gis:read` |

### 5. 流动人口 (`/floating`) — 15 API

| 页面 | API | 权限 |
|------|-----|------|
| FpRegisterList | `GET /api/fp/register` | `fp:read` |
| | `POST /api/fp/register` | `fp:write` |
| | `PUT /api/fp/register/{id}` | `fp:write` |
| | `DELETE /api/fp/register/{id}` | `fp:delete` |
| ResidenceList | `GET /api/fp/residence` | `fp:read` |
| | `POST/PUT/DELETE /api/fp/residence` | `fp:residence:write` |
| PermitList | `GET /api/fp/permit` | `fp:read` |
| | `POST /api/fp/permit/apply` `/renew` | `fp:write` |
| | `PUT /api/fp/permit/{id}/approve` | `fp:permit:approve` |
| | `PUT /api/fp/permit/{id}/issue` | `fp:permit:issue` |
| FpStatistics | `GET /api/fp/statistics/{heatmap,trend}` | `fp:read` |

### 6. 失踪人口 (`/missing`) — 5 API

| 页面 | API | 权限 |
|------|-----|------|
| MissingList | `GET /api/missing/search` | `missing:read` |
| | `POST /api/missing` | `missing:write` |
| | `DELETE /api/missing/{uuid}` | `missing:delete` |
| Recovery | `POST /api/missing/recovery` | `missing:recovery:write` |
| MissingStatistics | `GET /api/missing/statistics` | `missing:read` |

### 7. 日志审计 (`/log`) — 3 API

| 页面 | API | 权限 |
|------|-----|------|
| AuditLog | `GET /api/log/audit` | `log:audit:read` |
| LoginLog | `GET /api/log/login` | `log:login:read` |
| Export | `GET /api/log/export` | `log:export` |

### 8. 预警中心 (`/alert`) — 3 API

| 页面 | API | 权限 |
|------|-----|------|
| AlertList | `GET /api/alert/pending` | `alert:read` |
| | `GET /api/alert/search` | `alert:read` |
| | `PUT /api/alert/{id}/handle` | `alert:handle` |

### 9. 系统管理 (`/system`) — 13 API

| 页面 | API | 权限 |
|------|-----|------|
| UserList | `GET /api/auth/users` | `auth:user:read` |
| | `POST /api/auth/users` | `auth:user:write` |
| | `PUT /api/auth/users/{uuid}` | `auth:user:write` |
| | `PUT /api/auth/users/{uuid}/status` | `auth:user:status` |
| | `DELETE /api/auth/users/{uuid}` | `auth:user:write` |
| PoliceList | `GET /api/auth/police` | `auth:police:read` |
| | `POST /api/auth/police` | `auth:police:write` |
| | `PUT /api/auth/police/{no}` | `auth:police:write` |
| | `PUT /api/auth/police/{no}/status` | `auth:police:write` |
| PermissionGroup | `GET /api/auth/permission-groups` | `auth:permission:write` |
| | `POST/PUT/DELETE /api/auth/permission-groups/{id}` | `auth:permission:write` |

---

## 八、关键共用组件

| 组件 | 功能 | 关键依赖 |
|------|------|---------|
| `AppLayout.vue` | 侧栏（动态菜单）+ 顶栏（用户信息）+ 主内容区 | `usePermission()` |
| `PortalLayout.vue` | 群众门户简化布局，无管理菜单 | `role === '普通用户'` |
| `ApprovalFlow.vue` | 通用审批流：状态展示 + 通过/驳回/附加材料按钮 | `canApprove()`/`canAttachMaterial()` |
| `DataTable.vue` | 通用分页表格（vxe-table 虚拟滚动） | `usePagination()` |
| `SearchForm.vue` | 动态搜索表单，按模块配置字段 | - |
| `AreaCascader.vue` | 省市区三级联动（6位 GB/T 2260-2007） | `GET /api/area` |
| `IdCardInput.vue` | 身份证输入校验 (GB 11643-1999)，自动提取性别/出生日期 | `validate.ts` |
| `GbSelect.vue` | 国标下拉（民族/学历/婚姻），选中自动同步 code 字段 | `useGbConstants()` |
| `AttachmentUploader.vue` | 附件材料上传（街道办专用） | `household:material:attach` |
| `PermissionGuard.vue` | 包裹内容，无权限时自动隐藏 | `hasPermission()` |
| `HeatmapChart.vue` | ECharts 热力图 | - |
| `TrendChart.vue` | ECharts 趋势图 | - |
| `GisMap.vue` | Leaflet 地图（重点人员分布标记） | - |

---

## 九、Axios 拦截器

```typescript
const service = axios.create({ baseURL: '/api', timeout: 30000 })

service.interceptors.request.use(config => {
  const token = useAuthStore().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

service.interceptors.response.use(
  response => {
    const { code, message, data } = response.data
    if (code !== 200) { ElMessage.error(message); return Promise.reject(message) }
    return data
  },
  async error => {
    if (error.response?.status === 401) { /* refresh 或跳登录 */ }
    if (error.response?.status === 403) { ElMessage.error('权限不足') }
    if (error.response?.status >= 500) { ElMessage.error('服务器异常') }
    return Promise.reject(error)
  }
)
```

---

## 十、实施顺序

| 阶段 | 内容 | 页面数 | 预计工作量 |
|------|------|--------|-----------|
| **P0.1** | 项目脚手架：Vite+Vue3+TS+Pinia+Router+Axios+ElementPlus+UnoCSS+vxe-table+Leaflet+vue-echarts | - | 30min |
| **P0.2** | 国标常量：nations.ts/educations.ts/maritalStatuses.ts/bloodTypes.ts + useGbConstants | - | 15min |
| **P0.3** | 登录页 + 强制改密 + auth store + 路由守卫 + usePermission | 2 | 30min |
| **P0.4** | 主布局：AppLayout+PortalLayout+动态菜单 | 2 | 30min |
| **P0.5** | 常住人口：列表+GbSelect联动搜索+详情+编辑+关系+变更审批 | 5 | 2h |
| **P0.6** | 系统管理：用户CRUD+民警管理+权限组管理 | 3 | 1h |
| **P1.1** | 群众门户：PortalHome+我的申请+自主申报 | 4 | 1h |
| **P1.2** | 户籍管理：户口簿+业务+迁移+证件+AreaCascader+街道办附件 | 8 | 2.5h |
| **P1.3** | 流动人口：登记+居住地+居住证全流程+统计图表 | 4 | 1.5h |
| **P1.4** | 重点人员：列表+走访+信访+GIS地图 | 4 | 1.5h |
| **P2.1** | 失踪人口：列表+寻回+统计 | 2 | 30min |
| **P2.2** | 日志审计：审计日志+登录日志+导出 | 2 | 30min |
| **P2.3** | 预警中心：预警列表+处理 | 1 | 20min |
| **P2.4** | 仪表盘：统计卡片+ECharts趋势/热力+预警一览 | 1 | 30min |

**总计：~38 页面，预计 12-14 小时**

---

## 十一、验证方式

1. **国标常量验证**：民族/学历/婚姻下拉选择后，`nationCode`/`educationCode` 同步更新，提交后端 CHECK 约束不报错
2. **权限菜单验证**：分别登录 8 种角色，确认菜单项按权限矩阵显示/隐藏
3. **按钮权限验证**：各角色在各页面确认编辑/删除/审批/二审/附加材料按钮的正确可见性
4. **审批流验证**：采集员上报 → 街道办附件 → 民警一审通过/驳回 →（极少跨省→市局二审）
5. **群众自助流验证**：群众登录→自主申报→直达民警审核（跳过采集员+街道办）
6. **403 验证**：无权限页面 URL 直接访问 → 后端返回 403 → 前端提示"权限不足"
7. **分页验证**：所有列表页支持 page/size + 条件筛选（含 nationCode/educationCode）
8. **AreaCascader 验证**：省市区三级联动使用 6 位 GB/T 2260-2007 代码，匹配后端 CHAR(6)
