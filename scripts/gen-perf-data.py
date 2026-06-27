#!/usr/bin/env python3
"""
PDM 逼真性能测试数据生成器 v4
- resident + 全部关联表数据生成
- 数据分布符合第七次全国人口普查
- 分发到 pdm_db + pdm_shard_0/1/2/3
- GB11643身份证校验位, 31省人口权重分布
用法: python scripts/gen-perf-data.py --total 5000 --dry-run
      python scripts/gen-perf-data.py --total 5000 --insert
"""

import sys, os, uuid, random, hashlib, argparse, subprocess, json
from collections import Counter

# ─── GB 11643-1999 身份证校验位 ─────────────────────────────
ID_WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
ID_CHECK = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

def id_checksum(id17: str) -> str:
    total = sum(w * int(c) for w, c in zip(ID_WEIGHTS, id17))
    return ID_CHECK[total % 11]

def gen_id_card(area_code: str, birth: str, gender: str, seq: int) -> str:
    ymd = birth.replace('-', '')
    # Use wider range to reduce collisions: 1-997, odd/even by gender
    if gender == '男':
        s = ((seq * 37 + 13) % 499) * 2 + 1  # odd
    else:
        s = ((seq * 41 + 7) % 499) * 2 + 2    # even
    id17 = area_code + ymd + f"{s:03d}"
    return id17 + id_checksum(id17)

def hash_mod(val: str, mod: int = 4) -> int:
    return int(hashlib.md5(val.encode()).hexdigest(), 16) % mod

# ─── 中国31省市区人口分布 (2020七普) ──────────────────────
PROVINCES = [
    ("44", "广东省", "广州市", 12601, ["广州","深圳","东莞","佛山","珠海","中山","惠州","汕头","湛江","江门","肇庆","揭阳","梅州","茂名","清远","韶关","河源","阳江","潮州","汕尾","云浮"]),
    ("37", "山东省", "济南市", 10153, ["济南","青岛","烟台","潍坊","临沂","济宁","淄博","菏泽","德州","威海","东营","泰安","滨州","聊城","日照","枣庄"]),
    ("41", "河南省", "郑州市", 9937, ["郑州","洛阳","南阳","许昌","周口","新乡","商丘","驻马店","信阳","平顶山","开封","安阳","焦作","濮阳","漯河","三门峡","鹤壁"]),
    ("32", "江苏省", "南京市", 8475, ["南京","苏州","无锡","常州","南通","徐州","扬州","盐城","泰州","镇江","淮安","连云港","宿迁"]),
    ("51", "四川省", "成都市", 8367, ["成都","绵阳","宜宾","德阳","南充","泸州","达州","乐山","凉山","内江","自贡","眉山","遂宁","广安","攀枝花","广元","资阳","巴中","雅安"]),
    ("13", "河北省", "石家庄市", 7461, ["石家庄","唐山","保定","邯郸","沧州","邢台","廊坊","衡水","秦皇岛","张家口","承德"]),
    ("43", "湖南省", "长沙市", 6644, ["长沙","株洲","湘潭","衡阳","邵阳","岳阳","常德","益阳","郴州","永州","怀化","娄底","湘西","张家界"]),
    ("33", "浙江省", "杭州市", 6457, ["杭州","宁波","温州","嘉兴","绍兴","台州","金华","湖州","衢州","丽水","舟山"]),
    ("34", "安徽省", "合肥市", 6103, ["合肥","芜湖","蚌埠","淮南","马鞍山","淮北","铜陵","安庆","黄山","滁州","阜阳","宿州","六安","亳州","池州","宣城"]),
    ("42", "湖北省", "武汉市", 5775, ["武汉","襄阳","宜昌","荆州","黄冈","孝感","十堰","荆门","黄石","咸宁","恩施","鄂州","随州","仙桃","天门","潜江"]),
    ("45", "广西壮族自治区", "南宁市", 5013, ["南宁","柳州","桂林","玉林","贵港","北海","钦州","梧州","百色","河池","来宾","贺州","防城港","崇左"]),
    ("53", "云南省", "昆明市", 4721, ["昆明","曲靖","玉溪","保山","昭通","丽江","普洱","临沧","红河","文山","西双版纳","楚雄","大理","德宏"]),
    ("36", "江西省", "南昌市", 4519, ["南昌","九江","赣州","吉安","宜春","抚州","上饶","景德镇","萍乡","新余","鹰潭"]),
    ("21", "辽宁省", "沈阳市", 4259, ["沈阳","大连","鞍山","抚顺","本溪","丹东","锦州","营口","阜新","辽阳","盘锦","铁岭","朝阳","葫芦岛"]),
    ("35", "福建省", "福州市", 4154, ["福州","厦门","泉州","漳州","龙岩","三明","南平","莆田","宁德"]),
    ("61", "陕西省", "西安市", 3953, ["西安","咸阳","宝鸡","渭南","汉中","榆林","延安","安康","商洛","铜川"]),
    ("52", "贵州省", "贵阳市", 3856, ["贵阳","遵义","六盘水","安顺","毕节","铜仁","黔东南","黔南","黔西南"]),
    ("14", "山西省", "太原市", 3492, ["太原","大同","阳泉","长治","晋城","朔州","晋中","运城","忻州","临汾","吕梁"]),
    ("50", "重庆市", "重庆市", 3205, ["渝中","江北","沙坪坝","九龙坡","南岸","渝北","巴南","万州","涪陵","黔江","长寿","江津","合川","永川","南川"]),
    ("22", "吉林省", "长春市", 2407, ["长春","吉林","四平","辽源","通化","白山","松原","白城","延边"]),
    ("62", "甘肃省", "兰州市", 2502, ["兰州","嘉峪关","金昌","白银","天水","武威","张掖","平凉","酒泉","庆阳","定西","陇南","临夏","甘南"]),
    ("23", "黑龙江省", "哈尔滨市", 3185, ["哈尔滨","齐齐哈尔","牡丹江","佳木斯","大庆","鸡西","双鸭山","伊春","七台河","鹤岗","黑河","绥化","大兴安岭"]),
    ("15", "内蒙古自治区", "呼和浩特市", 2405, ["呼和浩特","包头","乌海","赤峰","通辽","鄂尔多斯","呼伦贝尔","巴彦淖尔","乌兰察布","兴安","锡林郭勒","阿拉善"]),
    ("65", "新疆维吾尔自治区", "乌鲁木齐市", 2585, ["乌鲁木齐","克拉玛依","吐鲁番","哈密","昌吉","博尔塔拉","巴音郭楞","阿克苏","克孜勒苏","喀什","和田","伊犁","塔城","阿勒泰"]),
    ("11", "北京市", "北京市", 2189, ["东城","西城","朝阳","丰台","石景山","海淀","顺义","通州","大兴","房山","门头沟","昌平","平谷","密云","怀柔","延庆"]),
    ("31", "上海市", "上海市", 2487, ["黄浦","徐汇","长宁","静安","普陀","虹口","杨浦","闵行","宝山","嘉定","浦东","金山","松江","青浦","奉贤","崇明"]),
    ("12", "天津市", "天津市", 1387, ["和平","河东","河西","南开","河北","红桥","东丽","西青","津南","北辰","武清","宝坻","滨海","宁河","静海","蓟州"]),
    ("46", "海南省", "海口市", 1008, ["海口","三亚","三沙","儋州","五指山","琼海","文昌","万宁","东方"]),
    ("64", "宁夏回族自治区", "银川市", 720, ["银川","石嘴山","吴忠","固原","中卫"]),
    ("63", "青海省", "西宁市", 592, ["西宁","海东","海北","黄南","海南","果洛","玉树","海西"]),
    ("54", "西藏自治区", "拉萨市", 365, ["拉萨","日喀则","昌都","林芝","山南","那曲","阿里"]),
]

