#!/usr/bin/env python3
"""
PDM 人口数据库管理系统 — 全量 API 自动化测试
测试所有 60 个 REST API 端点
用法: python3 scripts/test-api.py [base-url]
"""

import json
import sys
import time
import uuid
import urllib.request
import urllib.error
import urllib.parse
import ssl
from typing import Any, Optional

# ============================================================
# 配置
# ============================================================
BASE_URL = sys.argv[1] if len(sys.argv) > 1 else "http://localhost:8080"
TOKEN = ""
REFRESH_TOKEN = ""
USER_UUID = ""
TEST_TAG = str(uuid.uuid4())[:12]

def new_uuid() -> str:
    """Generate a valid UUID string for PostgreSQL UUID type"""
    return str(uuid.uuid4())

RESIDENT_UUID = new_uuid()
SPOUSE_UUID = new_uuid()

def generate_valid_id_card(area: str = "110101", birth: str = "19900101") -> str:
    """生成通过 GB 11643-1999 校验的合法身份证号"""
    import random
    seq = str(random.randint(0, 999)).zfill(3)
    base = f"{area}{birth}{seq}"
    weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
    check_chars = "10X98765432"
    s = sum(int(base[i]) * weights[i] for i in range(17))
    return base + check_chars[s % 11]

RESIDENT_IDCARD = generate_valid_id_card("110101", "19900101")
SPOUSE_IDCARD = generate_valid_id_card("110101", "19920202")

PASS = 0
FAIL = 0
SKIP = 0
RESULTS: list[dict] = []

# 禁用 SSL 验证（本地测试）
SSL_CTX = ssl.create_default_context()
SSL_CTX.check_hostname = False
SSL_CTX.verify_mode = ssl.CERT_NONE

# ============================================================
# HTTP 工具
# ============================================================

def _req(method: str, path: str, body: Any = None, headers: dict = None) -> tuple[int, dict]:
    """发送 HTTP 请求，返回 (status_code, response_json)。
    对含中文的 URL 自动进行编码。"""
    # 分离路径和查询参数，对中文进行 URL 编码
    if "?" in path:
        path_part, query_part = path.split("?", 1)
        encoded_query = urllib.parse.quote(query_part, safe='&=')
        url = f"{BASE_URL}{path_part}?{encoded_query}"
    else:
        url = f"{BASE_URL}{path}"

    data = json.dumps(body).encode("utf-8") if body is not None else None

    hdrs = {"Content-Type": "application/json"}
    if TOKEN:
        hdrs["Authorization"] = f"Bearer {TOKEN}"
    if headers:
        hdrs.update(headers)

    req = urllib.request.Request(url, data=data, headers=hdrs, method=method)
    try:
        with urllib.request.urlopen(req, timeout=30, context=SSL_CTX) as resp:
            raw = resp.read()
            if not raw:
                return resp.status, {}
            try:
                return resp.status, json.loads(raw)
            except json.JSONDecodeError:
                # 非JSON响应（如导出文件），只要HTTP 200就算成功
                return resp.status, {"code": 200, "message": f"binary {len(raw)} bytes"}
    except urllib.error.HTTPError as e:
        raw = e.read() if e.fp else b""
        try:
            return e.code, json.loads(raw) if raw else {}
        except json.JSONDecodeError:
            return e.code, {"message": raw.decode("utf-8", errors="replace")[:200] if raw else str(e)}
    except Exception as e:
        return 0, {"error": str(e)}


def get(path: str) -> tuple[int, dict]:
    return _req("GET", path)


def post(path: str, body: Any = None) -> tuple[int, dict]:
    return _req("POST", path, body)


def put(path: str, body: Any = None) -> tuple[int, dict]:
    return _req("PUT", path, body)


def delete(path: str) -> tuple[int, dict]:
    return _req("DELETE", path)


# ============================================================
# 测试框架
# ============================================================

