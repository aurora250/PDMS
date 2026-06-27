-- ============================================================
-- PDM (People Database Management) 人口数据库管理系统
-- 数据库初始化脚本 (PostgreSQL 16+)
-- 优化: VARCHAR(36)原生类型, TEXT, BRIN索引, 表分区, 物化视图
-- ============================================================

-- 启用扩展
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- ============================================================
-- 1. 系统用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    user_uuid VARCHAR(36) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    token TEXT,
    resident_uuid VARCHAR(36) NOT NULL,
    user_role VARCHAR(20) NOT NULL CHECK (user_role IN ('系统管理员','用户管理员','数据审查员','采集员','街道办','民警','市局负责人','普通用户')),
    permission_group_id BIGINT,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(45),
    failed_login_count INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    register_materials VARCHAR(500) NOT NULL,
    account_status VARCHAR(10) NOT NULL DEFAULT '审批中' CHECK (account_status IN ('审批中','有效','冻结','注销','锁定')),
    must_change_password BOOLEAN DEFAULT TRUE,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_user_uuid UNIQUE (user_uuid),
    CONSTRAINT uk_username UNIQUE (username)
);
COMMENT ON TABLE sys_user IS '系统用户表';

-- ============================================================
-- 2. 警员表
-- ============================================================
CREATE TABLE IF NOT EXISTS police (
    id BIGSERIAL PRIMARY KEY,
    police_number VARCHAR(20) NOT NULL,
    user_uuid VARCHAR(36),
    resident_uuid VARCHAR(36) NOT NULL,
    police_station VARCHAR(100) NOT NULL,
    jurisdiction VARCHAR(200) NOT NULL,
    area_id BIGINT,
    department VARCHAR(100) NOT NULL,
    police_rank VARCHAR(10) CHECK (police_rank IN ('警员','警司','警督','警监')),
    duty_status VARCHAR(10) NOT NULL DEFAULT '在岗' CHECK (duty_status IN ('在岗','调岗','离职')),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_police_number UNIQUE (police_number)
);
COMMENT ON TABLE police IS '警员表';

-- ============================================================
-- 3. 权限组表
-- ============================================================
CREATE TABLE IF NOT EXISTS permission_group (
    group_id BIGSERIAL PRIMARY KEY,
    group_name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    permissions TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_group_name UNIQUE (group_name)
);
COMMENT ON TABLE permission_group IS '权限组表';

-- ============================================================
-- 4. 户籍人员表
-- ============================================================
CREATE TABLE IF NOT EXISTS resident (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    name VARCHAR(50) NOT NULL,
    former_name VARCHAR(50),
    gender VARCHAR(4) NOT NULL CHECK (gender IN ('男','女')),
    id_card_no CHAR(18) NOT NULL,
    nation VARCHAR(20) NOT NULL,
    nation_code CHAR(2) COMMENT '民族代码 - 符合 GB 3304-1991，01-56为标准民族，97=其他，98=外籍',
    birth_date DATE NOT NULL,
    education_level VARCHAR(20) CHECK (education_level IN ('研究生','大学本科','大学专科','中等职业教育','技工学校','高中','初中','小学','文盲或半文盲','未知')),
    education_code CHAR(2) COMMENT '学历代码 - 符合 GB/T 4658-2006',
    blood_type VARCHAR(6) CHECK (blood_type IN ('A','B','AB','O','未知')),
    marital_status VARCHAR(20) CHECK (marital_status IN ('未婚','已婚','初婚','再婚','复婚','丧偶','离婚','未说明的婚姻状况')),
    occupation VARCHAR(100),
    phone VARCHAR(20) NOT NULL,
    photo VARCHAR(500),
    residence VARCHAR(200) NOT NULL,
    area_id BIGINT,
    household_type VARCHAR(15) NOT NULL CHECK (household_type IN ('农业户口','非农业户口','居民户口')),
    household_status VARCHAR(10) NOT NULL DEFAULT '正常' CHECK (household_status IN ('正常','死亡注销','失踪注销','迁出注销','恢复')),
    household_address VARCHAR(200) NOT NULL,
    household_area_id BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_resident_uuid UNIQUE (uuid),
    CONSTRAINT uk_id_card_no UNIQUE (id_card_no)
);
CREATE INDEX IF NOT EXISTS idx_resident_name ON resident (name);
CREATE INDEX IF NOT EXISTS idx_resident_create_time ON resident (create_time);
CREATE INDEX IF NOT EXISTS idx_resident_household_area_id ON resident (household_area_id);
CREATE INDEX IF NOT EXISTS idx_resident_gender ON resident (gender);
CREATE INDEX IF NOT EXISTS idx_resident_nation ON resident (nation);
CREATE INDEX IF NOT EXISTS idx_resident_nation_code ON resident (nation_code);
CREATE INDEX IF NOT EXISTS idx_resident_education_code ON resident (education_code);
COMMENT ON TABLE resident IS '户籍人员表';