# ─── 人口统计分布 ──────────────────────────────────────────
GENDER_BY_AGE = {
    (0, 17):   (0.517, 0.483),
    (18, 34):  (0.512, 0.488),
    (35, 59):  (0.508, 0.492),
    (60, 150): (0.480, 0.520),
}

AGE_GROUPS = [
    (0, 14, 0.170), (15, 34, 0.290), (35, 54, 0.320),
    (55, 74, 0.190), (75, 100, 0.030),
]

# 文化程度 — 匹配 resident CHECK: ('研究生','大学本科','大学专科','中等职业教育','技工学校','高中','初中','小学','文盲或半文盲','未知')
EDUCATION = [
    ("研究生", "01", 0.7), ("大学本科", "02", 5.8), ("大学专科", "03", 7.6),
    ("中等职业教育", "04", 5.6), ("技工学校", "05", 1.0),
    ("高中", "06", 12.5), ("初中", "07", 33.5), ("小学", "08", 24.8),
    ("文盲或半文盲", "09", 3.2), ("未知", "99", 5.3),
]

# 婚姻 — 匹配 resident CHECK: ('未婚','已婚','初婚','再婚','复婚','丧偶','离婚','未说明的婚姻状况')
MARITAL_BY_AGE = {
    (0, 19):   [("未婚", 0.99), ("初婚", 0.01)],
    (20, 29):  [("未婚", 0.55), ("初婚", 0.35), ("再婚", 0.05), ("离婚", 0.03), ("未说明的婚姻状况", 0.02)],
    (30, 39):  [("未婚", 0.08), ("初婚", 0.70), ("再婚", 0.08), ("离婚", 0.06), ("丧偶", 0.03), ("复婚", 0.03), ("未说明的婚姻状况", 0.02)],
    (40, 59):  [("未婚", 0.02), ("初婚", 0.72), ("再婚", 0.08), ("离婚", 0.07), ("丧偶", 0.06), ("复婚", 0.03), ("未说明的婚姻状况", 0.02)],
    (60, 150): [("未婚", 0.01), ("初婚", 0.55), ("再婚", 0.06), ("离婚", 0.03), ("丧偶", 0.28), ("复婚", 0.04), ("未说明的婚姻状况", 0.03)],
}

