-- ============================================================
-- PDM 人口数据库管理系统 — 万级测试数据生成脚本
-- 基于 resident_ratio.md 的人口构成比例
-- 适用于前端展示和功能测试
-- ============================================================

-- 清理已有测试数据（保留 permission_group 和 area 基础数据）
DELETE FROM login_log;
DELETE FROM audit_log;
DELETE FROM alert;
DELETE FROM missing_person_recovery;
DELETE FROM missing_person;
DELETE FROM visit_plan;
DELETE FROM petition_record;
DELETE FROM key_person;
DELETE FROM resident_permit_renewal;
DELETE FROM resident_permit;
DELETE FROM fp_register_record;
DELETE FROM resident_registration;
DELETE FROM resident_change_request;
DELETE FROM resident_relation;
DELETE FROM household_migration_request;
DELETE FROM household_business_request;
DELETE FROM approval_permit;
DELETE FROM migration_permit;
DELETE FROM household_register;
DELETE FROM police;
DELETE FROM sys_user WHERE user_uuid != 'admin-0000-0000-0000-000000000001';
DELETE FROM resident;

-- ============================================================
-- 辅助函数
-- ============================================================

-- GB 11643-1999 身份证校验码
CREATE OR REPLACE FUNCTION id_card_checksum(prefix17 TEXT)
RETURNS CHAR AS $$
DECLARE
    weights INT[] := ARRAY[7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2];
    check_chars CHAR[] := ARRAY['1','0','X','9','8','7','6','5','4','3','2'];
    total INT := 0;
BEGIN
    FOR i IN 1..17 LOOP
        total := total + (ASCII(SUBSTRING(prefix17, i, 1)) - 48) * weights[i];
    END LOOP;
    RETURN check_chars[(total % 11) + 1];
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 加权随机选择（cumulative weights 0-1）
CREATE OR REPLACE FUNCTION weighted_pick(vals TEXT[], cum_w FLOAT[])
RETURNS TEXT AS $$
DECLARE
    r FLOAT := random();
    i INT := 1;
    n INT := array_length(cum_w, 1);
BEGIN
    WHILE i < n AND cum_w[i] < r LOOP
        i := i + 1;
    END LOOP;
    RETURN vals[i];
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 数组随机取一 (TEXT)
CREATE OR REPLACE FUNCTION arr_rand(arr TEXT[])
RETURNS TEXT AS $$
BEGIN
    RETURN arr[floor(random() * array_length(arr, 1))::INT + 1];
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 数组随机取一 (BIGINT)
CREATE OR REPLACE FUNCTION arr_rand(arr BIGINT[])
RETURNS BIGINT AS $$
BEGIN
    RETURN arr[floor(random() * array_length(arr, 1))::INT + 1];
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 生成顺序化 UUID (格式: 00000000-0000-0000-XXXX-XXXXXXXXXXXX)
CREATE OR REPLACE FUNCTION seq_uuid(seq BIGINT)
RETURNS VARCHAR(36) AS $$
BEGIN
    RETURN LPAD(((seq >> 24) & 65535)::TEXT, 4, '0') || '0000-0000-0000-0000-' ||
           LPAD((seq & 16777215)::TEXT, 12, '0');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- 手机号生成
CREATE OR REPLACE FUNCTION random_phone()
RETURNS VARCHAR(20) AS $$
DECLARE
    prefixes TEXT[] := ARRAY['130','131','132','133','134','135','136','137','138','139',
                              '150','151','152','153','155','156','157','158','159',
                              '170','171','172','173','175','176','177','178',
                              '180','181','182','183','184','185','186','187','188','189',
                              '191','192','193','195','196','198','199'];
BEGIN
    RETURN arr_rand(prefixes) || LPAD(floor(random() * 100000000)::TEXT, 8, '0');
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 真实中国街道名生成
CREATE OR REPLACE FUNCTION random_street()
RETURNS TEXT AS $$
DECLARE
    roads TEXT[] := ARRAY[
        '中山路','解放路','人民路','建设路','文化路','长安街','南京路','北京路',
        '建国路','和平路','新华路','朝阳路','滨海路','长江路','黄河路','长城路',
        '青年路','学府路','科技路','创业路','光明路','幸福路','花园路','迎宾路',
        '复兴路','团结路','友谊路','前进路','发展路','振兴路','文明路','和谐路',
        '东风路','红旗路','延安路','井冈山路','大庆路','五一东路','五四西路',
        '太白路','子午路','雁塔路','未央路','钟楼街','鼓楼街','书院街','东大街',
        '西大街','南大街','北大街','正阳街','朝阳街','柳巷','平江路','观前街',
        '春熙路','锦里路','宽窄巷','武侯祠大街','夫子庙','秦淮路','户部巷',
        '天河路','北京路步行街','上下九','海岸城','福田路','华强北路'
    ];
BEGIN
    RETURN roads[floor(random() * array_length(roads, 1))::INT + 1];
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 真实地址生成 (省/市/区 + 街道 + 门牌号)
CREATE OR REPLACE FUNCTION gen_address(p_area_id BIGINT, p_seq BIGINT)
RETURNS TEXT AS $$
DECLARE
    v_area_name TEXT;
    v_parent_name TEXT;
    v_grandparent_name TEXT;
    v_parent_id BIGINT;
    v_grandparent_id BIGINT;
    v_level TEXT;
    v_street TEXT;
    v_door_no INT;
    v_building INT;
    v_room INT;
    v_suffix TEXT;
BEGIN
    -- 查找区名 (parent_id 存储的是 area_code, 不是 area_id)
    SELECT area_name, parent_id, area_level INTO v_area_name, v_parent_id, v_level
    FROM area WHERE area_id = p_area_id;

    -- 查找市名 (parent_id 是 area_code, 需通过 area_code 匹配)
    IF v_parent_id IS NOT NULL THEN
        SELECT area_name, parent_id INTO v_parent_name, v_grandparent_id
        FROM area WHERE area_code::BIGINT = v_parent_id;
    END IF;

    -- 查找省名
    IF v_grandparent_id IS NOT NULL THEN
        SELECT area_name INTO v_grandparent_name
        FROM area WHERE area_code::BIGINT = v_grandparent_id;
    END IF;

    IF v_area_name IS NULL THEN
        v_area_name := '';
    END IF;

    v_street := random_street();
    -- 用 seq 哈希保证确定性
    v_door_no := ((p_seq * 17 + 31) % 800)::INT + 1;
    v_building := (p_seq % 20)::INT + 1;
    v_room := ((p_seq * 7) % 30)::INT + 101;

    -- 随机选择后缀格式
    v_suffix := CASE (p_seq % 5)
        WHEN 0 THEN v_street || v_door_no || '号'
        WHEN 1 THEN v_street || v_door_no || '号' || v_building || '号楼'
        WHEN 2 THEN v_street || v_door_no || '号' || v_building || '栋' || v_room || '室'
        WHEN 3 THEN v_street || v_door_no || '号院' || v_building || '号楼' || v_room || '室'
        ELSE v_street || v_door_no || '弄' || v_building || '号'
    END;

    -- 格式: 省-市-区 + 街道门牌号
    IF v_grandparent_name IS NOT NULL AND v_parent_name IS NOT NULL THEN
        RETURN v_grandparent_name || v_parent_name || v_area_name || v_suffix;
    ELSIF v_parent_name IS NOT NULL THEN
        RETURN v_parent_name || v_area_name || v_suffix;
    ELSE
        RETURN COALESCE(v_area_name, '') || v_suffix;
    END IF;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 行政区域路径生成 (仅省/市/区，无街道门牌号 — 适用于失踪地点等粗略位置)
CREATE OR REPLACE FUNCTION gen_area_path(p_area_id BIGINT)
RETURNS TEXT AS $$
DECLARE
    v_area_name TEXT;
    v_parent_name TEXT;
    v_grandparent_name TEXT;
    v_parent_id BIGINT;
    v_grandparent_id BIGINT;
BEGIN
    SELECT area_name, parent_id INTO v_area_name, v_parent_id
    FROM area WHERE area_id = p_area_id;

    IF v_parent_id IS NOT NULL THEN
        SELECT area_name, parent_id INTO v_parent_name, v_grandparent_id
        FROM area WHERE area_code::BIGINT = v_parent_id;
    END IF;

    IF v_grandparent_id IS NOT NULL THEN
        SELECT area_name INTO v_grandparent_name
        FROM area WHERE area_code::BIGINT = v_grandparent_id;
    END IF;

    IF v_grandparent_name IS NOT NULL AND v_parent_name IS NOT NULL THEN
        RETURN v_grandparent_name || v_parent_name || COALESCE(v_area_name, '');
    ELSIF v_parent_name IS NOT NULL THEN
        RETURN v_parent_name || COALESCE(v_area_name, '');
    ELSE
        RETURN COALESCE(v_area_name, '');
    END IF;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 户口簿号生成 (GB标准: 区划6位 + 年4位 + 流水8位 = 18位)
