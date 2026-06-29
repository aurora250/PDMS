#!/usr/bin/env python3
"""
PDM 性能基准测试脚本 v3
模拟多并发用户执行核心业务操作，测量响应时间和吞吐量。
用法:
  python scripts/perf-test.py                    # 运行全部场景
  python scripts/perf-test.py --scene mixed      # 仅混合场景
  python scripts/perf-test.py --users 30 --duration 120  # 自定义参数
"""
import requests
import time
import json
import statistics
import sys
import os
import argparse
import threading
import resource
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from typing import Optional

BASE_URL = "http://127.0.0.1:8080"
TEST_USER = {"username": "admin", "password": "Admin@123"}


def apply_resource_limits(cpu_cores: int = 0, max_memory_mb: int = 0):
    """限制进程可用的 CPU 核心数和最大内存。

    Args:
        cpu_cores: 允许使用的 CPU 核心数（0 = 不限制）。Linux 下通过 sched_setaffinity 实现。
        max_memory_mb: 最大虚拟内存（MB，0 = 不限制）。通过 setrlimit(RLIMIT_AS) 实现，
                       超出后进程收到 SIGSEGV。
    """
    if cpu_cores > 0 and hasattr(os, "sched_setaffinity"):
        try:
            cpu_count = os.cpu_count() or 1
            cores = list(range(min(cpu_cores, cpu_count)))
            os.sched_setaffinity(0, cores)
            print(f"  🔒 CPU affinity: cores {cores}")
        except Exception as e:
            print(f"  ⚠️  无法设置 CPU affinity: {e}")

    if max_memory_mb > 0:
        limit_bytes = max_memory_mb * 1024 * 1024
        try:
            resource.setrlimit(resource.RLIMIT_AS, (limit_bytes, limit_bytes))
            print(f"  🔒 Memory limit: {max_memory_mb} MB")
        except Exception as e:
            print(f"  ⚠️  无法设置内存限制: {e}")


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
    def p75(self) -> float:
        return self._percentile(75) if self.response_times else 0

    @property
    def p90(self) -> float:
        return self._percentile(90) if self.response_times else 0

    @property
    def p95(self) -> float:
        return self._percentile(95) if self.response_times else 0

    @property
    def p99(self) -> float:
        return self._percentile(99) if self.response_times else 0

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
        return sorted_times[f] if sorted_times else 0

    def to_dict(self) -> dict:
        return {
            "scenario": self.name,
            "total_requests": self.total_requests,
            "success": self.success,
            "failed": self.failed,
            "success_rate": f"{self.success_rate:.2f}%",
            "duration_s": f"{self.duration_s:.1f}",
            "qps": f"{self.qps:.2f}",
            "mean_ms": f"{self.mean:.1f}",
            "p50_ms": f"{self.p50:.1f}",
            "p75_ms": f"{self.p75:.1f}",
            "p95_ms": f"{self.p95:.1f}",
            "p99_ms": f"{self.p99:.1f}",
        }