HOUSEHOLD_TYPES = [("居民户口", 0.45), ("农业户口", 0.40), ("非农业户口", 0.15)]

NATIONS = [
    ("汉族", "01", 91.1), ("壮族", "02", 1.38), ("回族", "04", 0.79),
    ("满族", "05", 0.78), ("维吾尔族", "06", 0.75), ("苗族", "07", 0.70),
    ("彝族", "08", 0.63), ("土家族", "09", 0.54), ("藏族", "10", 0.48),
    ("蒙古族", "11", 0.41), ("侗族", "13", 0.23), ("布依族", "14", 0.20),
    ("瑶族", "15", 0.19), ("白族", "16", 0.14), ("朝鲜族", "17", 0.12),
    ("哈尼族", "18", 0.12), ("其他", "97", 0.18),
]

OCCUPATIONS = [
    ("农、林、牧、渔业生产人员", 0.24), ("生产制造及有关人员", 0.18),
    ("社会生产服务和生活服务人员", 0.16), ("专业技术人员", 0.10),
    ("办事人员和有关人员", 0.08), ("商业、服务业人员", 0.10),
    ("国家机关、企事业单位负责人", 0.03), ("学生", 0.06),
    ("退休人员", 0.04), ("其他从业人员", 0.01),
]

BLOOD_TYPES = [("O", 0.34), ("A", 0.28), ("B", 0.29), ("AB", 0.07), ("未知", 0.02)]

MOBILE_PREFIXES = [
    '130','131','132','133','134','135','136','137','138','139',
    '150','151','152','153','155','156','157','158','159',
    '166','170','171','172','173','174','175','176','177','178',
    '180','181','182','183','184','185','186','187','188','189',
    '190','191','192','193','194','195','196','197','198','199',
]

# ─── 加权随机选择 ───────────────────────────────────────────
def weighted_choice(items):
    """items: list of (*values, weight) — last element is always weight"""
    total = sum(item[-1] for item in items)
    r = random.random() * total
    cumulative = 0.0
    for item in items:
        cumulative += item[-1]
        if r <= cumulative:
            return item[:-1] if len(item) > 2 else item[0]
    last = items[-1]
    return last[:-1] if len(last) > 2 else last[0]

def esc(val):
    if val is None: return "NULL"
    if isinstance(val, bool): return "TRUE" if val else "FALSE"
    if isinstance(val, (int, float)): return str(val)
    return "'" + str(val).replace("'", "''") + "'"

# ─── 姓名生成 ───────────────────────────────────────────────
SURNAMES = [
    ("王",70),("李",67),("张",62),("刘",50),("陈",46),("杨",35),("黄",28),
    ("赵",24),("吴",22),("周",22),("徐",17),("孙",16),("马",14),("朱",14),
    ("胡",13),("郭",13),("何",12),("高",11),("林",11),("罗",10),("郑",9),
    ("梁",9),("谢",8),("唐",8),("冯",7),("宋",7),("邓",7),("彭",7),("曹",7),
    ("曾",7),("田",6),("董",6),("潘",6),("袁",6),("蔡",6),("蒋",6),("余",6),
    ("于",5),("杜",5),("叶",5),("程",5),("苏",5),("魏",5),("吕",4),("丁",4),
    ("任",4),("卢",4),("姚",4),("钟",4),("姜",4),("崔",4),("谭",3),("陆",3),
    ("汪",3),("范",3),("金",3),("石",3),("廖",3),("贾",3),("夏",3),("韦",3),
    ("付",3),("方",3),("白",3),("邹",3),("孟",3),("熊",3),("秦",3),("邱",2),
    ("江",2),("尹",2),("薛",2),("闫",2),("段",2),("雷",2),("侯",2),("龙",2),
]

MALE_GIVEN = [
    "伟","强","磊","军","勇","杰","涛","明","超","平","辉","鹏","华","飞",
    "斌","浩","宇","轩","博","文","刚","毅","峰","俊","亮","龙","凯","瑞",
    "霖","鑫","志","国","建","忠","庆","新","海","林","东","成","思远","子涵",
    "一鸣","天宇","浩然","子轩","俊杰","志强","建国","建华","志明","文博",
]

FEMALE_GIVEN = [
    "芳","敏","静","丽","婷","雪","琳","萍","红","霞","娟","慧","颖","娜",
    "艳","琴","玲","秀","兰","洁","梅","燕","丹","莲","薇","倩","蓉","莉",
    "怡","月","蕾","云","露","瑶","雯","思","雨","佳","欣","诗涵","梓涵",
    "雨桐","一诺","欣怡","梓萱","梦瑶","语嫣","晓雪","思雨","若曦","紫萱",
]

