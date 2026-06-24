-- Integration test schema (minimal tables for auth service, PostgreSQL)
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    user_uuid VARCHAR(36) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    token TEXT,
    resident_uuid VARCHAR(36) NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    permission_group_id BIGINT,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_time TIMESTAMP,
    last_login_ip VARCHAR(45),
    failed_login_count INTEGER DEFAULT 0,
    locked_until TIMESTAMP,
    register_materials VARCHAR(500) NOT NULL,
    account_status VARCHAR(20) NOT NULL DEFAULT '审批中',
    must_change_password BOOLEAN DEFAULT TRUE,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_test_user_uuid UNIQUE (user_uuid),
    CONSTRAINT uk_test_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS police (
    id BIGSERIAL PRIMARY KEY,
    police_number VARCHAR(20) NOT NULL,
    user_uuid VARCHAR(36),
    resident_uuid VARCHAR(36) NOT NULL,
    police_station VARCHAR(100) NOT NULL,
    jurisdiction VARCHAR(200) NOT NULL,
    area_id BIGINT,
    department VARCHAR(100) NOT NULL,
    police_rank VARCHAR(20),
    duty_status VARCHAR(20) NOT NULL DEFAULT '在岗',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_test_police_number UNIQUE (police_number)
);

CREATE TABLE IF NOT EXISTS permission_group (
    group_id BIGSERIAL PRIMARY KEY,
    group_name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    permissions TEXT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_test_group_name UNIQUE (group_name)
);

CREATE TABLE IF NOT EXISTS login_log (
    log_id BIGSERIAL PRIMARY KEY,
    user_uuid VARCHAR(36) NOT NULL,
    login_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL,
    is_success SMALLINT NOT NULL DEFAULT 1,
    fail_reason VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP,
    is_deleted SMALLINT NOT NULL DEFAULT 0
);