def test(name: str, method: str, path: str, body: Any = None,
         want_code: int = 200, extract: str = None, skip: bool = False) -> Optional[Any]:
    """执行单个 API 测试，返回提取的字段值或 None"""
    global PASS, FAIL, SKIP

    if skip:
        SKIP += 1
        RESULTS.append({"name": name, "method": method, "path": path, "status": "SKIP"})
        print(f"  ⬜ SKIP  {method:6s} {path}")
        return None

    if body is not None:
        code, resp = _req(method, path, body)
    else:
        code, resp = _req(method, path)

    success = (code == want_code and resp.get("code") == 200)

    if success:
        PASS += 1
        RESULTS.append({"name": name, "path": path, "status": "PASS"})
        print(f"  ✅ PASS  {method:6s} {path}")
    else:
        FAIL += 1
        err_msg = resp.get("message", str(resp))[:120]
        RESULTS.append({"name": name, "path": path, "status": "FAIL",
                         "http": code, "response": err_msg})
        print(f"  ❌ FAIL  {method:6s} {path}  (HTTP {code}, {err_msg})")

    if extract and success:
        return _extract(resp, extract)
    return None


def _extract(resp: dict, path: str) -> Any:
    """从 JSON 响应中提取嵌套字段，如 'data.id'"""
    parts = path.split(".")
    val = resp
    for p in parts:
        if isinstance(val, dict):
            val = val.get(p)
        elif isinstance(val, list) and p.isdigit():
            val = val[int(p)] if int(p) < len(val) else None
        else:
            return None
    return val


def section(title: str):
    print(f"\n{'─'*60}")
    print(f"  {title}")
    print(f"{'─'*60}")


# ============================================================
# 检查环境
# ============================================================

print(f"\n{'='*60}")
print(f" PDM API 全量自动化测试")
print(f" 网关: {BASE_URL}")
print(f" 时间: {time.strftime('%Y-%m-%d %H:%M:%S')}")
print(f"{'='*60}")

# 连通性检查 & 登录
print("检查网关连通性...", end=" ")
code, resp = post("/api/auth/login", {"username": "admin", "password": "Admin@123"})
# 登录可能因为前次测试修改了角色或 must_change_password 而返回业务异常
# 仍然尝试解析 token，如果失败则记录并继续
if resp.get("code") == 200 and resp.get("data", {}).get("accessToken"):
    TOKEN = resp["data"]["accessToken"]
    REFRESH_TOKEN = resp["data"].get("refreshToken", "")
    USER_UUID = resp["data"]["userUuid"]
    print("OK")
else:
    print(f"(HTTP {code}, code={resp.get('code')}, msg={resp.get('message','')[:60]})")
    print("  注意: 登录异常，后续需认证的端点将失败。")
    print("  可能原因: 前次测试修改了 admin 角色或密码状态。")
    print("  解决: ./scripts/reset-database.sh 重置数据库后重试")

# ============================================================
# 模块 1: pdm-auth — 认证与用户管理 (18 端点)
# ============================================================

section("模块 1/8: pdm-auth — 认证与用户管理")

# 1.1 登录
print("\n── 1.1-1.3 登录认证 ──")
code, resp = post("/api/auth/login", {"username": "admin", "password": "Admin@123"})
if resp.get("code") == 200 and resp.get("data", {}).get("accessToken"):
    TOKEN = resp["data"]["accessToken"]
    REFRESH_TOKEN = resp["data"].get("refreshToken", "")
    USER_UUID = resp["data"]["userUuid"]
    PASS += 1
    RESULTS.append({"name": "登录 - 获取Token", "path": "/api/auth/login", "status": "PASS"})
    print(f"  ✅ PASS  POST   /api/auth/login  (role={resp['data'].get('role','?')})")
else:
    FAIL += 1
    msg = resp.get("message", str(resp))[:80]
    RESULTS.append({"name": "登录 - 获取Token", "path": "/api/auth/login", "status": "FAIL",
                     "http": code, "response": msg})
    print(f"  ❌ FAIL  POST   /api/auth/login  (HTTP {code}, {msg})")

# 1.2 刷新 Token (仅当有 refreshToken 时)
if REFRESH_TOKEN:
    code2, resp2 = post("/api/auth/refresh", {"refreshToken": REFRESH_TOKEN})
    if code2 == 200 and resp2.get("data", {}).get("accessToken"):
        TOKEN = resp2["data"]["accessToken"]
        test("刷新Token", "POST", "/api/auth/refresh", {"refreshToken": REFRESH_TOKEN}, want_code=200)
    else:
        test("刷新Token", "POST", "/api/auth/refresh", {"refreshToken": REFRESH_TOKEN}, want_code=200)

