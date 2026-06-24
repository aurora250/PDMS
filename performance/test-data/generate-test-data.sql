-- ============================================================
-- PDM 性能测试数据生成脚本
-- 生成 10,000+ 常住人口记录用于压力测试
-- ============================================================

USE pdm_db;

-- 清空测试数据 (保留 schema)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE resident;
TRUNCATE TABLE resident_relation;
TRUNCATE TABLE fp_register_record;
TRUNCATE TABLE resident_permit;
TRUNCATE TABLE key_person;
TRUNCATE TABLE visit_plan;
TRUNCATE TABLE petition_record;
SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 生成区域数据 (省/市/区/街道/社区 五级)
-- ============================================================
INSERT INTO area (area_code, area_name, parent_id, area_level) VALUES
('110000000000', '北京市', NULL, '省'),
('110100000000', '北京市市辖区', 1, '市'),
('110101000000', '东城区', 2, '区县'),
('110101001000', '东华门街道', 3, '街道'),
('110101001001', '多福巷社区', 4, '社区'),
('110101001002', '银闸社区', 4, '社区'),
('110101002000', '景山街道', 3, '街道'),
('110101002001', '隆福寺社区', 7, '社区'),
('110102000000', '西城区', 2, '区县'),
('110102001000', '西长安街街道', 9, '街道'),
('110102001001', '府右街南社区', 10, '社区');

-- ============================================================
-- 批量生成 10,000 条常住人口测试数据
-- ============================================================
DELIMITER //
CREATE OR REPLACE PROCEDURE generate_residents(IN count INT)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE v_uuid VARCHAR(36);
    DECLARE v_name VARCHAR(50);
    DECLARE v_id_card CHAR(18);
    DECLARE v_gender ENUM('男','女');
    DECLARE v_birth DATE;
    DECLARE v_area_id BIGINT;

    DECLARE surnames VARCHAR(200) DEFAULT '张李王刘陈杨赵黄周吴徐孙马胡朱郭何罗高林郑梁谢唐许冯宋韩邓彭曹曾田董潘袁蔡蒋余于杜叶程魏苏吕丁任卢姚钟姜崔谭陆汪范金石廖贾夏韦付方白邹孟熊秦邱江尹薛闫段雷侯龙史陶黎贺顾毛郝龚邵万钱严覃武戴莫孔向汤';
    DECLARE male_names VARCHAR(200) DEFAULT '伟强磊洋勇军杰涛明超平辉鹏华飞斌浩宇轩博文刚毅峰俊亮云龙翔凯瑞霖鑫';
    DECLARE female_names VARCHAR(200) DEFAULT '芳敏静丽婷雪琳萍红霞娟慧颖娜艳琴玲秀兰洁梅燕丹莲薇青倩蓉';

    DECLARE edu_levels VARCHAR(200) DEFAULT '小学及以下,初中,高中,中专,大专,本科,硕士研究生,博士研究生';
    DECLARE marital_statuses VARCHAR(50) DEFAULT '未婚,已婚,离异,丧偶';
    DECLARE blood_types VARCHAR(20) DEFAULT 'A,B,AB,O,未知';
    DECLARE nations VARCHAR(200) DEFAULT '汉族,蒙古族,回族,藏族,维吾尔族,苗族,彝族,壮族,布依族,朝鲜族,满族';

    WHILE i <= count DO
        -- Generate UUID
        SET v_uuid = CONCAT('R', LPAD(i, 15, '0'));

        -- Random gender
        SET v_gender = IF(RAND() < 0.51, '男', '女');

        -- Random name
        IF v_gender = '男' THEN
            SET v_name = CONCAT(
                SUBSTRING(surnames, FLOOR(1 + RAND() * CHAR_LENGTH(surnames)), 1),
                SUBSTRING(male_names, FLOOR(1 + RAND() * CHAR_LENGTH(male_names) / 3) * 3 - 2, 1),
                SUBSTRING(male_names, FLOOR(1 + RAND() * CHAR_LENGTH(male_names) / 3) * 3 - 2, 1)
            );
        ELSE
            SET v_name = CONCAT(
                SUBSTRING(surnames, FLOOR(1 + RAND() * CHAR_LENGTH(surnames)), 1),
                SUBSTRING(female_names, FLOOR(1 + RAND() * CHAR_LENGTH(female_names) / 3) * 3 - 2, 1),
                SUBSTRING(female_names, FLOOR(1 + RAND() * CHAR_LENGTH(female_names) / 3) * 3 - 2, 1)
            );
        END IF;

        -- Random birth date (1940-2010)
        SET v_birth = DATE_ADD('1940-01-01', INTERVAL FLOOR(RAND() * 25567) DAY);

        -- Random ID card (simplified valid format)
        SET v_id_card = CONCAT(
            LPAD(FLOOR(RAND() * 999999), 6, '0'),
            DATE_FORMAT(v_birth, '%Y%m%d'),
            LPAD(FLOOR(RAND() * 999), 3, '0'),
            IF(v_gender = '男', IF(RAND() < 0.5, '1', '3'), IF(RAND() < 0.5, '0', '2')),
            IF(RAND() < 0.5, FLOOR(RAND() * 10), 'X')
        );

        -- Random area (1-11)
        SET v_area_id = FLOOR(1 + RAND() * 11);

        INSERT INTO resident (uuid, name, gender, id_card_no, nation, birth_date,
            education_level, blood_type, marital_status, occupation, phone,
            residence, area_id, household_type, household_status,
            household_address, household_area_id)
        VALUES (
            v_uuid, v_name, v_gender, v_id_card,
            ELT(FLOOR(1 + RAND() * 11), '汉族','蒙古族','回族','藏族','维吾尔族','苗族','彝族','壮族','布依族','朝鲜族','满族'),
            v_birth,
            ELT(FLOOR(1 + RAND() * 8), '小学及以下','初中','高中','中专','大专','本科','硕士研究生','博士研究生'),
            ELT(FLOOR(1 + RAND() * 5), 'A','B','AB','O','未知'),
            ELT(FLOOR(1 + RAND() * 4), '未婚','已婚','离异','丧偶'),
            CONCAT('职业-', LPAD(i, 5, '0')),
            CONCAT('138', LPAD(FLOOR(RAND() * 99999999), 8, '0')),
            CONCAT('北京市', ELT(FLOOR(1+RAND()*2), '东城区','西城区'), '某某街道', i, '号'),
            v_area_id,
            '居民户口',
            IF(RAND() < 0.9, '正常', ELT(FLOOR(1+RAND()*3), '死亡注销','迁出注销','恢复')),
            CONCAT('北京市', ELT(FLOOR(1+RAND()*2), '东城区','西城区'), '某某路', i, '号'),
            v_area_id
        );

        SET i = i + 1;
        IF i % 1000 = 0 THEN
            COMMIT;
        END IF;
    END WHILE;
    COMMIT;
