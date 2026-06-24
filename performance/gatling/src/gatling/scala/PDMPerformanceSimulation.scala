package pdm

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.core.structure.PopulationBuilder

import scala.concurrent.duration._

/**
 * PDM 人口数据库管理系统 — Gatling 性能模拟
 *
 * 场景:
 * 1. 登录认证 — 50 并发用户持续登录
 * 2. 常住人口检索 — 30 用户多条件组合查询 (ES)
 * 3. 混合业务负载 — 户籍/重点人员/流动人口/区域树查询
 *
 * 性能目标: 查询响应 < 3s, 登录 P99 < 1s
 */
class PDMPerformanceSimulation extends Simulation {

  val httpProtocol = http
    .baseUrl("http://127.0.0.1:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")
    .userAgentHeader("Gatling-PerfTest/1.0")
    .disableCaching
    .shareConnections

  // ==================== 测试数据 ====================
  val userFeeder = csv("test-users.csv").random

  // ==================== 场景 1: 登录认证 ====================
  val loginScenario = scenario("Login Flow")
    .feed(userFeeder)
    .exec(
      http("POST /api/auth/login")
        .post("/api/auth/login")
        .body(StringBody("""{"username":"${username}","password":"${password}"}"""))
        .check(status.is(200))
        .check(jsonPath("$.data.accessToken").saveAs("accessToken"))
        .check(responseTimeInMillis.lte(1000)) // < 1s
    )
    .exec(
      http("GET /api/auth/users (authenticated)")
        .get("/api/auth/users?page=1&size=10")
        .header("Authorization", "Bearer ${accessToken}")
        .check(status.is(200))
    )
    .exec(
      http("POST /api/auth/logout")
        .post("/api/auth/logout")
        .header("Authorization", "Bearer ${accessToken}")
        .check(status.is(200))
    )

  // ==================== 场景 2: 常住人口检索 ====================
  val residentSearchScenario = scenario("Resident Search")
    .exec(
      http("POST /api/auth/login (obtain token)")
        .post("/api/auth/login")
        .body(StringBody("""{"username":"admin","password":"Admin@123"}"""))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .repeat(20) {
      exec(
        http("POST /api/resident/search (gender+status)")
          .post("/api/resident/search")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody("""{"page":1,"size":20,"gender":"男","householdStatus":"正常"}"""))
          .check(status.is(200))
          .check(responseTimeInMillis.lte(3000)) // < 3s
      )
      .pause(100.milliseconds)
      .exec(
        http("POST /api/resident/search (multi-filter)")
          .post("/api/resident/search")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody("""{"page":1,"size":20,"name":"张","educationLevel":"本科","maritalStatus":"已婚","minAge":25,"maxAge":50}"""))
          .check(status.is(200))
          .check(responseTimeInMillis.lte(3000))
      )
      .pause(100.milliseconds)
      .exec(
        http("POST /api/resident/search (by idCard)")
          .post("/api/resident/search")
          .header("Authorization", "Bearer ${authToken}")
          .body(StringBody("""{"page":1,"size":10,"idCardNo":"11010119900307663X"}"""))
          .check(status.is(200))
          .check(responseTimeInMillis.lte(3000))
      )
    }

  // ==================== 场景 3: 混合业务负载 ====================
  val mixedWorkloadScenario = scenario("Mixed Business Workload")
    .exec(
      http("Login")
        .post("/api/auth/login")
        .body(StringBody("""{"username":"admin","password":"Admin@123"}"""))
        .check(jsonPath("$.data.accessToken").saveAs("authToken"))
    )
    .during(5.minutes) {
      randomSwitch(
        30.0 -> exec(
          // 30% 区域查询
          http("GET /api/area (tree)")
            .get("/api/area")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.is(200))
            .check(responseTimeInMillis.lte(1000))
        ),
        25.0 -> exec(
          // 25% 重点人员查询
          http("GET /api/keyperson/search")
            .get("/api/keyperson/search?page=1&size=20")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.is(200))
            .check(responseTimeInMillis.lte(3000))
        ),
        20.0 -> exec(
          // 20% 流动人口统计
          http("GET /api/fp/statistics/heatmap")
            .get("/api/fp/statistics/heatmap")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.is(200))
            .check(responseTimeInMillis.lte(2000))
        ),
        15.0 -> exec(
          // 15% 失踪人员统计
          http("GET /api/missing/statistics")
            .get("/api/missing/statistics")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.is(200))
            .check(responseTimeInMillis.lte(1000))
        ),
        10.0 -> exec(
          // 10% 日志查询
          http("GET /api/log/audit")
            .get("/api/log/audit?page=1&size=20")
            .header("Authorization", "Bearer ${authToken}")
            .check(status.is(200))
            .check(responseTimeInMillis.lte(2000))
        )
      )
      .pause(200.milliseconds, 800.milliseconds)
    }

  // ==================== 压力注入策略 ====================
  setUp(
    // 50 并发用户，30s 爬坡，持续 5 分钟
    loginScenario.inject(
      rampUsers(50).during(30.seconds),
      constantUsersPerSec(10).during(270.seconds)
    ),
    // 30 用户检索场景
    residentSearchScenario.inject(
      rampUsers(30).during(20.seconds),
      constantUsersPerSec(5).during(280.seconds)
    ),
    // 混合负载 30 用户
    mixedWorkloadScenario.inject(
      rampUsers(30).during(20.seconds),
      constantUsersPerSec(6).during(280.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.responseTime.percentile3.lte(3000),   // P99.9 < 3s
      global.responseTime.percentile2.lte(2000),   // P99 < 2s
      global.successfulRequests.percent.gte(99.0), // 成功率 > 99%
      global.failedRequests.count.lte(50)          // 失败 < 50
    )
}