# 1.3 修改密码（弱密码应被正确拒绝，业务码 2010=密码强度不足）
code3, resp3 = put("/api/auth/change-password",
     {"oldPassword": "Admin@123", "newPassword": "123"})
if resp3.get("code") == 2010:
    PASS += 1
    RESULTS.append({"name": "修改密码 - 弱密码正确拒绝", "path": "/api/auth/change-password", "status": "PASS"})
    print(f"  ✅ PASS  PUT    /api/auth/change-password  (code=2010 密码强度不足 ✓)")
else:
    FAIL += 1
    RESULTS.append({"name": "修改密码 - 弱密码拒绝", "path": "/api/auth/change-password", "status": "FAIL",
                     "http": code3, "response": str(resp3)[:100]})
    print(f"  ❌ FAIL  PUT    /api/auth/change-password  (期望 2010, 实际 {resp3.get('code')})")

# 1.4 权限组列表
print("\n── 1.4-1.7 权限组管理 ──")
pg_id = test("获取权限组列表", "GET", "/api/auth/permission-groups",
             extract="data.0.groupId")

# 1.5 创建权限组
pg_id = test("创建权限组", "POST", "/api/auth/permission-groups",
              {"groupName": f"测试权限组-{TEST_TAG}",
               "description": "自动化测试",
               "permissions": '["resident:read","resident:write"]'},
              extract="data.groupId")

# 1.6 修改权限组
if pg_id:
    test("修改权限组", "PUT", f"/api/auth/permission-groups/{pg_id}",
         {"groupName": f"测试权限组-{TEST_TAG}",
          "description": "已修改",
          "permissions": '["resident:read","resident:write","household:read"]'})

# 1.7 删除权限组
if pg_id:
    test("删除权限组", "DELETE", f"/api/auth/permission-groups/{pg_id}")

# 1.8-1.12 民警管理
print("\n── 1.8-1.12 民警管理 ──")
police_no = f"P-{TEST_TAG}"
test("新增民警", "POST", "/api/auth/police",
     {"policeNumber": police_no, "userUuid": "",
      "residentUuid": "00000000-0000-0000-0000-000000000001",
      "policeStation": "测试派出所", "jurisdiction": "测试辖区",
      "areaId": None, "department": "治安大队",
      "policeRank": "警司", "dutyStatus": "在岗"})

test("分页查询民警", "GET", f"/api/auth/police?page=1&size=20")

test("按警号查询民警", "GET", f"/api/auth/police/{police_no}")

test("修改民警信息", "PUT", f"/api/auth/police/{police_no}",
     {"policeStation": "测试派出所(已修改)", "jurisdiction": "扩大辖区",
      "department": "刑侦大队", "policeRank": "警督", "dutyStatus": "在岗"})

test("修改民警执勤状态", "PUT", f"/api/auth/police/{police_no}/status",
     {"dutyStatus": "调岗"})

# 1.13-1.17 用户管理
print("\n── 1.13-1.17 用户管理 ──")
test("分页查询用户", "GET", "/api/auth/users?page=1&size=20")

test("按UUID查询用户", "GET", f"/api/auth/users/{USER_UUID}")

test("修改用户信息", "PUT", f"/api/auth/users/{USER_UUID}",
     {"phone": "13800138000", "email": "test@example.com",
      "userRole": "采集员", "permissionGroupId": 1})

test("修改用户状态", "PUT", f"/api/auth/users/{USER_UUID}/status",
     {"status": "有效"})

# 恢复管理员角色，避免后续 403
test("恢复管理员角色", "PUT", f"/api/auth/users/{USER_UUID}",
     {"phone": "13800138000", "email": "test@example.com",
      "userRole": "系统管理员", "permissionGroupId": 1})

# 1.18 登出
print("\n── 1.18 登出 ──")
test("用户登出", "POST", "/api/auth/logout", {})

