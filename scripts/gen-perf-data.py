#!/usr/bin/env python3
"""
PDM 逼真性能测试数据生成器 v3
- 数据分布符合第七次全国人口普查
- 分发到 pdm_db + pdm_shard_0/1/2/3
- 真实UUID + GB11643身份证校验位
- 中国31省人口权重分布
用法: python scripts/gen-perf-data.py [--total 20000]
"""
import sys, os, uuid, random, hashlib, argparse, subprocess, json
from collections import Counter

# ─── GB 11643-1999 身份证校验位 ─────────────────────────
ID_WEIGHTS = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2]
ID_CHECK = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2']

def id_checksum(id17: str) -> str:
    total = sum(w * int(c) for w, c in zip(ID_WEIGHTS, id17))
    return ID_CHECK[total % 11]

def gen_id_card(area_code: str, birth: str, gender: str, seq: int) -> str:
    """生成符合GB11643的有效身份证号"""
    ymd = birth.replace('-', '')
    # 顺序码: 男奇数 女偶数
    if gender == '男':
        s = (seq * 2 + 1) % 998 + 1
        if s % 2 == 0: s += 1
    else:
        s = (seq * 2) % 998 + 2
        if s % 2 == 1: s += 1
    id17 = area_code + ymd + f"{s:03d}"
    return id17 + id_checksum(id17)

# ─── 中国31省市区人口分布 (2020七普, 权重=人口/百万) ────
# 数据来源: 国家统计局第七次全国人口普查公报
PROVINCES = [
    # (行政区划代码前2位, 省名, 省会/首府, 人口权重, 城市列表)
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

# ─── 全国人口统计数据分布 ──────────────────────────────

# 性别比 (各年龄段不同)
# 出生~17: 男51.7% 女48.3%,  18-34: 男51.2% 女48.8%
# 35-59: 男50.8% 女49.2%,      60+: 男48.0% 女52.0%
GENDER_BY_AGE = {
    (0, 17):   (0.517, 0.483),
    (18, 34):  (0.512, 0.488),
    (35, 59):  (0.508, 0.492),
    (60, 150): (0.480, 0.520),
}

# 年龄分布 (2023年抽样)
AGE_GROUPS = [
    (0, 14, 0.170),    # 17.0%
    (15, 34, 0.290),   # 29.0%
    (35, 54, 0.320),   # 32.0%
    (55, 74, 0.190),   # 19.0%
    (75, 100, 0.030),  # 3.0%
]

# 文化程度 (6岁及以上人口, 2020七普)
EDUCATION = [
    ("小学及以下", 0.250),
    ("初中",       0.345),
    ("高中",       0.126),
    ("中专",       0.048),
    ("大专",       0.094),
    ("本科",       0.068),
    ("硕士研究生", 0.006),
    ("博士研究生", 0.001),
]

# 婚姻状况 (15岁及以上)
MARITAL_BY_AGE = {
    (0, 19):   [("未婚", 0.99), ("已婚", 0.01)],
    (20, 29):  [("未婚", 0.55), ("已婚", 0.42), ("离异", 0.03)],
    (30, 39):  [("未婚", 0.10), ("已婚", 0.82), ("离异", 0.06), ("丧偶", 0.02)],
    (40, 59):  [("未婚", 0.03), ("已婚", 0.84), ("离异", 0.07), ("丧偶", 0.06)],
    (60, 150): [("未婚", 0.01), ("已婚", 0.70), ("离异", 0.04), ("丧偶", 0.25)],
}

# 户口类型
HOUSEHOLD_TYPES = [
    ("居民户口", 0.45),
    ("农业户口", 0.40),
    ("非农业户口", 0.15),
]

# 民族分布 (七普)
NATIONS = [
    ("汉族", 0.911),
    ("壮族", 0.0138),
    ("维吾尔族", 0.0084),
    ("回族", 0.0075),
    ("苗族", 0.0069),
    ("满族", 0.0068),
    ("彝族", 0.0062),
    ("土家族", 0.0054),
    ("藏族", 0.0048),
    ("蒙古族", 0.0041),
    ("侗族", 0.0023),
    ("布依族", 0.0020),
    ("瑶族", 0.0019),
    ("朝鲜族", 0.0012),
    ("白族", 0.0012),
]

# 职业分布 (按行业门类)
OCCUPATIONS = [
    ("农、林、牧、渔业生产人员", 0.24),
    ("生产制造及有关人员", 0.18),
    ("社会生产服务和生活服务人员", 0.16),
    ("专业技术人员", 0.10),
    ("办事人员和有关人员", 0.08),
    ("商业、服务业人员", 0.10),
    ("国家机关、企事业单位负责人", 0.03),
    ("学生", 0.06),
    ("退休人员", 0.04),
    ("其他从业人员", 0.01),
]

# 血型分布 (中国人群)
BLOOD_TYPES = [
    ("O", 0.34),
    ("A", 0.28),
    ("B", 0.29),
    ("AB", 0.07),
    ("未知", 0.02),
]

# 街道后缀
STREET_SUFFIXES = ['街道', '镇', '乡', '路', '大街', '新区']

# ─── 加权随机选择 ───────────────────────────────────────
def weighted_choice(items):
    """通用加权随机: items为[(val, weight), ...] 或 [(a,b,weight), ...]; 返回第一项或前两项"""
    total = sum(item[-1] for item in items)
    r = random.random() * total
    cumulative = 0
    for item in items:
        cumulative += item[-1]
        if r <= cumulative:
            if len(item) == 2:
                return item[0]
            return item[:-1]  # return tuple of all but the weight
    if len(items[-1]) == 2:
        return items[-1][0]
    return items[-1][:-1]

def age_to_group(age):
    for lo, hi, _ in AGE_GROUPS:
        if lo <= age <= hi:
            return (lo, hi)
    return (75, 100)

# ─── 姓名生成 ───────────────────────────────────────────
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
    ("史",2),("陶",2),("黎",2),("贺",2),("顾",2),("毛",2),("郝",2),("龚",2),
    ("邵",2),("万",2),("钱",2),("严",2),("覃",2),("武",2),("戴",2),("莫",2),
    ("孔",2),("向",2),("汤",2),("温",1),("康",1),("施",1),("文",1),("牛",1),
    ("樊",1),("葛",1),("邢",1),("安",1),("齐",1),("易",1),("乔",1),("伍",1),
    ("庞",1),("颜",1),("倪",1),("庄",1),("聂",1),("章",1),("鲁",1),("岳",1),
    ("翟",1),("殷",1),("詹",1),("申",1),("欧",1),("耿",1),("关",1),("兰",1),
    ("焦",1),("俞",1),("柳",1),("荣",1),("甘",1),("祝",1),("包",1),("宁",1),
    ("尚",1),("符",1),("舒",1),("阮",1),("柯",1),("纪",1),("梅",1),("童",1),
    ("凌",1),("毕",1),("单",1),("季",1),("裴",1),("霍",1),("涂",1),("成",1),
]