CREATE OR REPLACE FUNCTION gen_household_book_no(p_area_id BIGINT, p_seq BIGINT, p_date DATE)
RETURNS TEXT AS $$
DECLARE
    v_area_code CHAR(6);
BEGIN
    SELECT area_code INTO v_area_code FROM area WHERE area_id = p_area_id;
    IF v_area_code IS NULL THEN v_area_code := '000000'; END IF;
    RETURN v_area_code || TO_CHAR(p_date, 'YYYY') || LPAD((p_seq % 100000000)::TEXT, 8, '0');
END;
$$ LANGUAGE plpgsql VOLATILE;

-- ============================================================
-- 初始化: 缓存 area_id 到数组
-- ============================================================
DO $$
DECLARE
    area_arr BIGINT[];
    district_arr BIGINT[];
BEGIN
    -- 将市级 area_id 存入临时表（市级别覆盖全国34省，区县级仅4个直辖市有数据）
    DROP TABLE IF EXISTS _area_districts;
    CREATE TEMP TABLE _area_districts AS
    SELECT array_agg(area_id) AS ids FROM area WHERE area_level = '市';
END $$;

-- ============================================================
-- 1. 系统用户 (200人, 各角色按比例)
-- ============================================================
DO $$
DECLARE
    i INT;
    role_arr TEXT[] := ARRAY['系统管理员','用户管理员','数据审查员','采集员','街道办','民警','市局负责人','普通用户'];
    role_weights FLOAT[] := ARRAY[0.01,0.02,0.04,0.18,0.10,0.20,0.05,0.40];
    cum_w FLOAT[];
    r FLOAT;
    idx INT;
    u_role TEXT;
    u_uuid VARCHAR(36);
    u_name VARCHAR(50);
    pg_id BIGINT;
    acc_status TEXT;
    area_ids BIGINT[];
BEGIN
    -- 构建累积权重
    cum_w := role_weights;
    FOR i IN 2..array_length(cum_w,1) LOOP
        cum_w[i] := cum_w[i] + cum_w[i-1];
    END LOOP;

    SELECT ids INTO area_ids FROM _area_districts;

    FOR i IN 1..200 LOOP
        r := random();
        idx := 1;
        WHILE idx < array_length(cum_w,1) AND cum_w[idx] < r LOOP
            idx := idx + 1;
        END LOOP;
        u_role := role_arr[idx];

        u_uuid := seq_uuid(i + 50000);
        u_name := arr_rand(ARRAY['张','李','王','刘','陈','杨','赵','黄','周','吴',
                                  '徐','孙','胡','朱','高','林','何','郭','马','罗',
                                  '梁','宋','郑','谢','韩','唐','冯','于','董','萧'])
                || arr_rand(ARRAY['伟','芳','娜','敏','静','强','磊','洋','勇','艳',
                                  '杰','涛','明','超','秀','英','华','丽','平','刚']);

        CASE u_role
            WHEN '系统管理员' THEN pg_id := 1;
            WHEN '民警' THEN pg_id := 2;
            WHEN '采集员' THEN pg_id := 3;
            WHEN '街道办' THEN pg_id := 4;
            WHEN '数据审查员' THEN pg_id := 5;
            WHEN '市局负责人' THEN pg_id := 6;
            WHEN '用户管理员' THEN pg_id := 7;
            WHEN '普通用户' THEN pg_id := 8;
        END CASE;

        acc_status := CASE WHEN random() < 0.85 THEN '有效'
                           WHEN random() < 0.70 THEN '审批中'
                           ELSE '冻结' END;

        INSERT INTO sys_user (user_uuid, username, password, resident_uuid, user_role,
                              permission_group_id, phone, email, account_status,
                              must_change_password, register_materials, is_deleted)
        VALUES (u_uuid, 'user' || LPAD(i::TEXT, 4, '0'), '$2b$10$prAIsqHtIZifAJkviOkbRe4IAJm4CEd7cS6tozqAZtw.O3DpWfVOC',
                u_uuid, u_role, pg_id, random_phone(),
                'user' || LPAD(i::TEXT, 4, '0') || '@pdm.test',
                acc_status, CASE WHEN random() < 0.3 THEN TRUE ELSE FALSE END,
                'test-init', 0);
    END LOOP;
END $$;

-- 3. 常住人口 (50,000) — 核心表
-- 使用 VOLATILE SQL 函数确保 random() 每行重新计算
-- ============================================================

-- 3a. 参考数据临时表
CREATE TEMP TABLE _surnames (name TEXT);
INSERT INTO _surnames VALUES ('王'),('李'),('张'),('刘'),('陈'),('杨'),('黄'),('赵'),('吴'),('周'),
('徐'),('孙'),('马'),('朱'),('胡'),('郭'),('何'),('林'),('高'),('罗'),
('郑'),('梁'),('谢'),('宋'),('唐'),('韩'),('曹'),('许'),('邓'),('冯'),
('萧'),('程'),('蔡'),('彭'),('潘'),('袁'),('于'),('董'),('余'),('苏'),
('叶'),('吕'),('魏'),('蒋'),('田'),('杜'),('丁'),('沈'),('姜'),('范'),
('江'),('傅'),('钟'),('卢'),('汪'),('戴'),('崔'),('任'),('陆'),('廖'),
('姚'),('方'),('金'),('邱'),('夏'),('谭'),('韦'),('贾'),('邹'),('石'),
('熊'),('孟'),('秦'),('阎'),('薛'),('侯'),('雷'),('白'),('龙'),('段'),
('郝'),('孔'),('邵'),('史'),('毛'),('常'),('万'),('顾'),('赖'),('武'),
('康'),('贺'),('严'),('尹'),('钱'),('施'),('牛'),('洪'),('龚'),('汤');

CREATE TEMP TABLE _male_names (name TEXT);
INSERT INTO _male_names VALUES ('伟'),('强'),('磊'),('军'),('勇'),('杰'),('涛'),('明'),('超'),('辉'),
('鹏'),('浩'),('斌'),('波'),('刚'),('健'),('峰'),('亮'),('毅'),('俊'),
('建国'),('志强'),('建华'),('文博'),('宇轩'),('子涵'),('浩然'),
('天宇'),('明哲'),('志远'),('文昊'),('一鸣'),('子轩'),('睿'),('恒');

CREATE TEMP TABLE _female_names (name TEXT);
INSERT INTO _female_names VALUES ('芳'),('敏'),('静'),('丽'),('艳'),('娟'),('霞'),('玲'),('秀'),('英'),
('娜'),('萍'),('红'),('梅'),('燕'),('兰'),('凤'),('洁'),('琳'),('雪'),
('欣怡'),('雨桐'),('梓涵'),('一诺'),('梦瑶'),('诗涵'),('思雨'),
('佳琪'),('晓萌'),('悦然'),('依晨'),('雨晴'),('婉清'),('若曦');

CREATE TEMP TABLE _occupations (name TEXT);
INSERT INTO _occupations VALUES ('工人'),('农民'),('教师'),('医生'),('护士'),('工程师'),('会计'),
('公务员'),('企业职员'),('个体工商户'),('自由职业者'),('司机'),
('厨师'),('服务员'),('销售'),('保安'),('快递员'),('程序员'),
('设计师'),('律师'),('记者'),('学生'),('退休'),('无业');