# 重新登录
code, resp = post("/api/auth/login", {"username": "admin", "password": "Admin@123"})
if resp.get("code") == 200:
    TOKEN = resp["data"]["accessToken"]

# ============================================================
# 模块 2: pdm-resident — 常住人口管理 (10 端点)
# ============================================================

section("模块 2/8: pdm-resident — 常住人口管理")

resident_uuid = RESIDENT_UUID
id_card = RESIDENT_IDCARD

# 2.1 新增常住人口
print("\n── 2.1-2.3 常住人口 CRUD ──")
test("新增常住人口", "POST", "/api/resident",
     {"uuid": resident_uuid, "name": "测试张三", "formerName": "",
      "gender": "男", "idCardNo": id_card,
      "nation": "汉族", "birthDate": "1990-01-01",
      "educationLevel": "本科", "bloodType": "A",
      "maritalStatus": "未婚", "occupation": "工程师",
      "phone": "13800138001", "photo": "",
      "residence": "北京市东城区测试路1号", "areaId": None,
      "householdType": "居民户口", "householdStatus": "正常",
      "householdAddress": "北京市东城区测试路1号",
      "householdAreaId": None})

# 2.2 查询
test("根据UUID查询人口", "GET", f"/api/resident/{resident_uuid}")

# 2.3 修改
test("修改人口信息", "PUT", f"/api/resident/{resident_uuid}",
     {"phone": "13900139001", "educationLevel": "硕士研究生",
      "occupation": "高级工程师", "residence": "北京市东城区测试路2号"})

# 2.4 多条件搜索
print("\n── 2.4 搜索 ──")
test("多条件搜索人口", "POST", "/api/resident/search",
     {"name": "测试张三", "gender": "男", "nation": "汉族",
      "educationLevel": "", "maritalStatus": "", "householdStatus": "",
      "idCardNo": "", "minAge": None, "maxAge": None,
      "page": 1, "size": 20})

# 2.5-2.6 家庭关系
print("\n── 2.5-2.6 家庭关系 ──")
spouse_uuid = SPOUSE_UUID
spouse_card = SPOUSE_IDCARD

# 创建配偶
test("创建配偶(前置)", "POST", "/api/resident",
     {"uuid": spouse_uuid, "name": "测试李四", "formerName": "",
      "gender": "女", "idCardNo": spouse_card,
      "nation": "汉族", "birthDate": "1992-02-02",
      "educationLevel": "本科", "bloodType": "B",
      "maritalStatus": "已婚", "occupation": "教师",
      "phone": "13800138002", "photo": "",
      "residence": "北京市东城区测试路1号", "areaId": None,
      "householdType": "居民户口", "householdStatus": "正常",
      "householdAddress": "北京市东城区测试路1号",
      "householdAreaId": None})

test("添加人口家庭关系", "POST", f"/api/resident/{resident_uuid}/relations",
     {"relationPersonUuid": resident_uuid,
      "fatherUuid": "", "motherUuid": "",
      "spouseUuid": spouse_uuid})

test("查询人口家庭关系", "GET", f"/api/resident/{resident_uuid}/relations")

# 2.7-2.8 变更申请
print("\n── 2.7-2.8 信息变更申请 ──")
change_rid = test("提交人口变更申请", "POST", "/api/resident/change-request",
     {"applicantUuid": resident_uuid,
      "changeField": "educationLevel",
      "originalData": "本科", "modifiedData": "硕士研究生",
      "status": "请求"}, extract="data.rid")

if change_rid:
    test("审批变更申请 - 通过", "PUT",
         f"/api/resident/change-request/{change_rid}/approve?status=通过",
         {})

# 2.9 批量导入
print("\n── 2.9 批量导入 ──")
test("批量导入人口(无文件)", "POST", "/api/resident/import",
     want_code=200, skip=True)  # multipart, skip for now

# 2.10 删除
print("\n── 2.10 删除 ──")
test("删除常住人口", "DELETE", f"/api/resident/{resident_uuid}")

# ============================================================
# 模块 3: pdm-household — 户籍管理 (11 端点)
# ============================================================

section("模块 3/8: pdm-household — 户籍管理")

