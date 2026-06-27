# PDM 测试数据说明文档

**生成时间：** 2026-06-27  
**生成脚本：** `scripts/gen-perf-data.py` v4  
**随机种子：** 42（可重现）

---

## 一、数据总览

| 数据库 | 角色 | 表数量 | 数据条数 |
|--------|------|--------|----------|
| pdm_db | 主库 | 24 | ~1,500 |
| pdm_shard_0 | 分片 0 | 9 | ~2,089 |
| pdm_shard_1 | 分片 1 | 9 | ~2,112 |
| pdm_shard_2 | 分片 2 | 9 | ~2,242 |
| pdm_shard_3 | 分片 3 | 9 | ~2,157 |
| **合计** | | | **~10,550** |

---

## 二、分片表数据 (pdm_shard_0 ~ 3)

分片算法：`HASH_MOD(MD5(uuid)) % 4`

### 2.1 resident（居民）— 5,000 条

| 分片 | 数量 | 占比 |
|------|------|------|
| shard_0 | 1,197 | 23.9% |
| shard_1 | 1,234 | 24.7% |
| shard_2 | 1,326 | 26.5% |
| shard_3 | 1,243 | 24.9% |

**字段分布特征：**

| 字段 | 分布依据 | 说明 |
|------|----------|------|
| name | 百家姓 + 常见名 | 45%双字名，3%有曾用名 |
| gender | 各年龄段性别比 | 0-17岁男51.7%，18-34岁男51.2%，60+女52% |
| id_card_no | GB 11643-1999 | 含校验位，前6位对应实际区划代码 |
| nation / nation_code | 七普民族比例 | 汉族91.1%，壮族1.4%，回族0.8%... |
| birth_date | 五段年龄分布 | 0-14岁17%，15-34岁29%，35-54岁32%，55-74岁19%，75+岁3% |
| education_level / education_code | 6岁以上学历分布 | 初中33.5%，小学24.8%，高中12.5%，大专7.6%，本科5.8%... |
| blood_type | 中国人群血型 | O型34%，B型29%，A型28%，AB型7% |
| marital_status | 各年龄段婚姻分布 | 20-29岁：55%未婚 35%初婚；60+：55%初婚 28%丧偶 |
| occupation | 按行业门类 + 年龄调整 | 16岁以下为学生，60岁以上多为退休 |
| phone | 国内手机号段 | 随机合法前缀 + 8位尾号 |
| residence / household_address | 省份+城市+街道+门牌 | 与身份证前6位对应的行政区划一致 |
| household_type | 户口类型 | 居民户口45%，农业户口40%，非农业户口15% |
| household_status | 户口状态 | 正常92%，迁出注销5%，死亡注销2%，恢复1% |
| area_id | 1-464 随机 | 对应 area 广播表 |

**省份覆盖：** 全部 31 个省级行政区，按七普人口权重分配。

### 2.2 key_person（重点人员）— 195 条

| 字段 | 分布 |
|------|------|
| control_level | 一级10%，二级30%，三级60% |
| control_type | 涉稳人员、涉毒人员、社区矫正人员等7类均匀分布 |
| responsible_police_no | P10000-P99999 随机 |

### 2.3 resident_relation（家庭关系）— 2,000 条

- 为 ~40% 居民生成家庭关系
- father_uuid：随机选取年龄大18岁以上的男性
- mother_uuid：随机选取年龄大18岁以上的女性
- spouse_uuid：随机选取年龄差<15岁的异性
- 所有 UUID 引用真实的 resident.uuid

### 2.4 其他分片表

| 表名 | 数量 | 说明 |
|------|------|------|
| resident_permit | 400 | 居住证，60%有效/30%过期/10%注销 |
| resident_registration | 600 | 居住登记，地址类型/目的/时长均有分布 |
| fp_register_record | 300 | 流动人口登记 |
| resident_change_request | 100 | 信息变更申请，含5种状态 |

---

## 三、主库表数据 (pdm_db)

| 表名 | 数量 | 说明 |
|------|------|------|
| sys_user | 11 (1 admin + 10 test) | 角色含用户管理员、采集员、街道办、民警、市局负责人、普通用户 |
| police | 15 | 含警员/警司/警督/警监，85%在岗 |
| household_register | 300 | 户籍登记本，75%有效状态 |
| household_business_request | 100 | 户籍业务（登记50%/注销30%/户主变更20%） |
| household_migration_request | 80 | 户籍迁移（市内40%/省内35%/跨省25%） |
| missing_person | 80 | 失踪人员记录，70%失踪中/30%已寻回 |
| missing_person_recovery | 25 | 寻回记录 |
| audit_log | 500 | 审计日志，含新增/修改/删除三种操作 |
| login_log | 300 | 登录日志，92%成功/8%失败 |
| visit_plan | 378 | 走访计划 |
| petition_record | 62 | 信访记录 |
| alert | 100 | 告警记录，含高/中/低三个等级 |

---

## 四、广播表（5个数据库均有）

| 表名 | 数量 | 来源 |
|------|------|------|
| area | 464 | `sql/init-area-data.sql` — 全国行政区划 |
| permission_group | 8 | `sql/init-schema.sql` — 权限组 |

---

## 五、测试账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | Admin@123 | 系统管理员 |
| testuser2 ~ testuser11 | Admin@123 (bcrypt) | 各类角色 |

---

## 六、数据生成方式

```bash
# 生成 SQL 文件（不插入）
python scripts/gen-perf-data.py --total 5000 --dry-run

# 生成并插入数据库
python scripts/gen-perf-data.py --total 5000 --insert

# 仅插入已有 SQL 文件
for i in 0 1 2 3; do
  cat performance/test-data/insert-pdm_shard_${i}.sql | \
    wsl docker exec -i pdm-postgresql psql -U pdm -d pdm_shard_${i}
done
cat performance/test-data/insert-main.sql | \
  wsl docker exec -i pdm-postgresql psql -U pdm -d pdm_db
```

---

## 七、重置数据库

```bash
# 清空测试数据（保留 admin 和广播表）
for db in pdm_db pdm_shard_0 pdm_shard_1 pdm_shard_2 pdm_shard_3; do
  echo "DELETE FROM resident; DELETE FROM key_person; ..." | \
    wsl docker exec -i pdm-postgresql psql -U pdm -d "$db"
done
```