MALE_GIVEN = [
    "伟","强","磊","军","勇","杰","涛","明","超","平","辉","鹏","华","飞",
    "斌","浩","宇","轩","博","文","刚","毅","峰","俊","亮","龙","凯","瑞",
    "霖","鑫","志","国","建","忠","庆","新","海","林","东","成","思远","子涵",
    "一鸣","天宇","浩然","子轩","俊杰","志强","建国","建华","志明","文博"
]

FEMALE_GIVEN = [
    "芳","敏","静","丽","婷","雪","琳","萍","红","霞","娟","慧","颖","娜",
    "艳","琴","玲","秀","兰","洁","梅","燕","丹","莲","薇","倩","蓉","莉",
    "怡","月","蕾","云","露","瑶","雯","思","雨","佳","欣","诗涵","梓涵",
    "雨桐","一诺","欣怡","梓萱","梦瑶","语嫣","晓雪","思雨","若曦","紫萱"
]

def gen_name(gender: str) -> tuple:
    surname = weighted_choice(SURNAMES)
    pool = MALE_GIVEN if gender == '男' else FEMALE_GIVEN
    given = random.choice(pool)
    if random.random() < 0.45:  # 45%概率双字名
        g2 = random.choice(pool)
        if g2 != given:
            given += g2
    former = None
    if random.random() < 0.03:  # 3%有曾用名
        former = surname + random.choice(pool)
    return surname + given, former

