# 人口数据库管理系统 — Postman API 测试列表

> **统一网关入口:** `http://localhost:8080`  
> **通用响应格式:** `{ "code": 200, "message": "success", "data": ..., "timestamp": 1719123456789 }`  
> **分页响应格式:** `{ "code": 200, "data": { "records": [...], "total": 100, "page": 1, "size": 20, "totalPages": 5 } }`  
> **认证方式:** Bearer Token (`Authorization: Bearer <accessToken>`)  
> **⚠ 启动前:** 确保所有模块已重新编译 (`mvn compile -pl pdm-gateway -am`)

---

## 常见问题排查

| 现象 | 原因 | 解决 |
|------|------|------|
| `POST /api/auth/login` 返回 403 + "An expected CSRF token cannot be found" | Gateway 层 Spring Security 自动配置默认开启 CSRF | 已修复：`GatewaySecurityConfig.java` 禁用 CSRF。如果仍然出现，请 `mvn clean package -pl pdm-gateway -am` 重新构建后重启网关 |
| 其他 POST/PUT/DELETE 返回 403 | 同上 CSRF 问题 | 同上 |
| 返回 401 "未登录或Token已过期" | Token 过期或未携带 | 重新调用 `/api/auth/login` 获取新 Token |

---

## 目录

1. [pdm-auth — 认证与用户管理 (8081)](#1-pdm-auth--认证与用户管理-8081)
2. [pdm-resident — 常住人口管理 (8082)](#2-pdm-resident--常住人口管理-8082)
3. [pdm-household — 户籍管理 (8083)](#3-pdm-household--户籍管理-8083)
4. [pdm-keyperson — 重点人员管理 (8084)](#4-pdm-keyperson--重点人员管理-8084)
5. [pdm-floating-population — 流动人口管理 (8085)](#5-pdm-floating-population--流动人口管理-8085)
6. [pdm-missingperson — 失踪人口管理 (8086)](#6-pdm-missingperson--失踪人口管理-8086)
7. [pdm-log — 日志审计 (8087)](#7-pdm-log--日志审计-8087)
8. [pdm-notification — 通知预警 (8088)](#8-pdm-notification--通知预警-8088)
9. [全局变量与测试流程建议](#9-全局变量与测试流程建议)

---

## 1. pdm-auth — 认证与用户管理 (8081)

> **直接地址:** `http://localhost:8081` | **网关地址:** `http://localhost:8080/api/auth`

### 1.1 登录

```
POST /api/auth/login
```

| 项目 | 内容 |
|------|------|
| **描述** | 用户登录，返回 accessToken / refreshToken |
| **认证** | 无 |

**Request Body:**
```json
{
    "username": "admin",
    "password": "Admin@123"
}
```

**Response:**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "tokenType": "Bearer",
        "expiresIn": 3600,
        "userUuid": "uuid-xxx",
        "username": "admin",
        "role": "ADMIN",
        "mustChangePassword": false
    }
}
```

> **📌 后续请求:** 将 `accessToken` 存入 Postman Collection Variable，Header 加 `Authorization: Bearer {{accessToken}}`

---

### 1.2 登出

```
POST /api/auth/logout
```

| 项目 | 内容 |
|------|------|
| **描述** | 用户登出，使当前 token 失效 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:** 无

**Response:**
```json
{
    "code": 200,
    "message": "success",
    "data": null
}
```

---

### 1.3 刷新 Token

```
POST /api/auth/refresh
```

| 项目 | 内容 |
|------|------|
| **描述** | 使用 refreshToken 刷新 accessToken |
| **认证** | 无 |

**Request Body:**
```json
{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response:**
```json
{
    "code": 200,
    "data": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "tokenType": "Bearer",
        "expiresIn": 3600,
        "userUuid": "uuid-xxx",
        "username": "admin",
        "role": "ADMIN",
        "mustChangePassword": false
    }
}
```

---

### 1.4 修改密码

```
PUT /api/auth/change-password
```

| 项目 | 内容 |
|------|------|
| **描述** | 修改当前用户密码 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "oldPassword": "123456",
    "newPassword": "654321"
}
```

**Response:**
```json
{
    "code": 200,
    "message": "success",
    "data": null
}
```

---

### 1.5 获取权限组列表

```
GET /api/auth/permission-groups
```

| 项目 | 内容 |
|------|------|
| **描述** | 获取所有权限组 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Response:**
```json
{
    "code": 200,
    "data": [
        {
            "id": 1,
            "groupName": "超级管理员",
            "description": "拥有所有权限",
            "permissions": "[\"USER_READ\",\"USER_WRITE\",\"RESIDENT_READ\",\"RESIDENT_WRITE\"]",
            "createTime": "2026-01-01T00:00:00",
            "updateTime": "2026-01-01T00:00:00"
        }
    ]
}
```

---

### 1.6 创建权限组

```
POST /api/auth/permission-groups
```

| 项目 | 内容 |
|------|------|
| **描述** | 新建权限组 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "groupName": "户籍民警",
    "description": "负责户籍管理和常住人口登记",
    "permissions": "[\"RESIDENT_READ\",\"RESIDENT_WRITE\",\"HOUSEHOLD_READ\",\"HOUSEHOLD_WRITE\"]"
}
```

---

### 1.7 修改权限组

```
PUT /api/auth/permission-groups/{id}
```

| 项目 | 内容 |
|------|------|
| **描述** | 修改指定权限组 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `id` — 权限组ID |

**Request Body:**
```json
{
    "groupName": "户籍民警",
    "description": "负责户籍管理、常住人口登记和信息变更",
    "permissions": "[\"RESIDENT_READ\",\"RESIDENT_WRITE\",\"HOUSEHOLD_READ\",\"HOUSEHOLD_WRITE\",\"RESIDENT_CHANGE\"]"
}
```

---

### 1.8 删除权限组

```
DELETE /api/auth/permission-groups/{id}
```

| 项目 | 内容 |
|------|------|
| **描述** | 删除指定权限组 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `id` — 权限组ID |

---

### 1.9 新增民警

```
POST /api/auth/police
```

| 项目 | 内容 |
|------|------|
| **描述** | 创建民警信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "policeNumber": "P20260001",
    "userUuid": "uuid-xxx",
    "residentUuid": "uuid-xxx",
    "policeStation": "某某派出所",
    "jurisdiction": "某某社区",
    "areaId": 110101,
    "department": "治安大队",
    "policeRank": "二级警司",
    "dutyStatus": "在岗"
}
```

---

### 1.10 分页查询民警列表

```
GET /api/auth/police?page=1&size=20&keyword=张三
```

| 项目 | 内容 |
|------|------|
| **描述** | 分页查询民警，支持关键词搜索 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **查询参数** | `page` — 页码(默认1), `size` — 每页数(默认20), `keyword` — 搜索关键词(可选) |

---

### 1.11 根据警号查询民警

```
GET /api/auth/police/{policeNumber}
```

| 项目 | 内容 |
|------|------|
| **描述** | 根据警号获取民警详情 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `policeNumber` — 警号 |

---

### 1.12 修改民警信息

```
PUT /api/auth/police/{policeNumber}
```

| 项目 | 内容 |
|------|------|
| **描述** | 更新民警信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `policeNumber` — 警号 |

**Request Body:**
```json
{
    "policeStation": "某某派出所",
    "jurisdiction": "某某社区-扩大范围",
    "department": "刑侦大队",
    "policeRank": "一级警司",
    "dutyStatus": "在岗"
}
```

---

### 1.13 修改民警执勤状态

```
PUT /api/auth/police/{policeNumber}/status
```

| 项目 | 内容 |
|------|------|
| **描述** | 快捷修改民警在岗/离岗状态 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `policeNumber` — 警号 |

**Request Body:**
```json
{
    "dutyStatus": "离岗"
}
```

---

### 1.14 分页查询用户列表

```
GET /api/auth/users?page=1&size=20&keyword=&role=&status=
```

| 项目 | 内容 |
|------|------|
| **描述** | 分页查询系统用户 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **查询参数** | `page`(默认1), `size`(默认20), `keyword`(可选), `role`(可选), `status`(可选) |

---

### 1.15 根据UUID查询用户

```
GET /api/auth/users/{uuid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 获取用户详情 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 用户UUID |

---

### 1.16 修改用户信息

```
PUT /api/auth/users/{uuid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 更新用户信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 用户UUID |

**Request Body:**
```json
{
    "phone": "13800138000",
    "email": "user@example.com",
    "userRole": "OPERATOR",
    "permissionGroupId": 2
}
```

---

### 1.17 修改用户状态

```
PUT /api/auth/users/{uuid}/status
```

| 项目 | 内容 |
|------|------|
| **描述** | 修改用户账户状态(启用/禁用/锁定) |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 用户UUID |

**Request Body:**
```json
{
    "status": "DISABLED"
}
```

---

### 1.18 删除用户

```
DELETE /api/auth/users/{uuid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 删除指定用户 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 用户UUID |

---

## 2. pdm-resident — 常住人口管理 (8082)

> **直接地址:** `http://localhost:8082` | **网关地址:** `http://localhost:8080/api/resident`

### 2.1 新增常住人口

```
POST /api/resident
```

| 项目 | 内容 |
|------|------|
| **描述** | 录入常住人口基本信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "uuid": "550e8400-e29b-41d4-a716-446655440001",
    "name": "张三",
    "formerName": "",
    "gender": "男",
    "idCardNo": "110101199001011234",
    "nation": "汉族",
    "birthDate": "1990-01-01",
    "educationLevel": "本科",
    "bloodType": "A",
    "maritalStatus": "已婚",
    "occupation": "工程师",
    "phone": "13800138001",
    "photo": "",
    "residence": "北京市东城区某某路1号",
    "areaId": 110101,
    "householdType": "常住",
    "householdStatus": "在户",
    "householdAddress": "北京市东城区某某路1号",
    "householdAreaId": 110101
}
```

---

### 2.2 根据UUID查询人口

```
GET /api/resident/{uuid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 查询常住人口详细信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 人口UUID |

---

### 2.3 修改人口信息

```
PUT /api/resident/{uuid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 更新常住人口信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 人口UUID |

**Request Body:**
```json
{
    "phone": "13900139001",
    "educationLevel": "硕士",
    "occupation": "高级工程师",
    "residence": "北京市东城区某某路2号"
}
```

---

### 2.4 删除人口

```
DELETE /api/resident/{uuid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 删除常住人口记录(逻辑删除) |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 人口UUID |

---

### 2.5 多条件搜索人口

```
POST /api/resident/search
```

| 项目 | 内容 |
|------|------|
| **描述** | 根据多条件组合搜索常住人口(分页) |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "name": "张三",
    "gender": "男",
    "nation": "汉族",
    "educationLevel": "",
    "maritalStatus": "",
    "householdStatus": "",
    "idCardNo": "",
    "minAge": null,
    "maxAge": null,
    "page": 1,
    "size": 20
}
```

---

### 2.6 查询人口家庭关系

```
GET /api/resident/{uuid}/relations
```

| 项目 | 内容 |
|------|------|
| **描述** | 获取该人口的亲属关系(父母/配偶/子女) |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 人口UUID |

---

### 2.7 添加人口家庭关系

```
POST /api/resident/{uuid}/relations
```

| 项目 | 内容 |
|------|------|
| **描述** | 为该人口建立家庭关系 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 人口UUID |

**Request Body:**
```json
{
    "relationPersonUuid": "550e8400-e29b-41d4-a716-446655440002",
    "fatherUuid": "",
    "motherUuid": "",
    "spouseUuid": "550e8400-e29b-41d4-a716-446655440003"
}
```

---

### 2.8 提交人口变更申请

```
POST /api/resident/change-request
```

| 项目 | 内容 |
|------|------|
| **描述** | 提交常住人口信息变更申请(需审批) |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "applicantUuid": "uuid-xxx",
    "changeField": "educationLevel",
    "originalData": "本科",
    "modifiedData": "硕士",
    "status": "PENDING"
}
```

---

### 2.9 审批人口变更申请

```
PUT /api/resident/change-request/{rid}/approve?status=APPROVED
```

| 项目 | 内容 |
|------|------|
| **描述** | 审批人口变更申请 |
| **认证** | `Authorization: Bearer {{accessToken}}`, `X-User-Uuid: {{handlerUuid}}` |
| **路径参数** | `rid` — 变更申请ID |
| **查询参数** | `status` — APPROVED / REJECTED |
| **请求头** | `X-User-Uuid` — 审批人UUID |

---

### 2.10 批量导入人口数据

```
POST /api/resident/import
```

| 项目 | 内容 |
|------|------|
| **描述** | 通过Excel/CSV文件批量导入人口数据 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **Content-Type** | `multipart/form-data` |
| **表单字段** | `file` — 导入文件(.xlsx / .csv) |

**Postman 配置:** Body → form-data → Key: `file` (File), Value: 选择文件

**Response:**
```json
{
    "code": 200,
    "data": {
        "totalCount": 100,
        "successCount": 95,
        "failCount": 5,
        "errorMessages": [
            "第3行: 身份证号格式错误",
            "第12行: 姓名不能为空"
        ]
    }
}
```

---

## 3. pdm-household — 户籍管理 (8083)

> **直接地址:** `http://localhost:8083` | **网关地址:** `http://localhost:8080`

### 3.1 申请户口簿

```
POST /api/household/book/apply
```

| 项目 | 内容 |
|------|------|
| **描述** | 申请新的户口簿 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "householdBookNo": "HB-110101-20260001",
    "householderUuid": "550e8400-e29b-41d4-a716-446655440001",
    "establishDate": "2026-06-23",
    "hukouAddress": "北京市东城区某某路1号",
    "hukouAreaId": 110101,
    "status": "ACTIVE",
    "memberUuidList": "uuid1,uuid2,uuid3"
}
```

---

### 3.2 补办户口簿

```
POST /api/household/book/reissue
```

| 项目 | 内容 |
|------|------|
| **描述** | 丢失补办户口簿 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "bookNo": "HB-110101-20260001"
}
```

---

### 3.3 换发户口簿

```
POST /api/household/book/renew
```

| 项目 | 内容 |
|------|------|
| **描述** | 更换/更新户口簿 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "bookNo": "HB-110101-20260001"
}
```

---

### 3.4 提交户籍业务申请

```
POST /api/household/business
```

| 项目 | 内容 |
|------|------|
| **描述** | 提交户籍业务办理申请(出生登记/死亡注销/迁入/迁出等) |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "handlerUuid": "",
    "applicantUuid": "550e8400-e29b-41d4-a716-446655440001",
    "attachment": "[]",
    "businessType": "BIRTH_REGISTRATION",
    "handleDate": "2026-06-23",
    "handleBasis": "《户口登记条例》第7条",
    "fee": 0.00,
    "status": "PENDING",
    "rejectReason": "",
    "remark": ""
}
```

---

### 3.5 审批户籍业务

```
PUT /api/household/business/{rid}/approve
```

| 项目 | 内容 |
|------|------|
| **描述** | 审批户籍业务申请 |
| **认证** | `Authorization: Bearer {{accessToken}}`, `X-User-Uuid: {{handlerUuid}}` |
| **路径参数** | `rid` — 业务申请ID |

**Request Body:**
```json
{
    "status": "APPROVED",
    "rejectReason": ""
}
```

---

### 3.6 提交户籍迁移申请

```
POST /api/household/migration
```

| 项目 | 内容 |
|------|------|
| **描述** | 提交户口迁移申请(市内/跨市/跨省) |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "handlerUuid": "",
    "applicantUuid": "550e8400-e29b-41d4-a716-446655440001",
    "incomingAddress": "上海市浦东新区某某路100号",
    "incomingAreaId": 310115,
    "outgoingAddress": "北京市东城区某某路1号",
    "outgoingAreaId": 110101,
    "attachment": "[]",
    "businessType": "MIGRATION_IN",
    "handleDate": "2026-06-23",
    "handleBasis": "《户口登记条例》第10条",
    "fee": 5.00,
    "status": "PENDING",
    "rejectReason": "",
    "approvalPermitNo": "",
    "migrationPermitNo": "",
    "remark": ""
}
```

---

### 3.7 审批户籍迁移

```
PUT /api/household/migration/{rid}/approve
```

| 项目 | 内容 |
|------|------|
| **描述** | 审批户口迁移申请 |
| **认证** | `Authorization: Bearer {{accessToken}}`, `X-User-Uuid: {{handlerUuid}}` |
| **路径参数** | `rid` — 迁移申请ID |

**Request Body:**
```json
{
    "status": "APPROVED",
    "rejectReason": ""
}
```

---

### 3.8 查询人口迁移轨迹

```
GET /api/household/migration/trace/{uuid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 查询指定人口的所有迁移记录 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 人口UUID |

---

### 3.9 申领准迁证

```
POST /api/household/approval-permit
```

| 项目 | 内容 |
|------|------|
| **描述** | 创建户口准迁证 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "permitNo": "AP-2026-00001",
    "issueDate": "2026-06-23",
    "expiryDate": "2026-09-23",
    "issuingAuthority": "北京市公安局东城分局",
    "status": "ACTIVE"
}
```

---

### 3.10 申领迁移证

```
POST /api/household/migration-permit
```

| 项目 | 内容 |
|------|------|
| **描述** | 创建户口迁移证 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "permitNo": "MP-2026-00001",
    "issueDate": "2026-06-23",
    "expiryDate": "2026-07-23",
    "outgoingPoliceStation": "某某派出所",
    "status": "ACTIVE"
}
```

---

### 3.11 查询行政区划

```
GET /api/area?parentId=110000
```

| 项目 | 内容 |
|------|------|
| **描述** | 查询行政区划树，parentId为空查省级 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **查询参数** | `parentId` — 父级区划ID(可选，为空查顶级) |

---

## 4. pdm-keyperson — 重点人员管理 (8084)

> **直接地址:** `http://localhost:8084` | **网关地址:** `http://localhost:8080/api/keyperson`

### 4.1 新增重点人员

```
POST /api/keyperson
```

| 项目 | 内容 |
|------|------|
| **描述** | 录入重点人员管控信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "uuid": "550e8400-e29b-41d4-a716-446655440010",
    "controlLevel": "A",
    "controlType": "重点上访人员",
    "designatedAt": "2026-06-23T10:00:00",
    "revokedAt": null,
    "responsiblePoliceNo": "P20260001"
}
```

---

### 4.2 修改管控级别

```
PUT /api/keyperson/{uuid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 调整重点人员管控级别 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 重点人员UUID |

**Request Body:**
```json
{
    "controlLevel": "B"
}
```

---

### 4.3 撤销管控

```
DELETE /api/keyperson/{uuid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 撤销对重点人员的管控 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `uuid` — 重点人员UUID |

---

### 4.4 多条件搜索重点人员

```
GET /api/keyperson/search?controlLevel=A&controlType=重点上访人员
```

| 项目 | 内容 |
|------|------|
| **描述** | 根据多条件查询重点人员列表 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **查询参数** | `controlLevel`, `controlType`, 或其他条件 (Map<String, Object>) |

---

### 4.5 制定走访计划

```
POST /api/keyperson/visit-plan
```

| 项目 | 内容 |
|------|------|
| **描述** | 为重点人员制定走访计划 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "planId": "VP-2026-00001",
    "keyPersonUuid": "550e8400-e29b-41d4-a716-446655440010",
    "plannedDate": "2026-07-01",
    "actualDate": null,
    "visitType": "定期走访",
    "status": "PLANNED",
    "assignedPoliceNo": "P20260001",
    "isAlerted": 0
}
```

---

### 4.6 执行走访计划

```
PUT /api/keyperson/visit-plan/{id}
```

| 项目 | 内容 |
|------|------|
| **描述** | 登记走访实际执行情况和信访记录 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `id` — 走访计划ID |

**Request Body:**
```json
{
    "actualDate": "2026-07-01",
    "petitionRecord": "人员情绪稳定，无异常行为"
}
```

---

### 4.7 登记信访记录

```
POST /api/keyperson/petition
```

| 项目 | 内容 |
|------|------|
| **描述** | 登记重点人员信访信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "keyPersonUuid": "550e8400-e29b-41d4-a716-446655440010",
    "handlerPoliceNo": "P20260001",
    "petitionTime": "2026-06-23T14:30:00",
    "address": "区政府门口",
    "remark": "因拆迁补偿问题上访",
    "evaluation": "一般"
}
```

---

### 4.8 GIS地图数据

```
GET /api/keyperson/gis
```

| 项目 | 内容 |
|------|------|
| **描述** | 获取重点人员GIS地理分布数据 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

---

## 5. pdm-floating-population — 流动人口管理 (8085)

> **直接地址:** `http://localhost:8085` | **网关地址:** `http://localhost:8080/api/fp`

### 5.1 流动人口登记

```
POST /api/fp/register
```

| 项目 | 内容 |
|------|------|
| **描述** | 登记流动人口信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "rid": "FP-2026-00001",
    "residencePermitNo": "",
    "uuid": "550e8400-e29b-41d4-a716-446655440020",
    "agentUuid": "",
    "attachment": "[]",
    "reviewerUuid": "",
    "rejectReason": "",
    "registerDate": "2026-06-23",
    "reviewDate": null
}
```

---

### 5.2 修改流动人口登记

```
PUT /api/fp/register/{rid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 修改流动人口登记信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `rid` — 登记记录ID |

**Request Body:**
```json
{
    "attachment": "[\"file1.pdf\",\"file2.jpg\"]",
    "registerDate": "2026-06-23"
}
```

---

### 5.3 删除流动人口登记

```
DELETE /api/fp/register/{rid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 删除流动人口登记记录 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `rid` — 登记记录ID |

---

### 5.4 申领居住证

```
POST /api/fp/permit/apply
```

| 项目 | 内容 |
|------|------|
| **描述** | 流动人口申领居住证 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "permitNo": "RP-2026-00001",
    "uuid": "550e8400-e29b-41d4-a716-446655440020",
    "issueDate": null,
    "expiryDate": "2027-06-23",
    "status": "PENDING"
}
```

---

### 5.5 审批居住证

```
PUT /api/fp/permit/{id}/approve
```

| 项目 | 内容 |
|------|------|
| **描述** | 审批流动人口居住证申请 |
| **认证** | `Authorization: Bearer {{accessToken}}`, `X-User-Uuid: {{reviewerUuid}}` |
| **路径参数** | `id` — 居住证ID |

---

### 5.6 制发居住证

```
PUT /api/fp/permit/{id}/issue
```

| 项目 | 内容 |
|------|------|
| **描述** | 制发已审批的居住证 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `id` — 居住证ID |

---

### 5.7 续期居住证

```
POST /api/fp/permit/{id}/renew
```

| 项目 | 内容 |
|------|------|
| **描述** | 续期居住证 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `id` — 居住证ID |

**Request Body:**
```json
{
    "renewalId": "RN-2026-00001",
    "permitNo": "RP-2026-00001",
    "oldExpiryDate": "2027-06-23",
    "newExpiryDate": "2028-06-23",
    "renewalDate": "2026-06-23",
    "operatorUuid": "uuid-xxx",
    "remark": "第二次续期"
}
```

---

### 5.8 居住登记

```
POST /api/fp/residence/register
```

| 项目 | 内容 |
|------|------|
| **描述** | 流动人口实际居住地址登记 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "rid": "RR-2026-00001",
    "uuid": "550e8400-e29b-41d4-a716-446655440020",
    "originalAddress": "河南省某市某县某村",
    "currentAddress": "北京市朝阳区某某小区3号楼501",
    "areaId": 110105,
    "addressType": "租赁",
    "houseOwnership": "租赁-整租",
    "purpose": "务工",
    "expectedDuration": "长期(>1年)",
    "workUnit": "某某科技有限公司",
    "registerDate": "2026-06-23"
}
```

---

### 5.9 修改居住登记

```
PUT /api/fp/residence/{rid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 修改居住登记信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `rid` — 居住登记ID |

---

### 5.10 删除居住登记

```
DELETE /api/fp/residence/{rid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 删除居住登记记录 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `rid` — 居住登记ID |

---

### 5.11 流动人口热力图

```
GET /api/fp/statistics/heatmap
```

| 项目 | 内容 |
|------|------|
| **描述** | 获取流动人口区域分布热力图数据 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Response:**
```json
{
    "code": 200,
    "data": [
        {"areaId": 110105, "areaName": "朝阳区", "count": 15230, "longitude": 116.44, "latitude": 39.92}
    ]
}
```

---

### 5.12 流动人口趋势统计

```
GET /api/fp/statistics/trend
```

| 项目 | 内容 |
|------|------|
| **描述** | 获取流动人口变化趋势统计数据 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Response:**
```json
{
    "code": 200,
    "data": [
        {"month": "2026-01", "inflow": 3200, "outflow": 1800, "net": 1400}
    ]
}
```

---

## 6. pdm-missingperson — 失踪人口管理 (8086)

> **直接地址:** `http://localhost:8086` | **网关地址:** `http://localhost:8080/api/missing`

### 6.1 登记失踪人口

```
POST /api/missing
```

| 项目 | 内容 |
|------|------|
| **描述** | 登记失踪人口信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "residentUuid": "550e8400-e29b-41d4-a716-446655440001",
    "missingDate": "2026-06-15",
    "missingPlace": "北京市东城区某某商场",
    "photo": "",
    "appearance": "身高170cm，偏瘦，短发，戴黑框眼镜",
    "medicalHistory": "无",
    "possibleWay": "疑似被拐卖",
    "contactPhone": "13800138001",
    "status": "MISSING"
}
```

---

### 6.2 撤销失踪登记

```
DELETE /api/missing/{rid}
```

| 项目 | 内容 |
|------|------|
| **描述** | 删除失踪人口记录 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `rid` — 失踪记录ID |

---

### 6.3 登记找回/恢复

```
POST /api/missing/recovery
```

| 项目 | 内容 |
|------|------|
| **描述** | 登记失踪人员找回信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Request Body:**
```json
{
    "missingRecordRid": 1,
    "recoveryDate": "2026-06-22",
    "summary": "经群众举报，在某出租屋内发现，已由民警带回"
}
```

---

### 6.4 搜索失踪人口

```
GET /api/missing/search?name=张三&status=MISSING&page=1&size=20
```

| 项目 | 内容 |
|------|------|
| **描述** | 多条件分页搜索失踪人口 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **查询参数** | `residentUuid`(可选), `status`(可选: MISSING/RECOVERED), `name`(可选), `page`(默认1), `size`(默认20) |

---

### 6.5 失踪人口统计

```
GET /api/missing/statistics
```

| 项目 | 内容 |
|------|------|
| **描述** | 获取失踪人口统计数据(总数/找回数/按区域等) |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Response:**
```json
{
    "code": 200,
    "data": {
        "totalMissing": 120,
        "totalRecovered": 85,
        "currentMissing": 35,
        "byMonth": [],
        "byArea": []
    }
}
```

---

## 7. pdm-log — 日志审计 (8087)

> **直接地址:** `http://localhost:8087` | **网关地址:** `http://localhost:8080/api/log`

### 7.1 查询审计日志

```
GET /api/log/audit?page=1&size=20&startTime=2026-06-01T00:00:00&endTime=2026-06-23T23:59:59&operationType=UPDATE
```

| 项目 | 内容 |
|------|------|
| **描述** | 分页查询操作审计日志 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **查询参数** | `startTime`(可选,ISO格式), `endTime`(可选), `operatorUuid`(可选), `operationType`(可选: CREATE/UPDATE/DELETE/QUERY), `page`(默认1), `size`(默认20) |

---

### 7.2 查询登录日志

```
GET /api/log/login?page=1&size=20&userUuid=uuid-xxx&isSuccess=1&startTime=2026-06-01T00:00:00&endTime=2026-06-23T23:59:59
```

| 项目 | 内容 |
|------|------|
| **描述** | 分页查询用户登录日志 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **查询参数** | `userUuid`(可选), `startTime`(可选), `endTime`(可选), `isSuccess`(可选: 0失败/1成功), `page`(默认1), `size`(默认20) |

---

### 7.3 导出审计日志

```
GET /api/log/export?startTime=2026-06-01T00:00:00&endTime=2026-06-23T23:59:59
```

| 项目 | 内容 |
|------|------|
| **描述** | 导出审计日志为Excel/CSV文件(直接下载) |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **查询参数** | `startTime`(可选), `endTime`(可选), `operatorUuid`(可选), `operationType`(可选) |

**Postman 配置:** Send and Download → 发送请求后保存文件

---

## 8. pdm-notification — 通知预警 (8088)

> **直接地址:** `http://localhost:8088` | **网关地址:** `http://localhost:8080/api/alert`

### 8.1 获取待处理预警

```
GET /api/alert/pending
```

| 项目 | 内容 |
|------|------|
| **描述** | 获取所有未处理的预警信息 |
| **认证** | `Authorization: Bearer {{accessToken}}` |

**Response:**
```json
{
    "code": 200,
    "data": [
        {
            "id": 1,
            "alertType": "VISIT_OVERDUE",
            "targetType": "KEY_PERSON",
            "targetId": "uuid-xxx",
            "alertContent": "重点人员超过30天未走访",
            "severity": "HIGH",
            "isHandled": 0,
            "handledBy": null,
            "handledAt": null,
            "createTime": "2026-06-23T08:00:00"
        }
    ]
}
```

---

### 8.2 处理预警

```
PUT /api/alert/{id}/handle?handledBy=admin
```

| 项目 | 内容 |
|------|------|
| **描述** | 标记预警为已处理 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **路径参数** | `id` — 预警ID |
| **查询参数** | `handledBy` — 处理人标识 |

---

### 8.3 搜索预警

```
GET /api/alert/search?alertType=VISIT_OVERDUE&severity=HIGH&isHandled=0&page=1&size=20
```

| 项目 | 内容 |
|------|------|
| **描述** | 多条件分页搜索预警记录 |
| **认证** | `Authorization: Bearer {{accessToken}}` |
| **查询参数** | `alertType`(可选: VISIT_OVERDUE/PERMIT_EXPIRY/KEY_PERSON_ANOMALY), `severity`(可选: HIGH/MEDIUM/LOW), `isHandled`(可选: 0未处理/1已处理), `page`(默认1), `size`(默认20) |

---

## 9. 全局变量与测试流程建议

### 9.1 Postman Collection Variables

在 Postman Collection 中设置以下变量：

| 变量名 | 初始值 | 说明 |
|--------|--------|------|
| `baseUrl` | `http://localhost:8080` | 网关地址 |
| `accessToken` | (登录后自动设置) | JWT Token |
| `refreshToken` | (登录后自动设置) | 刷新Token |
| `userUuid` | (登录后自动设置) | 当前登录用户UUID |
| `handlerUuid` | (登录后自动设置) | 审批人UUID |

### 9.2 Pre-request Script (自动添加Token)

```javascript
// 对所有需要认证的请求自动添加 Authorization Header
if (pm.collectionVariables.get('accessToken')) {
    pm.request.headers.add({
        key: 'Authorization',
        value: 'Bearer ' + pm.collectionVariables.get('accessToken')
    });
}
```

### 9.3 登录后自动设置变量 (Tests Script)

```javascript
// 在 Login 请求的 Tests 标签中添加
if (pm.response.code === 200) {
    const resp = pm.response.json();
    if (resp.code === 200) {
        pm.collectionVariables.set('accessToken', resp.data.accessToken);
        pm.collectionVariables.set('refreshToken', resp.data.refreshToken);
        pm.collectionVariables.set('userUuid', resp.data.userUuid);
    }
}
```

### 9.4 推荐测试流程

```
┌─────────────────────────────────────────────────────┐
│  Step 1: 登录 (POST /api/auth/login)                 │
│          → 自动获取 token 存入变量                   │
├─────────────────────────────────────────────────────┤
│  Step 2: 基础数据准备                                │
│          - 新建民警 (POST /api/auth/police)          │
│          - 查询行政区划 (GET /api/area)              │
├─────────────────────────────────────────────────────┤
│  Step 3: 常住人口管理                                │
│          - 新增人口 (POST /api/resident)             │
│          - 多条件搜索 (POST /api/resident/search)    │
│          - 建立家庭关系 (POST /api/resident/../relations)│
│          - 提交变更申请 (POST /api/resident/change-request)│
│          - 审批变更 (PUT /api/resident/change-request/../approve)│
├─────────────────────────────────────────────────────┤
│  Step 4: 户籍管理                                    │
│          - 申请户口簿 (POST /api/household/book/apply)│
│          - 提交迁移申请 (POST /api/household/migration)│
│          - 审批迁移 (PUT /api/household/migration/../approve)│
│          - 查询迁移轨迹 (GET /api/household/migration/trace/..)│
├─────────────────────────────────────────────────────┤
│  Step 5: 重点人员管理                                │
│          - 新增重点人员 (POST /api/keyperson)        │
│          - 制定走访计划 (POST /api/keyperson/visit-plan)│
│          - 执行走访 (PUT /api/keyperson/visit-plan/..)│
│          - 登记信访 (POST /api/keyperson/petition)   │
├─────────────────────────────────────────────────────┤
│  Step 6: 流动人口管理                                │
│          - 登记 (POST /api/fp/register)              │
│          - 居住登记 (POST /api/fp/residence/register)│
│          - 申领居住证 (POST /api/fp/permit/apply)    │
│          - 审批制发 (PUT /api/fp/permit/../approve)  │
│          - 统计查询 (GET /api/fp/statistics/*)       │
├─────────────────────────────────────────────────────┤
│  Step 7: 失踪人口管理                                │
│          - 登记失踪 (POST /api/missing)              │
│          - 搜索 (GET /api/missing/search)            │
│          - 登记找回 (POST /api/missing/recovery)     │
├─────────────────────────────────────────────────────┤
│  Step 8: 日志与预警                                  │
│          - 审计日志 (GET /api/log/audit)             │
│          - 登录日志 (GET /api/log/login)             │
│          - 待处理预警 (GET /api/alert/pending)       │
│          - 处理预警 (PUT /api/alert/../handle)       │
└─────────────────────────────────────────────────────┘
```

---

### 9.5 服务端口映射总表

| 模块 | 直接端口 | 网关路由前缀 |
|------|----------|-------------|
| pdm-gateway | 8080 | — |
| pdm-auth | 8081 | `/api/auth/**` |
| pdm-resident | 8082 | `/api/resident/**` |
| pdm-household | 8083 | `/api/household/**`, `/api/area/**` |
| pdm-keyperson | 8084 | `/api/keyperson/**` |
| pdm-floating-population | 8085 | `/api/fp/**` |
| pdm-missingperson | 8086 | `/api/missing/**` |
| pdm-log | 8087 | `/api/log/**` |
| pdm-notification | 8088 | `/api/alert/**` |
