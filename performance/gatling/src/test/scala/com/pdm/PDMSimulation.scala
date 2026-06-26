package com.pdm

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class PDMSimulation extends Simulation {
  val httpProtocol = http
    .baseUrl("http://172.22.217.154:8080")
    .contentTypeHeader("application/json;charset=UTF-8")
    .acceptHeader("application/json")

  def authHeader(session: io.gatling.core.session.Session): String =
    s"Bearer ${session("token").as[String]}"

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

  // Scenario 1: Login + resident search
  val searchScenario = scenario("Search Load")
    .exec(login)
    .during(2.minutes) {
      exec(residentSearch).pause(50.millis, 200.millis)
    }

  // Scenario 2: Mixed business
  val mixedScenario = scenario("Mixed Load")
    .exec(login)
    .during(2.minutes) {
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

  setUp(
    searchScenario.inject(
      rampUsers(15).during(10.seconds),
      constantUsersPerSec(5).during(110.seconds)
    ),
    mixedScenario.inject(
      rampUsers(15).during(10.seconds),
      constantUsersPerSec(5).during(110.seconds)
    )
  ).protocols(httpProtocol)
    .assertions(
      global.successfulRequests.percent.gt(95.0),
      global.responseTime.percentile3.lt(3000)
    )
}