# ─── 地址码 (区县级, 按省人口权重分布) ──────────────────
def build_area_codes():
    """根据省份权重构建加权地址码列表"""
    codes = []
    for prefix, prov_name, capital, weight, cities in PROVINCES:
        # 每个城市生成2-5个区县代码
        for ci, city in enumerate(cities):
            n = min(3 + ci % 3, len(cities))  # 每个城市2-4个区
            for di in range(n):
                district_code = f"{prefix}{10 + ci:02d}{1 + di:02d}"
                codes.append((district_code, f"{prov_name}{city}{['区','县','市'][di%3]}", weight / n))
    return codes

AREA_CODES = build_area_codes()

# ─── 手机号生成 ─────────────────────────────────────────
MOBILE_PREFIXES = [
    '130','131','132','133','134','135','136','137','138','139',
    '150','151','152','153','155','156','157','158','159',
    '166','170','171','172','173','174','175','176','177','178',
    '180','181','182','183','184','185','186','187','188','189',
    '190','191','192','193','194','195','196','197','198','199',
]

# ─── 生成SQL ────────────────────────────────────────────
def gen_resident_sql(i, total):
    """生成单条居民SQL"""
    # 省份/地址码 (按人口权重)
    area_code, area_name = weighted_choice(AREA_CODES)

    # 年龄
    age_range = weighted_choice(AGE_GROUPS)
    age = random.randint(age_range[0], age_range[1])
    birth_year = 2026 - age
    birth_month = random.randint(1, 12)
    birth_day = random.randint(1, 28)
    birth = f"{birth_year}-{birth_month:02d}-{birth_day:02d}"

    # 性别 (按年龄分布)
    for (lo, hi), (m, f) in GENDER_BY_AGE.items():
        if lo <= age <= hi:
            gender = '男' if random.random() < m else '女'
            break
    else:
        gender = '男' if random.random() < 0.51 else '女'

    # 姓名
    name, former_name = gen_name(gender)

    # 身份证
    row_uuid = str(uuid.uuid4())
    id_card = gen_id_card(area_code, birth, gender, i)

    # 民族
    nation = weighted_choice(NATIONS)

    # 文化程度 (6岁以上)
    if age >= 6:
        edu = weighted_choice(EDUCATION)
    else:
        edu = "小学及以下"

    # 血型
    blood = weighted_choice(BLOOD_TYPES)

    # 婚姻
    marital = None
    for (lo, hi), opts in MARITAL_BY_AGE.items():
        if lo <= age <= hi:
            marital = weighted_choice(opts)
            break
    if not marital:
        marital = "未婚" if age < 22 else "已婚"

    # 职业 (按年龄调整)
    if age < 16:
        occupation = "学生"
    elif age >= 60:
        occupation = "退休人员" if random.random() < 0.7 else random.choice(["农民","个体户","其他"])
    else:
        occupation = weighted_choice(OCCUPATIONS)

    # 电话
    phone = random.choice(MOBILE_PREFIXES) + f"{random.randint(0,99999999):08d}"

    # 户籍地
    hukou_city = area_name.split('市')[0] + '市' if '市' in area_name else area_name
    street = random.choice(['街道','镇','路','大街'])
    residence = f"{hukou_city}{random.choice(['东','西','南','北','中','新','老'])}{random.randint(1,300)}{street}{random.randint(1,999)}号"
    household_address = residence + f"{random.randint(1,30)}室" if random.random() < 0.6 else residence

    # 户口类型
    hukou_type = weighted_choice(HOUSEHOLD_TYPES)

    # 户口状态
    hukou_status = weighted_choice([("正常", 0.92), ("迁出注销", 0.05), ("死亡注销", 0.02), ("恢复", 0.01)])

    # area_id (引用真实的area表, 1-464)
    area_id = random.randint(1, 464)

    # 分片键: HASH_MOD(uuid) % 4
    shard = abs(hash(row_uuid)) % 4

    former_str = 'NULL' if former_name is None else f"'{former_name}'"

    return {
        'uuid': row_uuid,
        'shard': shard,
        'sql': (
            f"INSERT INTO resident (uuid, name, former_name, gender, id_card_no, nation, "
            f"birth_date, education_level, blood_type, marital_status, occupation, phone, "
            f"photo, residence, area_id, household_type, household_status, "
            f"household_address, household_area_id) VALUES ("
            f"'{row_uuid}', "
            f"'{name}', {former_str}, "
            f"'{gender}', '{id_card}', '{nation}', "
            f"'{birth}', '{edu}', '{blood}', '{marital}', "
            f"'{occupation}', '{phone}', "
            f"NULL, '{residence}', {area_id}, "
            f"'{hukou_type}', '{hukou_status}', "
            f"'{household_address}', {random.randint(1, 464)}"
            f") ON CONFLICT (id_card_no) DO NOTHING;"
        )
    }