-- ============================================================
-- 5. 人员关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS resident_relation (
    rid BIGSERIAL PRIMARY KEY,
    relation_person_uuid VARCHAR(36) NOT NULL,
    father_uuid VARCHAR(36),
    mother_uuid VARCHAR(36),
    spouse_uuid VARCHAR(36),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_relation_person_uuid UNIQUE (relation_person_uuid)
);
COMMENT ON TABLE resident_relation IS '人员关系表';

-- ============================================================
-- 6. 户籍人员信息变更请求表
-- ============================================================
CREATE TABLE IF NOT EXISTS resident_change_request (
    rid BIGSERIAL PRIMARY KEY,
    applicant_uuid VARCHAR(36) NOT NULL,
    handler_id_list VARCHAR(500),
    change_field VARCHAR(50) NOT NULL,
    request_time DATE NOT NULL,
    original_data TEXT NOT NULL,
    modified_data TEXT NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT '请求' CHECK (status IN ('请求','一审','二审','通过','驳回')),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_change_request_applicant ON resident_change_request (applicant_uuid);
CREATE INDEX IF NOT EXISTS idx_change_request_status ON resident_change_request (status);
COMMENT ON TABLE resident_change_request IS '户籍人员信息变更请求表';

-- ============================================================
-- 7. 居住证表
-- ============================================================
CREATE TABLE IF NOT EXISTS resident_permit (
    id BIGSERIAL PRIMARY KEY,
    permit_no VARCHAR(36) NOT NULL,
    uuid VARCHAR(36) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT '有效' CHECK (status IN ('有效','过期','注销')),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_permit_no UNIQUE (permit_no)
);
CREATE INDEX IF NOT EXISTS idx_resident_permit_uuid ON resident_permit (uuid);
CREATE INDEX IF NOT EXISTS idx_resident_permit_expiry ON resident_permit (expiry_date, status);
COMMENT ON TABLE resident_permit IS '居住证表';

-- ============================================================
-- 8. 居住地登记表
-- ============================================================
CREATE TABLE IF NOT EXISTS resident_registration (
    rid BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    original_address VARCHAR(200),
    current_address VARCHAR(200) NOT NULL,
    area_id BIGINT,
    address_type VARCHAR(20) CHECK (address_type IN ('自有住房','租赁房屋','单位宿舍','学校宿舍','亲友借住','其他')),
    house_ownership VARCHAR(100),
    purpose VARCHAR(20) NOT NULL CHECK (purpose IN ('务工','经商','求学','投靠亲属','其他')),
    expected_duration VARCHAR(10) NOT NULL CHECK (expected_duration IN ('短租','中租','长租')),
    work_unit VARCHAR(200),
    register_date DATE NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_resident_registration_uuid ON resident_registration (uuid);
CREATE INDEX IF NOT EXISTS idx_resident_registration_area ON resident_registration (area_id);
COMMENT ON TABLE resident_registration IS '居住地登记表';

-- ============================================================
-- 9. 流动人口登记表
-- ============================================================
CREATE TABLE IF NOT EXISTS fp_register_record (
    rid BIGSERIAL PRIMARY KEY,
    residence_permit_no VARCHAR(36),
    uuid VARCHAR(36) NOT NULL,
    agent_uuid VARCHAR(36),
    attachment VARCHAR(500),
    reviewer_uuid VARCHAR(36),
    reject_reason VARCHAR(500),
    register_date DATE NOT NULL,
    review_date DATE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_fp_uuid ON fp_register_record (uuid);
CREATE INDEX IF NOT EXISTS idx_fp_register_date ON fp_register_record (register_date);
COMMENT ON TABLE fp_register_record IS '流动人口登记表';

-- ============================================================
-- 10. 居住证延期记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS resident_permit_renewal (
    renewal_id BIGSERIAL PRIMARY KEY,
    permit_no VARCHAR(36) NOT NULL,
    old_expiry_date DATE NOT NULL,
    new_expiry_date DATE NOT NULL,
    renewal_date DATE NOT NULL,
    operator_uuid VARCHAR(36) NOT NULL,
    remark VARCHAR(200),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_permit_renewal_permit ON resident_permit_renewal (permit_no);
COMMENT ON TABLE resident_permit_renewal IS '居住证延期记录表';

-- ============================================================
-- 11. 重点人员表
-- ============================================================
CREATE TABLE IF NOT EXISTS key_person (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    control_level VARCHAR(6) NOT NULL CHECK (control_level IN ('一级','二级','三级')),
    control_type VARCHAR(40) NOT NULL CHECK (control_type IN ('涉稳人员','涉毒人员','社区矫正人员','精神障碍患者(肇事肇祸风险)','刑满释放人员','信访重点人员','其他重点人员')),
    designated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    responsible_police_no VARCHAR(20) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_key_person_uuid UNIQUE (uuid)
);
CREATE INDEX IF NOT EXISTS idx_kp_control_level ON key_person (control_level);
CREATE INDEX IF NOT EXISTS idx_kp_police ON key_person (responsible_police_no);
CREATE INDEX IF NOT EXISTS idx_kp_control_type ON key_person (control_type);
COMMENT ON TABLE key_person IS '重点人员表';

-- ============================================================
-- 12. 重点人员上访/走访记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS petition_record (
    id BIGSERIAL PRIMARY KEY,
    key_person_uuid VARCHAR(36) NOT NULL,
    handler_police_no VARCHAR(20) NOT NULL,
    petition_time TIMESTAMP NOT NULL,
    address VARCHAR(200) NOT NULL,
    remark TEXT,
    evaluation VARCHAR(200) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_petition_kp ON petition_record (key_person_uuid);
CREATE INDEX IF NOT EXISTS idx_petition_time ON petition_record (petition_time);
COMMENT ON TABLE petition_record IS '重点人员上访记录表';

-- ============================================================
-- 13. 走访计划表
-- ============================================================
CREATE TABLE IF NOT EXISTS visit_plan (
    plan_id BIGSERIAL PRIMARY KEY,
    key_person_uuid VARCHAR(36) NOT NULL,
    planned_date DATE NOT NULL,
    actual_date DATE,
    visit_type VARCHAR(10) NOT NULL DEFAULT '入户走访' CHECK (visit_type IN ('入户走访','电话','视频')),
    status VARCHAR(10) NOT NULL DEFAULT '待走访' CHECK (status IN ('待走访','已完成','已逾期','已取消')),
    assigned_police_no VARCHAR(20) NOT NULL,
    is_alerted SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_visit_plan_kp_uuid ON visit_plan (key_person_uuid);
CREATE INDEX IF NOT EXISTS idx_visit_plan_status_date ON visit_plan (status, planned_date);
CREATE INDEX IF NOT EXISTS idx_visit_plan_police ON visit_plan (assigned_police_no);
COMMENT ON TABLE visit_plan IS '走访计划表';

-- ============================================================
-- 14. 失踪人员表
-- ============================================================
CREATE TABLE IF NOT EXISTS missing_person (
    rid BIGSERIAL PRIMARY KEY,
    resident_uuid VARCHAR(36) NOT NULL,
    missing_date DATE NOT NULL,
    missing_place VARCHAR(200) NOT NULL,
    photo VARCHAR(500) NOT NULL,
    appearance TEXT NOT NULL,
    medical_history TEXT,
    possible_way VARCHAR(200),
    contact_phone VARCHAR(20) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT '失踪中' CHECK (status IN ('失踪中','已经寻回')),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_missing_status ON missing_person (status);
CREATE INDEX IF NOT EXISTS idx_missing_resident ON missing_person (resident_uuid);
COMMENT ON TABLE missing_person IS '失踪人员表';

-- ============================================================
-- 15. 失踪人员寻回记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS missing_person_recovery (
    rid BIGSERIAL PRIMARY KEY,
    missing_record_rid BIGINT NOT NULL,
    recovery_date DATE NOT NULL,
    summary TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
COMMENT ON TABLE missing_person_recovery IS '失踪人员寻回记录表';

-- ============================================================
-- 16. 户口本表
-- ============================================================
CREATE TABLE IF NOT EXISTS household_register (
    id BIGSERIAL PRIMARY KEY,
    household_book_no VARCHAR(36) NOT NULL,
    householder_uuid VARCHAR(36) NOT NULL,
    establish_date DATE NOT NULL,
    hukou_address VARCHAR(200) NOT NULL,
    hukou_area_id BIGINT,
    status VARCHAR(10) NOT NULL DEFAULT '审批中' CHECK (status IN ('审批中','有效','冻结','无效')),
    member_uuid_list TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_household_book_no UNIQUE (household_book_no)
);
CREATE INDEX IF NOT EXISTS idx_household_holder ON household_register (householder_uuid);
COMMENT ON TABLE household_register IS '户口本表';

-- ============================================================
-- 17. 户籍地业务请求表
-- ============================================================
CREATE TABLE IF NOT EXISTS household_business_request (
    rid BIGSERIAL PRIMARY KEY,
    handler_uuid VARCHAR(36),
    applicant_uuid VARCHAR(36) NOT NULL,
    attachment VARCHAR(500) NOT NULL,
    business_type VARCHAR(10) NOT NULL CHECK (business_type IN ('登记','注销','户主变更')),
    handle_date DATE NOT NULL,
    handle_basis VARCHAR(200),
    fee DECIMAL(10,2),
    status VARCHAR(10) NOT NULL DEFAULT '审批中' CHECK (status IN ('审批中','批准','驳回')),
    reject_reason VARCHAR(500),
    remark TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_business_applicant ON household_business_request (applicant_uuid);
CREATE INDEX IF NOT EXISTS idx_business_status ON household_business_request (status);
COMMENT ON TABLE household_business_request IS '户籍地业务请求表';

-- ============================================================
-- 18. 户籍迁移业务请求表
-- ============================================================
CREATE TABLE IF NOT EXISTS household_migration_request (
    rid BIGSERIAL PRIMARY KEY,
    handler_uuid VARCHAR(36),
    applicant_uuid VARCHAR(36) NOT NULL,
    incoming_address VARCHAR(200),
    incoming_area_id BIGINT,
    outgoing_address VARCHAR(200),
    outgoing_area_id BIGINT,
    attachment VARCHAR(500) NOT NULL,
    business_type VARCHAR(10) NOT NULL CHECK (business_type IN ('市内','省内','跨省')),
    handle_date DATE NOT NULL,
    handle_basis VARCHAR(200),
    fee DECIMAL(10,2),
    status VARCHAR(20) NOT NULL CHECK (status IN ('准迁证审批中','准迁证审批驳回','迁移证审批中','迁移证审批驳回','迁移审批中','迁移审批驳回','迁移审批通过')),
    reject_reason VARCHAR(500),
    approval_permit_no VARCHAR(36),
    migration_permit_no VARCHAR(36),
    remark TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_migration_applicant ON household_migration_request (applicant_uuid);
CREATE INDEX IF NOT EXISTS idx_migration_status ON household_migration_request (status);
COMMENT ON TABLE household_migration_request IS '户籍迁移业务请求表';

-- ============================================================
-- 19. 准迁证表
-- ============================================================
CREATE TABLE IF NOT EXISTS approval_permit (
    id BIGSERIAL PRIMARY KEY,
    permit_no VARCHAR(36) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    issuing_authority VARCHAR(100) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT '审批中' CHECK (status IN ('审批中','有效','作废')),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_approval_permit_no UNIQUE (permit_no)
);
COMMENT ON TABLE approval_permit IS '准迁证表';

-- ============================================================
-- 20. 迁移证表
-- ============================================================
CREATE TABLE IF NOT EXISTS migration_permit (
    id BIGSERIAL PRIMARY KEY,
    permit_no VARCHAR(36) NOT NULL,
    issue_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    outgoing_police_station VARCHAR(100) NOT NULL,
    status VARCHAR(10) NOT NULL DEFAULT '审批中' CHECK (status IN ('审批中','有效','作废')),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_migration_permit_no UNIQUE (permit_no)
);
COMMENT ON TABLE migration_permit IS '迁移证表';

-- ============================================================
-- 21. 区域表
-- ============================================================
CREATE TABLE IF NOT EXISTS area (
    area_id BIGSERIAL PRIMARY KEY,
    area_code CHAR(6) NOT NULL,
    area_name VARCHAR(100) NOT NULL,
    parent_id BIGINT,
    area_level VARCHAR(10) NOT NULL CHECK (area_level IN ('省','市','区县','街道','社区')),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_area_code UNIQUE (area_code)
);
CREATE INDEX IF NOT EXISTS idx_area_parent_id ON area (parent_id);
CREATE INDEX IF NOT EXISTS idx_area_level ON area (area_level);
COMMENT ON TABLE area IS '行政区划表 - 符合 GB/T 2260-2007 中华人民共和国行政区划代码';
COMMENT ON COLUMN area.area_code IS '行政区划代码 - 符合 GB/T 2260-2007，6位数字';

-- ============================================================
-- 22. 操作日志表 (按月分区)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_log (
    log_id BIGSERIAL,
    operator_uuid VARCHAR(36) NOT NULL,
    operation_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    operation_type VARCHAR(10) NOT NULL CHECK (operation_type IN ('新增','修改','删除')),
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(36),
    before_data TEXT,
    after_data TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (log_id, operation_time)
) PARTITION BY RANGE (operation_time);

-- 创建初始分区 (2024-2027)
CREATE TABLE IF NOT EXISTS audit_log_2024 PARTITION OF audit_log
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE IF NOT EXISTS audit_log_2025 PARTITION OF audit_log
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS audit_log_2026 PARTITION OF audit_log
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS audit_log_2027 PARTITION OF audit_log
    FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE IF NOT EXISTS audit_log_default PARTITION OF audit_log DEFAULT;

CREATE INDEX IF NOT EXISTS idx_audit_operator_uuid ON audit_log (operator_uuid);
CREATE INDEX IF NOT EXISTS idx_audit_operation_time_brin ON audit_log USING BRIN (operation_time) WITH (pages_per_range = 32);
CREATE INDEX IF NOT EXISTS idx_audit_target_type ON audit_log (target_type);
COMMENT ON TABLE audit_log IS '操作日志表(按月分区)';

-- ============================================================
-- 23. 预警表 (按月分区)
-- ============================================================
CREATE TABLE IF NOT EXISTS alert (
    alert_id BIGSERIAL,
    alert_type VARCHAR(20) NOT NULL CHECK (alert_type IN ('居住证到期','走访逾期','重点人员匹配','证件到期','其他')),
    target_type VARCHAR(50) NOT NULL,
    target_id VARCHAR(36) NOT NULL,
    alert_content VARCHAR(500) NOT NULL,
    severity VARCHAR(4) NOT NULL CHECK (severity IN ('高','中','低')),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_handled SMALLINT NOT NULL DEFAULT 0,
    handled_by VARCHAR(36),
    handled_at TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (alert_id, create_time)
) PARTITION BY RANGE (create_time);

CREATE TABLE IF NOT EXISTS alert_2024 PARTITION OF alert
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE IF NOT EXISTS alert_2025 PARTITION OF alert
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS alert_2026 PARTITION OF alert
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS alert_2027 PARTITION OF alert
    FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE IF NOT EXISTS alert_default PARTITION OF alert DEFAULT;

CREATE INDEX IF NOT EXISTS idx_alert_create_time_brin ON alert USING BRIN (create_time) WITH (pages_per_range = 32);
CREATE INDEX IF NOT EXISTS idx_alert_alert_type ON alert (alert_type);
CREATE INDEX IF NOT EXISTS idx_alert_is_handled ON alert (is_handled);
CREATE INDEX IF NOT EXISTS idx_alert_unhandled ON alert (is_handled, create_time) WHERE is_handled = 0;
COMMENT ON TABLE alert IS '预警表(按月分区)';

-- ============================================================
-- 24. 登录日志表 (按月分区)
-- ============================================================
CREATE TABLE IF NOT EXISTS login_log (
    log_id BIGSERIAL,
    user_uuid VARCHAR(36) NOT NULL,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL,
    is_success SMALLINT NOT NULL DEFAULT 1,
    fail_reason VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    PRIMARY KEY (log_id, login_time)
) PARTITION BY RANGE (login_time);

CREATE TABLE IF NOT EXISTS login_log_2024 PARTITION OF login_log
    FOR VALUES FROM ('2024-01-01') TO ('2025-01-01');
CREATE TABLE IF NOT EXISTS login_log_2025 PARTITION OF login_log
    FOR VALUES FROM ('2025-01-01') TO ('2026-01-01');
CREATE TABLE IF NOT EXISTS login_log_2026 PARTITION OF login_log
    FOR VALUES FROM ('2026-01-01') TO ('2027-01-01');
CREATE TABLE IF NOT EXISTS login_log_2027 PARTITION OF login_log
    FOR VALUES FROM ('2027-01-01') TO ('2028-01-01');
CREATE TABLE IF NOT EXISTS login_log_default PARTITION OF login_log DEFAULT;

CREATE INDEX IF NOT EXISTS idx_login_user_uuid ON login_log (user_uuid);
CREATE INDEX IF NOT EXISTS idx_login_time_brin ON login_log USING BRIN (login_time) WITH (pages_per_range = 32);
COMMENT ON TABLE login_log IS '登录日志表(按月分区)';

-- ============================================================
-- 物化视图: 居住热力图数据
-- ============================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_resident_heatmap AS
SELECT area_id, address_type, COUNT(*) as cnt
FROM resident_registration
WHERE is_deleted = 0
GROUP BY area_id, address_type;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_heatmap ON mv_resident_heatmap (area_id, address_type);

-- ============================================================
-- 物化视图: 流动人口趋势
-- ============================================================
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_fp_trend AS
SELECT register_date, COUNT(*) as cnt
FROM fp_register_record
WHERE is_deleted = 0
GROUP BY register_date
ORDER BY register_date;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_fp_trend ON mv_fp_trend (register_date);

-- ============================================================
-- 初始化数据
-- ============================================================

-- 默认权限组（与 PermissionConstants 对齐）
INSERT INTO permission_group (group_name, description, permissions) VALUES
('系统管理员组', '拥有全部系统权限',
 '["*"]'),
('民警组', '户籍民警：业务受理、审核、制证、常住人口管理',
 '["resident:read","resident:write","resident:import","resident:export","resident:change-request:approve",
   "household:read","household:write","household:approve",
   "keyperson:read","keyperson:write",
   "missing:read","missing:write",
   "fp:read","fp:permit:approve","fp:permit:issue",
   "alert:read","alert:handle",
   "police:read","police:write"]'),
('采集员组', '一线采集员：流动人口、重点人员、失踪人口数据采集',
 '["fp:read","fp:write","fp:delete","fp:residence:write",
   "keyperson:read","keyperson:write","keyperson:gis:read","keyperson:visit-plan:write","keyperson:petition:write",
   "missing:read","missing:write","missing:recovery:write"]'),
('街道办组', '街道办负责人：查看数据、附加补充材料（无审批权）',
 '["resident:read",
   "household:read","household:material:attach",
   "fp:read","keyperson:read","missing:read",
   "alert:read"]'),
('数据审查员组', '独立数据审查：审核数据质量，标记审查状态',
 '["fp:read","fp:review",
   "keyperson:read","keyperson:review",
   "missing:read","missing:review"]'),
('市局负责人组', '市局领导：全市范围查看、极少数重要业务二审',
 '["resident:read","resident:change-request:second-approve",
   "household:read","household:second-approve",
   "fp:read","keyperson:read","missing:read",
   "alert:read","alert:handle",
   "police:read",
   "statistics:read"]'),
('用户管理员组', '用户账号管理：创建、修改、状态变更',
 '["auth:user:read","auth:user:write","auth:user:status"]'),
('普通用户组', '群众自助服务：自我申报、业务查询',
 '["self:resident:read","self:fp:write","self:household:apply","self:missing:recovery:write","statistics:read"]');

-- 默认管理员账号 (密码: Admin@123)
INSERT INTO sys_user (user_uuid, username, password, resident_uuid, user_role, permission_group_id, phone, account_status, must_change_password, register_materials)
VALUES ('admin-0000-0000-0000-000000000001', 'admin',
        '$2b$10$prAIsqHtIZifAJkviOkbRe4IAJm4CEd7cS6tozqAZtw.O3DpWfVOC',
        'admin-0000-0000-0000-000000000001',
        '系统管理员', 1, '13800000000', '有效', TRUE, 'system-init');
-- ============================================================
