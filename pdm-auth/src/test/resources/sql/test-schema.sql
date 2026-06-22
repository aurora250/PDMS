-- Integration test schema (minimal tables for auth service)
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_uuid VARCHAR(36) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    token VARCHAR(255),
    resident_uuid VARCHAR(36) NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    permission_group_id BIGINT,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_time DATETIME,
    last_login_ip VARCHAR(45),
    failed_login_count INT DEFAULT 0,
    locked_until DATETIME,
    register_materials VARCHAR(500) NOT NULL,
    account_status VARCHAR(20) NOT NULL DEFAULT '审批中',
    must_change_password TINYINT(1) DEFAULT 1,
    update_time DATETIME,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_user_uuid (user_uuid),
    UNIQUE INDEX uk_username (username)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `police` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    police_number VARCHAR(20) NOT NULL,
    user_uuid VARCHAR(36),
    resident_uuid VARCHAR(36) NOT NULL,
    police_station VARCHAR(100) NOT NULL,
    jurisdiction VARCHAR(200) NOT NULL,
    area_id BIGINT,
    department VARCHAR(100) NOT NULL,
    police_rank VARCHAR(20),
    duty_status VARCHAR(20) NOT NULL DEFAULT '在岗',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_police_number (police_number)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `permission_group` (
    group_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    permissions TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_group_name (group_name)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS `login_log` (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_uuid VARCHAR(36) NOT NULL,
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45) NOT NULL,
    is_success TINYINT UNSIGNED NOT NULL DEFAULT 1,
    fail_reason VARCHAR(100),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB;
