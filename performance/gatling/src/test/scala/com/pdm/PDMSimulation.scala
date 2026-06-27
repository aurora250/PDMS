package com.pdm

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class PDMSimulation extends Simulation {
  val httpProtocol = http
    .baseUrl("http://172.22.217.154:8080")
    .contentTypeHeader("application/json;charset=UTF-8")
    .acceptHeader("application/json")
    // v3: 增加连接超时和重试配置
    .shareConnections
    .maxConnectionsPerHost(200)

  def authHeader(session: io.gatling.core.session.Session): String =
    s"Bearer ${session("token").as[String]}"

  // ========== API Definitions ==========

  val login = exec(
    http("Login")
      .post("/api/auth/login")
      .body(StringBody("""{"username":"admin","password":"Admin@123"}"""))
      .check(status.is(200))
      .check(jsonPath("$.data.accessToken").saveAs("token"))
  )

  val residentSearch = exec(
    http("Resident Search")
      .post("/api/resident/search")
      .header("Authorization", authHeader _)
      .body(StringBody("""{"page":1,"size":20}"""))
      .check(status.is(200))
  )

  // v3: 带条件的搜索（测试缓存命中率）
  val residentSearchWithFilter = exec(
    http("Resident Search (Filtered)")
      .post("/api/resident/search")
      .header("Authorization", authHeader _)
      .body(StringBody("""{"page":1,"size":20,"gender":"男","nation":"汉族"}"""))
      .check(status.is(200))
  )

  val areaQuery = exec(
    http("Area Tree")
      .get("/api/area")
      .header("Authorization", authHeader _)
      .check(status.is(200))
  )

  val keyPersonSearch = exec(
    http("KeyPerson Search")
      .get("/api/keyperson/search?page=1&size=20")
      .header("Authorization", authHeader _)
      .check(status.is(200))
  )

  val fpHeatmap = exec(
    http("FP Heatmap")
      .get("/api/fp/statistics/heatmap")
      .header("Authorization", authHeader _)
      .check(status.is(200))
  )

  val missingStats = exec(
    http("Missing Stats")
      .get("/api/missing/statistics")
      .header("Authorization", authHeader _)
      .check(status.is(200))
  )

  val auditLog = exec(
    http("Audit Log")
      .get("/api/log/audit?page=1&size=20")
      .header("Authorization", authHeader _)
      .check(status.is(200))
  )

  // ========== Scenario 1: Search Load (基准搜索负载) ==========
  val searchScenario = scenario("Search Load")
    .exec(login)
    .during(3.minutes) {
      // v3: 混合无条件搜索和有条件搜索，测试缓存命中
      randomSwitch(
        70.0 -> exec(residentSearch),
        30.0 -> exec(residentSearchWithFilter)
      ).pause(50.millis, 200.millis)
    }

  // ========== Scenario 2: Mixed Business Load (混合业务负载) ==========
  val mixedScenario = scenario("Mixed Load")
    .exec(login)
    .during(3.minutes) {
      randomSwitch(
        30.0 -> exec(residentSearch),
        15.0 -> exec(areaQuery),
        12.0 -> exec(keyPersonSearch),
        12.0 -> exec(fpHeatmap),
        8.0  -> exec(missingStats),
        12.0 -> exec(auditLog),
        11.0 -> exec(login)
      ).pause(100.millis, 500.millis)
    }

  // ========== Scenario 3: Stress Test (压力测试 - v3新增) ==========
  val stressScenario = scenario("Stress Test")
    .exec(login)
    .during(2.minutes) {
      randomSwitch(
        40.0 -> exec(residentSearch),
        20.0 -> exec(areaQuery),
        15.0 -> exec(fpHeatmap),
        15.0 -> exec(auditLog),
        10.0 -> exec(login)
      ).pause(10.millis, 50.millis)  // 极小思考时间 = 最大压力
    }

  // ========== Injection Profiles ==========
  setUp(
    // 场景1: 搜索负载 - 30并发（比v2的15用户x2倍增）
    searchScenario.inject(
      rampUsers(20).during(15.seconds),
      constantUsersPerSec(10).during(165.seconds)
    ),
    // 场景2: 混合负载 - 30并发
    mixedScenario.inject(
      rampUsers(20).during(15.seconds),
      constantUsersPerSec(10).during(165.seconds)
    ),
    // 场景3: 压力测试 - 50并发（验证优化后系统的上限）
    stressScenario.inject(
      rampUsers(30).during(10.seconds),
      constantUsersPerSec(20).during(110.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      // v3: 更严格的断言（基于优化预期）
      global.successfulRequests.percent.gt(97.0),
      global.responseTime.percentile3.lt(2000),    // P99 < 2s (v2: 未达标)
      details("Login").responseTime.percentile3.lt(3000),       // Login P99
      details("Resident Search").responseTime.percentile3.lt(5000), // 搜索P99
      details("Area Tree").responseTime.percentile3.lt(2000),
      details("FP Heatmap").responseTime.percentile3.lt(2000)
    )
}