# ─── Main ────────────────────────────────────────────────
def main():
    parser = argparse.ArgumentParser(description="PDM 性能测试数据生成器")
    parser.add_argument("--total", type=int, default=10000, help="居民总数 (default: 10000)")
    parser.add_argument("--batch", type=int, default=500, help="每批插入数量 (default: 500)")
    parser.add_argument("--dry-run", action="store_true", help="仅生成SQL不执行")
    args = parser.parse_args()

    total = args.total
    batch_size = args.batch
    residents = []
    shard_counts = Counter()
    area_dist = Counter()
    gender_counts = Counter()
    education_counts = Counter()

    print(f"生成 {total} 条逼真人口数据...")
    for i in range(1, total + 1):
        r = gen_resident_sql(i, total)
        residents.append(r)
        shard_counts[r['shard']] += 1
        if i % 2000 == 0:
            print(f"  已生成 {i}/{total}...")

    # 统计
    print(f"\n分片分布 (HASH_MOD % 4):")
    for s in range(4):
        print(f"  pdm_shard_{s}: {shard_counts[s]} ({shard_counts[s]/total*100:.1f}%)")

    if args.dry_run:
        # 输出SQL到文件
        for db_name in ['pdm_db'] + [f'pdm_shard_{i}' for i in range(4)]:
            fname = f"performance/test-data/insert-{db_name}.sql"
            with open(fname, 'w', encoding='utf-8') as f:
                f.write("BEGIN;\n")
                if db_name == 'pdm_db':
                    # 主库: 所有数据
                    for r in residents:
                        f.write(r['sql'] + '\n')
                else:
                    shard_id = int(db_name.split('_')[-1])
                    for r in residents:
                        if r['shard'] == shard_id:
                            f.write(r['sql'] + '\n')
                f.write("COMMIT;\n")
            print(f"  SQL已写入 {fname}")
        return

    # 执行插入
    db_map = {
        'pdm_db': (-1, [r for r in residents]),  # 主库: 全部数据
        'pdm_shard_0': (0, []),
        'pdm_shard_1': (1, []),
        'pdm_shard_2': (2, []),
        'pdm_shard_3': (3, []),
    }
    for r in residents:
        for name, (sid, lst) in db_map.items():
            if name == 'pdm_db':
                continue  # 已在上面处理
            if sid == r['shard']:
                lst.append(r)

    for db_name in ['pdm_db'] + [f'pdm_shard_{i}' for i in range(4)]:
        if db_name == 'pdm_db':
            db_residents = residents
        else:
            db_residents = db_map[db_name][1]

        print(f"\n写入 {db_name}: {len(db_residents)} 条...")
        for batch_start in range(0, len(db_residents), batch_size):
            batch = db_residents[batch_start:batch_start + batch_size]
            sql = "BEGIN;\n" + "\n".join(r['sql'] for r in batch) + "\nCOMMIT;\n"
            try:
                result = subprocess.run(
                    ['wsl', 'docker', 'exec', '-i', 'pdm-postgresql',
                     'psql', '-U', 'pdm', '-d', db_name, '-v', 'ON_ERROR_STOP=0'],
                    input=sql, capture_output=True, text=True, timeout=120
                )
                if result.returncode != 0 and 'duplicate key' not in result.stderr:
                    print(f"  警告: {result.stderr[:200]}")
            except subprocess.TimeoutExpired:
                print(f"  批次 {batch_start} 超时")
            print(f"  已写入 {min(batch_start + batch_size, len(db_residents))}/{len(db_residents)}", end='\r')
        print()

    # 验证
    print("\n=== 数据分布验证 ===")
    for db_name in ['pdm_db'] + [f'pdm_shard_{i}' for i in range(4)]:
        result = subprocess.run(
            ['wsl', 'docker', 'exec', 'pdm-postgresql',
             'psql', '-U', 'pdm', '-d', db_name, '-t', '-c',
             "SELECT COUNT(*) FROM resident;"],
            capture_output=True, text=True, timeout=15
        )
        count = result.stdout.strip()
        print(f"  {db_name}: {count} residents")

    print("\n完成!")

if __name__ == '__main__':
    main()