-- 3b. 核心生成函数 (所有参考数据内联为数组, 避免temp table作用域问题)
CREATE OR REPLACE FUNCTION gen_resident(seq BIGINT, area_ids BIGINT[])
RETURNS TABLE(
    uuid UUID, name TEXT, former_name TEXT, gender VARCHAR, id_card_no CHAR,
    nation VARCHAR, nation_code CHAR, birth_date DATE,
    education_level VARCHAR, education_code CHAR, blood_type VARCHAR,
    marital_status VARCHAR, occupation VARCHAR, phone VARCHAR,
    photo VARCHAR, residence VARCHAR, area_id BIGINT,
    household_type VARCHAR, household_status VARCHAR,
    household_address VARCHAR, household_area_id BIGINT
) AS $$
DECLARE
    surnames TEXT[] := ARRAY['王','李','张','刘','陈','杨','黄','赵','吴','周','徐','孙','马','朱','胡','郭','何','林','高','罗','郑','梁','谢','宋','唐','韩','曹','许','邓','冯','萧','程','蔡','彭','潘','袁','于','董','余','苏','叶','吕','魏','蒋','田','杜','丁','沈','姜','范','江','傅','钟','卢','汪','戴','崔','任','陆','廖','姚','方','金','邱','夏','谭','韦','贾','邹','石','熊','孟','秦','阎','薛','侯','雷','白','龙','段','郝','孔','邵','史','毛','常','万','顾','赖','武','康','贺','严','尹','钱','施','牛','洪','龚','汤'];
    m_names TEXT[] := ARRAY['伟','强','磊','军','勇','杰','涛','明','超','辉','鹏','浩','斌','波','刚','健','峰','亮','毅','俊','建国','志强','建华','文博','宇轩','子涵','浩然','天宇','明哲','志远','文昊','一鸣','子轩','睿','恒'];
    f_names TEXT[] := ARRAY['芳','敏','静','丽','艳','娟','霞','玲','秀','英','娜','萍','红','梅','燕','兰','凤','洁','琳','雪','欣怡','雨桐','梓涵','一诺','梦瑶','诗涵','思雨','佳琪','晓萌','悦然','依晨','雨晴','婉清','若曦'];
    occs TEXT[] := ARRAY['工人','农民','教师','医生','护士','工程师','会计','公务员','企业职员','个体工商户','自由职业者','司机','厨师','服务员','销售','保安','快递员','程序员','设计师','律师','记者','学生','退休','无业'];
    nat_names TEXT[] := ARRAY['汉族','壮族','回族','满族','维吾尔族','苗族','彝族','土家族','藏族','蒙古族','布依族','侗族','瑶族','朝鲜族','白族','哈尼族','黎族','哈萨克族','傣族','傈僳族','佤族','畲族','高山族','拉祜族','水族','东乡族','纳西族','景颇族','柯尔克孜族','土族','达斡尔族','仫佬族','羌族','布朗族','撒拉族','毛南族','仡佬族','锡伯族','阿昌族','普米族','塔吉克族','怒族','乌孜别克族','俄罗斯族','鄂温克族','德昂族','保安族','裕固族','京族','塔塔尔族','独龙族','鄂伦春族','赫哲族','门巴族','珞巴族','基诺族','其他','外籍'];
    nat_codes TEXT[] := ARRAY['01','08','03','11','05','06','07','15','04','02','09','12','13','10','14','16','19','17','18','20','21','22','23','24','25','26','27','28','29','30','31','32','33','34','35','36','37','38','39','40','41','42','43','44','45','46','47','48','49','50','51','52','53','54','55','56','97','98'];
    edu_names TEXT[] := ARRAY['研究生','大学本科','大学专科','中等职业教育','技工学校','高中','初中','小学','文盲或半文盲','未知'];
    edu_codes TEXT[] := ARRAY['10','20','30','40','50','60','70','80','90','99'];
    mar_names TEXT[] := ARRAY['未婚','初婚','再婚','复婚','丧偶','离婚','未说明的婚姻状况'];
    g_val VARCHAR;
    r FLOAT; n_idx INT; e_idx INT; m_idx INT; ac INT;
BEGIN
    ac := array_length(area_ids, 1);
    g_val := CASE WHEN random() < 0.511 THEN '男' ELSE '女' END;

    -- 姓名
    name := surnames[floor(random()*100)::INT+1] || CASE WHEN g_val='男' THEN m_names[floor(random()*35)::INT+1] ELSE f_names[floor(random()*34)::INT+1] END;
    former_name := CASE WHEN random() < 0.05 THEN surnames[floor(random()*100)::INT+1] || CASE WHEN g_val='男' THEN m_names[floor(random()*35)::INT+1] ELSE f_names[floor(random()*34)::INT+1] END ELSE NULL END;
    gender := g_val;

    -- 出生日期 (确定性派生自seq，素数步长保证单射: gcd(499,25568)=1)
    -- 499:prime, 25568=2^5*17*47, coprime → injective for seq < 25,568,000
    birth_date := TO_DATE('1950-01-01', 'YYYY-MM-DD')
                  + (((seq / 1000)::INT * 499) % 25568)::INTEGER;

    -- 身份证号 (确定性派生，seq∈[1,50000]内保证无碰撞)
    -- seq%1000=序列号, seq/1000*499%25568=出生日偏移, 数学可证单射
    id_card_no := '11010' || LPAD((1 + (seq / 1000) % 9)::TEXT, 1, '0') ||
                  TO_CHAR(birth_date, 'YYYYMMDD') ||
                  LPAD((seq % 1000)::TEXT, 3, '0');
    id_card_no := id_card_no || id_card_checksum(id_card_no);

    -- 民族
    r := random();
    IF r<0.9111 THEN n_idx:=1; ELSIF r<0.9249 THEN n_idx:=2; ELSIF r<0.9328 THEN n_idx:=3;
    ELSIF r<0.9399 THEN n_idx:=4; ELSIF r<0.9462 THEN n_idx:=5; ELSIF r<0.9515 THEN n_idx:=6;
    ELSIF r<0.9562 THEN n_idx:=7; ELSIF r<0.9605 THEN n_idx:=8; ELSIF r<0.9647 THEN n_idx:=9;
    ELSIF r<0.9688 THEN n_idx:=10; ELSIF r<0.9990 THEN n_idx:=11+floor(random()*46)::INT;
    ELSIF r<0.9997 THEN n_idx:=57; ELSE n_idx:=58; END IF;
    nation := nat_names[n_idx]; nation_code := nat_codes[n_idx];

    -- 学历
    r := random();
    IF r<0.010 THEN e_idx:=1; ELSIF r<0.080 THEN e_idx:=2; ELSIF r<0.180 THEN e_idx:=3;
    ELSIF r<0.230 THEN e_idx:=4; ELSIF r<0.260 THEN e_idx:=5; ELSIF r<0.380 THEN e_idx:=6;
    ELSIF r<0.730 THEN e_idx:=7; ELSIF r<0.970 THEN e_idx:=8; ELSIF r<0.998 THEN e_idx:=9;
    ELSE e_idx:=10; END IF;
    education_level := edu_names[e_idx]; education_code := edu_codes[e_idx];

    -- 血型
    r := random();
    IF r<0.28 THEN blood_type:='A'; ELSIF r<0.58 THEN blood_type:='B';
    ELSIF r<0.89 THEN blood_type:='O'; ELSIF r<0.98 THEN blood_type:='AB';
    ELSE blood_type:='未知'; END IF;

    -- 婚姻
    r := random();
    IF r<0.19 THEN m_idx:=1; ELSIF r<0.75 THEN m_idx:=2; ELSIF r<0.80 THEN m_idx:=3;
    ELSIF r<0.82 THEN m_idx:=4; ELSIF r<0.89 THEN m_idx:=5; ELSIF r<0.97 THEN m_idx:=6;
    ELSE m_idx:=7; END IF;
    marital_status := mar_names[m_idx];

    -- 户籍
    r := random();
    IF r<0.72 THEN household_type:='居民户口'; ELSIF r<0.97 THEN household_type:='农业户口';
    ELSE household_type:='非农业户口'; END IF;
    r := random();
    IF r<0.94 THEN household_status:='正常'; ELSIF r<0.965 THEN household_status:='迁出注销';
    ELSIF r<0.985 THEN household_status:='死亡注销'; ELSIF r<0.99 THEN household_status:='失踪注销';
    ELSE household_status:='恢复'; END IF;

    occupation := occs[floor(random()*24)::INT+1];
    phone := random_phone();
    photo := CASE WHEN random()<0.02 THEN 'http://photo.pdm.test/'||seq||'.jpg' ELSE NULL END;
    -- 先选取 area_id，再基于同一 ID 生成地址，确保地址文本与 area_id 对应
    area_id := area_ids[floor(random()*ac)::INT+1];
    household_area_id := area_ids[floor(random()*ac)::INT+1];
    residence := gen_address(area_id, seq);
    household_address := gen_address(household_area_id, seq + 100000);
    uuid := gen_random_uuid();
    RETURN NEXT;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- 3c. 批量生成 (纯SQL调用函数)
DO $$
DECLARE
    batch INT := 5000;
    batches INT := 10;
    b INT;
    start_seq BIGINT;
BEGIN
    FOR b IN 0..batches-1 LOOP
        start_seq := b * batch + 1;
        RAISE NOTICE 'Generating residents: batch %/%, seq %', b+1, batches, start_seq;
        INSERT INTO resident (uuid, name, former_name, gender, id_card_no, nation, nation_code,
                              birth_date, education_level, education_code, blood_type, marital_status,
                              occupation, phone, photo, residence, area_id, household_type,
                              household_status, household_address, household_area_id, is_deleted)
        SELECT (gen_resident(gs, _a.area_ids)).*, 0
        FROM generate_series(start_seq, start_seq + batch - 1) AS gs,
LATERAL (SELECT ids FROM _area_districts) AS _a(area_ids);
    END LOOP;
    RAISE NOTICE 'Resident generation complete: % rows', (SELECT COUNT(*) FROM resident);
END $$;

-- 3d. 清理生成函数和参考表
DROP FUNCTION IF EXISTS gen_resident;

-- 3e. 按七普省份权重重新分配居民（替换均匀分布）
-- 优化: 预计算省份城市数避免关联子查询，减少_ca副本数
DO $$
DECLARE
    total INT := 50000;
    max_per_prov INT;
