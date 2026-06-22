-- ============================================================
-- PDM (People Database Management) 人口数据库管理系统
-- 数据库初始化脚本 (MySQL 8.0+)
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS pdm_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE pdm_db;

-- ============================================================
-- 1. 系统用户表
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_uuid VARCHAR(36) NOT NULL COMMENT '系统用户通用全局唯一业务标识',
    username VARCHAR(50) NOT NULL COMMENT '登录系统使用的用户名，全局唯一',
    password VARCHAR(255) NOT NULL COMMENT '账号登录密码，BCrypt加密存储',
    token VARCHAR(255) COMMENT '会话登录令牌，JWT',
    resident_uuid VARCHAR(36) NOT NULL COMMENT '实名认证绑定的户籍人员UUID',
    user_role ENUM('系统管理员','用户管理员','数据审查员','采集员','街道办','民警','市局负责人','普通用户')
        NOT NULL COMMENT '用户角色',
    permission_group_id BIGINT UNSIGNED COMMENT 'RBAC权限组关联',
    phone VARCHAR(20) NOT NULL COMMENT '联系手机号',
    email VARCHAR(100) COMMENT '联系邮箱',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建的系统时间',
    last_login_time DATETIME COMMENT '账号最近一次登录系统的时间',
    last_login_ip VARCHAR(45) COMMENT '账号最后登录的设备IP',
    failed_login_count INT DEFAULT 0 COMMENT '连续登录失败次数',
    locked_until DATETIME COMMENT '账号锁定截止时间，过期自动解除',
    register_materials VARCHAR(500) NOT NULL COMMENT '注册时证明身份的所需材料',
    account_status ENUM('审批中','有效','冻结','注销','锁定') NOT NULL DEFAULT '审批中' COMMENT '账号状态',
    must_change_password TINYINT(1) DEFAULT 1 COMMENT '是否首次登录需修改密码',
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
    UNIQUE INDEX uk_user_uuid (user_uuid),
    UNIQUE INDEX uk_username (username)
) ENGINE=InnoDB COMMENT='系统用户表';

-- ============================================================
-- 2. 警员表
-- ============================================================
CREATE TABLE IF NOT EXISTS `police` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    police_number VARCHAR(20) NOT NULL COMMENT '警员唯一标识',
    user_uuid VARCHAR(36) COMMENT '用户账户对应的UUID',
    resident_uuid VARCHAR(36) NOT NULL COMMENT '实名认证的户籍UUID',
    police_station VARCHAR(100) NOT NULL COMMENT '所属警局名称',
    jurisdiction VARCHAR(200) NOT NULL COMMENT '管辖的片区范围',
    area_id BIGINT UNSIGNED COMMENT '关联区域表',
    department VARCHAR(100) NOT NULL COMMENT '所属警务部门/科室',
    police_rank ENUM('警员','警司','警督','警监') COMMENT '警衔',
    duty_status ENUM('在岗','调岗','离职') NOT NULL DEFAULT '在岗' COMMENT '值班状态',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_police_number (police_number)
) ENGINE=InnoDB COMMENT='警员表';

-- ============================================================
-- 3. 权限组表
-- ============================================================
CREATE TABLE IF NOT EXISTS `permission_group` (
    group_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    group_name VARCHAR(50) NOT NULL COMMENT '权限组名称',
    description VARCHAR(200) COMMENT '权限组描述',
    permissions TEXT COMMENT '功能权限列表，JSON格式',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_group_name (group_name)
) ENGINE=InnoDB COMMENT='权限组表';