# 3.1 查询行政区划
print("\n── 3.1 行政区划 ──")
test("查询顶级行政区划", "GET", "/api/area")

# 3.2-3.4 户口簿管理
print("\n── 3.2-3.4 户口簿 ──")
book_no = f"HB-{TEST_TAG}"
test("申请户口簿", "POST", "/api/household/book/apply",
     {"householdBookNo": book_no, "householderUuid": resident_uuid,
      "establishDate": "2026-06-23", "hukouAddress": "北京市东城区测试路1号",
      "hukouAreaId": None, "status": "有效",
      "memberUuidList": resident_uuid})

test("补办户口簿", "POST", "/api/household/book/reissue",
     {"bookNo": book_no})

test("换发户口簿", "POST", "/api/household/book/renew",
     {"bookNo": book_no})

# 3.5-3.6 户籍业务
print("\n── 3.5-3.6 户籍业务 ──")
biz_rid = test("提交户籍业务申请", "POST", "/api/household/business",
     {"handlerUuid": "", "applicantUuid": resident_uuid,
      "attachment": "[]", "businessType": "登记",
      "handleDate": "2026-06-23",
      "handleBasis": "《户口登记条例》第7条",
      "fee": 0.00, "status": "审批中",
      "rejectReason": "", "remark": "自动化测试"},
     extract="data.rid")

if biz_rid:
    test("审批户籍业务-批准", "PUT", f"/api/household/business/{biz_rid}/approve",
         {"status": "批准", "rejectReason": ""})

# 3.7-3.9 户籍迁移
print("\n── 3.7-3.9 户籍迁移 ──")
mig_rid = test("提交户籍迁移申请", "POST", "/api/household/migration",
     {"handlerUuid": "", "applicantUuid": resident_uuid,
      "incomingAddress": "上海市浦东新区测试路100号",
      "incomingAreaId": None,
      "outgoingAddress": "北京市东城区测试路1号",
      "outgoingAreaId": None,
      "attachment": "[]", "businessType": "市内",
      "handleDate": "2026-06-23",
      "handleBasis": "《户口登记条例》第10条",
      "fee": 5.00, "status": "准迁证审批中",
      "rejectReason": "", "approvalPermitNo": "",
      "migrationPermitNo": "", "remark": "自动化测试"},
     extract="data.rid")

if mig_rid:
    test("审批迁移-通过", "PUT", f"/api/household/migration/{mig_rid}/approve",
         {"status": "迁移审批通过", "rejectReason": ""})

test("查询人口迁移轨迹", "GET", f"/api/household/migration/trace/{resident_uuid}")

# 3.10-3.11 证件管理
print("\n── 3.10-3.11 证件管理 ──")
test("申领准迁证", "POST", "/api/household/approval-permit",
     {"permitNo": f"AP-{TEST_TAG}", "issueDate": "2026-06-23",
      "expiryDate": "2026-09-23",
      "issuingAuthority": "北京市公安局测试分局",
      "status": "有效"})

test("申领迁移证", "POST", "/api/household/migration-permit",
     {"permitNo": f"MP-{TEST_TAG}", "issueDate": "2026-06-23",
      "expiryDate": "2026-07-23",
      "outgoingPoliceStation": "测试派出所",
      "status": "有效"})

# ============================================================
# 模块 4: pdm-keyperson — 重点人员管理 (8 端点)
# ============================================================

section("模块 4/8: pdm-keyperson — 重点人员管理")

kp_uuid = new_uuid()

# 4.1 新增重点人员
print("\n── 4.1-4.3 重点人员 CRUD ──")
test("新增重点人员", "POST", "/api/keyperson/",
     {"uuid": kp_uuid, "controlLevel": "一级",
      "controlType": "信访重点人员",
      "designatedAt": "2026-06-23T10:00:00",
      "responsiblePoliceNo": police_no})

test("修改管控级别", "PUT", f"/api/keyperson/{kp_uuid}",
     {"controlLevel": "二级"})

test("搜索重点人员", "GET",
     f"/api/keyperson/search?controlLevel=二级&controlType=信访重点人员")

