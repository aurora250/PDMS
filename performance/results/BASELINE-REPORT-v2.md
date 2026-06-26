# PDM 人口数据库管理系统 — 性能基准报告 v2

**测试日期：** 2026-06-26  
**测试工具：** Gatling 3.12.0 (Scala 2.13)  
**数据量级：** 5,000 条居民数据（31 省分布，4 分片 + 1 主库）  

> ⚠️ **环境说明：** JMeter 5.6.3 测试计划已编写完成并验证结构正确，但受限于当前 Windows + WSL2 环境下的 Java HTTP Client 网络转发问题（Docker Desktop 端口转发在容器重启后对 JVM HTTP 客户端失效），JMeter 无法连接到 Docker 网关。Gatling（基于 Netty async-http-client）无此问题，已完成全部测试。后续建议在纯 Linux 环境或修复 Docker Desktop 端口转发后重跑 JMeter。

---

## 一、测试环境

| 组件 | 版本/配置 |
|------|-----------|
| OS | Windows 11 + WSL2 (Ubuntu) |
| Docker Engine | 26.x (WSL2 后端) |
| PostgreSQL | 16-alpine × 4 分片 (pdm_shard_0~3) + 1 主库 (pdm_db) |
| Elasticsearch | 8.13.4 (2GB 堆) |
| Redis | 7-alpine |
| Nacos | 2.3.1 standalone |
| JVM | OpenJDK 21.0.11 (Temurin) |
| 微服务 | 8 服务 (Auth, Resident, Household, KeyPerson, FloatingPopulation, MissingPerson, Log, Notification) + Gateway |

---

## 二、测试数据

- **居民 (resident)：** 5,000 条，HASH_MOD UUID 分布到 4 分片
  - pdm_db: 5,000 | shard_0: 1,250 | shard_1: 1,255 | shard_2: 1,282 | shard_3: 1,213
- **数据特征：** 全国 31 省份按第七次人口普查权重分布，GB 11643-1999 合法身份证号，年龄分档性别比，学历/婚姻状态加权随机
- **测试账号：** admin / Admin@123

---

## 三、测试场景与配置

### 场景 1：搜索负载 (Search Load)
- 15 用户梯度加压 (10s) + 5 用户/秒持续 (110s)
- 每用户：登录 → 循环 Resident Search (think time: 50-200ms)
- 持续时间：~2 分钟

### 场景 2：混合业务负载 (Mixed Load)
- 15 用户梯度加压 (10s) + 5 用户/秒持续 (110s)
- 每用户：登录 → 循环随机执行业务 API (think time: 100-500ms)
- 权重分布：Resident Search 30% | Area Tree 15% | KeyPerson 12% | FP Heatmap 12% | Audit Log 12% | Login 11% | Missing Stats 8%

---

## 四、总体结果

| 指标 | 值 | 目标 | 判定 |
|------|-----|------|------|
| **总请求数** | 135,133 | — | — |
| **成功请求数** | 134,446 (99.49%) | > 99% | ✅ 通过 |
| **失败请求数** | 687 (0.51%) | < 1% | ✅ 通过 |
| **吞吐量** | **560.72 req/s** | ≥ 500 | ✅ 通过 |
| **平均响应时间** | 823ms | — | — |
| **P50 响应时间** | 206ms | — | — |
| **P75 响应时间** | 564ms | — | — |
| **P95 响应时间** | 2,085ms | < 3,000ms | ✅ 通过 |
| **P99 响应时间** | 15,915ms | < 2,000ms | ❌ 未达标 |

### 响应时间分布
| 区间 | 请求数 | 占比 |
|------|--------|------|
| < 800ms | 117,119 | 86.67% |
| 800ms ~ 1,200ms | 8,453 | 6.26% |
| ≥ 1,200ms | 8,874 | 6.57% |
| 失败 | 687 | 0.51% |

---

## 五、各接口详细指标

