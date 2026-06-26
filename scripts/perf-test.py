#!/usr/bin/env python3
"""
PDM 性能基准测试脚本
模拟多并发用户执行核心业务操作，测量响应时间和吞吐量。
用法:
  python scripts/perf-test.py                    # 运行全部场景
  python scripts/perf-test.py --scene login      # 仅登录场景
  python scripts/perf-test.py --users 100 --duration 60  # 自定义参数
"""

import requests
import time
import json
import statistics
import sys
import argparse
import itertools
import threading
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from typing import Optional

BASE_URL = "http://127.0.0.1:8080"
TEST_USER = {"username": "admin", "password": "Admin@123"}

# ─── Data classes ───────────────────────────────────────────

@dataclass
class RequestResult:
    name: str
    status: int
    elapsed_ms: float
    error: Optional[str] = None

@dataclass
class ScenarioReport:
    name: str
    total_requests: int = 0
    success: int = 0
    failed: int = 0
    response_times: list = field(default_factory=list)
    errors: list = field(default_factory=list)
    qps: float = 0.0
    duration_s: float = 0.0

    @property
    def p50(self) -> float:
        return statistics.median(self.response_times) if self.response_times else 0

    @property
    def p90(self) -> float:
        return self._percentile(90) if self.response_times else 0

    @property
    def p99(self) -> float:
        return self._percentile(99) if self.response_times else 0

    @property
    def p999(self) -> float:
        return self._percentile(99.9) if self.response_times else 0

    @property
    def mean(self) -> float:
        return statistics.mean(self.response_times) if self.response_times else 0

    @property
    def success_rate(self) -> float:
        return (self.success / self.total_requests * 100) if self.total_requests else 0

    def _percentile(self, pct: float) -> float:
        sorted_times = sorted(self.response_times)
        k = (len(sorted_times) - 1) * pct / 100
        f = int(k)
        c = k - f
        if f + 1 < len(sorted_times):
            return sorted_times[f] + c * (sorted_times[f + 1] - sorted_times[f])
        return sorted_times[f]

    def to_dict(self) -> dict:
        return {
            "scenario": self.name,
            "total_requests": self.total_requests,
            "success": self.success,
            "failed": self.failed,
            "success_rate": f"{self.success_rate:.2f}%",
            "duration_s": f"{self.duration_s:.2f}",
            "qps": f"{self.qps:.2f}",
            "mean_ms": f"{self.mean:.2f}",
            "p50_ms": f"{self.p50:.2f}",
            "p90_ms": f"{self.p90:.2f}",
            "p99_ms": f"{self.p99:.2f}",
            "p999_ms": f"{self.p999:.2f}",
        }

# ─── HTTP helpers ───────────────────────────────────────────

def login(session: requests.Session) -> str:
    """Login and return access token."""
    resp = session.post(
        f"{BASE_URL}/api/auth/login",
        json=TEST_USER,
        timeout=10,
    )
    data = resp.json()
    if data.get("code") != 200:
        raise RuntimeError(f"Login failed: {data}")
    return data["data"]["accessToken"]

def timed_request(session: requests.Session, method: str, path: str,
                  json_body: dict = None, headers: dict = None) -> RequestResult:
    """Make a request and return timing info."""
    url = f"{BASE_URL}{path}"
    start = time.perf_counter()
    try:
        if method == "GET":
            resp = session.get(url, headers=headers, timeout=15)
        elif method == "POST":
            resp = session.post(url, json=json_body, headers=headers, timeout=15)
        elif method == "PUT":
            resp = session.put(url, json=json_body, headers=headers, timeout=15)
        else:
            return RequestResult(name=path, status=0, elapsed_ms=0, error=f"Unknown method: {method}")
        elapsed = (time.perf_counter() - start) * 1000
        return RequestResult(name=path, status=resp.status_code, elapsed_ms=elapsed)
    except Exception as e:
        elapsed = (time.perf_counter() - start) * 1000
        return RequestResult(name=path, status=0, elapsed_ms=elapsed, error=str(e))

# ─── Scenario executors ─────────────────────────────────────

