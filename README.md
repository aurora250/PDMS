# 人口数据库管理系统 (PDM)

People Database Management System — 面向公安机关的人口数据库综合管理平台

## 技术栈

- **Java 21** + Virtual Threads
- **Spring Boot 3.3** + Spring Cloud 2023.0.2 (Gateway + Nacos)
- **Spring Security** + JWT 无状态认证
- **MyBatis-Plus 3.5.7** + MySQL 8.0
- **Apache ShardingSphere-JDBC** 分库分表
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
│   ├── pdm-common-mybatis/        # MyBatis-Plus配置
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
└── docker/                        # Docker部署配置
```

## 快速启动

### 1. 启动基础设施

```bash
cd docker
docker-compose up -d
```

### 2. 初始化数据库

```bash
mysql -h127.0.0.1 -uroot -proot < sql/init-schema.sql
```

### 3. 启动微服务

```bash
mvn clean package -DskipTests
java -jar pdm-gateway/target/pdm-gateway-1.0-SNAPSHOT.jar &
java -jar pdm-auth/target/pdm-auth-1.0-SNAPSHOT.jar &
# ... 依次启动其他服务
```

### 4. 默认账号

- 用户名: `admin`
- 密码: `Admin@123`

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

## 设计文档

- [需求规约](02.第01组-需求规约-人口数据库管理系统.docx)
- [数据库设计说明书](07.第01组-数据库设计说明书-人口数据库管理系统.xlsx)
- [项目功能结构表](08.第01组-项目功能结构表-人口数据库管理系统.xlsx)
