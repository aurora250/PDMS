-- ============================================================
-- PDM 分片数据库共享表结构 (PostgreSQL 16+)
-- 应用于: pdm_shard_0, pdm_shard_1, pdm_shard_2, pdm_shard_3
-- 优化: VARCHAR(36)原生类型, TEXT, 分区索引
-- ============================================================


-- ============================================================
-- 1. 户籍人员表 (分片键: uuid)
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
-- 2. 人员关系表 (分片键: relation_person_uuid)
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
-- 3. 户籍人员信息变更请求表 (分片键: applicant_uuid)
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
-- 4. 居住证表 (分片键: uuid)
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
-- 5. 居住地登记表 (分片键: uuid)
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
-- 6. 流动人口登记表 (分片键: uuid)
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
-- 7. 重点人员表 (分片键: uuid)
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
-- 广播表
-- ============================================================

-- 区域表 (广播到所有分片)
CREATE TABLE IF NOT EXISTS area (
    area_id BIGSERIAL PRIMARY KEY,
    area_code VARCHAR(12) NOT NULL,
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
COMMENT ON TABLE area IS '区域表(广播)';

-- 权限组表 (广播到所有分片)
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
COMMENT ON TABLE permission_group IS '权限组表(广播)';