class ScenarioRunner:
    def __init__(self, users: int, duration_s: int, ramp_up_s: int = 10):
        self.users = users
        self.duration_s = duration_s
        self.ramp_up_s = ramp_up_s
        self.report = None
        self.running = True

    def _worker(self, thread_id: int, report: ScenarioReport,
                auth_header: dict, scenario_fn):
        """Worker function running requests in a loop."""
        session = requests.Session()
        session.headers.update(auth_header)
        count = 0

        # Ramp-up delay
        if self.ramp_up_s > 0:
            delay = (thread_id / max(self.users, 1)) * self.ramp_up_s
            time.sleep(delay)

        consecutive_errors = 0
        while self.running:
            try:
                results = scenario_fn(session, thread_id, count)
                consecutive_errors = 0
            except Exception as e:
                results = [RequestResult(
                    name="worker_error",
                    status=0,
                    elapsed_ms=0,
                    error=f"[thread {thread_id}] {type(e).__name__}: {e}"
                )]
                consecutive_errors += 1
                # Back off on repeated errors
                if consecutive_errors > 3:
                    time.sleep(1.0)

            for r in results if isinstance(results, list) else [results]:
                report.response_times.append(r.elapsed_ms)
                report.total_requests += 1
                if r.status == 200 or r.status == 201:
                    report.success += 1
                else:
                    report.failed += 1
                    report.errors.append(f"[{r.name}] status={r.status} error={r.error}")
            count += 1
            # Small think time for realism (50-200ms)
            if self.duration_s < 60:  # stress mode
                time.sleep(0.05)

    def run(self, name: str, scenario_fn) -> ScenarioReport:
        """Launch workers and measure performance."""
        report = ScenarioReport(name=name)
        print(f"\n{'='*60}")
        print(f"  Scenario: {name}")
        print(f"  Users: {self.users}, Duration: {self.duration_s}s, Ramp-up: {self.ramp_up_s}s")
        print(f"{'='*60}")

        # Get auth token
        session = requests.Session()
        try:
            token = login(session)
        except Exception as e:
            print(f"  [FATAL] Login failed: {e}")
            report.errors.append(f"Login failed: {e}")
            return report
        auth_header = {"Authorization": f"Bearer {token}"}

        start_time = time.perf_counter()

        with ThreadPoolExecutor(max_workers=self.users) as executor:
            futures = [
                executor.submit(self._worker, i, report, auth_header, scenario_fn)
                for i in range(self.users)
            ]

            # Wait for duration
            time.sleep(self.duration_s)
            self.running = False

            # Collect results
            for f in as_completed(futures):
                f.result()  # Propagate exceptions

        report.duration_s = time.perf_counter() - start_time
        report.qps = report.total_requests / report.duration_s if report.duration_s > 0 else 0
        self.report = report
        return report

# ─── Scenario definitions ───────────────────────────────────

def scenario_login(session: requests.Session, thread_id: int, count: int) -> list:
    """Login concurrency test."""
    results = []
    # Login
    r = timed_request(session, "POST", "/api/auth/login", json_body=TEST_USER)
    results.append(r)
    # Extract token and check user list (authenticated operation)
    if r.status == 200:
        data = session.post(f"{BASE_URL}/api/auth/login", json=TEST_USER, timeout=10).json()
        token = data["data"]["accessToken"]
        headers = {"Authorization": f"Bearer {token}"}
        r2 = timed_request(session, "GET", "/api/auth/users?page=1&size=10", headers=headers)
        results.append(r2)
    return results

def scenario_resident_search(session: requests.Session, thread_id: int, count: int) -> list:
    """Resident search - the most frequent operation."""
    results = []
    # Multi-condition search (ES-backed)
    queries = [
        {"name": "测试", "gender": None, "nation": None, "minAge": None, "maxAge": None},
        {"name": "张", "gender": "男", "nation": "汉族", "minAge": 20, "maxAge": 50},
        {"name": None, "gender": "女", "nation": None, "minAge": None, "maxAge": None},
    ]
    body = queries[count % len(queries)]
    r = timed_request(session, "POST", "/api/resident/search", json_body=body)
    results.append(r)
    return results

def scenario_mixed(session: requests.Session, thread_id: int, count: int) -> list:
    """Mixed business operations - weighted per plan."""
    results = []
    tc = thread_id * 1000 + count  # unique counter

    # 30% resident search
    if tc % 10 < 3:
        r = timed_request(session, "POST", "/api/resident/search",
                         json_body={"name": "测试", "gender": None})
        results.append(r)

    # 15% area tree
    if tc % 10 == 3 or tc % 10 == 4:
        r = timed_request(session, "GET", "/api/area")
        results.append(r)

    # 10% keyperson search
    if tc % 10 == 5:
        r = timed_request(session, "GET", "/api/keyperson/search")
        results.append(r)

    # 10% FP heatmap
    if tc % 10 == 6:
        r = timed_request(session, "GET", "/api/fp/statistics/heatmap")
        results.append(r)

    # 5% missing stats
    if tc % 10 == 7:
        r = timed_request(session, "GET", "/api/missing/statistics")
        results.append(r)

    # 10% audit log query
    if tc % 10 == 8:
        r = timed_request(session, "GET", "/api/log/audit?page=1&size=20")
        results.append(r)

    # 5% alert pending
    if tc % 10 == 9:
        r = timed_request(session, "GET", "/api/alert/pending")
        results.append(r)

    if not results:
        r = timed_request(session, "POST", "/api/resident/search",
                         json_body={"name": "测试"})
        results.append(r)
    return results