def gen_name(gender: str) -> tuple:
    surname = weighted_choice(SURNAMES)
    pool = MALE_GIVEN if gender == '男' else FEMALE_GIVEN
    given = random.choice(pool)
    if random.random() < 0.45:
        g2 = random.choice(pool)
        if g2 != given: given += g2
    former = None
    if random.random() < 0.03:
        former = surname + random.choice(pool)
    return surname + given, former

# ─── 地址码 ─────────────────────────────────────────────────
def build_area_codes():
    codes = []
    for prefix, prov_name, capital, weight, cities in PROVINCES:
        for ci, city in enumerate(cities):
            n = min(3 + ci % 3, len(cities))
            for di in range(n):
                district_code = f"{prefix}{10 + ci:02d}{1 + di:02d}"
                codes.append((district_code, f"{prov_name}{city}{['区','县','市'][di%3]}", weight / n))
    return codes

AREA_CODES = build_area_codes()

# ─── Resident 生成 ──────────────────────────────────────────
def gen_resident_sql(i, total):
    area_code, area_name = weighted_choice(AREA_CODES)

    age_range = weighted_choice(AGE_GROUPS)
    age = random.randint(age_range[0], age_range[1])
    birth_year = 2026 - age
    birth_month = random.randint(1, 12)
    birth_day = random.randint(1, 28)
    birth = f"{birth_year}-{birth_month:02d}-{birth_day:02d}"

    for (lo, hi), (m, f) in GENDER_BY_AGE.items():
        if lo <= age <= hi:
            gender = '男' if random.random() < m else '女'
            break
    else:
        gender = '男' if random.random() < 0.51 else '女'

    name, former_name = gen_name(gender)
    row_uuid = str(uuid.uuid4())
    id_card = gen_id_card(area_code, birth, gender, i)

    nation_info = weighted_choice(NATIONS)
    if isinstance(nation_info, tuple):
        nation, nation_code = nation_info[0], nation_info[1]
    else:
        nation, nation_code = str(nation_info), "01"

    if age >= 6:
        edu_info = weighted_choice(EDUCATION)
        if isinstance(edu_info, tuple):
            edu, edu_code = edu_info[0], edu_info[1]
        else:
            edu, edu_code = str(edu_info), "99"
    else:
        edu, edu_code = "未知", "99"

    blood = weighted_choice(BLOOD_TYPES)

    marital = None
    for (lo, hi), opts in MARITAL_BY_AGE.items():
        if lo <= age <= hi:
            marital = weighted_choice(opts)
            break
    if not marital:
        marital = "未婚" if age < 22 else "初婚"

    if age < 16:
        occupation = "学生"
    elif age >= 60:
        occupation = "退休人员" if random.random() < 0.7 else random.choice(["农民","个体户","其他"])
    else:
        occupation = weighted_choice(OCCUPATIONS)

    phone = random.choice(MOBILE_PREFIXES) + f"{random.randint(0,99999999):08d}"
    hukou_city = area_name.split('市')[0] + '市' if '市' in area_name else area_name
    street = random.choice(['街道','镇','路','大街'])
    residence = f"{hukou_city}{random.choice(['东','西','南','北','中','新','老'])}{random.randint(1,300)}{street}{random.randint(1,999)}号"
    household_address = residence + f"{random.randint(1,30)}室" if random.random() < 0.6 else residence
    hukou_type = weighted_choice(HOUSEHOLD_TYPES)
    hukou_status = weighted_choice([("正常", 0.92), ("迁出注销", 0.05), ("死亡注销", 0.02), ("恢复", 0.01)])
    area_id = random.randint(1, 464)
    shard = abs(hash(row_uuid)) % 4
    former_str = 'NULL' if former_name is None else esc(former_name)

    return {
        'uuid': row_uuid,
        'shard': shard,
        'gender': gender,
        'age': age,
        'sql': (
            f"INSERT INTO resident (uuid, name, former_name, gender, id_card_no, nation, nation_code, "
            f"birth_date, education_level, education_code, blood_type, marital_status, occupation, phone, "
            f"photo, residence, area_id, household_type, household_status, "
            f"household_address, household_area_id) VALUES ("
            f"{esc(row_uuid)}, {esc(name)}, {former_str}, "
            f"{esc(gender)}, {esc(id_card)}, {esc(nation)}, {esc(nation_code)}, "
            f"{esc(birth)}, {esc(edu)}, {esc(edu_code)}, {esc(blood)}, {esc(marital)}, "
            f"{esc(occupation)}, {esc(phone)}, "
            f"NULL, {esc(residence)}, {area_id}, "
            f"{esc(hukou_type)}, {esc(hukou_status)}, "
            f"{esc(household_address)}, {random.randint(1, 464)}"
            f") ON CONFLICT (id_card_no) DO NOTHING;"
        )
    }

