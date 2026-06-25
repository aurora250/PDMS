# 人口数据库管理系统 (PDM)

People Database Management System — 面向公安机关的人口数据库综合管理平台

## 技术栈

- **Java 21** + Virtual Threads
- **Spring Boot 3.3** + Spring Cloud 2023.0.2 (Gateway + Nacos)
- **Spring Security** + JWT 无状态认证
- **MyBatis-Plus 3.5.7** + PostgreSQL 16
- **Elasticsearch 8.x** 全文检索
- **Redis 7.x** 缓存/分布式锁
- **Nginx** 反向代理
- **Docker Compose** 一键部署

## 项目结构

```
PeopleDatabaseManagement/
├── pdm-common/                    # 公共模块
│   ├── pdm-common-core/           # 核心工具类、异常、常量
│   ├── pdm-common-dto/            # 通用DTO/VO
│   ├── pdm-common-security/       # JWT、Security公共配置
│   ├── pdm-common-mybatis/        # MyBatis-Plus配置 + BaseEntity/BaseNamedEntity
│   └── pdm-common-es/             # ES公共操作封装
├── pdm-gateway/                   # API网关 (8080)
├── pdm-auth/                      # 认证授权服务 (8081)
├── pdm-resident/                  # 常住人口管理 (8082)
├── pdm-household/                 # 户籍管理 (8083)
├── pdm-keyperson/                 # 重点人员管控 (8084)
├── pdm-floating-population/       # 流动人口/居住证 (8085)
├── pdm-missingperson/             # 失踪人员管理 (8086)
├── pdm-log/                       # 日志审计 (8087)
├── pdm-notification/              # 预警通知 (8088)
├── sql/                           # 数据库初始化脚本
├── scripts/                       # 运维脚本 & API测试
├── docs/                          # 文档
└── docker/                        # Docker & Nginx配置
```

## 快速启动

### 1. 一键启动全部服务

```bash
docker compose up -d
```

### 2. 查看服务状态

```bash
docker compose ps
```

### 3. 默认账号

- 用户名: `admin`
- 密码: `Admin@123`
- 角色: 系统管理员 (首次登录需修改密码)

## 服务端口

| 服务 | 端口 | 网关路由 |
|------|------|----------|
| pdm-gateway | 8080 | — |
| pdm-auth | 8081 | `/api/auth/**` |
| pdm-resident | 8082 | `/api/resident/**` |
| pdm-household | 8083 | `/api/household/**` `/api/area/**` |
| pdm-keyperson | 8084 | `/api/keyperson/**` |
| pdm-floating-population | 8085 | `/api/fp/**` |
| pdm-missingperson | 8086 | `/api/missing/**` |
| pdm-log | 8087 | `/api/log/**` |
| pdm-notification | 8088 | `/api/alert/**` |
| PostgreSQL | 15432 | — |
| Redis | 16379 | — |
| Elasticsearch | 9200 | — |
| Nacos | 8848 | — |
| Nginx | 18080 | — |

## 运维脚本

| 脚本 | 用途 |
|------|------|
| `scripts/test-api.py` | **全量 API 自动化测试** — 测试全部 60 个端点 |
| `scripts/reset-database.sh` | 清空所有数据并重建容器 (`--hard` 重新编译) |
| `scripts/rebuild-and-restart.sh` | 编译所有模块并重启 (`--reset-db` 同时清数据) |
| `scripts/build-and-start.sh` | 编译、构建镜像、启动全套服务 |

```bash
# 运行 API 测试
python3 scripts/test-api.py

# 重置数据库
./scripts/reset-database.sh

# 完全重建（重新编译+清数据）
./scripts/reset-database.sh --hard
```

## API 文档

完整 API 文档见 `docs/postman-api-test-list.md`，包含全部 60 个 REST API 端点的请求/响应示例。

所有枚举值使用中文（与数据库 CHECK 约束一致）：
- 角色: `系统管理员` / `民警` / `采集员` / …
- 管控等级: `一级` / `二级` / `三级`
- 户籍状态: `正常` / `死亡注销` / `失踪注销` / `迁出注销` / `恢复`
- 走访状态: `待走访` / `已完成` / `已逾期` / `已取消`

## 核心功能模块

| 模块 | 功能 |
|------|------|
| 常住人口管理 | 全生命周期档案、关系图谱、批量导入导出 |
| 户籍管理 | 户口本申领/补办、户籍迁移(市内/省内/跨省)、四级审批 |
| 流动人口管理 | 流动登记、居住证全流程、热力图与趋势分析 |
| 重点人员管控 | 分级管控(一级/二级/三级)、走访计划、GIS分布 |
| 失踪人员管理 | 登记撤销、寻回闭环、统计看板 |
| 系统管理 | RBAC权限组、警员管理、操作/登录审计日志 |
| 预警通知 | 居住证到期/走访逾期/重点人员匹配预警 |

## 数据库

- **数据库**: PostgreSQL 16
- **连接**: `localhost:15432`, 用户 `pdm`, 密码 `pdm123`, 库 `pdm_db`
- **设计文档**: `07.第01组-数据库设计说明书-人口数据库管理系统.xlsx`
- **SQL 脚本**: `sql/init-schema.sql` (24 张表), `sql/init-schema-shard.sql` (分片表)

```bash
# 手动连接数据库
psql -h localhost -p 15432 -U pdm -d pdm_db
```