END //
DELIMITER ;

-- 执行生成
CALL generate_residents(10000);

-- ============================================================
-- 生成流动人口测试数据 (2,000条)
-- ============================================================
INSERT INTO fp_register_record (uuid, register_date, reviewer_uuid, reject_reason)
SELECT uuid, DATE_ADD(birth_date, INTERVAL 20 YEAR), NULL, NULL
FROM resident
ORDER BY RAND()
LIMIT 2000;

-- ============================================================
-- 生成重点人员测试数据 (500条)
-- ============================================================
INSERT INTO key_person (uuid, control_level, control_type, designated_at, responsible_police_no)
SELECT uuid,
    ELT(FLOOR(1+RAND()*3), '一级','二级','三级'),
    ELT(FLOOR(1+RAND()*7), '涉稳人员','涉毒人员','社区矫正人员','精神障碍患者(肇事肇祸风险)','刑满释放人员','信访重点人员','其他重点人员'),
    NOW(),
    CONCAT('P', LPAD(FLOOR(RAND() * 99999), 5, '0'))
FROM resident
ORDER BY RAND()
LIMIT 500;

-- ============================================================
-- 统计
-- ============================================================
SELECT '常住人口' AS data_type, COUNT(*) AS count FROM resident
UNION ALL
SELECT '流动人口', COUNT(*) FROM fp_register_record
UNION ALL
SELECT '重点人员', COUNT(*) FROM key_person
UNION ALL
SELECT '区域', COUNT(*) FROM area;