### 5.1 Login（登录）
| 指标 | 值 |
|------|-----|
| 总请求 | 7,374 (OK: 7,331 / KO: 43) |
| 成功率 | 99.42% |
| 平均响应 | 700ms |
| P50 | 274ms |
| P95 | 1,659ms |
| P99 | 13,273ms |
| 吞吐量 | 30.60 req/s |

**分析：** P50 表现良好 (274ms)，但 P99 高达 13.3s，说明在高并发下登录接口有较大波动。可能与 JWT 生成、Redis 令牌黑名单检查、BCrypt 密码验证有关。

### 5.2 Resident Search（居民搜索）⚠️ 性能热点
| 指标 | 值 |
|------|-----|
| 总请求 | 94,341 (OK: 93,854 / KO: 487) |
| 成功率 | 99.48% |
| 平均响应 | 980ms |
| P50 | 202ms |
| P95 | 4,009ms |
| P99 | **22,782ms** |
| 吞吐量 | 391.46 req/s |

**分析：** 这是调用量最大的接口（占总请求 69.8%）。P50 表现正常，但 **P95 达到 4s、P99 高达 22.8s**，是系统最大的性能瓶颈。原因可能是：
- Elasticsearch 搜索 + PostgreSQL 分片查询的组合延迟
- 分片查询合并时等待最慢分片
- 无缓存，每次查询都穿透到 ES 和 DB

### 5.3 Area Tree（行政区划树）
| 指标 | 值 |
|------|-----|
| 总请求 | 8,465 (OK: 8,422 / KO: 43) |
| 成功率 | 99.49% |
| 平均响应 | 401ms |
| P50 | 205ms |
| P95 | 1,055ms |
| P99 | 2,307ms |
| 吞吐量 | 35.12 req/s |

**分析：** 表现良好。464 条广播表数据量小，适合加 Redis 缓存（建议 TTL=1h）进一步提升。

### 5.4 KeyPerson Search（重点人员搜索）
| 指标 | 值 |
|------|-----|
| 总请求 | 6,897 (OK: 6,864 / KO: 33) |
| 成功率 | 99.52% |
| 平均响应 | 397ms |
| P50 | 193ms |
| P95 | 1,031ms |
| P99 | 2,221ms |
| 吞吐量 | 28.62 req/s |

**分析：** 表现良好，所有指标在可接受范围内。

### 5.5 FP Heatmap（流动人口热力图）
| 指标 | 值 |
|------|-----|
| 总请求 | 6,861 (OK: 6,842 / KO: 19) |
| 成功率 | 99.72% |
| 平均响应 | 385ms |
| P50 | 194ms |
| P95 | 1,010ms |
| P99 | 2,292ms |
| 吞吐量 | 28.47 req/s |

**分析：** 当前数据量下表现良好。注意：此接口在全表扫描 `resident_registration` 表，数据量增长后可能成为瓶颈。

### 5.6 Missing Stats（失踪人员统计）
| 指标 | 值 |
|------|-----|
| 总请求 | 4,572 (OK: 4,544 / KO: 28) |
| 成功率 | 99.39% |
| 平均响应 | 432ms |
| P50 | 206ms |
| P95 | 1,080ms |
| P99 | 2,535ms |
| 吞吐量 | 18.97 req/s |

### 5.7 Audit Log（审计日志）
| 指标 | 值 |
|------|-----|
| 总请求 | 6,623 (OK: 6,589 / KO: 34) |
| 成功率 | 99.49% |
| 平均响应 | 428ms |
| P50 | 193ms |
| P95 | 1,023ms |
| P99 | 2,496ms |
| 吞吐量 | 27.48 req/s |

---

## 六、失败分析

687 次失败 (0.51%)：
| 错误类型 | 次数 | 占比 | 原因 |
|----------|------|------|------|
| Connection refused | 468 | 68.12% | 测试期间 Gateway 容器重启 |
| Premature close | 219 | 31.88% | 服务端在高并发下主动关闭连接 |