def login(session: requests.Session) -> str:
    """Login and return access token."""
    resp = session.post(
        f"{BASE_URL}/api/auth/login",
        json=TEST_USER,
        timeout=15,
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
        else:
            return RequestResult(name=path, status=0, elapsed_ms=0, error=f"Unknown method: {method}")
        elapsed = (time.perf_counter() - start) * 1000
        return RequestResult(name=path, status=resp.status_code, elapsed_ms=elapsed)
    except Exception as e:
        elapsed = (time.perf_counter() - start) * 1000
        return RequestResult(name=path, status=0, elapsed_ms=elapsed, error=str(e)[:100])


class ScenarioRunner:
    def __init__(self, users: int, duration_s: int, ramp_up_s: int = 10):
        self.users = users
        self.duration_s = duration_s
        self.ramp_up_s = ramp_up_s
        self.running = True

    def _worker(self, thread_id: int, report: ScenarioReport,
                auth_header: dict, scenario_fn):
        """Worker function running requests in a loop."""
        session = requests.Session()
        session.headers.update(auth_header)
        count = 0

        if self.ramp_up_s > 0 and self.users > 1:
            delay = (thread_id / (self.users - 1)) * self.ramp_up_s
            time.sleep(delay)

        consecutive_errors = 0
        while self.running:
            try:
                results = scenario_fn(session, thread_id, count)
                consecutive_errors = 0
            except Exception as e:
                results = [RequestResult(
                    name="worker_error", status=0, elapsed_ms=0,
                    error=f"[t{thread_id}] {type(e).__name__}: {e}"
                )]
                consecutive_errors += 1
                if consecutive_errors > 5:
                    time.sleep(2.0)
                    consecutive_errors = 0

            for r in results if isinstance(results, list) else [results]:
                report.response_times.append(r.elapsed_ms)
                report.total_requests += 1
                if r.status == 200 or r.status == 201:
                    report.success += 1
                else:
                    report.failed += 1
                    if r.error:
                        report.errors.append(f"[{r.name}] {r.error}")
                    else:
                        report.errors.append(f"[{r.name}] HTTP {r.status}")
            count += 1

            # 思考时间：模拟真实用户
            think_ms = 0.05 + (thread_id % 4) * 0.05  # 50-200ms
            time.sleep(think_ms)

    def run(self, name: str, scenario_fn) -> ScenarioReport:
        """Launch workers and measure performance."""
        report = ScenarioReport(name=name)
        print(f"\n{'='*65}")
        print(f"  {name} — {self.users} users × {self.duration_s}s (ramp-up {self.ramp_up_s}s)")
        print(f"{'='*65}")

        # Get auth token
        session = requests.Session()
        try:
            token = login(session)
            print(f"  ✅ Login OK")
        except Exception as e:
            print(f"  ❌ Login failed: {e}")
            report.errors.append(f"Login failed: {e}")
            return report
        auth_header = {"Authorization": f"Bearer {token}"}
        self.running = True

        start_time = time.perf_counter()

        with ThreadPoolExecutor(max_workers=self.users) as executor:
            futures = [
                executor.submit(self._worker, i, report, auth_header, scenario_fn)
                for i in range(self.users)
            ]

            time.sleep(self.duration_s)
            self.running = False

            for f in as_completed(futures, timeout=15):
                try:
                    f.result()
                except Exception:
                    pass

        report.duration_s = time.perf_counter() - start_time
        report.qps = report.total_requests / report.duration_s if report.duration_s > 0 else 0
        return report


# ─── v3 场景定义 ────────────────────────────────────────────

def scenario_resident_search(session: requests.Session, tid: int, cnt: int) -> list:
    """搜索负载：70% 无条件 + 30% 过滤条件（测试缓存命中率）"""
    if cnt % 10 < 7:
        body = {"page": 1, "size": 20}
    else:
        body = {"page": 1, "size": 20, "gender": "男", "nation": "汉族"}
    r = timed_request(session, "POST", "/api/resident/search", json_body=body)
    return [r]


def scenario_mixed(session: requests.Session, tid: int, cnt: int) -> list:
    """混合业务负载 — 与 Gatling/JMeter v3 权重一致"""
    tc = tid * 1000 + cnt
    m = tc % 100

    if m < 30:
        r = timed_request(session, "POST", "/api/resident/search",
                          json_body={"page": 1, "size": 20})
    elif m < 45:
        r = timed_request(session, "GET", "/api/area")
    elif m < 57:
        r = timed_request(session, "GET", "/api/keyperson/search?page=1&size=20")
    elif m < 69:
        r = timed_request(session, "GET", "/api/fp/statistics/heatmap")
    elif m < 77:
        r = timed_request(session, "GET", "/api/missing/statistics")
    elif m < 89:
        r = timed_request(session, "GET", "/api/log/audit?page=1&size=20")
    else:
        r = timed_request(session, "POST", "/api/auth/login", json_body=TEST_USER)
    return [r]


def scenario_stress(session: requests.Session, tid: int, cnt: int) -> list:
    """压力测试 — 高频调用核心 API，最小思考时间"""
    tc = tid * 1000 + cnt
    m = tc % 100

    if m < 40:
        r = timed_request(session, "POST", "/api/resident/search",
                          json_body={"page": 1, "size": 20})
    elif m < 60:
        r = timed_request(session, "GET", "/api/area")
    elif m < 75:
        r = timed_request(session, "GET", "/api/fp/statistics/heatmap")
    elif m < 90:
        r = timed_request(session, "GET", "/api/log/audit?page=1&size=20")
    else:
        r = timed_request(session, "POST", "/api/auth/login", json_body=TEST_USER)
    return [r]


# ─── 按 API 分类统计 ──────────────────────────────────────────

class ApiBreakdown:
    """收集每个 API 的独立统计"""
    def __init__(self):
        self.lock = threading.Lock()
        self.apis = {}

    def record(self, name, duration_ms, error=None):
        with self.lock:
            if name not in self.apis:
                self.apis[name] = {"times": [], "errors": 0, "success": 0}
            self.apis[name]["times"].append(duration_ms)
            if error:
                self.apis[name]["errors"] += 1
            else:
                self.apis[name]["success"] += 1

    def summary(self):
        result = {}
        for name, data in sorted(self.apis.items()):
            times = sorted(data["times"])
            total = len(times)
            if total == 0:
                continue
            result[name] = {
                "count": total,
                "success": data["success"],
                "errors": data["errors"],
                "mean_ms": round(statistics.mean(times), 1),
                "p50_ms": round(times[int(total * 0.50)] if times else 0, 1),
                "p95_ms": round(times[min(int(total * 0.95), total - 1)] if times else 0, 1),
                "p99_ms": round(times[min(int(total * 0.99), total - 1)] if times else 0, 1),
            }
        return result


# ─── Main ────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="PDM Performance Test v3")
    parser.add_argument("--scene", choices=["search", "mixed", "stress", "all"],
                        default="all", help="Test scenario")
    parser.add_argument("--users", type=int, default=20,
                        help="Concurrent users per scenario")
    parser.add_argument("--duration", type=int, default=120,
                        help="Test duration in seconds")
    parser.add_argument("--ramp-up", type=int, default=10,
                        help="Ramp-up time in seconds")
    parser.add_argument("--output", type=str, default=None,
                        help="Output JSON file path")
    parser.add_argument("--cpu-cores", type=int, default=0,
                        help="Restrict to N CPU cores (0 = no limit, Linux only)")
    parser.add_argument("--max-memory", type=int, default=0,
                        help="Max virtual memory in MB (0 = no limit)")
    args = parser.parse_args()

    # 应用资源限制（必须在创建线程池之前）
    apply_resource_limits(cpu_cores=args.cpu_cores, max_memory_mb=args.max_memory)

    print("=" * 65)
    print("  PDM 性能基准测试 v3")
    print(f"  Target: {BASE_URL}")
    print(f"  Time:   {time.strftime('%Y-%m-%d %H:%M:%S')}")
    print("=" * 65)

    # 连通性检查
    print("\n>>> 连通性检查...")
    try:
        resp = requests.get(f"{BASE_URL}/api/auth/login", timeout=10)
        print(f"  Gateway: OK (status={resp.status_code})")
    except Exception as e:
        print(f"  ❌ 无法连接 Gateway at {BASE_URL}: {e}")
        sys.exit(1)

    # 快速登录验证
    sess = requests.Session()
    try:
        token = login(sess)
        print(f"  Login:   OK — token obtained")
    except Exception as e:
        print(f"  ❌ Login failed: {e}")
        sys.exit(1)

    runner = ScenarioRunner(
        users=args.users, duration_s=args.duration, ramp_up_s=args.ramp_up
    )

    scenarios = []
    if args.scene in ("search", "all"):
        scenarios.append(("01-Search-Load", scenario_resident_search))
    if args.scene in ("mixed", "all"):
        scenarios.append(("02-Mixed-Load", scenario_mixed))
    if args.scene in ("stress", "all"):
        scenarios.append(("03-Stress-Test", scenario_stress))

    reports = []
    for name, fn in scenarios:
        report = runner.run(name, fn)
        reports.append(report)

        # 实时输出摘要
        d = report.to_dict()
        print(f"\n  ┌─ {name} ─────────────────────────────────────┐")
        print(f"  │ Total: {report.total_requests:>6} | QPS: {report.qps:>7.1f}                 │")
        print(f"  │ Mean:  {d['mean_ms']:>7}ms | P50: {d['p50_ms']:>7}ms               │")
        print(f"  │ P95:   {d['p95_ms']:>7}ms | P99: {d['p99_ms']:>7}ms               │")
        print(f"  │ Success: {report.success_rate:>5.1f}%  | Errors: {report.failed:>6}                │")
        print(f"  └──────────────────────────────────────────────┘")

    # ─── 汇总 ───
    if len(reports) > 1:
        total_req = sum(r.total_requests for r in reports)
        total_ok = sum(r.success for r in reports)
        total_err = sum(r.failed for r in reports)
        all_times = []
        for r in reports:
            all_times.extend(r.response_times)

        sorted_t = sorted(all_times) if all_times else [0]
        n = len(sorted_t)

        print(f"\n{'='*65}")
        print(f"  总 体 汇 总")
        print(f"{'='*65}")
        print(f"  总请求数:      {total_req}")
        print(f"  成功数:        {total_ok}")
        print(f"  失败数:        {total_err}")
        print(f"  成功率:        {total_ok/max(total_req,1)*100:.2f}%")
        print(f"  总体 QPS:      {total_req/max(r.duration_s for r in reports):.1f}")
        if n > 0:
            print(f"  P50 响应时间:  {sorted_t[int(n*0.50)]:.1f}ms")
            print(f"  P75 响应时间:  {sorted_t[int(n*0.75)]:.1f}ms")
            print(f"  P95 响应时间:  {sorted_t[min(int(n*0.95), n-1)]:.1f}ms")
            print(f"  P99 响应时间:  {sorted_t[min(int(n*0.99), n-1)]:.1f}ms")
            print(f"  平均响应时间:  {statistics.mean(all_times):.1f}ms")

        # 性能评估
        p99_val = sorted_t[min(int(n * 0.99), n - 1)] if n > 0 else 0
        p95_val = sorted_t[min(int(n * 0.95), n - 1)] if n > 0 else 0
        print(f"\n  性能评估:")
        for check_name, target, actual in [
            ("成功率 > 97%", 97.0, total_ok / max(total_req, 1) * 100),
            ("P99 < 2000ms", 2000.0, p99_val),
            ("P95 < 1500ms", 1500.0, p95_val),
        ]:
            passed = actual >= target if "成功率" in check_name else actual <= target
            mark = "✅ PASS" if passed else "❌ FAIL"
            print(f"    {mark}  {check_name}  (target={target}, actual={actual:.1f})")

    # 输出每个场景的 JSON
    output_data = {"reports": [r.to_dict() for r in reports]}
    if args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            json.dump(output_data, f, indent=2, ensure_ascii=False)
        print(f"\n  📄 Report saved to: {args.output}")

    # 输出 JSON 用于报告更新
    print("\n--- JSON ---")
    print(json.dumps(output_data, indent=2, ensure_ascii=False))

    # 返回码：性能不达标则退出非 0
    if n > 0 and p99_val > 2000:
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