BEGIN
    -- 省份权重表（第七次全国人口普查）
    DROP TABLE IF EXISTS _pa;
    CREATE TEMP TABLE _pa AS
    SELECT province_name,
      ROUND(total * population::NUMERIC / SUM(population) OVER()) AS n
    FROM (VALUES
      ('广东省',126012510),('山东省',101527453),('河南省',99365519),
      ('江苏省',84748016),('四川省',83674866),('河北省',74610235),
      ('湖南省',66444864),('浙江省',64567568),('安徽省',61027171),
      ('湖北省',57752557),('广西壮族自治区',50126804),
      ('云南省',47209277),('江西省',45188635),('辽宁省',42591407),
      ('福建省',41540086),('陕西省',39528999),('贵州省',38562148),
      ('山西省',34915616),('重庆市',32054159),('黑龙江省',31850088),
      ('新疆维吾尔自治区',25852345),('甘肃省',25019831),
      ('上海市',24870895),('吉林省',24073453),('内蒙古自治区',24049155),
      ('北京市',21893095),('天津市',13866009),('海南省',10081232),
      ('宁夏回族自治区',7202654),('青海省',5923957),
      ('西藏自治区',3648100),('香港特别行政区',7474200),
      ('澳门特别行政区',683218),('台湾省',23561236)
    ) t(province_name, population);
    UPDATE _pa SET n = n + (total - (SELECT SUM(n) FROM _pa)) WHERE province_name = '广东省';

    -- 城市→省份映射
    DROP TABLE IF EXISTS _cp;
    CREATE TEMP TABLE _cp AS
    SELECT a.area_id, COALESCE(p.area_name, c.area_name) AS prov
    FROM area a
    LEFT JOIN area c ON a.parent_id = c.area_code::BIGINT
    LEFT JOIN area p ON c.parent_id = p.area_code::BIGINT
    WHERE a.area_level = '市' AND a.is_deleted = 0;

    -- 居民→省份（按id排序，省份累积区间分配）
    DROP TABLE IF EXISTS _res;
    CREATE TEMP TABLE _res AS
    SELECT r.id, r2.province_name,
      row_number() OVER (PARTITION BY r2.province_name ORDER BY r.id) AS prov_rn
    FROM (
      SELECT id, row_number() OVER (ORDER BY id) AS rn FROM resident WHERE is_deleted = 0
    ) r
    JOIN (
      SELECT province_name, n,
        SUM(n) OVER (ORDER BY n DESC) - n + 1 AS lo,
        SUM(n) OVER (ORDER BY n DESC) AS hi
      FROM _pa
    ) r2 ON r.rn BETWEEN r2.lo AND r2.hi;

    -- 预计算各省城市数（避免关联子查询扫全表）
    DROP TABLE IF EXISTS _prov_cnt;
    CREATE TEMP TABLE _prov_cnt AS
    SELECT prov, COUNT(*) AS cnt FROM _cp GROUP BY prov;
    CREATE INDEX IF NOT EXISTS idx_prov_cnt ON _prov_cnt (prov);

    -- 最大居民数的省份所需的城市副本数（省份最多 ~4350人，最少城市数 ~1）
    SELECT CEIL(MAX(n)::NUMERIC / 2) INTO max_per_prov FROM _pa;
    IF max_per_prov < 100 THEN max_per_prov := 100; END IF;

    -- 城市分配表（副本数匹配最大省份需求）
    DROP TABLE IF EXISTS _ca;
    CREATE TEMP TABLE _ca AS
    SELECT prov, area_id,
      row_number() OVER (PARTITION BY prov ORDER BY random()) AS city_rn
    FROM _cp CROSS JOIN generate_series(1, max_per_prov) g;
    CREATE INDEX IF NOT EXISTS idx_ca_prov_rn ON _ca (prov, city_rn);

    -- 更新area_id并重新生成地址（使用预计算的省份城市数）
    UPDATE resident r SET
      area_id = ca.area_id,
      household_area_id = ca2.area_id,
      residence = gen_address(ca.area_id, r.id),
      household_address = gen_address(ca2.area_id, r.id + 100000)
    FROM _res
    JOIN _prov_cnt pc ON pc.prov = _res.province_name
    JOIN _ca ca ON ca.prov = _res.province_name
      AND ca.city_rn = ((_res.prov_rn - 1) % pc.cnt) + 1
    JOIN _ca ca2 ON ca2.prov = _res.province_name
      AND ca2.city_rn = (((_res.prov_rn + 7919) - 1) % pc.cnt) + 1
    WHERE r.id = _res.id;

    DROP TABLE IF EXISTS _pa; DROP TABLE IF EXISTS _cp;
    DROP TABLE IF EXISTS _res; DROP TABLE IF EXISTS _ca;
    DROP TABLE IF EXISTS _prov_cnt;
END $$;

-- 清理临时表
DROP TABLE IF EXISTS _surnames;
DROP TABLE IF EXISTS _male_names;
DROP TABLE IF EXISTS _female_names;
DROP TABLE IF EXISTS _occupations;
DROP TABLE IF EXISTS _nations;
DROP TABLE IF EXISTS _educations;
DROP TABLE IF EXISTS _blood_types;
DROP TABLE IF EXISTS _marital;
DROP TABLE IF EXISTS _hh_types;
DROP TABLE IF EXISTS _hh_statuses;
DROP TABLE IF EXISTS _area_ids;


-- ============================================================
-- 2. 警员 (80人) — 放在常住人口之后以便关联真实居民
-- ============================================================
DO $$
DECLARE
    i INT;
    idx INT;
    seq_map INT[];
    selected_pfx TEXT;
    ranks TEXT[] := ARRAY['警员','警司','警督','警监'];
    r_weights FLOAT[] := ARRAY[0.55,0.30,0.12,0.03];
    cum_r FLOAT[];
    stations TEXT[] := ARRAY['东城分局','西城分局','朝阳分局','海淀分局','丰台分局',
                              '石景山分局','通州分局','大兴分局','顺义分局','昌平分局'];
    depts TEXT[] := ARRAY['治安大队','刑侦大队','户政科','社区警务队','巡逻队','指挥中心'];
    user_uuids VARCHAR(36)[];
    resident_uuids VARCHAR(36)[];
    area_ids BIGINT[];
    area_prefixes TEXT[];
BEGIN
    cum_r := r_weights;
    FOR i IN 2..4 LOOP cum_r[i] := cum_r[i] + cum_r[i-1]; END LOOP;

    -- 构建 area_id 与 area_code 前4位的映射数组（只含市级区域）
    WITH district_data AS (
        SELECT a.area_id, LEFT(a.area_code, 4) AS pfx
        FROM area a JOIN (SELECT unnest(ids) AS area_id FROM _area_districts) d USING (area_id)
    )
    SELECT array_agg(area_id), array_agg(pfx) INTO area_ids, area_prefixes FROM district_data;

    -- 初始化序号数组
    seq_map := ARRAY(SELECT 0 FROM generate_series(1, array_length(area_ids, 1)));

    SELECT array_agg(user_uuid) INTO user_uuids FROM sys_user WHERE user_role = '民警' LIMIT 80;
    SELECT array_agg(uuid) INTO resident_uuids FROM (SELECT uuid FROM resident ORDER BY random() LIMIT 80) sub;

    FOR i IN 1..80 LOOP
        idx := floor(random() * array_length(area_ids, 1) + 1)::INT;
        seq_map[idx] := seq_map[idx] + 1;
        selected_pfx := area_prefixes[idx];

        INSERT INTO police (police_number, user_uuid, resident_uuid,
                           police_station, jurisdiction, area_id, department,
                           police_rank, duty_status, is_deleted)
        VALUES ('P' || selected_pfx || LPAD(seq_map[idx]::TEXT, 4, '0'),
                user_uuids[i],
                resident_uuids[i],
                arr_rand(stations), gen_address(area_ids[idx], i + 600000),
                area_ids[idx],
                arr_rand(depts),
                weighted_pick(ranks, cum_r),
                CASE WHEN random() < 0.88 THEN '在岗' WHEN random() < 0.70 THEN '调岗' ELSE '离职' END,
                0);
    END LOOP;
END $$;

-- ============================================================
-- 4. 户口本 (5,000)
-- ============================================================
DO $$
DECLARE
    area_ids BIGINT[];
    res_uuids VARCHAR(36)[];
    est_d DATE; hukou_aid BIGINT;
BEGIN
    SELECT ids INTO area_ids FROM _area_districts;
    SELECT array_agg(uuid) INTO res_uuids FROM resident ORDER BY RANDOM() LIMIT 5000;

    FOR i IN 1..5000 LOOP
        est_d := CURRENT_DATE - (floor(random() * 3650)::INT || ' days')::INTERVAL;
        hukou_aid := arr_rand(area_ids);
        INSERT INTO household_register (household_book_no, householder_uuid, establish_date,
                                        hukou_address, hukou_area_id, status, member_uuid_list, is_deleted)
        VALUES (gen_household_book_no(hukou_aid, i, est_d),
                res_uuids[i],
                est_d,
                gen_address(hukou_aid, i),
                hukou_aid,
                CASE WHEN random() < 0.90 THEN '有效'
                     WHEN random() < 0.55 THEN '审批中'
                     WHEN random() < 0.50 THEN '冻结'
                     ELSE '无效' END,
                '[' || res_uuids[i] || ']',
                0);
    END LOOP;
END $$;

