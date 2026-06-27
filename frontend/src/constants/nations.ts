/** GB 3304-1991 民族代码: 代码↔名称 双向映射 */
export const NATIONS = [
  { code: '01', name: '汉族' }, { code: '02', name: '蒙古族' }, { code: '03', name: '回族' },
  { code: '04', name: '藏族' }, { code: '05', name: '维吾尔族' }, { code: '06', name: '苗族' },
  { code: '07', name: '彝族' }, { code: '08', name: '壮族' }, { code: '09', name: '布依族' },
  { code: '10', name: '朝鲜族' }, { code: '11', name: '满族' }, { code: '12', name: '侗族' },
  { code: '13', name: '瑶族' }, { code: '14', name: '白族' }, { code: '15', name: '土家族' },
  { code: '16', name: '哈尼族' }, { code: '17', name: '哈萨克族' }, { code: '18', name: '傣族' },
  { code: '19', name: '黎族' }, { code: '20', name: '傈僳族' }, { code: '21', name: '佤族' },
  { code: '22', name: '畲族' }, { code: '23', name: '高山族' }, { code: '24', name: '拉祜族' },
  { code: '25', name: '水族' }, { code: '26', name: '东乡族' }, { code: '27', name: '纳西族' },
  { code: '28', name: '景颇族' }, { code: '29', name: '柯尔克孜族' }, { code: '30', name: '土族' },
  { code: '31', name: '达斡尔族' }, { code: '32', name: '仫佬族' }, { code: '33', name: '羌族' },
  { code: '34', name: '布朗族' }, { code: '35', name: '撒拉族' }, { code: '36', name: '毛南族' },
  { code: '37', name: '仡佬族' }, { code: '38', name: '锡伯族' }, { code: '39', name: '阿昌族' },
  { code: '40', name: '普米族' }, { code: '41', name: '塔吉克族' }, { code: '42', name: '怒族' },
  { code: '43', name: '乌孜别克族' }, { code: '44', name: '俄罗斯族' }, { code: '45', name: '鄂温克族' },
  { code: '46', name: '德昂族' }, { code: '47', name: '保安族' }, { code: '48', name: '裕固族' },
  { code: '49', name: '京族' }, { code: '50', name: '塔塔尔族' }, { code: '51', name: '独龙族' },
  { code: '52', name: '鄂伦春族' }, { code: '53', name: '赫哲族' }, { code: '54', name: '门巴族' },
  { code: '55', name: '珞巴族' }, { code: '56', name: '基诺族' },
  { code: '97', name: '其他' }, { code: '98', name: '外籍' },
]

export const NATION_CODE_MAP: Record<string, string> = Object.fromEntries(
  NATIONS.map(n => [n.code, n.name])
)
export const NATION_NAME_MAP: Record<string, string> = Object.fromEntries(
  NATIONS.map(n => [n.name, n.code])
)