def scenario_high_risk(session: requests.Session, thread_id: int, count: int) -> list:
    """High-risk API stress — targets known full-table-scan endpoints."""
    results = []
    tc = thread_id * 1000 + count

    if tc % 4 == 0:
        r = timed_request(session, "GET", "/api/fp/statistics/heatmap")
    elif tc % 4 == 1:
        r = timed_request(session, "GET", "/api/fp/statistics/trend")
    elif tc % 4 == 2:
        r = timed_request(session, "GET", "/api/keyperson/gis")
    else:
        r = timed_request(session, "GET", "/api/keyperson/search")
    results.append(r)
    return results

# ─── Main ────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="PDM Performance Test")
    parser.add_argument("--scene", choices=["login", "search", "mixed", "highrisk", "all"],
                        default="all", help="Test scenario (default: all)")
    parser.add_argument("--users", type=int, default=50, help="Concurrent users (default: 50)")
    parser.add_argument("--duration", type=int, default=60, help="Test duration in seconds (default: 60)")
    parser.add_argument("--ramp-up", type=int, default=10, help="Ramp-up time in seconds (default: 10)")
    parser.add_argument("--output", type=str, default=None, help="Output JSON file path")
    args = parser.parse_args()

    # Verify connectivity
    print("Checking connectivity...")
    try:
        resp = requests.get(f"{BASE_URL}/api/auth/login", timeout=5)
        print(f"  Gateway: OK (status={resp.status_code})")
    except Exception as e:
        print(f"  [ERROR] Cannot reach gateway at {BASE_URL}: {e}")
        sys.exit(1)

    runner = ScenarioRunner(users=args.users, duration_s=args.duration, ramp_up_s=args.ramp_up)

    scenarios = []
    if args.scene in ("login", "all"):
        scenarios.append(("login", scenario_login))
    if args.scene in ("search", "all"):
        scenarios.append(("resident_search", scenario_resident_search))
    if args.scene in ("mixed", "all"):
        scenarios.append(("mixed_business", scenario_mixed))
    if args.scene in ("highrisk", "all"):
        scenarios.append(("high_risk_apis", scenario_high_risk))

    reports = []
    for name, fn in scenarios:
        report = runner.run(name, fn)
        reports.append(report)
        # Print immediate summary
        print(f"\n  ┌─ {name} Results ──────────────────┐")
        print(f"  │ Requests:  {report.total_requests:>8}               │")
        print(f"  │ Success:   {report.success:>8} ({report.success_rate:.1f}%)        │")
        print(f"  │ Failed:    {report.failed:>8}                    │")
        print(f"  │ QPS:       {report.qps:>8.2f}                  │")
        print(f"  │ Mean:      {report.mean:>8.2f} ms               │")
        print(f"  │ P50:       {report.p50:>8.2f} ms               │")
        print(f"  │ P90:       {report.p90:>8.2f} ms               │")
        print(f"  │ P99:       {report.p99:>8.2f} ms               │")
        print(f"  │ P99.9:     {report.p999:>8.2f} ms               │")
        print(f"  └──────────────────────────────────┘")

    # Overall summary
    if len(reports) > 1:
        total_req = sum(r.total_requests for r in reports)
        total_success = sum(r.success for r in reports)
        total_failed = sum(r.failed for r in reports)
        all_times = []
        for r in reports:
            all_times.extend(r.response_times)
        print(f"\n{'='*60}")
        print(f"  OVERALL SUMMARY")
        print(f"{'='*60}")
        print(f"  Total Requests:  {total_req}")
        print(f"  Total Success:   {total_success} ({total_success/max(total_req,1)*100:.1f}%)")
        print(f"  Total Failed:    {total_failed}")
        print(f"  P50: {statistics.median(all_times):.2f}ms  P90: {_p(all_times, 90):.2f}ms  P99: {_p(all_times, 99):.2f}ms")

        # Performance assessment
        p90 = _p(all_times, 90)
        p99 = _p(all_times, 99)
        if p99 < 2000 and p90 < 1000:
            print(f"\n  ✓ Performance: PASS (P99={p99:.0f}ms < 2000ms, P90={p90:.0f}ms < 1000ms)")
        elif p99 < 3000:
            print(f"\n  ⚠ Performance: WARN (P99={p99:.0f}ms, target < 2000ms)")
        else:
            print(f"\n  ✗ Performance: FAIL (P99={p99:.0f}ms, target < 2000ms)")

    # Output JSON
    output = {"reports": [r.to_dict() for r in reports]}
    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(output, f, indent=2, ensure_ascii=False)
        print(f"\n  Results saved to: {args.output}")

    # Return non-zero if performance targets missed
    if reports:
        worst_p99 = max(r.p99 for r in reports)
        if worst_p99 > 2000:
            sys.exit(1)

def _p(times: list, pct: float) -> float:
    sorted_t = sorted(times)
    k = (len(sorted_t) - 1) * pct / 100
    f = int(k)
    c = k - f
    if f + 1 < len(sorted_t):
        return sorted_t[f] + c * (sorted_t[f + 1] - sorted_t[f])
    return sorted_t[f] if sorted_t else 0

if __name__ == "__main__":
    main()