-- ============================================================
-- 4. 户籍人员表
-- ============================================================
CREATE TABLE IF NOT EXISTS `resident` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL COMMENT '户籍登记人员通用唯一识别码',
    name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    former_name VARCHAR(50) COMMENT '曾用名',
    gender ENUM('男','女') NOT NULL COMMENT '性别',
    id_card_no CHAR(18) NOT NULL COMMENT '身份证号，符合GB 11643-1999',
    nation VARCHAR(20) NOT NULL COMMENT '民族',
    birth_date DATE NOT NULL COMMENT '出生日期，由身份证号自动提取',
    education_level ENUM('小学及以下','初中','高中','中专','大专','本科','硕士研究生','博士研究生')
        NOT NULL COMMENT '文化水平',
    blood_type ENUM('A','B','AB','O','未知') COMMENT '血型',
    marital_status ENUM('未婚','已婚','离异','丧偶') NOT NULL COMMENT '婚姻状况',
    occupation VARCHAR(100) COMMENT '职业',
    phone VARCHAR(20) NOT NULL COMMENT '联系电话',
    photo VARCHAR(500) COMMENT '证件照存储路径',
    residence VARCHAR(200) NOT NULL COMMENT '当前实际居住地址',
    area_id BIGINT UNSIGNED COMMENT '居住地关联区域表',
    household_type ENUM('农业户口','非农业户口','居民户口') NOT NULL COMMENT '户口类型',
    household_status ENUM('正常','死亡注销','失踪注销','迁出注销','恢复')
        NOT NULL DEFAULT '正常' COMMENT '户籍状态',
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

-- ============================================================
-- 5. 人员关系表
-- ============================================================
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

-- ============================================================
-- 6. 户籍人员信息变更请求表
-- ============================================================
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

-- ============================================================
-- 7. 居住证表
-- ============================================================
CREATE TABLE IF NOT EXISTS `resident_permit` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    permit_no VARCHAR(36) NOT NULL COMMENT '居住证业务唯一标识，系统自动生成',
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

-- ============================================================
-- 8. 居住地登记表
-- ============================================================
CREATE TABLE IF NOT EXISTS `resident_registration` (
    rid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    uuid VARCHAR(36) NOT NULL COMMENT '居住证持有人UUID',
    original_address VARCHAR(200) COMMENT '变更前居住地址',
    current_address VARCHAR(200) NOT NULL COMMENT '精确到门牌号',
    area_id BIGINT UNSIGNED COMMENT '现居住地关联区域表',
    address_type ENUM('自有住房','租赁房屋','单位宿舍','学校宿舍','亲友借住','其他') COMMENT '地址类型',
    house_ownership VARCHAR(100) COMMENT '房屋权属证明',
    purpose ENUM('务工','经商','求学','投靠亲属','其他') NOT NULL COMMENT '来本地目的',
    expected_duration ENUM('短租','中租','长租') NOT NULL COMMENT '预计居住时间，短租<3个月/中租3-12个月/长租>12个月',
    work_unit VARCHAR(200) COMMENT '雇主或单位名称',
    register_date DATE NOT NULL COMMENT '登记日期',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    INDEX idx_uuid (uuid)
) ENGINE=InnoDB COMMENT='居住地登记表';