-- ============================================================
-- 辅助: 缓存 resident 和 sys_user 数据到临时表，避免重复 ORDER BY RANDOM()
-- ============================================================
DO $$
BEGIN
    -- 所有 resident UUIDs（含性别，用于关系表）
    DROP TABLE IF EXISTS _res_pool;
    CREATE TEMP TABLE _res_pool AS SELECT uuid, name, gender FROM resident;

    -- 所有 sys_user UUIDs（按角色分类，用于 handler/agent 引用）
    DROP TABLE IF EXISTS _user_pool;
    CREATE TEMP TABLE _user_pool AS SELECT user_uuid, user_role FROM sys_user;

    -- 所有 police_number
    DROP TABLE IF EXISTS _police_pool;
    CREATE TEMP TABLE _police_pool AS SELECT police_number FROM police;

    -- 居住证池
    DROP TABLE IF EXISTS _permit_pool;
    CREATE TEMP TABLE _permit_pool AS SELECT permit_no, expiry_date FROM resident_permit WHERE status = '过期';

    CREATE INDEX IF NOT EXISTS _res_pool_idx ON _res_pool(uuid);
    CREATE INDEX IF NOT EXISTS _user_pool_idx ON _user_pool(user_uuid);
END $$;

-- 修复 sys_user.resident_uuid 和 police.resident_uuid: 关联到真实 resident UUID
UPDATE sys_user
SET resident_uuid = (SELECT uuid FROM _res_pool ORDER BY random() LIMIT 1)
WHERE resident_uuid = user_uuid;

UPDATE police
SET resident_uuid = (SELECT uuid FROM _res_pool ORDER BY random() LIMIT 1)
WHERE resident_uuid NOT IN (SELECT uuid FROM _res_pool);

-- 快速随机取一行（利用 OFFSET + 主键扫描，比 ORDER BY RANDOM() 快很多）
CREATE OR REPLACE FUNCTION rand_row(tbl TEXT)
RETURNS SETOF RECORD AS $$
DECLARE
    cnt BIGINT;
    offs BIGINT;
BEGIN
    EXECUTE 'SELECT COUNT(*) FROM ' || tbl INTO cnt;
    offs := floor(random() * cnt)::BIGINT;
    RETURN QUERY EXECUTE 'SELECT * FROM ' || tbl || ' LIMIT 1 OFFSET ' || offs;
END;
$$ LANGUAGE plpgsql VOLATILE;

-- ============================================================
-- 5. 人员关系表 (15,000)
-- ============================================================
DO $$
DECLARE
    pool VARCHAR(36)[]; genders TEXT[]; pc INT;
    male_pool VARCHAR(36)[]; female_pool VARCHAR(36)[];
    i INT; r_uuid VARCHAR(36); r_gender TEXT;
    father_uuid VARCHAR(36); mother_uuid VARCHAR(36); spouse_uuid VARCHAR(36);
    idx INT; tmp VARCHAR(36); tmp_g TEXT;
BEGIN
    SELECT array_agg(uuid), array_agg(gender) INTO pool, genders FROM _res_pool;
    pc := array_length(pool, 1);
    SELECT array_agg(uuid) INTO male_pool FROM _res_pool WHERE gender = '男';
    SELECT array_agg(uuid) INTO female_pool FROM _res_pool WHERE gender = '女';

        -- Fisher-Yates部分洗牌: 前15000个UUID不会重复
        FOR i IN 1..15000 LOOP
            idx := floor(random() * (pc - i + 1))::INT + i;
            -- swap pool[i] with pool[idx]
            r_uuid := pool[i]; pool[i] := pool[idx]; pool[idx] := r_uuid;
            r_gender := genders[i]; genders[i] := genders[idx]; genders[idx] := r_gender;
        END LOOP;
        -- 现在 pool[1..15000] 是15000个互不重复的UUID
        FOR i IN 1..15000 LOOP
            r_uuid := pool[i];
            r_gender := genders[i];
            father_uuid := CASE WHEN random() < 0.85 THEN male_pool[floor(random()*array_length(male_pool,1))::INT+1] ELSE NULL END;
            mother_uuid := CASE WHEN random() < 0.90 THEN female_pool[floor(random()*array_length(female_pool,1))::INT+1] ELSE NULL END;
            spouse_uuid := CASE WHEN random() < 0.55 THEN
                CASE WHEN r_gender = '男' THEN female_pool[floor(random()*array_length(female_pool,1))::INT+1]
                     ELSE male_pool[floor(random()*array_length(male_pool,1))::INT+1] END
                ELSE NULL END;
            INSERT INTO resident_relation (relation_person_uuid, father_uuid, mother_uuid, spouse_uuid, is_deleted)
            VALUES (r_uuid, father_uuid, mother_uuid, spouse_uuid, 0);
        END LOOP;
END $$;

-- ============================================================
-- 6. 户籍人员信息变更请求 (1,500)
-- ============================================================
DO $$
DECLARE
    pool VARCHAR(36)[]; pc INT; i INT;
BEGIN
    SELECT array_agg(uuid) INTO pool FROM _res_pool; pc := array_length(pool,1);
    FOR i IN 1..1500 LOOP
        INSERT INTO resident_change_request (applicant_uuid, change_field, request_time,
            original_data, modified_data, status, is_deleted)
        VALUES (pool[floor(random()*pc)::INT+1],
            arr_rand(ARRAY['name','phone','residence','occupation','education_level','marital_status']),
            CURRENT_DATE - (floor(random()*180)::INT || ' days')::INTERVAL,
            '{"original":"test"}', '{"modified":"test_new"}',
            CASE WHEN random()<0.35 THEN '请求' WHEN random()<0.55 THEN '一审'
                 WHEN random()<0.70 THEN '二审' WHEN random()<0.85 THEN '通过' ELSE '驳回' END,
            0);
    END LOOP;
END $$;

-- ============================================================
-- 7. 居住证 (5,000)
-- ============================================================
DO $$
DECLARE
    pool VARCHAR(36)[]; pc INT; i INT; issue_d DATE;
BEGIN
    SELECT array_agg(uuid) INTO pool FROM _res_pool; pc := array_length(pool,1);
    FOR i IN 1..5000 LOOP
        issue_d := CURRENT_DATE - (floor(random()*1095)::INT || ' days')::INTERVAL;
        INSERT INTO resident_permit (permit_no, uuid, issue_date, expiry_date, status, is_deleted)
        VALUES ('000000' || TO_CHAR(issue_d, 'YYYYMM') || LPAD(i::TEXT, 6, '0'), pool[floor(random()*pc)::INT+1], issue_d,
            issue_d + (365 + floor(random()*1095)::INT || ' days')::INTERVAL,
            CASE
                WHEN random()<0.10 THEN '申领'
                WHEN random()<0.25 THEN '已批准'
                WHEN random()<0.75 THEN '有效'
                WHEN random()<0.85 THEN '过期'
                ELSE '注销'
            END, 0);
    END LOOP;
    -- 刷新居住证池（全部有效证，供流动人口登记引用）
    DROP TABLE IF EXISTS _permit_pool_all;
    CREATE TEMP TABLE _permit_pool_all AS SELECT permit_no FROM resident_permit;
    -- 刷新过期居住证池（供延期记录引用）
    DROP TABLE IF EXISTS _permit_pool;
    CREATE TEMP TABLE _permit_pool AS SELECT permit_no, expiry_date FROM resident_permit WHERE status = '过期';
END $$;

-- ============================================================
-- 8. 居住地登记表 (6,000)
-- ============================================================
DO $$
DECLARE
    pool VARCHAR(36)[]; pc INT; i INT; r1 FLOAT; r2 FLOAT; r3 FLOAT; r4 FLOAT;
    area_ids BIGINT[]; ac INT;
BEGIN
    SELECT array_agg(uuid) INTO pool FROM _res_pool; pc := array_length(pool,1);
    SELECT ids INTO area_ids FROM _area_districts; ac := array_length(area_ids,1);
    FOR i IN 1..6000 LOOP
        r1 := random(); r2 := random(); r3 := random(); r4 := random();
        INSERT INTO resident_registration (uuid, original_address, current_address, area_id, address_type,
            house_ownership, purpose, expected_duration, work_unit, register_date, is_deleted)
        VALUES (pool[floor(random()*pc)::INT+1],
            gen_address(area_ids[floor(random()*ac)::INT+1], i),
            gen_address(area_ids[floor(random()*ac)::INT+1], i + 10000),
            area_ids[floor(random()*ac)::INT+1],
            CASE WHEN r1<0.45 THEN '租赁房屋' WHEN r1<0.75 THEN '自有住房'
                 WHEN r1<0.87 THEN '单位宿舍' WHEN r1<0.92 THEN '学校宿舍'
                 WHEN r1<0.97 THEN '亲友借住' ELSE '其他' END,
            CASE WHEN r2<0.60 THEN '自有产权' ELSE '租赁' END,
            CASE WHEN r3<0.55 THEN '务工' WHEN r3<0.70 THEN '经商'
                 WHEN r3<0.82 THEN '投靠亲属' WHEN r3<0.92 THEN '求学' ELSE '其他' END,
            CASE WHEN r4<0.35 THEN '短租' WHEN r4<0.75 THEN '中租' ELSE '长租' END,
            arr_rand(ARRAY['XX公司','YY工厂','ZZ集团','AA企业','BB有限公司','CC科技','DD商贸','EE实业']),
            CURRENT_DATE - (floor(random()*730)::INT || ' days')::INTERVAL, 0);
    END LOOP;
