-- Shared schema tables for shard databases (PostgreSQL 16+)
-- (Resident and related tables; system tables stay in pdm_db)

CREATE TABLE IF NOT EXISTS resident (
    id BIGSERIAL PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    name VARCHAR(50) NOT NULL,
    former_name VARCHAR(50),
    gender VARCHAR(4) NOT NULL CHECK (gender IN ('男','女')),
    id_card_no CHAR(18) NOT NULL,
    nation VARCHAR(20) NOT NULL,
    birth_date DATE NOT NULL,
    education_level VARCHAR(20) NOT NULL CHECK (education_level IN ('小学及以下','初中','高中','中专','大专','本科','硕士研究生','博士研究生')),
    blood_type VARCHAR(6) CHECK (blood_type IN ('A','B','AB','O','未知')),
    marital_status VARCHAR(6) NOT NULL CHECK (marital_status IN ('未婚','已婚','离异','丧偶')),
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
COMMENT ON TABLE resident IS '户籍人员表';

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
COMMENT ON TABLE resident_change_request IS '户籍人员信息变更请求表';

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
COMMENT ON TABLE resident_permit IS '居住证表';

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
COMMENT ON TABLE resident_registration IS '居住地登记表';

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
COMMENT ON TABLE fp_register_record IS '流动人口登记表';

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
COMMENT ON TABLE key_person IS '重点人员表';