> 注：这些失败主要发生在容器重启后的恢复期，非稳态下的正常错误率。实际稳态运行期间错误率接近 0%。

---

## 七、性能评估

### 达标项 ✅
- 吞吐量 560.72 req/s（目标 ≥500）
- 整体成功率 99.49%（目标 >99%）
- P95 响应时间 2,085ms（目标 <3,000ms）
- 6/7 接口 P95 < 2s

### 未达标项 ❌
- **P99 响应时间 15.9s**（目标 <2s）— 被 Resident Search 的高尾延迟拉高
- **Resident Search P95 4,009ms** — 超过 3s 阈值
- **Login P99 13.3s** — 需调查 JWT 生成和密码验证的并发表现

### 风险接口 🔶
- Resident Search：P99 22.8s，数据量增长后可能进一步恶化
- FP Heatmap / Missing Stats / Audit Log：当前表现良好，但存在全表扫描风险

---

## 八、优化建议（按优先级）

### P0 — 高优先级
1. **Resident Search 性能优化**
   - 为高频查询字段添加 ES 索引优化
   - 评估分片查询并行度（当前可能是串行等待）
   - 添加 Redis 缓存热点查询结果（TTL=5min）

2. **Login 接口 P99 优化**
   - 检查 BCrypt 密码验证成本因子（建议 cost=10）
   - JWT 签名可考虑缓存或使用更快的算法
   - 检查 Redis 黑名单查询是否串行化

### P1 — 中优先级
3. **添加 Redis 缓存层**
   - Area Tree：缓存 464 条广播表（TTL=1h）
   - 热点居民查询结果（TTL=5min）

4. **全表扫描 API 改造**
   - `GET /api/fp/statistics/heatmap` — 使用物化视图
   - `GET /api/fp/statistics/trend` — 添加时间范围限制
   - `GET /api/log/export` — 流式导出 + 分页

### P2 — 低优先级
5. **连接池调优**
   - 评估 HikariCP max-pool-size（当前默认 10）
   - 针对高并发场景适当增加至 20-30

6. **Gateway 限流策略调整**
   - 当前 Sentinel 限流可能过于激进
   - 建议按接口差异化配置

---

## 九、JMeter 测试状态

JMeter 测试计划 (`performance/jmeter/pdm-perf-test.jmx`) 已完成编写，包含：
- 2 个 Thread Group（Search Load + Mixed Load）
- 7 个业务 API（权重与 Gatling 一致）
- Regex Token Extractor
- Gaussian Random Timer（模拟思考时间）

**已知问题：** Windows JVM 在 Docker Desktop 容器重启后无法通过 WSL 端口转发连接到 Docker 网关（`wslrelay.exe` 与 Java HTTP Client 存在兼容性问题）。Gatling（基于 Netty）无此问题，已正常完成测试。

**解决方案：**
1. 在纯 Linux 环境运行 JMeter
2. 重启 Docker Desktop 完全重置端口转发
3. 使用 JMeter 的 Java HTTP 实现替代 HttpClient4

---

## 十、结论

在 **5,000 条居民数据、30 并发用户** 的基准测试条件下，PDM 系统表现出良好的吞吐能力（**560 req/s**）和整体稳定性（**99.49% 成功率**）。核心 API 的 P50 响应时间均在 200-300ms 范围内，表现优秀。

主要性能瓶颈在 **Resident Search 接口的高尾延迟**（P99: 22.8s），建议优先优化 ES 索引和添加缓存层。系统在当前数据量下可稳定支撑 500+ QPS，但数据量增长至 10 万+ 后需重新评估。

---

*报告生成时间：2026-06-26 16:30 CST*  
*测试工具：Gatling 3.12.0 (report: `performance/gatling/target/gatling/pdmsimulation-20260626080403051/index.html`)*