END $$;

-- ============================================================
-- 9. 流动人口登记表 (5,000)
-- ============================================================
DO $$
DECLARE
    pool VARCHAR(36)[]; pc INT; i INT; reg_d DATE;
    agents VARCHAR(36)[]; reviewers VARCHAR(36)[];
    p_permit_nos VARCHAR(20)[]; p_pc INT;
BEGIN
    SELECT array_agg(uuid) INTO pool FROM _res_pool; pc := array_length(pool,1);
    SELECT array_agg(user_uuid) INTO agents FROM _user_pool WHERE user_role='采集员';
    SELECT array_agg(user_uuid) INTO reviewers FROM _user_pool WHERE user_role='数据审查员';
    -- 居住证号池（引用真实 resident_permit 数据）
    SELECT array_agg(permit_no) INTO p_permit_nos FROM _permit_pool_all;
    p_pc := array_length(p_permit_nos, 1);

    FOR i IN 1..5000 LOOP
        reg_d := CURRENT_DATE - (floor(random()*365)::INT || ' days')::INTERVAL;
        INSERT INTO fp_register_record (residence_permit_no, uuid, agent_uuid, attachment,
            reviewer_uuid, reject_reason, register_date, review_date, is_deleted)
        VALUES (CASE WHEN random()<0.60 AND p_pc > 0 THEN p_permit_nos[floor(random()*p_pc)::INT+1] ELSE NULL END,
            pool[floor(random()*pc)::INT+1],
            CASE WHEN random()<0.70 AND agents IS NOT NULL THEN agents[floor(random()*array_length(agents,1))::INT+1] ELSE NULL END,
            CASE WHEN random()<0.20 THEN 'attachment_'||i||'.pdf' ELSE NULL END,
            CASE WHEN random()<0.60 AND reviewers IS NOT NULL THEN reviewers[floor(random()*array_length(reviewers,1))::INT+1] ELSE NULL END,
            CASE WHEN random()<0.10 THEN '材料不完整' ELSE NULL END,
            reg_d,
            CASE WHEN random()<0.60 THEN reg_d + (floor(random()*30)::INT || ' days')::INTERVAL ELSE NULL END, 0);
    END LOOP;
END $$;

-- ============================================================
-- 10. 居住证延期记录 (800)
-- ============================================================
DO $$
DECLARE
    permits VARCHAR(36)[]; expiries DATE[]; pc INT; i INT; idx INT;
    police_users VARCHAR(36)[];
BEGIN
    SELECT array_agg(permit_no), array_agg(expiry_date) INTO permits, expiries FROM _permit_pool;
    pc := array_length(permits, 1);
    SELECT array_agg(user_uuid) INTO police_users FROM _user_pool WHERE user_role='民警';

    FOR i IN 1..LEAST(800, pc) LOOP
        idx := floor(random()*pc)::INT + 1;
        INSERT INTO resident_permit_renewal (permit_no, old_expiry_date, new_expiry_date,
            renewal_date, operator_uuid, remark, is_deleted)
        VALUES (permits[idx], expiries[idx], expiries[idx] + (365||' days')::INTERVAL,
            expiries[idx] - (30||' days')::INTERVAL,
            CASE WHEN police_users IS NOT NULL THEN police_users[floor(random()*array_length(police_users,1))::INT+1] ELSE NULL END,
            CASE WHEN random()<0.30 THEN '正常续期' ELSE NULL END, 0);
    END LOOP;
END $$;

-- ============================================================
-- 11. 重点人员 (1,000)
-- ============================================================
DO $$
DECLARE
    pool VARCHAR(36)[]; pc INT; i INT; idx INT; r_uuid VARCHAR(36); r1 FLOAT; r2 FLOAT;
    p_nos VARCHAR(20)[];
BEGIN
    SELECT array_agg(uuid) INTO pool FROM _res_pool; pc := array_length(pool,1);
    SELECT array_agg(police_number) INTO p_nos FROM _police_pool;
    -- Fisher-Yates部分洗牌: 前1000个UUID不重复
    FOR i IN 1..1000 LOOP
        idx := floor(random() * (pc - i + 1))::INT + i;
        r_uuid := pool[i]; pool[i] := pool[idx]; pool[idx] := r_uuid;
    END LOOP;
    FOR i IN 1..1000 LOOP
        r1 := random(); r2 := random();
        INSERT INTO key_person (uuid, control_level, control_type, designated_at, revoked_at,
            responsible_police_no, is_deleted)
        VALUES (pool[i],
            CASE WHEN r1<0.50 THEN '一级' WHEN r1<0.75 THEN '二级' ELSE '三级' END,
            CASE WHEN r2<0.22 THEN '刑满释放人员' WHEN r2<0.42 THEN '社区矫正人员'
                 WHEN r2<0.60 THEN '涉毒人员' WHEN r2<0.75 THEN '信访重点人员'
                 WHEN r2<0.85 THEN '涉稳人员' WHEN r2<0.93 THEN '精神障碍患者(肇事肇祸风险)'
                 ELSE '其他重点人员' END,
            CURRENT_DATE - (floor(random()*1095)::INT || ' days')::INTERVAL,
            CASE WHEN random()<0.15 THEN CURRENT_DATE - (floor(random()*365)::INT || ' days')::INTERVAL ELSE NULL END,
            CASE WHEN p_nos IS NOT NULL THEN p_nos[floor(random()*array_length(p_nos,1))::INT+1] ELSE 'P000001' END, 0);
    END LOOP;
    -- 刷新 key_person 池
    DROP TABLE IF EXISTS _kp_pool;
    CREATE TEMP TABLE _kp_pool AS SELECT uuid, responsible_police_no FROM key_person;
END $$;

-- ============================================================
-- 12. 走访计划 (2,000)
-- ============================================================
DO $$
DECLARE
    kp_uuids VARCHAR(36)[]; kp_police VARCHAR(20)[]; kc INT; i INT; idx INT; planned_d DATE;
BEGIN
    SELECT array_agg(uuid), array_agg(responsible_police_no) INTO kp_uuids, kp_police FROM _kp_pool;
    kc := array_length(kp_uuids, 1);
    FOR i IN 1..2000 LOOP
        idx := floor(random()*kc)::INT + 1;
        planned_d := CURRENT_DATE - (floor(random()*90)::INT || ' days')::INTERVAL;
        INSERT INTO visit_plan (key_person_uuid, planned_date, actual_date, visit_type, status,
            assigned_police_no, is_alerted, is_deleted)
        VALUES (kp_uuids[idx], planned_d,
            CASE WHEN random()<0.70 THEN planned_d + (floor(random()*3)::INT || ' days')::INTERVAL ELSE NULL END,
            CASE WHEN random()<0.75 THEN '入户走访' WHEN random()<0.90 THEN '电话' ELSE '视频' END,
            CASE WHEN random()<0.55 THEN '已完成' WHEN random()<0.65 THEN '已逾期'
                 WHEN random()<0.70 THEN '已取消' ELSE '待走访' END,
            kp_police[idx], CASE WHEN random()<0.25 THEN 1 ELSE 0 END, 0);
    END LOOP;
END $$;

-- ============================================================
-- 13. 信访记录 (1,500)
-- ============================================================
DO $$
DECLARE
    kp_uuids VARCHAR(36)[]; kp_police VARCHAR(20)[]; kc INT; i INT; idx INT;
    area_ids BIGINT[]; ac INT;
BEGIN
    SELECT array_agg(uuid), array_agg(responsible_police_no) INTO kp_uuids, kp_police FROM _kp_pool;
    kc := array_length(kp_uuids, 1);
    SELECT ids INTO area_ids FROM _area_districts; ac := array_length(area_ids,1);
    FOR i IN 1..1500 LOOP
        idx := floor(random()*kc)::INT + 1;
        INSERT INTO petition_record (key_person_uuid, handler_police_no, petition_time, address,
            remark, evaluation, is_deleted)
        VALUES (kp_uuids[idx], kp_police[idx],
            CURRENT_TIMESTAMP - (floor(random()*730)::INT || ' days')::INTERVAL,
            gen_address(area_ids[floor(random()*ac)::INT+1], i + 300000),
            CASE WHEN random()<0.40 THEN arr_rand(ARRAY['反映问题已解决','情绪稳定','要求复查','已解释政策']) ELSE NULL END,
            arr_rand(ARRAY['满意','基本满意','不满意','待评价','无法评价']), 0);
    END LOOP;
END $$;

