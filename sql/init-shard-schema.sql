-- Shared schema tables for shard databases
-- (Resident and related tables go here; system tables stay in pdm_db)

CREATE TABLE IF NOT EXISTS `resident` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL COMMENT '户籍登记人员通用唯一识别码',
    name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    former_name VARCHAR(50) COMMENT '曾用名',
    gender ENUM('男','女') NOT NULL COMMENT '性别',
    id_card_no CHAR(18) NOT NULL COMMENT '身份证号',
    nation VARCHAR(20) NOT NULL COMMENT '民族',
    birth_date DATE NOT NULL COMMENT '出生日期',
    education_level ENUM('小学及以下','初中','高中','中专','大专','本科','硕士研究生','博士研究生') NOT NULL COMMENT '文化水平',
    blood_type ENUM('A','B','AB','O','未知') COMMENT '血型',
    marital_status ENUM('未婚','已婚','离异','丧偶') NOT NULL COMMENT '婚姻状况',
    occupation VARCHAR(100) COMMENT '职业',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    photo VARCHAR(500) COMMENT '证件照存储路径',
    residence VARCHAR(200) NOT NULL COMMENT '当前实际居住地址',
    area_id BIGINT UNSIGNED COMMENT '居住地关联区域表',
    household_type ENUM('农业户口','非农业户口','居民户口') NOT NULL COMMENT '户口类型',
    household_status ENUM('正常','死亡注销','失踪注销','迁出注销','恢复') NOT NULL DEFAULT '正常' COMMENT '户籍状态',
    household_address VARCHAR(200) NOT NULL COMMENT '户籍登记地址',
    household_area_id BIGINT UNSIGNED COMMENT '户籍地址关联区域，分片路由键',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_uuid (uuid),
    UNIQUE INDEX uk_id_card_no (id_card_no),
    INDEX idx_name (name),
    INDEX idx_create_time (create_time),
    INDEX idx_household_area_id (household_area_id)
) ENGINE=InnoDB COMMENT='户籍人员表';

CREATE TABLE IF NOT EXISTS `resident_relation` (
    rid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    relation_person_uuid VARCHAR(36) NOT NULL COMMENT '关联人的户籍UUID',
    father_uuid VARCHAR(36) COMMENT '关联人父亲的户籍UUID',
    mother_uuid VARCHAR(36) COMMENT '关联人母亲的户籍UUID',
    spouse_uuid VARCHAR(36) COMMENT '关联配偶的户籍UUID，双向关联',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_relation_person_uuid (relation_person_uuid)
) ENGINE=InnoDB COMMENT='人员关系表';

CREATE TABLE IF NOT EXISTS `resident_change_request` (
    rid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    applicant_uuid VARCHAR(36) NOT NULL COMMENT '申请变更信息的人员业务UUID',
    handler_id_list VARCHAR(500) COMMENT '处理变更申请的各人员UUID，分号分隔',
    change_field VARCHAR(50) NOT NULL COMMENT '请求变更的属性字段名',
    request_time DATE NOT NULL COMMENT '请求变更时间',
    original_data TEXT NOT NULL COMMENT '变更前字段数据，JSON格式',
    modified_data TEXT NOT NULL COMMENT '请求变更的字段数据，JSON格式',
    status ENUM('请求','一审','二审','通过','驳回') NOT NULL DEFAULT '请求' COMMENT '审批状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='户籍人员信息变更请求表';

CREATE TABLE IF NOT EXISTS `resident_permit` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    permit_no VARCHAR(36) NOT NULL COMMENT '居住证业务唯一标识',
    uuid VARCHAR(36) NOT NULL COMMENT '居住证持有人业务UUID',
    issue_date DATE NOT NULL COMMENT '居住证签发日',
    expiry_date DATE NOT NULL COMMENT '居住证到期日',
    status ENUM('有效','过期','注销') NOT NULL DEFAULT '有效' COMMENT '居住证状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_permit_no (permit_no),
    INDEX idx_uuid (uuid)
) ENGINE=InnoDB COMMENT='居住证表';

CREATE TABLE IF NOT EXISTS `resident_registration` (
    rid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    uuid VARCHAR(36) NOT NULL COMMENT '居住证持有人UUID',
    original_address VARCHAR(200) COMMENT '变更前居住地址',
    current_address VARCHAR(200) NOT NULL COMMENT '精确到门牌号',
    area_id BIGINT UNSIGNED COMMENT '现居住地关联区域表',
    address_type ENUM('自有住房','租赁房屋','单位宿舍','学校宿舍','亲友借住','其他') COMMENT '地址类型',
    house_ownership VARCHAR(100) COMMENT '房屋权属证明',
    purpose ENUM('务工','经商','求学','投靠亲属','其他') NOT NULL COMMENT '来本地目的',
    expected_duration ENUM('短租','中租','长租') NOT NULL COMMENT '预计居住时间',
    work_unit VARCHAR(200) COMMENT '雇主或单位名称',
    register_date DATE NOT NULL COMMENT '登记日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    INDEX idx_uuid (uuid)
) ENGINE=InnoDB COMMENT='居住地登记表';

CREATE TABLE IF NOT EXISTS `fp_register_record` (
    rid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '流动人口登记记录ID',
    residence_permit_no VARCHAR(36) COMMENT '居住证唯一标识',
    uuid VARCHAR(36) NOT NULL COMMENT '居住证持有人UUID',
    agent_uuid VARCHAR(36) COMMENT '代办人UUID',
    attachment VARCHAR(500) COMMENT '相关证明材料附件路径',
    reviewer_uuid VARCHAR(36) COMMENT '审核人警号',
    reject_reason VARCHAR(500) COMMENT '驳回原因',
    register_date DATE NOT NULL COMMENT '登记时间',
    review_date DATE COMMENT '审核通过/驳回时填入',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='流动人口登记表';

CREATE TABLE IF NOT EXISTS `key_person` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL COMMENT '重点人员UUID',
    control_level ENUM('一级','二级','三级') NOT NULL COMMENT '管控等级',
    control_type ENUM('涉稳人员','涉毒人员','社区矫正人员','精神障碍患者(肇事肇祸风险)','刑满释放人员','信访重点人员','其他重点人员') NOT NULL,
    designated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '认定为重点人员的日期',
    revoked_at DATETIME COMMENT '撤销管控的日期',
    responsible_police_no VARCHAR(20) NOT NULL COMMENT '负责该重点人员管控的责任民警',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_uuid (uuid)
) ENGINE=InnoDB COMMENT='重点人员表';