# 4.4-4.5 走访计划
print("\n── 4.4-4.5 走访计划 ──")
vp_id = test("制定走访计划", "POST", "/api/keyperson/visit-plan",
     {"keyPersonUuid": kp_uuid, "plannedDate": "2026-07-01",
      "actualDate": None, "visitType": "入户走访",
      "status": "待走访", "assignedPoliceNo": police_no,
      "isAlerted": 0},
     extract="data.planId")

if vp_id:
    test("执行走访计划", "PUT", f"/api/keyperson/visit-plan/{vp_id}",
         {"actualDate": "2026-07-01",
          "petitionRecord": "人员情绪稳定"})

# 4.6 信访记录
print("\n── 4.6 信访记录 ──")
test("登记信访记录", "POST", "/api/keyperson/petition",
     {"keyPersonUuid": kp_uuid, "handlerPoliceNo": police_no,
      "petitionTime": "2026-06-23T14:30:00",
      "address": "区政府门口",
      "remark": "因拆迁补偿问题上访",
      "evaluation": "一般"})

# 4.7 GIS
print("\n── 4.7 GIS ──")
test("获取重点人员GIS数据", "GET", "/api/keyperson/gis")

# 4.8 撤销管控
print("\n── 4.8 撤销管控 ──")
test("撤销重点人员管控", "DELETE", f"/api/keyperson/{kp_uuid}")

# ============================================================
# 模块 5: pdm-floating-population — 流动人口管理 (12 端点)
# ============================================================

section("模块 5/8: pdm-floating-population — 流动人口管理")

fp_uuid = new_uuid()

# 5.1-5.3 流动人口登记
print("\n── 5.1-5.3 流动人口登记 ──")
fp_rid = test("流动人口登记", "POST", "/api/fp/register",
     {"residencePermitNo": "", "uuid": fp_uuid,
      "agentUuid": "", "attachment": "[]",
      "reviewerUuid": "", "rejectReason": "",
      "registerDate": "2026-06-23", "reviewDate": None},
     extract="data.rid")

if fp_rid:
    test("修改流动人口登记", "PUT", f"/api/fp/register/{fp_rid}",
         {"attachment": "[\"file1.pdf\"]",
          "registerDate": "2026-06-23"})

# 5.4-5.5 居住登记
print("\n── 5.4-5.5 居住登记 ──")
reg_rid = test("居住登记", "POST", "/api/fp/residence/register",
     {"uuid": fp_uuid,
      "originalAddress": "测试省测试市测试县测试村",
      "currentAddress": "北京市朝阳区测试小区3号楼501",
      "areaId": None, "addressType": "租赁房屋",
      "houseOwnership": "租赁-整租", "purpose": "务工",
      "expectedDuration": "短租",
      "workUnit": "测试科技有限公司",
      "registerDate": "2026-06-23"},
     extract="data.rid")

if reg_rid:
    test("修改居住登记", "PUT", f"/api/fp/residence/{reg_rid}", {})

# 5.6-5.10 居住证管理
print("\n── 5.6-5.10 居住证管理 ──")
permit_id = test("申领居住证", "POST", "/api/fp/permit/apply",
     {"permitNo": f"RP-{TEST_TAG}", "uuid": fp_uuid,
      "issueDate": None, "expiryDate": "2027-06-23",
      "status": "有效"},
     extract="data.id")

if permit_id:
    test("审批居住证", "PUT", f"/api/fp/permit/{permit_id}/approve",
         {}, want_code=200)

    test("制发居住证", "PUT", f"/api/fp/permit/{permit_id}/issue",
         {}, want_code=200)

    test("续期居住证", "POST", f"/api/fp/permit/{permit_id}/renew",
         {"permitNo": f"RP-{TEST_TAG}",
          "oldExpiryDate": "2027-06-23",
          "newExpiryDate": "2028-06-23",
          "renewalDate": "2026-06-23",
          "operatorUuid": USER_UUID,
          "remark": "第二次续期"})

# 5.11-5.12 统计
print("\n── 5.11-5.12 统计查询 ──")
test("流动人口热力图", "GET", "/api/fp/statistics/heatmap")
test("流动人口趋势统计", "GET", "/api/fp/statistics/trend")

# 清理
print("\n── 清理 ──")
if reg_rid:
    test("删除居住登记", "DELETE", f"/api/fp/residence/{reg_rid}")