-- ============================================================
-- 14. 失踪人员 (200)
-- ============================================================
DO $$
DECLARE
    pool VARCHAR(36)[]; pc INT; i INT;
    area_ids BIGINT[]; ac INT;
    v_uuid VARCHAR(36); v_name VARCHAR(50); v_gender VARCHAR(4);
BEGIN
    SELECT array_agg(uuid) INTO pool FROM _res_pool; pc := array_length(pool,1);
    SELECT ids INTO area_ids FROM _area_districts; ac := array_length(area_ids,1);
    FOR i IN 1..200 LOOP
        -- 预取随机 UUID 和对应的 name/gender，避免每轮全表扫描 _res_pool
        v_uuid := pool[floor(random()*pc)::INT+1];
        SELECT name, gender INTO v_name, v_gender FROM _res_pool WHERE uuid = v_uuid;
        INSERT INTO missing_person (resident_uuid, name, gender, missing_date, missing_place, photo, appearance,
            medical_history, possible_way, contact_phone, status, is_deleted)
        VALUES (v_uuid, v_name, v_gender,
            CURRENT_DATE - (floor(random()*730)::INT || ' days')::INTERVAL,
            gen_area_path(area_ids[floor(random()*ac)::INT+1]), 'http://photo.pdm.test/missing_'||i||'.jpg',
            arr_rand(ARRAY['身高约170cm，体型中等','身高约165cm，偏瘦','身高约175cm，偏胖','身高约160cm','身高约180cm','身高约155cm，微胖']),
            CASE WHEN random()<0.15 THEN arr_rand(ARRAY['高血压','糖尿病','心脏病','抑郁症史']) ELSE NULL END,
            CASE WHEN random()<0.30 THEN arr_rand(ARRAY['可能去往外省','可能去往邻市','可能投靠亲属']) ELSE NULL END,
            '138'||LPAD(floor(random()*100000000)::TEXT,8,'0'),
            CASE WHEN random()<0.75 THEN '失踪中' ELSE '已经寻回' END, 0);
    END LOOP;
    -- 刷新失踪池
    DROP TABLE IF EXISTS _missing_pool;
    CREATE TEMP TABLE _missing_pool AS SELECT rid, missing_date FROM missing_person WHERE status='已经寻回';
END $$;

-- ============================================================
-- 15. 失踪寻回记录 (50)
-- ============================================================
DO $$
DECLARE
    m_rids BIGINT[]; m_dates DATE[]; mc INT; i INT; idx INT;
BEGIN
    SELECT array_agg(rid), array_agg(missing_date) INTO m_rids, m_dates FROM _missing_pool;
    mc := array_length(m_rids, 1); IF mc IS NULL THEN mc := 0; END IF;
    FOR i IN 1..LEAST(50, mc) LOOP
        idx := floor(random()*mc)::INT + 1;
        INSERT INTO missing_person_recovery (missing_record_rid, recovery_date, summary, is_deleted)
        VALUES (m_rids[idx], m_dates[idx] + (floor(random()*365)::INT+7 || ' days')::INTERVAL,
            CASE WHEN random()<0.60 THEN '经多方寻找，已安全寻回' ELSE '自行返回' END, 0);
    END LOOP;
END $$;

-- ============================================================
-- 16. 户籍业务请求 (800)
-- ============================================================
DO $$
DECLARE
    pool VARCHAR(36)[]; pc INT; i INT;
    handlers VARCHAR(36)[];
BEGIN
    SELECT array_agg(uuid) INTO pool FROM _res_pool; pc := array_length(pool,1);
    SELECT array_agg(user_uuid) INTO handlers FROM _user_pool WHERE user_role='民警';
    FOR i IN 1..800 LOOP
        INSERT INTO household_business_request (handler_uuid, applicant_uuid, attachment,
            business_type, handle_date, handle_basis, fee, status, reject_reason, remark, is_deleted)
        VALUES (CASE WHEN random()<0.70 AND handlers IS NOT NULL THEN handlers[floor(random()*array_length(handlers,1))::INT+1] ELSE NULL END,
            pool[floor(random()*pc)::INT+1], 'attachment_biz_'||i||'.pdf',
            CASE WHEN random()<0.20 THEN '出生登记' WHEN random()<0.40 THEN '死亡注销' WHEN random()<0.60 THEN '户口迁移' WHEN random()<0.80 THEN '登记项目变更' ELSE '分户立户' END,
            CURRENT_DATE - (floor(random()*180)::INT || ' days')::INTERVAL,
            CASE WHEN random()<0.50 THEN '户籍管理条例第'||floor(random()*10+1)::TEXT||'条' ELSE NULL END,
            CASE WHEN random()<0.30 THEN (floor(random()*100)::INT)::NUMERIC(10,2) ELSE NULL END,
            CASE WHEN random()<0.40 THEN '已批准' WHEN random()<0.55 THEN '审批中' WHEN random()<0.70 THEN '已驳回' ELSE '待受理' END,
            CASE WHEN random()<0.10 THEN '材料不全' ELSE NULL END,
            CASE WHEN random()<0.20 THEN '备注信息'||i ELSE NULL END, 0);
    END LOOP;
END $$;

-- ============================================================
-- 17. 户籍迁移业务请求 (每人2-5次连续迁移，~600条)
-- ============================================================
DO $$
DECLARE
    pool VARCHAR(36)[]; pc INT;
    area_ids BIGINT[]; ac INT;
    handlers VARCHAR(36)[];
    v_person_uuid TEXT;
    v_chain_len INT;
    v_prev_area BIGINT;
    v_curr_area BIGINT;
    v_bt TEXT;
    v_base_date DATE;
    i INT; j INT;
BEGIN
    SELECT array_agg(uuid) INTO pool FROM _res_pool; pc := array_length(pool,1);
    SELECT ids INTO area_ids FROM _area_districts; ac := array_length(area_ids,1);
    SELECT array_agg(user_uuid) INTO handlers FROM _user_pool WHERE user_role='民警';

    FOR i IN 1..200 LOOP
        v_person_uuid := pool[1 + (i % pc)];
        v_chain_len := 2 + (i % 4);  -- 确定性链长2-5
        v_base_date := CURRENT_DATE - ((i * 7 % 1095) || ' days')::INTERVAL;
        v_prev_area := area_ids[1 + (i % ac)];

        FOR j IN 1..v_chain_len LOOP
            v_curr_area := area_ids[1 + ((i + j) % ac)];
            v_bt := CASE WHEN (i + j) % 3 = 0 THEN '跨省' WHEN (i + j) % 3 = 1 THEN '省内' ELSE '市内' END;

            INSERT INTO household_migration_request (
                handler_uuid, applicant_uuid, incoming_address,
                incoming_area_id, outgoing_address, outgoing_area_id,
                attachment, business_type, handle_date, handle_basis,
                fee, status, reject_reason, approval_permit_no,
                migration_permit_no, remark, is_deleted
            ) VALUES (
                CASE WHEN array_length(handlers,1) > 0
                    THEN handlers[1 + ((i+j) % array_length(handlers,1))] ELSE NULL END,
                v_person_uuid,
                gen_address(v_curr_area, i*1000 + j*100 + 400000),
                v_curr_area,
                gen_address(v_prev_area, i*1000 + j*100 + 500000),
                v_prev_area,
                'mig_'||i||'_'||j||'.pdf',
                v_bt,
                v_base_date + ((j * 37) || ' days')::INTERVAL,
                CASE WHEN j%2=0 THEN '迁移管理条例第'||(1+i%8)::TEXT||'条' ELSE NULL END,
                CASE WHEN j%4=0 THEN ((i*j*10)%200)::NUMERIC(10,2) ELSE NULL END,
                CASE WHEN i%5=0 THEN '迁移审批通过' WHEN i%5=1 THEN '准迁证审批中'
                     WHEN i%5=2 THEN '迁移审批中' WHEN i%5=3 THEN '迁移证审批中'
                     ELSE '准迁证审批驳回' END,
                CASE WHEN i%10=0 THEN '材料不全' ELSE NULL END,
                CASE WHEN i%3=0 THEN 'AP'||LPAD(i::TEXT,8,'0') ELSE NULL END,
                CASE WHEN i%4=0 THEN 'MP'||LPAD(i::TEXT,8,'0') ELSE NULL END,
                CASE WHEN i%7=0 THEN '连续迁移第'||j::TEXT||'步' ELSE NULL END,
                0
            );

            v_prev_area := v_curr_area;
        END LOOP;
    END LOOP;
END $$;

-- ============================================================
-- 18. 准迁证 (300) — 无外部依赖，generate_series 效率高
-- ============================================================
INSERT INTO approval_permit (permit_no, issue_date, expiry_date, issuing_authority, status, is_deleted)
SELECT
    '000000' || TO_CHAR(issue_d, 'YYYY') || LPAD(gs::TEXT, 6, '0'),
    issue_d,
    issue_d + (30 + floor(random() * 60)::INT || ' days')::INTERVAL,
    arr_rand(ARRAY['北京市公安局','上海市公安局','广州市公安局','深圳市公安局',
                    '成都市公安局','武汉市公安局','南京市公安局','杭州市公安局']),
    CASE WHEN random() < 0.60 THEN '有效' WHEN random() < 0.30 THEN '审批中' ELSE '作废' END,
    0