# ─── 其他表生成器 ────────────────────────────────────────────
def gen_relations_sql(residents):
    """resident_relation: 为~40%居民生成家庭关系"""
    sqls = []
    eligible = [r for r in residents]
    count = min(len(residents) * 4 // 10, len(eligible))
    for i, r in enumerate(random.sample(eligible, count)):
        relation_uuid = str(uuid.uuid4())
        fathers = [x for x in residents if x['gender'] == '男' and x['age'] > r['age'] + 18 and x['uuid'] != r['uuid']]
        mothers = [x for x in residents if x['gender'] == '女' and x['age'] > r['age'] + 18 and x['uuid'] != r['uuid']]
        spouses = [x for x in residents if x['gender'] != r['gender'] and abs(x['age'] - r['age']) < 15 and x['uuid'] != r['uuid']]
        father_uuid = random.choice(fathers)['uuid'] if fathers and random.random() < 0.7 else None
        mother_uuid = random.choice(mothers)['uuid'] if mothers and random.random() < 0.7 else None
        spouse_uuid = random.choice(spouses)['uuid'] if spouses and random.random() < 0.5 else None
        sql = (f"INSERT INTO resident_relation (relation_person_uuid, father_uuid, mother_uuid, spouse_uuid) "
               f"VALUES ({esc(relation_uuid)}, {esc(father_uuid)}, {esc(mother_uuid)}, {esc(spouse_uuid)});")
        sqls.append(("pdm_shard_" + str(hash_mod(relation_uuid)), sql))
    return sqls

def gen_keypersons_sql(residents):
    sqls = []
    control_types = ["涉稳人员","涉毒人员","社区矫正人员","精神障碍患者(肇事肇祸风险)","刑满释放人员","信访重点人员","其他重点人员"]
    levels = [("一级",10.0),("二级",30.0),("三级",60.0)]
    kp_uuids = []
    for _ in range(max(1, len(residents) * 4 // 100)):
        r = random.choice(residents)
        kp_uuids.append(r['uuid'])
        sql = (f"INSERT INTO key_person (uuid, control_level, control_type, responsible_police_no) "
               f"VALUES ({esc(r['uuid'])}, {esc(weighted_choice(levels))}, {esc(random.choice(control_types))}, "
               f"{esc(f'P{random.randint(10000,99999)}')});")
        sqls.append(("pdm_shard_" + str(hash_mod(r['uuid'])), sql))
    return sqls, kp_uuids

def gen_visit_plans_sql(kp_uuids):
    """visit_plan 在 pdm_db (shard无此表)"""
    sqls = []
    for kp_uuid in kp_uuids:
        for _ in range(random.randint(1, 3)):
            planned = f"202{random.randint(4,6)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}"
            vtype = random.choice(["入户走访","电话","视频"])
            vstat = weighted_choice([("待走访",40.0),("已完成",35.0),("已逾期",15.0),("已取消",10.0)])
            actual = "NULL" if vstat == "待走访" else esc(f"202{random.randint(4,6)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}")
            sql = (f"INSERT INTO visit_plan (key_person_uuid, planned_date, actual_date, visit_type, "
                   f"status, assigned_police_no) "
                   f"VALUES ({esc(kp_uuid)}, {esc(planned)}, {actual}, {esc(vtype)}, "
                   f"{esc(vstat)}, {esc(f'P{random.randint(10000,99999)}')});")
            sqls.append(("main", sql))
    return sqls

def gen_petitions_sql(kp_uuids):
    """petition_record 在 pdm_db (shard无此表)"""
    sqls = []
    for kp_uuid in kp_uuids:
        if random.random() < 0.7: continue
        sql = (f"INSERT INTO petition_record (key_person_uuid, handler_police_no, petition_time, "
               f"address, remark, evaluation) "
               f"VALUES ({esc(kp_uuid)}, {esc(f'P{random.randint(10000,99999)}')}, "
               f"NOW() - INTERVAL '{random.randint(1,365)} days', {esc(f'{random.choice([p[1] for p in PROVINCES])}信访办')}, "
               f"{esc(random.choice(['合理诉求','需要调解','已化解','情绪激动']))}, "
               f"{esc(random.choice(['满意','基本满意','不满意','未评价']))});")
        sqls.append(("main", sql))
    return sqls

def gen_resident_permits_sql(residents):
    sqls = []
    for _ in range(max(1, len(residents) * 8 // 100)):
        r = random.choice(residents)
        sql = (f"INSERT INTO resident_permit (permit_no, uuid, issue_date, expiry_date, status) "
               f"VALUES ({esc(f'RP-{random.randint(2020000000,2025999999)}')}, {esc(r['uuid'])}, "
               f"{esc(f'202{random.randint(2,5)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}')}, "
               f"{esc(f'202{random.randint(5,7)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}')}, "
               f"{esc(weighted_choice([('有效',60.0),('过期',30.0),('注销',10.0)]))});")
        sqls.append(("pdm_shard_" + str(hash_mod(r['uuid'])), sql))
    return sqls

def gen_resident_registrations_sql(residents):
    sqls = []
    for _ in range(max(1, len(residents) * 12 // 100)):
        r = random.choice(residents)
        p = random.choice(PROVINCES)
        sql = (f"INSERT INTO resident_registration (uuid, original_address, current_address, "
               f"address_type, house_ownership, purpose, expected_duration, register_date) "
               f"VALUES ({esc(r['uuid'])}, {esc(p[1]+'某地原址')}, {esc(p[1]+'某地现址')}, "
               f"{esc(random.choice(['自有住房','租赁房屋','单位宿舍','学校宿舍','亲友借住','其他']))}, "
               f"{esc(random.choice(['自购','租赁','单位分配','借住']))}, "
               f"{esc(random.choice(['务工','经商','求学','投靠亲属','其他']))}, "
               f"{esc(random.choice(['短租','中租','长租']))}, "
               f"{esc(f'202{random.randint(2,5)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}')});")
        sqls.append(("pdm_shard_" + str(hash_mod(r['uuid'])), sql))
    return sqls

def gen_fp_register_sql(residents):
    sqls = []
    for _ in range(max(1, len(residents) * 6 // 100)):
        r = random.choice(residents)
        sql = (f"INSERT INTO fp_register_record (uuid, register_date) "
               f"VALUES ({esc(r['uuid'])}, "
               f"{esc(f'202{random.randint(3,5)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}')});")
        sqls.append(("pdm_shard_" + str(hash_mod(r['uuid'])), sql))
    return sqls

def gen_change_requests_sql(residents):
    sqls = []
    for _ in range(max(1, len(residents) * 2 // 100)):
        r = random.choice(residents)
        field = random.choice(["name","phone","residence","occupation"])
        sql = (f"INSERT INTO resident_change_request (applicant_uuid, change_field, request_time, "
               f"original_data, modified_data, status) "
               f"VALUES ({esc(r['uuid'])}, {esc(field)}, "
               f"{esc(f'202{random.randint(4,6)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}')}, "
               f"'{{\"old\":\"值\"}}', '{{\"new\":\"值\"}}', "
               f"{esc(weighted_choice([('请求',30.0),('一审',25.0),('二审',20.0),('通过',15.0),('驳回',10.0)]))});")
        sqls.append(("pdm_shard_" + str(hash_mod(r['uuid'])), sql))
    return sqls

def gen_alerts_sql(residents):
    """alert 在 pdm_db (shard无此表)"""
    sqls = []
    atypes = ["居住证到期","走访逾期","重点人员匹配","证件到期","其他"]
    for _ in range(max(1, len(residents) * 2 // 100)):
        r = random.choice(residents)
        sql = (f"INSERT INTO alert (alert_type, target_type, target_id, alert_content, severity) "
               f"VALUES ({esc(random.choice(atypes))}, 'resident', {esc(r['uuid'])}, "
               f"{esc('自动生成告警')}, {esc(weighted_choice([('高',10.0),('中',30.0),('低',60.0)]))});")
        sqls.append(("main", sql))
    return sqls

# ─── 主库表 (pdm_db only) ────────────────────────────────────
def gen_users_sql(residents):
    sqls = []
    roles = ["用户管理员","数据审查员","采集员","街道办","民警","市局负责人","普通用户"]
    for i in range(10):
        r = residents[i * 500 + i * 7 if i * 500 + i * 7 < len(residents) else i]
        user_uuid = f"user-{i+2:04d}-0000-0000-000000000{str(i+2).zfill(3)}"
        sql = (f"INSERT INTO sys_user (user_uuid, username, password, resident_uuid, user_role, "
               f"permission_group_id, phone, register_materials, account_status, must_change_password) "
               f"VALUES ({esc(user_uuid)}, {esc(f'testuser{i+2}')}, "
               f"'$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', "
               f"{esc(r['uuid'])}, {esc(random.choice(roles))}, {random.randint(1,8)}, "
               f"{esc(random.choice(MOBILE_PREFIXES) + f'{random.randint(0,99999999):08d}')}, "
               f"'测试注册材料', '有效', FALSE);")
        sqls.append(("main", sql))
    return sqls

def gen_police_sql(residents):
    sqls = []
    ranks = ["警员","警司","警督","警监"]
    stations = ["东城派出所","西城派出所","南城派出所","北城派出所","城中派出所","城郊派出所"]
    depts = ["治安大队","刑侦大队","交警大队","经侦大队","禁毒大队","网安大队"]
    for _ in range(15):
        r = random.choice(residents)
        sql = (f"INSERT INTO police (police_number, resident_uuid, police_station, jurisdiction, "
               f"department, police_rank, duty_status) "
               f"VALUES ({esc(f'P{random.randint(10000,99999)}')}, {esc(r['uuid'])}, "
               f"{esc(random.choice(stations))}, {esc(random.choice([p[1] for p in PROVINCES])+'辖区')}, "
               f"{esc(random.choice(depts))}, {esc(weighted_choice([('警员',40.0),('警司',35.0),('警督',20.0),('警监',5.0)]))}, "
               f"{esc(weighted_choice([('在岗',85.0),('调岗',10.0),('离职',5.0)]))});")
        sqls.append(("main", sql))
    return sqls

def gen_household_register_sql(residents):
    sqls = []
    for _ in range(300):
        r = random.choice(residents)
        sql = (f"INSERT INTO household_register (household_book_no, householder_uuid, establish_date, "
               f"hukou_address, status, member_uuid_list) "
               f"VALUES ({esc(f'HH-{random.randint(2020000000,2025999999)}')}, {esc(r['uuid'])}, "
               f"{esc(f'202{random.randint(0,5)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}')}, "
               f"{esc(random.choice([p[1] for p in PROVINCES])+'某街道'+str(random.randint(1,500))+'号')}, "
               f"{esc(weighted_choice([('审批中',15.0),('有效',75.0),('冻结',5.0),('无效',5.0)]))}, "
               f"'{json.dumps([r['uuid'], random.choice(residents)['uuid']])}');")
        sqls.append(("main", sql))
    return sqls

def gen_household_business_sql(residents):
    sqls = []
    for _ in range(100):
        r = random.choice(residents)
        sql = (f"INSERT INTO household_business_request (applicant_uuid, attachment, business_type, "
               f"handle_date, status) "
               f"VALUES ({esc(r['uuid'])}, 'test.pdf', "
               f"{esc(weighted_choice([('登记',50.0),('注销',30.0),('户主变更',20.0)]))}, "
               f"{esc(f'202{random.randint(4,6)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}')}, "
               f"{esc(weighted_choice([('审批中',30.0),('批准',55.0),('驳回',15.0)]))});")
        sqls.append(("main", sql))
    return sqls

def gen_household_migration_sql(residents):
    sqls = []
    for _ in range(80):
        r = random.choice(residents)
        p1, p2 = random.choice(PROVINCES)[1], random.choice(PROVINCES)[1]
        sql = (f"INSERT INTO household_migration_request (applicant_uuid, incoming_address, "
               f"outgoing_address, attachment, business_type, handle_date, status) "
               f"VALUES ({esc(r['uuid'])}, {esc(p1+'某地')}, {esc(p2+'某地')}, "
               f"'migration.pdf', {esc(random.choice(['市内','省内','跨省']))}, "
               f"{esc(f'202{random.randint(4,6)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}')}, "
               f"'迁移审批通过');")
        sqls.append(("main", sql))
    return sqls

def gen_missing_persons_sql(residents):
    sqls = []
    for _ in range(80):
        r = random.choice(residents)
        appearance = f'身高{random.randint(155,180)}cm，体型{random.choice(["偏瘦","中等","偏胖"])}'
        sql = (f"INSERT INTO missing_person (resident_uuid, missing_date, missing_place, photo, "
               f"appearance, possible_way, contact_phone, status) "
               f"VALUES ({esc(r['uuid'])}, "
               f"{esc(f'202{random.randint(4,6)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}')}, "
               f"{esc(random.choice([p[1] for p in PROVINCES])+'某地')}, '/photos/placeholder.jpg', "
               f"{esc(appearance)}, "
               f"{esc(random.choice(['离家出走','走失','疑似被拐','联系中断','其他']))}, "
               f"{esc(random.choice(MOBILE_PREFIXES) + f'{random.randint(0,99999999):08d}')}, "
               f"{esc(weighted_choice([('失踪中',70.0),('已经寻回',30.0)]))});")
        sqls.append(("main", sql))
    return sqls

def gen_missing_recovery_sql(residents=None):
    sqls = []
    for _ in range(25):
        sql = (f"INSERT INTO missing_person_recovery (missing_record_rid, recovery_date, summary) "
               f"VALUES ({random.randint(1,80)}, "
               f"{esc(f'202{random.randint(5,6)}-{random.randint(1,12):02d}-{random.randint(1,28):02d}')}, "
               f"{esc(random.choice(['警方寻回','自行返回','家属找到','群众举报','其他方式']))});")
        sqls.append(("main", sql))
    return sqls

def gen_audit_logs_sql(residents):
    sqls = []
    op_types = ["新增","修改","删除"]
    ttypes = ["resident","household","key_person","missing_person","fp_register","sys_user"]
    for _ in range(500):
        r = random.choice(residents)
        sql = (f"INSERT INTO audit_log (operator_uuid, operation_type, target_type, target_id, ip_address) "
               f"VALUES ({esc(f'user-{random.randint(1,11):04d}-0000-0000-00000000000{random.randint(1,9)}')}, "
               f"{esc(random.choice(op_types))}, {esc(random.choice(ttypes))}, {esc(r['uuid'])}, "
               f"{esc(f'192.168.{random.randint(1,255)}.{random.randint(1,255)}')});")
        sqls.append(("main", sql))
    return sqls

def gen_login_logs_sql(residents=None):
    sqls = []
    for _ in range(300):
        sql = (f"INSERT INTO login_log (user_uuid, ip_address, is_success, fail_reason) "
               f"VALUES ({esc(f'user-{random.randint(1,11):04d}-0000-0000-00000000000{random.randint(1,9)}')}, "
               f"{esc(f'192.168.{random.randint(1,255)}.{random.randint(1,255)}')}, "
               f"{'0' if random.random() < 0.08 else '1'}, "
               f"{esc(random.choice(['密码错误','账户冻结',None]))});")
        sqls.append(("main", sql))
    return sqls

# ─── Main ────────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="PDM 性能测试数据生成器 v4")
    parser.add_argument("--total", type=int, default=5000, help="居民总数 (default: 5000)")
    parser.add_argument("--dry-run", action="store_true", help="仅生成SQL文件不执行")
    parser.add_argument("--insert", action="store_true", help="直接插入数据库")
    args = parser.parse_args()

    if not args.dry_run and not args.insert:
        args.dry_run = True

    total = args.total
    residents = []
    shard_counts = Counter()

    print(f"生成 {total} 条逼真居民数据...")
    for i in range(1, total + 1):
        r = gen_resident_sql(i, total)
        residents.append(r)
        shard_counts[r['shard']] += 1
        if i % 2000 == 0:
            print(f"  已生成 {i}/{total}...")

    print(f"\n分片分布 (HASH_MOD % 4):")
    for s in range(4):
        print(f"  pdm_shard_{s}: {shard_counts[s]} ({shard_counts[s]/total*100:.1f}%)")

    # 生成所有SQL
    all_sql = {"pdm_shard_0": [], "pdm_shard_1": [], "pdm_shard_2": [], "pdm_shard_3": [], "main": []}
    seen = set()

    def add_sql(db, sql):
        all_sql[db].append(sql)
        seen.add(sql[:80])

    # Residents -> shards
    for r in residents:
        all_sql[f"pdm_shard_{r['shard']}"].append(r['sql'])

    # Sharded tables
    for fn in [gen_relations_sql, gen_resident_permits_sql, gen_resident_registrations_sql,
                gen_fp_register_sql, gen_change_requests_sql, gen_alerts_sql]:
        for db, sql in fn(residents):
            add_sql(db, sql)

    kp_sqls, kp_uuids = gen_keypersons_sql(residents)
    for db, sql in kp_sqls: add_sql(db, sql)
    for db, sql in gen_visit_plans_sql(kp_uuids): add_sql(db, sql)
    for db, sql in gen_petitions_sql(kp_uuids): add_sql(db, sql)

    # Main-DB tables
    for fn in [gen_users_sql, gen_police_sql, gen_household_register_sql,
                gen_household_business_sql, gen_household_migration_sql,
                gen_missing_persons_sql, gen_missing_recovery_sql,
                gen_audit_logs_sql, gen_login_logs_sql]:
        for db, sql in fn(residents):
            add_sql(db, sql)

    total_sql = sum(len(v) for v in all_sql.values())
    print(f"\n总SQL: {total_sql}")
    for db in ["pdm_shard_0","pdm_shard_1","pdm_shard_2","pdm_shard_3","main"]:
        print(f"  {db}: {len(all_sql[db])}")

    # 写入SQL文件 (shard不用事务包裹, 避免单条失败导致全部回滚)
    os.makedirs("performance/test-data", exist_ok=True)
    for db in ["pdm_shard_0","pdm_shard_1","pdm_shard_2","pdm_shard_3","main"]:
        fname = f"performance/test-data/insert-{db}.sql"
        with open(fname, 'w', encoding='utf-8') as f:
            if db == "main":
                f.write("BEGIN;\n")
            for sql in all_sql[db]:
                f.write(sql + '\n')
            if db == "main":
                f.write("COMMIT;\n")
        print(f"  SQL: {fname} ({os.path.getsize(fname)/1024:.0f} KB)")

    if args.insert:
        print("\n插入数据库...")
        db_names = {"pdm_shard_0":"pdm_shard_0","pdm_shard_1":"pdm_shard_1",
                     "pdm_shard_2":"pdm_shard_2","pdm_shard_3":"pdm_shard_3","main":"pdm_db"}
        for db_key, db_name in db_names.items():
            fpath = f"performance/test-data/insert-{db_key}.sql"
            print(f"  {db_name}...", end=' ', flush=True)
            r = subprocess.run(
                f'type "{fpath}" | wsl docker exec -i pdm-postgresql psql -U pdm -d {db_name}',
                shell=True, capture_output=True, text=True, timeout=120
            )
            if r.returncode != 0:
                print(f"ERROR: {r.stderr[:200]}")
            else:
                print("OK")

        # 验证
        print("\n=== 数据分布验证 ===")
        for db_name in ['pdm_db','pdm_shard_0','pdm_shard_1','pdm_shard_2','pdm_shard_3']:
            r = subprocess.run(
                f'echo "SELECT COUNT(*) FROM resident;" | wsl docker exec -i pdm-postgresql psql -U pdm -d {db_name} -t',
                shell=True, capture_output=True, text=True, timeout=15
            )
            count = r.stdout.strip()
            print(f"  {db_name}: {count} residents")

    print("\n完成!")

if __name__ == '__main__':
    main()