if fp_rid:
    test("删除流动人口登记", "DELETE", f"/api/fp/register/{fp_rid}")

# ============================================================
# 模块 6: pdm-missingperson — 失踪人口管理 (5 端点)
# ============================================================

section("模块 6/8: pdm-missingperson — 失踪人口管理")

# 6.1 登记失踪人口
print("\n── 6.1-6.3 失踪人口管理 ──")
mp_id = test("登记失踪人口", "POST", "/api/missing",
     {"residentUuid": resident_uuid,
      "missingDate": "2026-06-15",
      "missingPlace": "北京市东城区测试商场",
      "photo": "", "appearance": "身高170cm，偏瘦，短发",
      "medicalHistory": "无",
      "possibleWay": "疑似被拐卖",
      "contactPhone": "13800138001",
      "status": "失踪中"},
     extract="data.rid")

# 6.2 搜索
test("搜索失踪人口", "GET",
     f"/api/missing/search?name=测试张三&status=失踪中&page=1&size=20")

# 6.3 登记找回
if mp_id:
    test("登记失踪人员找回", "POST", "/api/missing/recovery",
         {"missingRecordRid": mp_id,
          "recoveryDate": "2026-06-22",
          "summary": "经群众举报找回"})

# 6.4 统计
print("\n── 6.4 统计 ──")
test("失踪人口统计", "GET", "/api/missing/statistics")

# 6.5 撤销
print("\n── 6.5 撤销 ──")
if mp_id:
    test("撤销失踪登记", "DELETE", f"/api/missing/{mp_id}")

# ============================================================
# 模块 7: pdm-log — 日志审计 (3 端点)
# ============================================================

section("模块 7/8: pdm-log — 日志审计")

print("\n── 7.1 审计日志 ──")
test("查询审计日志", "GET",
     "/api/log/audit?page=1&size=20&startTime=2026-01-01T00:00:00&endTime=2026-12-31T23:59:59")

test("按操作类型查询审计日志", "GET",
     "/api/log/audit?page=1&size=20&operationType=新增")

print("\n── 7.2 登录日志 ──")
test("查询登录日志", "GET",
     f"/api/log/login?page=1&size=20&userUuid={USER_UUID}&isSuccess=1")

print("\n── 7.3 导出 ──")
test("导出审计日志", "GET",
     "/api/log/export?startTime=2026-01-01T00:00:00&endTime=2026-12-31T23:59:59",
     want_code=200)

# ============================================================
# 模块 8: pdm-notification — 通知预警 (3 端点)
# ============================================================

section("模块 8/8: pdm-notification — 通知预警")

# 8.1 待处理预警
print("\n── 8.1 待处理预警 ──")
alert_id = test("获取待处理预警", "GET", "/api/alert/pending",
                extract="data.0.alertId")

# 8.2 处理预警
if alert_id:
    print("\n── 8.2 处理预警 ──")
    test("处理预警", "PUT", f"/api/alert/{alert_id}/handle?handledBy={USER_UUID}",
         {}, want_code=200)

# 8.3 搜索预警
print("\n── 8.3 搜索预警 ──")
test("搜索预警", "GET",
     "/api/alert/search?alertType=走访逾期&severity=高&isHandled=0&page=1&size=20")

# ============================================================
# 结果汇总
# ============================================================

total = PASS + FAIL + SKIP
rate = (PASS / max(PASS + FAIL, 1)) * 100

print(f"\n{'='*60}")
print(f"  测试结果汇总")
print(f"{'='*60}")
print(f"  ✅ 通过: {PASS}")
print(f"  ❌ 失败: {FAIL}")
print(f"  ⬜ 跳过: {SKIP}")
print(f"  📊 总计: {total}  通过率: {rate:.1f}%")
print(f"{'='*60}")

# 失败详情
if FAIL > 0:
    print(f"\n── 失败详情 ──")
    for r in RESULTS:
        if r["status"] == "FAIL":
            print(f"  ❌ [{r.get('http','?')}] {r['path']} — {r.get('response','')[:100]}")

sys.exit(0 if FAIL == 0 else 1)
