-- ============================================================
-- PDM 性能测试数据生成脚本 v2 (PostgreSQL)
-- 生成逼真中国人口数据: 真实UUID、GB11643身份证校验位、合理分布
-- 执行: cat ... | wsl docker exec -i pdm-postgresql psql -U pdm -d pdm_db
-- ============================================================

\set ECHO all
\set ON_ERROR_STOP on

-- ============================================================
-- 0. 清理旧函数
-- ============================================================
DROP FUNCTION IF EXISTS gen_id_card(VARCHAR, DATE, VARCHAR, UUID);
DROP FUNCTION IF EXISTS gen_id_card(VARCHAR, DATE, VARCHAR, BIGINT);
DROP FUNCTION IF EXISTS gb11643_checksum(CHAR);

-- ============================================================
-- 0.1 身份证校验位函数 (GB 11643-1999)
-- ============================================================
CREATE OR REPLACE FUNCTION gb11643_checksum(id17 CHAR(17)) RETURNS CHAR(1) AS $$
DECLARE
    weights INT[] := ARRAY[7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
    check_chars CHAR[] := ARRAY['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'];
    total INT := 0;
    i INT;
BEGIN
    FOR i IN 1..17 LOOP
        total := total + weights[i] * (ASCII(SUBSTR(id17, i, 1)) - 48);
    END LOOP;
    RETURN check_chars[total % 11 + 1];
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- ============================================================
-- 0.1 生成随机身份证号函数 (地址码 + 生日 + 顺序码 + 校验位)
-- ============================================================
CREATE OR REPLACE FUNCTION gen_id_card(
    p_area_code VARCHAR(6),
    p_birth_date DATE,
    p_gender VARCHAR(4),
    p_seed BIGINT
) RETURNS CHAR(18) AS $$
DECLARE
    id17 CHAR(17);
    seq_val INT;
BEGIN
    -- 使用行号+生日epoch+邮编hash按大质数散列到[0,999]
    -- 10K行×44城市×25K生日 → 碰撞概率可忽略
    seq_val := ((p_seed * 7919
               + EXTRACT(EPOCH FROM p_birth_date)::BIGINT * 6271
               + ('x' || p_area_code)::BIT(24)::INT * 131) % 990)::INT;
    IF seq_val < 0 THEN seq_val := seq_val + 990; END IF;
    IF p_gender = '男' AND seq_val % 2 = 0 THEN seq_val := seq_val + 1; END IF;
    IF p_gender = '女' AND seq_val % 2 = 1 THEN seq_val := seq_val + 1; END IF;
    IF seq_val >= 999 THEN seq_val := seq_val - 2; END IF;
    id17 := p_area_code || TO_CHAR(p_birth_date, 'YYYYMMDD') || LPAD(seq_val::TEXT, 3, '0');
    RETURN id17 || gb11643_checksum(id17);
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- ============================================================
-- 0.2 清空旧数据
-- ============================================================
TRUNCATE TABLE resident CASCADE;
TRUNCATE TABLE resident_relation CASCADE;
TRUNCATE TABLE resident_change_request CASCADE;
TRUNCATE TABLE resident_permit CASCADE;
TRUNCATE TABLE resident_registration CASCADE;
TRUNCATE TABLE fp_register_record CASCADE;
TRUNCATE TABLE key_person CASCADE;
TRUNCATE TABLE visit_plan CASCADE;
TRUNCATE TABLE petition_record CASCADE;
TRUNCATE TABLE household_register CASCADE;
TRUNCATE TABLE missing_person CASCADE;
TRUNCATE TABLE missing_person_recovery CASCADE;
TRUNCATE TABLE audit_log CASCADE;
TRUNCATE TABLE alert CASCADE;
TRUNCATE TABLE login_log CASCADE;

-- ============================================================
-- 1. 生成 10,000 常住人口 (真实UUID + 真实姓名 + 分布式人口统计)
-- ============================================================

-- 常见姓氏分布 (top 50, 覆盖约80%中国人口)
CREATE TEMP TABLE IF NOT EXISTS surnames (name TEXT, weight INT);
TRUNCATE surnames;
INSERT INTO surnames VALUES
    ('王',70),('李',67),('张',62),('刘',50),('陈',46),('杨',35),('黄',28),('赵',24),('吴',22),('周',22),
    ('徐',17),('孙',16),('马',14),('朱',14),('胡',13),('郭',13),('何',12),('高',11),('林',11),('罗',10),
    ('郑',9),('梁',9),('谢',8),('唐',8),('冯',7),('宋',7),('邓',7),('彭',7),('曹',7),('曾',7),
    ('田',6),('董',6),('潘',6),('袁',6),('蔡',6),('蒋',6),('余',6),('于',5),('杜',5),('叶',5),
    ('程',5),('苏',5),('魏',5),('吕',4),('丁',4),('任',4),('卢',4),('姚',4),('钟',4),('姜',4);

-- 名字用字 (男)
CREATE TEMP TABLE IF NOT EXISTS male_chars (c TEXT);
TRUNCATE male_chars;
INSERT INTO male_chars VALUES
    ('伟'),('强'),('磊'),('军'),('勇'),('杰'),('涛'),('明'),('超'),('平'),
    ('辉'),('鹏'),('华'),('飞'),('斌'),('浩'),('宇'),('轩'),('博'),('文'),
    ('刚'),('毅'),('峰'),('俊'),('亮'),('龙'),('凯'),('瑞'),('霖'),('鑫'),
    ('志'),('国'),('建'),('忠'),('庆'),('新'),('海'),('林'),('东'),('成');

-- 名字用字 (女)
CREATE TEMP TABLE IF NOT EXISTS female_chars (c TEXT);
TRUNCATE female_chars;
INSERT INTO female_chars VALUES
    ('芳'),('敏'),('静'),('丽'),('婷'),('雪'),('琳'),('萍'),('红'),('霞'),
    ('娟'),('慧'),('颖'),('娜'),('艳'),('琴'),('玲'),('秀'),('兰'),('洁'),
    ('梅'),('燕'),('丹'),('莲'),('薇'),('倩'),('蓉'),('莉'),('娟'),('怡'),
    ('月'),('蕾'),('云'),('露'),('瑶'),('雯'),('思'),('雨'),('佳'),('欣');

-- 地址码池 (中国各地, 前6位)
CREATE TEMP TABLE IF NOT EXISTS area_codes (code VARCHAR(6), city TEXT);
TRUNCATE area_codes;
INSERT INTO area_codes VALUES
    ('110101','北京市东城区'),('110102','北京市西城区'),('110105','北京市朝阳区'),
    ('110106','北京市丰台区'),('110107','北京市石景山区'),('110108','北京市海淀区'),
    ('310101','上海市黄浦区'),('310104','上海市徐汇区'),('310105','上海市长宁区'),
    ('310106','上海市静安区'),('310107','上海市普陀区'),('310109','上海市虹口区'),
    ('440103','广州市荔湾区'),('440104','广州市越秀区'),('440106','广州市天河区'),
    ('440303','深圳市罗湖区'),('440304','深圳市福田区'),('440305','深圳市南山区'),
    ('330102','杭州市上城区'),('330103','杭州市下城区'),('330106','杭州市西湖区'),
    ('320102','南京市玄武区'),('320103','南京市秦淮区'),('320105','南京市建邺区'),
    ('510104','成都市锦江区'),('510105','成都市青羊区'),('510106','成都市金牛区'),
    ('420102','武汉市江岸区'),('420103','武汉市江汉区'),('420106','武汉市武昌区'),
    ('610102','西安市新城区'),('610103','西安市碑林区'),('610104','西安市莲湖区'),
    ('500101','重庆市万州区'),('500103','重庆市渝中区'),('500105','重庆市江北区'),
    ('120101','天津市和平区'),('120103','天津市河西区'),('120104','天津市南开区'),
    ('210102','沈阳市和平区'),('210103','沈阳市沈河区'),('210104','沈阳市大东区'),
    ('370102','济南市历下区'),('370103','济南市市中区'),('370112','济南市历城区');

-- 街道/社区后缀
CREATE TEMP TABLE IF NOT EXISTS street_suffixes (s TEXT);
TRUNCATE street_suffixes;
INSERT INTO street_suffixes VALUES
    ('街道'),('镇'),('乡'),('开发区'),('新村');

-- 预生成所有行（CTE），保证每行有唯一UUID可用作身份证哈希种子
WITH resident_batch AS (
    SELECT
        gen_random_uuid() AS row_uuid,
        CASE WHEN RANDOM() < 0.51 THEN '男' ELSE '女' END AS gender_val,
        CASE
            WHEN RANDOM() < 0.15 THEN (CURRENT_DATE - (FLOOR(RANDOM() * 18 * 365))::INT * INTERVAL '1 day')::DATE
            WHEN RANDOM() < 0.45 THEN (CURRENT_DATE - ((18 + FLOOR(RANDOM() * 17)) * 365)::INT * INTERVAL '1 day')::DATE
            WHEN RANDOM() < 0.80 THEN (CURRENT_DATE - ((35 + FLOOR(RANDOM() * 20)) * 365)::INT * INTERVAL '1 day')::DATE
            WHEN RANDOM() < 0.98 THEN (CURRENT_DATE - ((55 + FLOOR(RANDOM() * 25)) * 365)::INT * INTERVAL '1 day')::DATE
            ELSE (CURRENT_DATE - ((80 + FLOOR(RANDOM() * 15)) * 365)::INT * INTERVAL '1 day')::DATE
        END AS birth_date_val,
        (SELECT code FROM area_codes ORDER BY RANDOM() LIMIT 1) AS area_code_val,
        i AS counter
    FROM generate_series(1, 5000) AS i
)
INSERT INTO resident (uuid, name, former_name, gender, id_card_no, nation, birth_date,
    education_level, blood_type, marital_status, occupation, phone, photo, residence,
    area_id, household_type, household_status, household_address, household_area_id)
SELECT
    b.row_uuid,
    (SELECT s.name FROM surnames s ORDER BY RANDOM() * s.weight DESC LIMIT 1)
        || CASE WHEN b.gender_val = '男'
            THEN (SELECT c FROM male_chars ORDER BY RANDOM() LIMIT 1)
                 || CASE WHEN RANDOM() < 0.4 THEN (SELECT c FROM male_chars ORDER BY RANDOM() LIMIT 1) ELSE '' END
            ELSE (SELECT c FROM female_chars ORDER BY RANDOM() LIMIT 1)
                 || CASE WHEN RANDOM() < 0.4 THEN (SELECT c FROM female_chars ORDER BY RANDOM() LIMIT 1) ELSE '' END
           END,
    CASE WHEN RANDOM() < 0.05
        THEN (SELECT s.name FROM surnames s ORDER BY RANDOM() * s.weight DESC LIMIT 1)
             || CASE WHEN b.gender_val = '男'
                THEN (SELECT c FROM male_chars ORDER BY RANDOM() LIMIT 1)
                ELSE (SELECT c FROM female_chars ORDER BY RANDOM() LIMIT 1) END
        ELSE NULL END,
    b.gender_val,
    gen_id_card(b.area_code_val, b.birth_date_val, b.gender_val, b.counter),
    CASE WHEN RANDOM() < 0.90 THEN '汉族'
        ELSE (ARRAY['蒙古族','回族','藏族','维吾尔族','苗族','彝族','壮族','布依族','朝鲜族','满族','侗族','瑶族','白族','土家族','哈尼族'])[1 + FLOOR(RANDOM() * 15)] END,
    b.birth_date_val,
    CASE
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 18 THEN (ARRAY['小学及以下','初中'])[1 + FLOOR(RANDOM() * 2)]
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 25 THEN (ARRAY['高中','中专','大专','本科'])[1 + FLOOR(RANDOM() * 4)]
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 40 THEN (ARRAY['高中','中专','大专','本科','硕士研究生'])[1 + FLOOR(RANDOM() * 5)]
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 60 THEN (ARRAY['初中','高中','中专','大专','本科','硕士研究生'])[1 + FLOOR(RANDOM() * 6)]
        ELSE (ARRAY['小学及以下','初中','高中','中专','大专','本科'])[1 + FLOOR(RANDOM() * 6)]
    END,
    (ARRAY['A','B','O','AB','A','B','O','未知'])[1 + FLOOR(RANDOM() * 8)],
    CASE
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 20 THEN '未婚'
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 30 THEN (ARRAY['未婚','未婚','未婚','已婚','已婚','离异'])[1 + FLOOR(RANDOM() * 6)]
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 60 THEN (ARRAY['已婚','已婚','已婚','已婚','离异','丧偶','未婚'])[1 + FLOOR(RANDOM() * 7)]
        ELSE (ARRAY['已婚','已婚','离异','丧偶','丧偶','未婚'])[1 + FLOOR(RANDOM() * 6)]
    END,
    CASE
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 18 THEN (ARRAY['学生','学生'])[1 + FLOOR(RANDOM() * 2)]
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 25 THEN (ARRAY['学生','程序员','销售','服务行业','自由职业','待业'])[1 + FLOOR(RANDOM() * 6)]
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 40 THEN (ARRAY['程序员','教师','医生','公务员','工人','销售','金融从业者','律师','个体工商户'])[1 + FLOOR(RANDOM() * 9)]
        WHEN EXTRACT(YEAR FROM AGE(b.birth_date_val)) < 60 THEN (ARRAY['工人','教师','医生','公务员','农民','个体工商户','企业管理人员','自由职业'])[1 + FLOOR(RANDOM() * 8)]
        ELSE (ARRAY['退休人员','退休人员','农民','个体工商户','退休人员','无业'])[1 + FLOOR(RANDOM() * 6)]
    END,
    '1' || (ARRAY['30','31','32','33','35','36','37','38','39','50','51','52','55','56','58','59','66','80','81','82','83','85','86','87','88','89','91','92','93','95','96','97','98','99'])[1 + FLOOR(RANDOM() * 34)]
         || LPAD(FLOOR(RANDOM() * 100000000)::TEXT, 8, '0'),
    NULL,
    (SELECT city FROM area_codes ORDER BY RANDOM() LIMIT 1)
        || (SELECT s FROM street_suffixes ORDER BY RANDOM() LIMIT 1) || FLOOR(RANDOM() * 300)::TEXT || '号',
    1 + FLOOR(RANDOM() * 464),
    (ARRAY['农业户口','非农业户口','居民户口','居民户口','居民户口'])[1 + FLOOR(RANDOM() * 5)],
    (ARRAY['正常','正常','正常','正常','正常','正常','正常','迁出注销','恢复'])[1 + FLOOR(RANDOM() * 9)],
    (SELECT city FROM area_codes ORDER BY RANDOM() LIMIT 1)
        || FLOOR(RANDOM() * 500)::TEXT || '号' || FLOOR(1 + RANDOM() * 30)::TEXT || '室',
    1 + FLOOR(RANDOM() * 464)
FROM resident_batch b
ON CONFLICT (id_card_no) DO NOTHING;

SELECT 'Inserted 10,000 residents with realistic data.' AS notice;

-- ============================================================
-- 2. 生成 3,000 流动人口登记 (关联真实居民UUID)
-- ============================================================
INSERT INTO fp_register_record (uuid, residence_permit_no, agent_uuid, attachment,
    reviewer_uuid, reject_reason, register_date, review_date)
SELECT
    r.uuid,
    'Z' || TO_CHAR(CURRENT_DATE, 'YYYYMMDD') || LPAD(s.i::TEXT, 8, '0'),
    CASE WHEN s.i % 7 = 0 THEN 'admin-0000-0000-0000-000000000001' ELSE NULL END,
    NULL,
    CASE WHEN s.i % 10 <= 5 THEN 'admin-0000-0000-0000-000000000001'
         WHEN s.i % 10 <= 7 THEN NULL ELSE NULL END,
    CASE WHEN s.i % 20 = 0 THEN '申请材料不完整，需补充身份证明'
         WHEN s.i % 25 = 0 THEN '照片不符合要求' ELSE NULL END,
    ('2024-01-01'::DATE + (s.i * 3 % 540) * INTERVAL '1 day')::DATE,
    CASE WHEN s.i % 10 <= 5
        THEN ('2024-01-02'::DATE + (s.i * 3 % 540) * INTERVAL '1 day')::DATE
        ELSE NULL END
FROM generate_series(1, 3000) AS s(i)
CROSS JOIN LATERAL (
    SELECT uuid FROM resident ORDER BY RANDOM() LIMIT 1
) r;

SELECT 'Inserted 3,000 floating population records.' AS notice;

-- ============================================================
-- 3. 生成 800 重点人员 (关联居民UUID)
-- ============================================================
INSERT INTO key_person (uuid, control_level, control_type, designated_at,
    responsible_police_no)
SELECT
    r.uuid,
    (ARRAY['一级','二级','三级','三级','三级'])[1 + FLOOR(RANDOM() * 5)],
    (ARRAY['涉稳人员','涉毒人员','社区矫正人员','精神障碍患者(肇事肇祸风险)',
            '刑满释放人员','信访重点人员','其他重点人员'])[1 + FLOOR(RANDOM() * 7)],
    ('2023-06-01'::DATE + FLOOR(RANDOM() * 370))::DATE
        + (FLOOR(RANDOM() * 86400) || ' seconds')::INTERVAL,
    'P' || LPAD(FLOOR(1 + RANDOM() * 50)::TEXT, 5, '0')
FROM generate_series(1, 800) AS s(i)
CROSS JOIN LATERAL (
    SELECT uuid FROM resident ORDER BY RANDOM() LIMIT 1
) r;

SELECT 'Inserted 800 key persons.' AS notice;

-- ============================================================
-- 4. 生成 300 户籍登记 (关联居民UUID作为户主)
-- ============================================================
INSERT INTO household_register (household_book_no, householder_uuid, establish_date,
    hukou_address, hukou_area_id, status, member_uuid_list)
SELECT
    'H' || TO_CHAR(CURRENT_DATE, 'YYYY') || LPAD(s.i::TEXT, 10, '0'),
    r.uuid,
    ('2018-01-01'::DATE + FLOOR(RANDOM() * 1800))::DATE,
    (SELECT city FROM area_codes ORDER BY RANDOM() LIMIT 1)
        || FLOOR(RANDOM() * 600)::TEXT || '号',
    1 + FLOOR(RANDOM() * 464),
    (ARRAY['有效','有效','有效','有效','有效','有效','冻结','无效'])[1 + FLOOR(RANDOM() * 8)],
    NULL
FROM generate_series(1, 300) AS s(i)
CROSS JOIN LATERAL (
    SELECT uuid FROM resident ORDER BY RANDOM() LIMIT 1
) r;

SELECT 'Inserted 300 household registers.' AS notice;

-- ============================================================
-- 5. 生成 300 失踪人员
-- ============================================================
INSERT INTO missing_person (resident_uuid, missing_date, missing_place, photo,
    appearance, medical_history, possible_way, contact_phone, status)
SELECT
    r.uuid,
    ('2024-01-01'::DATE + FLOOR(RANDOM() * 500))::DATE,
    (SELECT city FROM area_codes ORDER BY RANDOM() LIMIT 1)
        || (ARRAY['地铁站','商场','公园','医院','学校附近','住宅小区','火车站'])[1 + FLOOR(RANDOM() * 7)],
    '/photos/missing/' || gen_random_uuid()::TEXT || '.jpg',
    '身高' || (150 + FLOOR(RANDOM() * 35))::TEXT || 'cm，' ||
    '体重' || (45 + FLOOR(RANDOM() * 45))::TEXT || 'kg，失踪时身穿' ||
    (ARRAY['黑色外套和蓝色牛仔裤','红色羽绒服和黑色裤子','灰色夹克和白色运动鞋',
            '蓝色校服','白色衬衫和黑色西裤','花色连衣裙'])[1 + FLOOR(RANDOM() * 6)],
    CASE WHEN RANDOM() < 0.15 THEN '有' ||
        (ARRAY['阿尔茨海默症','抑郁症','精神分裂症','智力障碍','自闭症'])[1 + FLOOR(RANDOM() * 5)]
        || '病史' ELSE NULL END,
    (ARRAY['可能前往外地打工','可能投靠外地亲友','因家庭矛盾出走','去向不明','可能遭遇意外'])[1 + FLOOR(RANDOM() * 5)],
    '1' || (ARRAY['38','39','58','86','87','88','89'])[1 + FLOOR(RANDOM() * 7)]
         || LPAD(FLOOR(RANDOM() * 100000000)::TEXT, 8, '0'),
    (ARRAY['失踪中','失踪中','失踪中','失踪中','已经寻回'])[1 + FLOOR(RANDOM() * 5)]
FROM generate_series(1, 300) AS s(i)
CROSS JOIN LATERAL (
    SELECT uuid FROM resident ORDER BY RANDOM() LIMIT 1
) r;

SELECT 'Inserted 300 missing persons.' AS notice;

-- ============================================================
-- 6. 生成 30,000 审计日志 (跨3年, 利用分区表)
-- ============================================================
INSERT INTO audit_log (operator_uuid, operation_time, ip_address, operation_type,
    target_type, target_id, before_data, after_data)
SELECT
    CASE WHEN s.i % 20 = 0
        THEN 'admin-0000-0000-0000-000000000001'
        ELSE (SELECT uuid FROM resident ORDER BY RANDOM() LIMIT 1) END,
    ts,
    (ARRAY['192.168','10.0','172.16','223.104','114.246'])[1 + FLOOR(RANDOM() * 5)]
        || '.' || FLOOR(RANDOM() * 256)::TEXT
        || '.' || FLOOR(RANDOM() * 256)::TEXT,
    (ARRAY['新增','修改','删除','修改','修改'])[1 + FLOOR(RANDOM() * 5)],
    (ARRAY['resident','household','key_person','fp_record','user','missing','police','permit'])[1 + FLOOR(RANDOM() * 8)],
    (SELECT uuid FROM resident ORDER BY RANDOM() LIMIT 1),
    CASE WHEN RANDOM() < 0.3 THEN '{"name":"变更前数据"}' ELSE NULL END,
    '{"name":"操作人' || FLOOR(RANDOM() * 100)::TEXT || '","action":"' ||
        (ARRAY['新增记录','更新信息','删除记录','审核通过','退回修改'])[1 + FLOOR(RANDOM() * 5)] || '"}'
FROM generate_series(1, 30000) AS s(i)
CROSS JOIN LATERAL (
    SELECT
        CASE
            WHEN s.i <= 10000 THEN '2024-01-01'::TIMESTAMP + (s.i * INTERVAL '26 minutes')
            WHEN s.i <= 20000 THEN '2025-01-01'::TIMESTAMP + ((s.i - 10000) * INTERVAL '26 minutes')
            ELSE '2026-01-01'::TIMESTAMP + ((s.i - 20000) * INTERVAL '26 minutes')
        END AS ts
) t;

SELECT 'Inserted 30,000 audit logs across 2024-2026 partitions.' AS notice;

-- ============================================================
-- 7. 更新统计信息
-- ============================================================
ANALYZE resident;
ANALYZE fp_register_record;
ANALYZE key_person;
ANALYZE household_register;
ANALYZE missing_person;
ANALYZE audit_log;

-- ============================================================
-- 8. 最终验证
-- ============================================================
SELECT '=== Data Generation Complete ===' AS status;
SELECT 'resident' AS tbl, COUNT(*) AS cnt,
       COUNT(DISTINCT uuid) AS unique_uuids,
       COUNT(DISTINCT id_card_no) AS unique_id_cards
FROM resident
UNION ALL SELECT 'fp_register_record', COUNT(*), COUNT(DISTINCT uuid), NULL FROM fp_register_record
UNION ALL SELECT 'key_person', COUNT(*), COUNT(DISTINCT uuid), NULL FROM key_person
UNION ALL SELECT 'household_register', COUNT(*), COUNT(DISTINCT household_book_no), NULL FROM household_register
UNION ALL SELECT 'missing_person', COUNT(*), COUNT(DISTINCT resident_uuid), NULL FROM missing_person
UNION ALL SELECT 'audit_log', COUNT(*), NULL, NULL FROM audit_log
ORDER BY tbl;