FROM generate_series(1, 300) AS gs,
LATERAL (SELECT CURRENT_DATE - (floor(random() * 365)::INT || ' days')::INTERVAL AS issue_d) AS d;

-- ============================================================
-- 19. 迁移证 (300)
-- ============================================================
INSERT INTO migration_permit (permit_no, issue_date, expiry_date, outgoing_police_station, status, is_deleted)
SELECT
    '000000' || TO_CHAR(issue_d, 'YYYY') || LPAD(gs::TEXT, 6, '0'),
    issue_d,
    issue_d + (30 + floor(random() * 60)::INT || ' days')::INTERVAL,
    arr_rand(ARRAY['东城分局','西城分局','朝阳分局','海淀分局','丰台分局',
                    '通州分局','大兴分局','顺义分局','昌平分局','白云分局']),
    CASE WHEN random() < 0.60 THEN '有效' WHEN random() < 0.30 THEN '审批中' ELSE '作废' END,
    0
FROM generate_series(1, 300) AS gs,
LATERAL (SELECT CURRENT_DATE - (floor(random() * 365)::INT || ' days')::INTERVAL AS issue_d) AS d;

-- ============================================================
-- 20. 操作日志 (15,000, 分布到分区表)
-- ============================================================
DO $$
DECLARE
    uuids VARCHAR(36)[]; uc INT; i INT;
BEGIN
    SELECT array_agg(user_uuid) INTO uuids FROM _user_pool; uc := array_length(uuids,1);
    FOR i IN 1..15000 LOOP
        INSERT INTO audit_log (operator_uuid, operation_time, ip_address, operation_type,
            target_type, target_id, before_data, after_data, is_deleted)
        VALUES (uuids[floor(random()*uc)::INT+1],
            CURRENT_TIMESTAMP - (floor(random()*365)::INT || ' days')::INTERVAL,
            '192.168.'||floor(random()*255)::TEXT||'.'||floor(random()*255)::TEXT,
            CASE WHEN random()<0.45 THEN '修改' WHEN random()<0.75 THEN '新增' ELSE '删除' END,
            arr_rand(ARRAY['resident','key_person','missing_person','household','fp_register','alert']),
            seq_uuid(floor(random()*50000)::BIGINT+1),
            CASE WHEN random()<0.30 THEN '{"before":"old_data"}' ELSE NULL END,
            CASE WHEN random()<0.50 THEN '{"after":"new_data"}' ELSE NULL END, 0);
    END LOOP;
END $$;

-- ============================================================
-- 21. 预警 (2,000, 分布到分区表)
-- ============================================================
DO $$
DECLARE
    uuids VARCHAR(36)[]; uc INT; i INT;
    handlers VARCHAR(36)[];
BEGIN
    SELECT array_agg(user_uuid) INTO uuids FROM _user_pool; uc := array_length(uuids,1);
    SELECT array_agg(user_uuid) INTO handlers FROM _user_pool WHERE user_role IN ('民警','系统管理员');
    FOR i IN 1..2000 LOOP
        INSERT INTO alert (alert_type, target_type, target_id, alert_content, severity,
            create_time, is_handled, handled_by, handled_at, is_deleted)
        VALUES (CASE WHEN random()<0.35 THEN '居住证到期' WHEN random()<0.60 THEN '证件到期'
                     WHEN random()<0.80 THEN '走访逾期' WHEN random()<0.92 THEN '重点人员匹配'
                     ELSE '其他' END,
            arr_rand(ARRAY['resident_permit','visit_plan','key_person','resident']),
            seq_uuid(floor(random()*50000)::BIGINT+1),
            arr_rand(ARRAY['居住证将于30日内到期，请及时续期','走访计划已逾期7天，请尽快完成',
                           '发现重点人员与常住人口匹配','户口迁移证件即将到期',
                           '流动人口登记信息异常','人员信息变更需要审核']),
            CASE WHEN random()<0.20 THEN '高' WHEN random()<0.60 THEN '中' ELSE '低' END,
            CURRENT_TIMESTAMP - (floor(random()*180)::INT || ' days')::INTERVAL,
            CASE WHEN random()<0.55 THEN 1 ELSE 0 END,
            CASE WHEN random()<0.55 AND handlers IS NOT NULL THEN handlers[floor(random()*array_length(handlers,1))::INT+1] ELSE NULL END,
            CASE WHEN random()<0.55 THEN CURRENT_TIMESTAMP - (floor(random()*90)::INT || ' days')::INTERVAL ELSE NULL END, 0);
    END LOOP;
END $$;

-- ============================================================
-- 22. 登录日志 (5,000, 分布到分区表)
-- ============================================================
DO $$
DECLARE
    uuids VARCHAR(36)[]; uc INT; i INT;
BEGIN
    SELECT array_agg(user_uuid) INTO uuids FROM _user_pool; uc := array_length(uuids,1);
    FOR i IN 1..5000 LOOP
        INSERT INTO login_log (user_uuid, login_time, ip_address, is_success, fail_reason, is_deleted)
        VALUES (uuids[floor(random()*uc)::INT+1],
            CURRENT_TIMESTAMP - (floor(random()*90)::INT || ' days')::INTERVAL,
            '10.0.'||floor(random()*255)::TEXT||'.'||floor(random()*255)::TEXT,
            CASE WHEN random()<0.92 THEN 1 ELSE 0 END,
            CASE WHEN random()<0.08 THEN arr_rand(ARRAY['密码错误','账号被锁定','账号已注销','验证码错误']) ELSE NULL END, 0);
    END LOOP;
END $$;

-- ============================================================
-- 清理临时对象
-- ============================================================
DROP TABLE IF EXISTS _res_pool;
DROP TABLE IF EXISTS _user_pool;
DROP TABLE IF EXISTS _police_pool;
DROP TABLE IF EXISTS _permit_pool;
DROP TABLE IF EXISTS _permit_pool_all;
DROP TABLE IF EXISTS _kp_pool;
DROP TABLE IF EXISTS _missing_pool;
DROP TABLE IF EXISTS _area_districts;
DROP FUNCTION IF EXISTS id_card_checksum;
DROP FUNCTION IF EXISTS weighted_pick;
DROP FUNCTION IF EXISTS arr_rand(TEXT[]);
DROP FUNCTION IF EXISTS arr_rand(BIGINT[]);
DROP FUNCTION IF EXISTS seq_uuid;
DROP FUNCTION IF EXISTS random_phone;
DROP FUNCTION IF EXISTS rand_row;

-- ============================================================
-- 统计输出
-- ============================================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE ' 测试数据生成完毕！';
    RAISE NOTICE '========================================';
    RAISE NOTICE ' resident:            %', (SELECT COUNT(*) FROM resident);
    RAISE NOTICE ' sys_user:            %', (SELECT COUNT(*) FROM sys_user);
    RAISE NOTICE ' police:              %', (SELECT COUNT(*) FROM police);
    RAISE NOTICE ' resident_relation:   %', (SELECT COUNT(*) FROM resident_relation);
    RAISE NOTICE ' resident_change_req: %', (SELECT COUNT(*) FROM resident_change_request);
    RAISE NOTICE ' resident_permit:     %', (SELECT COUNT(*) FROM resident_permit);
    RAISE NOTICE ' resident_registration:%', (SELECT COUNT(*) FROM resident_registration);
    RAISE NOTICE ' fp_register_record:  %', (SELECT COUNT(*) FROM fp_register_record);
    RAISE NOTICE ' resident_permit_renewal:%', (SELECT COUNT(*) FROM resident_permit_renewal);
    RAISE NOTICE ' key_person:          %', (SELECT COUNT(*) FROM key_person);
    RAISE NOTICE ' petition_record:     %', (SELECT COUNT(*) FROM petition_record);
    RAISE NOTICE ' visit_plan:          %', (SELECT COUNT(*) FROM visit_plan);
    RAISE NOTICE ' missing_person:      %', (SELECT COUNT(*) FROM missing_person);
    RAISE NOTICE ' missing_recovery:    %', (SELECT COUNT(*) FROM missing_person_recovery);
    RAISE NOTICE ' household_register:  %', (SELECT COUNT(*) FROM household_register);
    RAISE NOTICE ' household_business:  %', (SELECT COUNT(*) FROM household_business_request);
    RAISE NOTICE ' household_migration: %', (SELECT COUNT(*) FROM household_migration_request);
    RAISE NOTICE ' approval_permit:     %', (SELECT COUNT(*) FROM approval_permit);
    RAISE NOTICE ' migration_permit:    %', (SELECT COUNT(*) FROM migration_permit);
    RAISE NOTICE ' audit_log:           %', (SELECT COUNT(*) FROM audit_log);
    RAISE NOTICE ' alert:               %', (SELECT COUNT(*) FROM alert);
    RAISE NOTICE ' login_log:           %', (SELECT COUNT(*) FROM login_log);
    RAISE NOTICE '========================================';
END $$;