-- ============================================================
-- 9. 流动人口登记表
-- ============================================================
CREATE TABLE IF NOT EXISTS `fp_register_record` (
    rid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '流动人口登记记录ID',
    residence_permit_no VARCHAR(36) COMMENT '居住证唯一标识',
    uuid VARCHAR(36) NOT NULL COMMENT '居住证持有人UUID',
    agent_uuid VARCHAR(36) COMMENT '代办人UUID',
    attachment VARCHAR(500) COMMENT '相关证明材料附件路径，分号分隔',
    reviewer_uuid VARCHAR(36) COMMENT '审核人警号',
    reject_reason VARCHAR(500) COMMENT '驳回原因',
    register_date DATE NOT NULL COMMENT '登记时间',
    review_date DATE COMMENT '审核通过/驳回时填入',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='流动人口登记表';

-- ============================================================
-- 10. 居住证延期记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `resident_permit_renewal` (
    renewal_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '延期记录ID',
    permit_no VARCHAR(36) NOT NULL COMMENT '被延期的居住证号',
    old_expiry_date DATE NOT NULL COMMENT '延期前到期日',
    new_expiry_date DATE NOT NULL COMMENT '延期后到期日',
    renewal_date DATE NOT NULL COMMENT '办理延期日期',
    operator_uuid VARCHAR(36) NOT NULL COMMENT '办理延期业务的警员',
    remark VARCHAR(200) COMMENT '延期备注说明',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='居住证延期记录表';

-- ============================================================
-- 11. 重点人员表
-- ============================================================
CREATE TABLE IF NOT EXISTS `key_person` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL COMMENT '重点人员UUID',
    control_level ENUM('一级','二级','三级') NOT NULL COMMENT '管控等级：一级重点严控每7天/二级常规管控每30天/三级一般关注每90天',
    control_type ENUM('涉稳人员','涉毒人员','社区矫正人员','精神障碍患者(肇事肇祸风险)','刑满释放人员','信访重点人员','其他重点人员') NOT NULL,
    designated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '认定为重点人员的日期',
    revoked_at DATETIME COMMENT '撤销管控的日期，NULL=仍在管控中',
    responsible_police_no VARCHAR(20) NOT NULL COMMENT '负责该重点人员管控的责任民警',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_uuid (uuid)
) ENGINE=InnoDB COMMENT='重点人员表';

-- ============================================================
-- 12. 重点人员上访/走访记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `petition_record` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    key_person_uuid VARCHAR(36) NOT NULL COMMENT '上访当事人的UUID',
    handler_police_no VARCHAR(20) NOT NULL COMMENT '处置本次上访事件的警号',
    petition_time DATETIME NOT NULL COMMENT '上访时间',
    address VARCHAR(200) NOT NULL COMMENT '上访实际发生的详细地理位置',
    remark TEXT COMMENT '上访事件补充说明：诉求内容/现场情况/沟通过程/人员规模/过激行为/处置措施',
    evaluation VARCHAR(200) NOT NULL COMMENT '现场风险分级评估结果',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='重点人员上访记录表';

-- ============================================================
-- 13. 走访计划表
-- ============================================================
CREATE TABLE IF NOT EXISTS `visit_plan` (
    plan_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '走访计划ID',
    key_person_uuid VARCHAR(36) NOT NULL COMMENT '走访对象UUID',
    planned_date DATE NOT NULL COMMENT '计划走访日期',
    actual_date DATE COMMENT '实际走访日期',
    visit_type ENUM('入户走访','电话','视频') NOT NULL DEFAULT '入户走访' COMMENT '走访方式',
    status ENUM('待走访','已完成','已逾期','已取消') NOT NULL DEFAULT '待走访',
    assigned_police_no VARCHAR(20) NOT NULL COMMENT '负责走访的民警',
    is_alerted TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '超期未走访的预警是否已发送',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='走访计划表';

-- ============================================================
-- 14. 失踪人员表
-- ============================================================
CREATE TABLE IF NOT EXISTS `missing_person` (
    rid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    resident_uuid VARCHAR(36) NOT NULL COMMENT '失踪人的户籍业务UUID，不确定时可使用占位符',
    missing_date DATE NOT NULL COMMENT '失踪日期，可为范围',
    missing_place VARCHAR(200) NOT NULL COMMENT '失踪地点',
    photo VARCHAR(500) NOT NULL COMMENT '失踪人照片路径，1-5张，单张≤5MB，分号分隔',
    appearance TEXT NOT NULL COMMENT '失踪时特征：穿着/面部特征/动作习惯等',
    medical_history TEXT COMMENT '既往病史记录',
    possible_way VARCHAR(200) COMMENT '可能的失踪方式',
    contact_phone VARCHAR(20) NOT NULL COMMENT '失踪人关系人的联系方式',
    status ENUM('失踪中','已经寻回') NOT NULL DEFAULT '失踪中',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='失踪人员表';

-- ============================================================
-- 15. 失踪人员寻回记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS `missing_person_recovery` (
    rid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    missing_record_rid BIGINT UNSIGNED NOT NULL COMMENT '失踪人员记录的记录ID',
    recovery_date DATE NOT NULL COMMENT '寻回日期',
    summary TEXT COMMENT '寻回成功的案例摘要',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='失踪人员寻回记录表';

-- ============================================================
-- 16. 户口本表
-- ============================================================
CREATE TABLE IF NOT EXISTS `household_register` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    household_book_no VARCHAR(36) NOT NULL COMMENT '户口本的唯一编号',
    householder_uuid VARCHAR(36) NOT NULL COMMENT '户主的UUID',
    establish_date DATE NOT NULL COMMENT '立户日期',
    hukou_address VARCHAR(200) NOT NULL COMMENT '户籍地址',
    hukou_area_id BIGINT UNSIGNED COMMENT '户籍地址关联区域表',
    status ENUM('审批中','有效','冻结','无效') NOT NULL DEFAULT '审批中',
    member_uuid_list TEXT COMMENT '同户人UUID列表，分号分隔',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_household_book_no (household_book_no)
) ENGINE=InnoDB COMMENT='户口本表';

-- ============================================================
-- 17. 户籍地业务请求表
-- ============================================================
CREATE TABLE IF NOT EXISTS `household_business_request` (
    rid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '处理记录的唯一标识符',
    handler_uuid VARCHAR(36) COMMENT '审核或处理该业务的工作人员UUID',
    applicant_uuid VARCHAR(36) NOT NULL COMMENT '实际受理办理该业务的群众UUID',
    attachment VARCHAR(500) NOT NULL COMMENT '相关证明材料附件路径，分号分隔',
    business_type ENUM('登记','注销','户主变更') NOT NULL COMMENT '业务类型',
    handle_date DATE NOT NULL COMMENT '办理日期',
    handle_basis VARCHAR(200) COMMENT '法律/政策依据',
    fee DECIMAL(10,2) COMMENT '办理收费金额',
    status ENUM('审批中','批准','驳回') NOT NULL DEFAULT '审批中',
    reject_reason VARCHAR(500) COMMENT '驳回原因',
    remark TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='户籍地业务请求表';

-- ============================================================
-- 18. 户籍迁移业务请求表
-- ============================================================
CREATE TABLE IF NOT EXISTS `household_migration_request` (
    rid BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '处理记录的唯一标识符',
    handler_uuid VARCHAR(36) COMMENT '审核或处理该业务的工作人员UUID',
    applicant_uuid VARCHAR(36) NOT NULL COMMENT '实际受理办理该业务的群众UUID',
    incoming_address VARCHAR(200) COMMENT '迁入地址',
    incoming_area_id BIGINT UNSIGNED COMMENT '迁入地关联区域表',
    outgoing_address VARCHAR(200) COMMENT '迁出地址',
    outgoing_area_id BIGINT UNSIGNED COMMENT '迁出地关联区域表',
    attachment VARCHAR(500) NOT NULL COMMENT '相关证明材料附件路径',
    business_type ENUM('市内','省内','跨省') NOT NULL COMMENT '迁移类型',
    handle_date DATE NOT NULL COMMENT '办理日期',
    handle_basis VARCHAR(200) COMMENT '法律/政策依据',
    fee DECIMAL(10,2) COMMENT '办理收费金额',
    status ENUM('准迁证审批中','准迁证审批驳回','迁移证审批中','迁移证审批驳回','迁移审批中','迁移审批驳回','迁移审批通过') NOT NULL,
    reject_reason VARCHAR(500) COMMENT '驳回原因',
    approval_permit_no VARCHAR(36) COMMENT '准迁证编号',
    migration_permit_no VARCHAR(36) COMMENT '迁移证编号',
    remark TEXT COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0
) ENGINE=InnoDB COMMENT='户籍迁移业务请求表';

-- ============================================================
-- 19. 准迁证表
-- ============================================================
CREATE TABLE IF NOT EXISTS `approval_permit` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    permit_no VARCHAR(36) NOT NULL COMMENT '准迁证号',
    issue_date DATE NOT NULL COMMENT '签发日期',
    expiry_date DATE NOT NULL COMMENT '有效期至',
    issuing_authority VARCHAR(100) NOT NULL COMMENT '签发机关',
    status ENUM('审批中','有效','作废') NOT NULL DEFAULT '审批中',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_permit_no (permit_no)
) ENGINE=InnoDB COMMENT='准迁证表';

-- ============================================================
-- 20. 迁移证表
-- ============================================================
CREATE TABLE IF NOT EXISTS `migration_permit` (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    permit_no VARCHAR(36) NOT NULL COMMENT '迁移证号',
    issue_date DATE NOT NULL COMMENT '签发日期',
    expiry_date DATE NOT NULL COMMENT '有效期至',
    outgoing_police_station VARCHAR(100) NOT NULL COMMENT '迁出地派出所',
    status ENUM('审批中','有效','作废') NOT NULL DEFAULT '审批中',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_permit_no (permit_no)
) ENGINE=InnoDB COMMENT='迁移证表';

-- ============================================================
-- 21. 区域表
-- ============================================================
CREATE TABLE IF NOT EXISTS `area` (
    area_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '区域ID',
    area_code VARCHAR(12) NOT NULL COMMENT '行政区划编码，如130302001001，参考GB/T 2260-2013',
    area_name VARCHAR(100) NOT NULL COMMENT '区域名称',
    parent_id BIGINT UNSIGNED COMMENT '自引用上级ID，省级为NULL',
    area_level ENUM('省','市','区县','街道','社区') NOT NULL COMMENT '区域层级',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_area_code (area_code),
    INDEX idx_parent_id (parent_id),
    INDEX idx_area_level (area_level)
) ENGINE=InnoDB COMMENT='区域表';

-- ============================================================
-- 22. 操作日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `audit_log` (
    log_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
    operator_uuid VARCHAR(36) NOT NULL COMMENT '操作人UUID',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    ip_address VARCHAR(45) COMMENT '操作设备IP地址',
    operation_type ENUM('新增','修改','删除') NOT NULL COMMENT '操作类型',
    target_type VARCHAR(50) NOT NULL COMMENT '操作的数据表名',
    target_id VARCHAR(36) COMMENT '操作记录的主键ID',
    before_data TEXT COMMENT '变更前JSON快照，新增时为NULL',
    after_data TEXT COMMENT '变更后JSON快照，删除时为NULL',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    INDEX idx_operator_uuid (operator_uuid),
    INDEX idx_operation_time (operation_time),
    INDEX idx_target_type (target_type)
) ENGINE=InnoDB COMMENT='操作日志表';

-- ============================================================
-- 23. 预警表
-- ============================================================
CREATE TABLE IF NOT EXISTS `alert` (
    alert_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '预警ID',
    alert_type ENUM('居住证到期','走访逾期','重点人员匹配','证件到期','其他') NOT NULL,
    target_type VARCHAR(50) NOT NULL COMMENT '关联实体类型',
    target_id VARCHAR(36) NOT NULL COMMENT '关联实体的主键ID',
    alert_content VARCHAR(500) NOT NULL COMMENT '预警详细描述',
    severity ENUM('高','中','低') NOT NULL COMMENT '严重级别',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预警生成时间',
    is_handled TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '是否已被人工处理',
    handled_by VARCHAR(36) COMMENT '处理预警的用户UUID',
    handled_at DATETIME COMMENT '预警处理时间',
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    INDEX idx_create_time (create_time),
    INDEX idx_alert_type (alert_type),
    INDEX idx_is_handled (is_handled)
) ENGINE=InnoDB COMMENT='预警表';

-- ============================================================
-- 24. 登录日志表
-- ============================================================
CREATE TABLE IF NOT EXISTS `login_log` (
    log_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '登录日志ID',
    user_uuid VARCHAR(36) NOT NULL COMMENT '登录用户UUID',
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    ip_address VARCHAR(45) NOT NULL COMMENT '登录设备IP地址',
    is_success TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '登录成功/失败',
    fail_reason VARCHAR(100) COMMENT '失败原因：密码错误/账号锁定/账号注销/账号不存在',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT UNSIGNED NOT NULL DEFAULT 0,
    INDEX idx_user_uuid (user_uuid),
    INDEX idx_login_time (login_time)
) ENGINE=InnoDB COMMENT='登录日志表';

-- ============================================================
-- 初始化数据
-- ============================================================
-- 默认权限组
INSERT INTO permission_group (group_name, description, permissions) VALUES
('系统管理员组', '拥有全部系统权限', '["*"]'),
('民警组', '民警操作权限', '["resident:read","resident:write","keyperson:read","keyperson:write","household:read","missing:read"]'),
('街道办组', '街道办审核权限', '["resident:read","household:read","household:approve","fp:read"]'),
('普通用户组', '基础查询权限', '["resident:read","statistics:read"]');

-- 默认管理员账号 (密码: Admin@123)
INSERT INTO user (user_uuid, username, password, resident_uuid, user_role, permission_group_id, phone, account_status, must_change_password, register_materials)
VALUES ('admin-0000-0000-0000-000000000001', 'admin',
        '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EHsM', 'R00000000000000000001',
        '系统管理员', 1, '13800000000', '有效', 1, 'system-init');
