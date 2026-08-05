/** ICD 疾病编码表（来源：CODE-大类.xlsx），共 1956 条 */
export interface CodeOption { value: string, label: string }

export const DISEASE_CODE_OPTIONS: CodeOption[] = [
  {
    value: "A00",
    label: "霍乱"
  },
  {
    value: "A01",
    label: "伤寒和副伤寒"
  },
  {
    value: "A02",
    label: "其他沙门氏菌感染"
  },
  {
    value: "A03",
    label: "志贺菌病"
  },
  {
    value: "A04",
    label: "其他细菌性肠道感染"
  },
  {
    value: "A05",
    label: "其他细菌性食物中毒"
  },
  {
    value: "A06",
    label: "阿米巴病"
  },
  {
    value: "A07",
    label: "其他原生动物性肠道疾病"
  },
  {
    value: "A08",
    label: "病毒性和其他特指的肠道感染"
  },
  {
    value: "A09",
    label: "推测为传染性病因的腹泻和胃肠炎"
  },
  {
    value: "A15",
    label: "呼吸道结核病，经细菌学和组织学证实"
  },
  {
    value: "A16",
    label: "呼吸道结核病，未经细菌学或组织学所证实"
  },
  {
    value: "A17",
    label: "神经系统的结核病"
  },
  {
    value: "A18",
    label: "其他器官的结核病"
  },
  {
    value: "A19",
    label: "粟粒性结核病"
  },
  {
    value: "A20",
    label: "鼠疫"
  },
  {
    value: "A21",
    label: "土拉菌病"
  },
  {
    value: "A22",
    label: "炭疽"
  },
  {
    value: "A23",
    label: "布鲁氏菌病"
  },
  {
    value: "A24",
    label: "鼻疽和类鼻疽"
  },
  {
    value: "A25",
    label: "鼠咬热"
  },
  {
    value: "A26",
    label: "类丹毒"
  },
  {
    value: "A27",
    label: "钩端螺旋体病"
  },
  {
    value: "A28",
    label: "其他动物传染的细菌性疾病，不可归类在他处者"
  },
  {
    value: "A30",
    label: "麻风［汉森病］"
  },
  {
    value: "A31",
    label: "由于其他分支杆菌引起的感染"
  },
  {
    value: "A32",
    label: "利斯特菌病"
  },
  {
    value: "A33",
    label: "新生儿破伤风"
  },
  {
    value: "A34",
    label: "产科破伤风"
  },
  {
    value: "A35",
    label: "其他破伤风"
  },
  {
    value: "A36",
    label: "白喉"
  },
  {
    value: "A37",
    label: "百日咳"
  },
  {
    value: "A38",
    label: "猩红热"
  },
  {
    value: "A39",
    label: "脑膜炎球菌感染"
  },
  {
    value: "A40",
    label: "链球菌性败血症"
  },
  {
    value: "A41",
    label: "其他败血病"
  },
  {
    value: "A42",
    label: "放线菌病"
  },
  {
    value: "A43",
    label: "诺卡［放线］菌病"
  },
  {
    value: "A44",
    label: "巴尔通体病"
  },
  {
    value: "A46",
    label: "丹毒"
  },
  {
    value: "A48",
    label: "其他细菌性疾病，不可归类在他处者"
  },
  {
    value: "A49",
    label: "未特指部位的细菌性感染"
  },
  {
    value: "A50",
    label: "先天性梅毒"
  },
  {
    value: "A51",
    label: "早期梅毒"
  },
  {
    value: "A52",
    label: "晚期梅毒"
  },
  {
    value: "A53",
    label: "其他和未特指的梅毒"
  },
  {
    value: "A54",
    label: "淋球菌感染"
  },
  {
    value: "A55",
    label: "衣原体（性病性）淋巴肉芽肿"
  },
  {
    value: "A56",
    label: "其他性传播的衣原体疾病"
  },
  {
    value: "A57",
    label: "软下疳"
  },
  {
    value: "A58",
    label: "腹股沟肉芽肿"
  },
  {
    value: "A59",
    label: "毛滴虫病"
  },
  {
    value: "A60",
    label: "肛门生殖器的疱疹病毒［单纯性疱疹］感染"
  },
  {
    value: "A63",
    label: "其他主要为性传播的疾病，不可归类在他处者"
  },
  {
    value: "A64",
    label: "未特指的性传播疾病"
  },
  {
    value: "A65",
    label: "非性病性梅毒"
  },
  {
    value: "A66",
    label: "雅司病"
  },
  {
    value: "A67",
    label: "品他病"
  },
  {
    value: "A68",
    label: "回归热"
  },
  {
    value: "A69",
    label: "其他螺旋体感染"
  },
  {
    value: "A70",
    label: "鹦鹉热衣原体感染"
  },
  {
    value: "A71",
    label: "沙眼"
  },
  {
    value: "A74",
    label: "由衣原体引起的其他疾病"
  },
  {
    value: "A75",
    label: "斑疹伤寒"
  },
  {
    value: "A77",
    label: "斑疹热［蜱媒介的立克次氏体病］"
  },
  {
    value: "A78",
    label: "寇热"
  },
  {
    value: "A79",
    label: "其他立克次氏体病"
  },
  {
    value: "A80",
    label: "急性脊髓灰质炎"
  },
  {
    value: "A81",
    label: "中枢神经系统的慢病毒感染"
  },
  {
    value: "A82",
    label: "狂犬病"
  },
  {
    value: "A83",
    label: "蚊媒介的病毒性脑炎"
  },
  {
    value: "A84",
    label: "蜱媒介的病毒性脑炎"
  },
  {
    value: "A85",
    label: "其他病毒性脑炎，不可归类在他处者"
  },
  {
    value: "A86",
    label: "未特指的病毒性脑炎"
  },
  {
    value: "A87",
    label: "病毒性脑膜炎"
  },
  {
    value: "A88",
    label: "中枢神经系统的其他病毒性感染，不可归类在他处者"
  },
  {
    value: "A89",
    label: "中枢神经系统未特指的病毒性感染"
  },
  {
    value: "A90",
    label: "登革热［古典登革热］"
  },
  {
    value: "A91",
    label: "登革出血热"
  },
  {
    value: "A92",
    label: "其他蚊媒介的病毒性发热"
  },
  {
    value: "A93",
    label: "其他节肢动物媒介的病毒性发热，不可归类在他处者"
  },
  {
    value: "A94",
    label: "未特指的节肢动物媒介的病毒性发热"
  },
  {
    value: "A95",
    label: "黄热病"
  },
  {
    value: "A96",
    label: "沙粒病毒性出血热"
  },
  {
    value: "A98",
    label: "其他病毒性出血热，不可归类在他处者"
  },
  {
    value: "A99",
    label: "未特指的病毒性出血热"
  },
  {
    value: "B00",
    label: "疱疹病毒［单纯性疱疹］感染"
  },
  {
    value: "B01",
    label: "水痘"
  },
  {
    value: "B02",
    label: "带状疱疹"
  },
  {
    value: "B03",
    label: "天花"
  },
  {
    value: "B04",
    label: "猴痘"
  },
  {
    value: "B05",
    label: "麻疹"
  },
  {
    value: "B06",
    label: "风疹"
  },
  {
    value: "B07",
    label: "病毒性疣"
  },
  {
    value: "B08",
    label: "其他以皮肤和粘膜损害为特征的病毒性感染，不可归类在他处者"
  },
  {
    value: "B09",
    label: "未特指的以皮肤和粘膜损害为特征的病毒性感染"
  },
  {
    value: "B15",
    label: "急性甲型肝炎"
  },
  {
    value: "B16",
    label: "急性乙型肝炎"
  },
  {
    value: "B17",
    label: "其他急性病毒性肝炎"
  },
  {
    value: "B18",
    label: "慢性病毒性肝炎"
  },
  {
    value: "B19",
    label: "未特指的病毒性肝炎"
  },
  {
    value: "B20",
    label: "人类免疫缺陷病毒［HIV］病造成的传染病和寄生虫病"
  },
  {
    value: "B21",
    label: "人类免疫缺陷病毒［HIV］病造成的恶性肿瘤"
  },
  {
    value: "B22",
    label: "人类免疫缺陷病毒［HIV］病造成的其他特指的疾病"
  },
  {
    value: "B23",
    label: "人类免疫缺陷病毒［HIV］病造成的其他情况"
  },
  {
    value: "B24",
    label: "未特指的人类免疫缺陷病毒［HIV］病/艾滋病"
  },
  {
    value: "B25",
    label: "巨细胞病毒病"
  },
  {
    value: "B26",
    label: "流行性腮腺炎"
  },
  {
    value: "B27",
    label: "传染性单核细胞增多症"
  },
  {
    value: "B30",
    label: "病毒性结膜炎"
  },
  {
    value: "B33",
    label: "其他病毒性疾病，不可归类在他处者"
  },
  {
    value: "B34",
    label: "未特指部位的病毒性感染"
  },
  {
    value: "B35",
    label: "皮真菌病"
  },
  {
    value: "B36",
    label: "其他表面霉菌病"
  },
  {
    value: "B37",
    label: "念珠菌病"
  },
  {
    value: "B38",
    label: "球孢子菌病"
  },
  {
    value: "B39",
    label: "组织胞浆菌病"
  },
  {
    value: "B40",
    label: "芽生菌病"
  },
  {
    value: "B41",
    label: "类球孢子菌病"
  },
  {
    value: "B42",
    label: "孢子丝菌病"
  },
  {
    value: "B43",
    label: "着色真菌病与棕色真菌病性脓肿"
  },
  {
    value: "B44",
    label: "曲霉病"
  },
  {
    value: "B45",
    label: "隐球菌病"
  },
  {
    value: "B46",
    label: "接合菌病"
  },
  {
    value: "B47",
    label: "足分支菌病"
  },
  {
    value: "B48",
    label: "其他霉菌病，不可归类在他处者"
  },
  {
    value: "B49",
    label: "未特指的霉菌病"
  },
  {
    value: "B50",
    label: "恶性疟原虫疟疾"
  },
  {
    value: "B51",
    label: "间日疟原虫疟疾"
  },
  {
    value: "B52",
    label: "三日疟原虫疟疾"
  },
  {
    value: "B53",
    label: "其他经寄生虫学证实的疟疾"
  },
  {
    value: "B54",
    label: "未特指的疟疾"
  },
  {
    value: "B55",
    label: "利什曼病"
  },
  {
    value: "B56",
    label: "非洲锥虫病"
  },
  {
    value: "B57",
    label: "查加斯病"
  },
  {
    value: "B58",
    label: "弓形体病"
  },
  {
    value: "B59",
    label: "肺囊虫病"
  },
  {
    value: "B60",
    label: "其他原生动物性疾病，不可归类在他处者"
  },
  {
    value: "B64",
    label: "未特指的原生动物性疾病"
  },
  {
    value: "B65",
    label: "血吸虫病［裂体吸虫病］"
  },
  {
    value: "B66",
    label: "其他吸虫感染"
  },
  {
    value: "B67",
    label: "棘球蚴病"
  },
  {
    value: "B68",
    label: "绦虫病"
  },
  {
    value: "B69",
    label: "囊虫病［囊尾蚴病］"
  },
  {
    value: "B70",
    label: "裂头绦虫病和裂头蚴病"
  },
  {
    value: "B71",
    label: "其他绦虫感染"
  },
  {
    value: "B72",
    label: "麦地那丝虫病"
  },
  {
    value: "B73",
    label: "盘尾丝虫病"
  },
  {
    value: "B74",
    label: "丝虫病"
  },
  {
    value: "B75",
    label: "旋毛虫病"
  },
  {
    value: "B76",
    label: "钩虫病"
  },
  {
    value: "B77",
    label: "蛔虫病"
  },
  {
    value: "B78",
    label: "类圆线虫病"
  },
  {
    value: "B79",
    label: "鞭虫病"
  },
  {
    value: "B80",
    label: "蛲虫病"
  },
  {
    value: "B81",
    label: "其他肠道蠕虫病，不可归类在他处者"
  },
  {
    value: "B82",
    label: "未特指的肠道寄生虫病"
  },
  {
    value: "B83",
    label: "其他蠕虫病"
  },
  {
    value: "B85",
    label: "虱病和阴虱病"
  },
  {
    value: "B86",
    label: "疥疮"
  },
  {
    value: "B87",
    label: "蝇蛆病"
  },
  {
    value: "B88",
    label: "其他病虫侵染"
  },
  {
    value: "B89",
    label: "未特指的寄生虫病"
  },
  {
    value: "B90",
    label: "结核病后遗症"
  },
  {
    value: "B91",
    label: "脊髓灰质炎后遗症"
  },
  {
    value: "B92",
    label: "麻风后遗症"
  },
  {
    value: "B94",
    label: "其他和未特指的传染病和寄生虫病的后遗症"
  },
  {
    value: "B95",
    label: "链球菌和葡萄球菌作为分类于其他章疾病的原因"
  },
  {
    value: "B96",
    label: "其他细菌性病原体作为分类于其他章疾病的原因"
  },
  {
    value: "B97",
    label: "病毒性病原体作为分类于其他章疾病的原因"
  },
  {
    value: "B99",
    label: "其他和未特指的传染病"
  },
  {
    value: "C00",
    label: "唇恶性肿瘤"
  },
  {
    value: "C01",
    label: "舌根恶性肿瘤"
  },
  {
    value: "C02",
    label: "舌的其他和未特指部位的恶性肿瘤"
  },
  {
    value: "C03",
    label: "牙龈恶性肿瘤"
  },
  {
    value: "C04",
    label: "口底恶性肿瘤"
  },
  {
    value: "C05",
    label: "腭恶性肿瘤"
  },
  {
    value: "C06",
    label: "口的其他和未特指部位的恶性肿瘤"
  },
  {
    value: "C07",
    label: "腮腺恶性肿瘤"
  },
  {
    value: "C08",
    label: "其他和未特指的大涎腺恶性肿瘤"
  },
  {
    value: "C09",
    label: "扁桃体恶性肿瘤"
  },
  {
    value: "C10",
    label: "口咽恶性肿瘤"
  },
  {
    value: "C11",
    label: "鼻咽恶性肿瘤"
  },
  {
    value: "C12",
    label: "梨状窦恶性肿瘤"
  },
  {
    value: "C13",
    label: "咽下部恶性肿瘤"
  },
  {
    value: "C14",
    label: "唇，口腔和咽的其他和部位不明的恶性肿瘤"
  },
  {
    value: "C15",
    label: "食管恶性肿瘤"
  },
  {
    value: "C16",
    label: "胃恶性肿瘤"
  },
  {
    value: "C17",
    label: "小肠恶性肿瘤"
  },
  {
    value: "C18",
    label: "结肠恶性肿瘤"
  },
  {
    value: "C19",
    label: "直肠乙状结肠连接处恶性肿瘤"
  },
  {
    value: "C20",
    label: "直肠恶性肿瘤"
  },
  {
    value: "C21",
    label: "肛门和肛管的恶性肿瘤"
  },
  {
    value: "C22",
    label: "肝和肝内胆管恶性肿瘤"
  },
  {
    value: "C23",
    label: "胆囊恶性肿瘤"
  },
  {
    value: "C24",
    label: "胆道其他和未特指部位的恶性肿瘤"
  },
  {
    value: "C25",
    label: "胰恶性肿瘤"
  },
  {
    value: "C26",
    label: "其他和不明确的消化器官恶性肿瘤"
  },
  {
    value: "C30",
    label: "鼻腔和中耳恶性肿瘤"
  },
  {
    value: "C31",
    label: "副鼻窦恶性肿瘤"
  },
  {
    value: "C32",
    label: "喉恶性肿瘤"
  },
  {
    value: "C33",
    label: "气管恶性肿瘤"
  },
  {
    value: "C34",
    label: "支气管和肺恶性肿瘤"
  },
  {
    value: "C37",
    label: "胸腺恶性肿瘤"
  },
  {
    value: "C38",
    label: "心脏、纵隔和胸膜恶性肿瘤"
  },
  {
    value: "C39",
    label: "呼吸和胸腔内器官的其他和部位不明确的恶性肿瘤"
  },
  {
    value: "C40",
    label: "四肢的骨和关节软骨恶性肿瘤"
  },
  {
    value: "C41",
    label: "其他和未特指部位的骨和关节软骨恶性肿瘤"
  },
  {
    value: "C43",
    label: "皮肤恶性黑色素瘤"
  },
  {
    value: "C44",
    label: "皮肤其他恶性肿瘤"
  },
  {
    value: "C45",
    label: "间皮瘤"
  },
  {
    value: "C46",
    label: "卡波西肉瘤"
  },
  {
    value: "C47",
    label: "周围神经和自主神经系统恶性肿瘤"
  },
  {
    value: "C48",
    label: "腹膜后和腹膜恶性肿瘤"
  },
  {
    value: "C49",
    label: "其他结缔组织和软组织恶性肿瘤"
  },
  {
    value: "C50",
    label: "乳房恶性肿瘤"
  },
  {
    value: "C51",
    label: "外阴恶性肿瘤"
  },
  {
    value: "C52",
    label: "阴道恶性肿瘤"
  },
  {
    value: "C53",
    label: "宫颈恶性肿瘤"
  },
  {
    value: "C54",
    label: "子宫体恶性肿瘤"
  },
  {
    value: "C55",
    label: "未特指部位的子宫恶性肿瘤"
  },
  {
    value: "C56",
    label: "卵巢恶性肿瘤"
  },
  {
    value: "C57",
    label: "其他和未特指的女性生殖器官恶性肿瘤"
  },
  {
    value: "C58",
    label: "胎盘恶性肿瘤"
  },
  {
    value: "C60",
    label: "阴茎恶性肿瘤"
  },
  {
    value: "C61",
    label: "前列腺恶性肿瘤"
  },
  {
    value: "C62",
    label: "睾丸恶性肿瘤"
  },
  {
    value: "C63",
    label: "其他和未特指的男性生殖器官恶性肿瘤"
  },
  {
    value: "C64",
    label: "肾恶性肿瘤，除外肾盂"
  },
  {
    value: "C65",
    label: "肾盂恶性肿瘤"
  },
  {
    value: "C66",
    label: "输尿管恶性肿瘤"
  },
  {
    value: "C67",
    label: "膀胱恶性肿瘤"
  },
  {
    value: "C68",
    label: "其他和未特指的泌尿器官恶性肿瘤"
  },
  {
    value: "C69",
    label: "眼和附器恶性肿瘤"
  },
  {
    value: "C70",
    label: "脑脊膜恶性肿瘤"
  },
  {
    value: "C71",
    label: "脑恶性肿瘤"
  },
  {
    value: "C72",
    label: "脊髓，颅神经和中枢神经系统其他部位的恶性肿瘤"
  },
  {
    value: "C73",
    label: "甲状腺恶性肿瘤"
  },
  {
    value: "C74",
    label: "肾上腺恶性肿瘤"
  },
  {
    value: "C75",
    label: "其他内分泌腺和有关结构的恶性肿瘤"
  },
  {
    value: "C76",
    label: "其他和不明确部位的恶性肿瘤"
  },
  {
    value: "C77",
    label: "淋巴结继发性和未特指的恶性肿瘤"
  },
  {
    value: "C78",
    label: "呼吸和消化器官的继发性恶性肿瘤"
  },
  {
    value: "C79",
    label: "其他部位的继发性恶性肿瘤"
  },
  {
    value: "C80",
    label: "未特指部位的恶性肿瘤"
  },
  {
    value: "C81",
    label: "霍奇金［何杰金］病"
  },
  {
    value: "C82",
    label: "滤泡性［结节性］非霍奇金淋巴瘤"
  },
  {
    value: "C83",
    label: "弥漫性非霍奇金淋巴瘤"
  },
  {
    value: "C84",
    label: "周围和皮的Ｔ细胞淋巴瘤"
  },
  {
    value: "C85",
    label: "非霍奇金淋巴瘤的其他和未特指类型"
  },
  {
    value: "C88",
    label: "恶性免疫增生性疾病"
  },
  {
    value: "C90",
    label: "多发性骨髓瘤和恶性浆细胞肿瘤"
  },
  {
    value: "C91",
    label: "淋巴样白血病"
  },
  {
    value: "C92",
    label: "髓样白血病"
  },
  {
    value: "C93",
    label: "单核细胞白血病"
  },
  {
    value: "C94",
    label: "特指细胞类型的其他白血病"
  },
  {
    value: "C95",
    label: "未特指细胞类型的白血病"
  },
  {
    value: "C96",
    label: "其他和未特指的淋巴、造血和有关组织的恶性肿瘤"
  },
  {
    value: "C97",
    label: "独立的多个部位的（原发性）恶性肿瘤"
  },
  {
    value: "D00",
    label: "口腔、食管和胃原位癌"
  },
  {
    value: "D01",
    label: "其他和未特指的消化器官原位癌"
  },
  {
    value: "D02",
    label: "中耳和呼吸系统原位癌"
  },
  {
    value: "D03",
    label: "原位黑（色素）瘤"
  },
  {
    value: "D04",
    label: "皮肤原位癌"
  },
  {
    value: "D05",
    label: "乳房原位癌"
  },
  {
    value: "D06",
    label: "宫颈原位癌"
  },
  {
    value: "D07",
    label: "其他和未特指的生殖器官原位癌"
  },
  {
    value: "D09",
    label: "其他和未特指部位的原位癌"
  },
  {
    value: "D10",
    label: "口和咽良性肿瘤"
  },
  {
    value: "D11",
    label: "大涎腺良性肿瘤"
  },
  {
    value: "D12",
    label: "结肠、直肠、肛门和肛管良性肿瘤"
  },
  {
    value: "D13",
    label: "消化系统其他和不明确部位的良性肿瘤"
  },
  {
    value: "D14",
    label: "中耳和呼吸系统良性肿瘤"
  },
  {
    value: "D15",
    label: "其他和未特指的胸腔内器官良性肿瘤"
  },
  {
    value: "D16",
    label: "骨和关节软骨良性肿瘤"
  },
  {
    value: "D17",
    label: "良性脂肪瘤样肿瘤"
  },
  {
    value: "D18",
    label: "血管瘤和淋巴管瘤，任何部位"
  },
  {
    value: "D19",
    label: "间皮组织良性肿瘤"
  },
  {
    value: "D20",
    label: "腹膜后和腹膜软组织良性肿瘤"
  },
  {
    value: "D21",
    label: "结缔组织和其他软组织的其他良性肿瘤"
  },
  {
    value: "D22",
    label: "黑素细胞痣"
  },
  {
    value: "D23",
    label: "皮肤其他良性肿瘤"
  },
  {
    value: "D24",
    label: "乳房良性肿瘤"
  },
  {
    value: "D25",
    label: "子宫平滑肌瘤"
  },
  {
    value: "D26",
    label: "子宫其他良性肿瘤"
  },
  {
    value: "D27",
    label: "卵巢良性肿瘤"
  },
  {
    value: "D28",
    label: "其他和未特指的女性生殖器官良性肿瘤"
  },
  {
    value: "D29",
    label: "男性生殖器官良性肿瘤"
  },
  {
    value: "D30",
    label: "泌尿器官良性肿瘤"
  },
  {
    value: "D31",
    label: "眼和附器良性肿瘤"
  },
  {
    value: "D32",
    label: "脑脊膜良性肿瘤"
  },
  {
    value: "D33",
    label: "脑和中枢神经系统其他部位的良性肿瘤"
  },
  {
    value: "D34",
    label: "甲状腺良性肿瘤"
  },
  {
    value: "D35",
    label: "其他和未特指的内分泌腺良性肿瘤"
  },
  {
    value: "D36",
    label: "其他和未特指部位的良性肿瘤"
  },
  {
    value: "D37",
    label: "口腔和消化器官动态未定或动态未知的肿瘤"
  },
  {
    value: "D38",
    label: "中耳、呼吸和胸腔内器官动态未定或动态未知的肿瘤"
  },
  {
    value: "D39",
    label: "女性生殖器官动态未定或动态未知的肿瘤"
  },
  {
    value: "D40",
    label: "男性生殖器官动态未定或动态未知的肿瘤"
  },
  {
    value: "D41",
    label: "泌尿器官动态未定或动态未知的肿瘤"
  },
  {
    value: "D42",
    label: "脑脊膜动态未定或动态未知的肿瘤"
  },
  {
    value: "D43",
    label: "脑和中枢神经系统动态未定或动态未知的肿瘤"
  },
  {
    value: "D44",
    label: "内分泌腺动态未定或动态未知的肿瘤"
  },
  {
    value: "D45",
    label: "真性红细胞增多症"
  },
  {
    value: "D46",
    label: "骨髓增生异常综合征"
  },
  {
    value: "D47",
    label: "淋巴、造血和有关组织动态未定或动态未知的其他肿瘤"
  },
  {
    value: "D48",
    label: "其他和未特指部位动态未定或动态未知的肿瘤"
  },
  {
    value: "D50",
    label: "缺铁性贫血"
  },
  {
    value: "D51",
    label: "维生素Ｂ12缺乏性贫血"
  },
  {
    value: "D52",
    label: "叶酸盐缺乏性贫血"
  },
  {
    value: "D53",
    label: "其他营养性贫血"
  },
  {
    value: "D55",
    label: "由于酶障碍引起的贫血"
  },
  {
    value: "D56",
    label: "地中海贫血"
  },
  {
    value: "D57",
    label: "镰状细胞疾患"
  },
  {
    value: "D58",
    label: "其他遗传性溶血性贫血"
  },
  {
    value: "D59",
    label: "后天性溶血性贫血"
  },
  {
    value: "D60",
    label: "后天性纯红细胞再生障碍［幼红细胞减少症］"
  },
  {
    value: "D61",
    label: "其他再生障碍性贫血"
  },
  {
    value: "D62",
    label: "急性出血后贫血"
  },
  {
    value: "D63",
    label: "分类于他处的慢性疾病引起的贫血"
  },
  {
    value: "D64",
    label: "其他贫血"
  },
  {
    value: "D65",
    label: "播散性血管内凝血［去纤维蛋白综合征］"
  },
  {
    value: "D66",
    label: "遗传性因子Ⅷ缺乏"
  },
  {
    value: "D67",
    label: "遗传性因子Ⅸ缺乏"
  },
  {
    value: "D68",
    label: "其他凝血缺陷"
  },
  {
    value: "D69",
    label: "紫癜及其他出血情况"
  },
  {
    value: "D70",
    label: "粒细胞缺乏"
  },
  {
    value: "D71",
    label: "中性多形核白细胞的功能性疾患"
  },
  {
    value: "D72",
    label: "其他白细胞疾患"
  },
  {
    value: "D73",
    label: "脾疾病"
  },
  {
    value: "D74",
    label: "高铁血红蛋白血症"
  },
  {
    value: "D75",
    label: "血液和造血器官的其他疾病"
  },
  {
    value: "D76",
    label: "某些涉及淋巴网状内皮细胞组织和网状组织细胞系统的疾病"
  },
  {
    value: "D77",
    label: "分类于他处的疾病引起的血液和造血器官的其他疾患"
  },
  {
    value: "D80",
    label: "抗体缺陷为主的免疫缺陷"
  },
  {
    value: "D81",
    label: "联合免疫缺陷"
  },
  {
    value: "D82",
    label: "与其他严重缺陷有关的免疫缺陷"
  },
  {
    value: "D83",
    label: "普通易变型免疫缺陷"
  },
  {
    value: "D84",
    label: "其他免疫缺陷"
  },
  {
    value: "D86",
    label: "结节病"
  },
  {
    value: "D89",
    label: "其他涉及免疫机制的疾患，不可归类在他处者"
  },
  {
    value: "E00",
    label: "先天性碘缺乏综合征"
  },
  {
    value: "E01",
    label: "碘缺乏相关性甲状腺疾患和有关情况"
  },
  {
    value: "E02",
    label: "临床症状不明显[亚临床]的碘缺乏性甲状腺功能减退症"
  },
  {
    value: "E03",
    label: "其他甲状腺功能减退症"
  },
  {
    value: "E04",
    label: "其他非毒性甲状腺肿"
  },
  {
    value: "E05",
    label: "甲状腺毒症［甲状腺功能亢进症］"
  },
  {
    value: "E06",
    label: "甲状腺炎"
  },
  {
    value: "E07",
    label: "其他甲状腺疾患"
  },
  {
    value: "E10",
    label: "胰岛素依赖型糖尿病(Ⅰ型糖尿病)"
  },
  {
    value: "E11",
    label: "非胰岛素依赖型糖尿病(Ⅱ型糖尿病)"
  },
  {
    value: "E12",
    label: "营养不良相关性糖尿病"
  },
  {
    value: "E13",
    label: "其他特指的糖尿病"
  },
  {
    value: "E14",
    label: "未特指的糖尿病"
  },
  {
    value: "E15",
    label: "非糖尿病低血糖性昏迷"
  },
  {
    value: "E16",
    label: "胰腺内分泌的其他疾患"
  },
  {
    value: "E20",
    label: "甲状旁腺功能减退症"
  },
  {
    value: "E21",
    label: "甲状旁腺功能亢进和其他甲状旁腺疾患"
  },
  {
    value: "E22",
    label: "垂体机能亢进"
  },
  {
    value: "E23",
    label: "垂体机能减退和其他疾患"
  },
  {
    value: "E24",
    label: "库欣［柯兴］综合征"
  },
  {
    value: "E25",
    label: "肾上腺性征疾患"
  },
  {
    value: "E26",
    label: "醛固酮过多症"
  },
  {
    value: "E27",
    label: "其他肾上腺疾患"
  },
  {
    value: "E28",
    label: "卵巢机能障碍"
  },
  {
    value: "E29",
    label: "睾丸机能障碍"
  },
  {
    value: "E30",
    label: "青春期疾患，不可归类在他处者"
  },
  {
    value: "E31",
    label: "多腺体机能障碍"
  },
  {
    value: "E32",
    label: "胸腺病"
  },
  {
    value: "E34",
    label: "其他内分泌疾患"
  },
  {
    value: "E35",
    label: "分类于他处的疾病引起的内分泌腺疾患"
  },
  {
    value: "E40",
    label: "恶性营养不良病"
  },
  {
    value: "E41",
    label: "营养性消瘦"
  },
  {
    value: "E42",
    label: "消瘦性恶性营养不良病"
  },
  {
    value: "E43",
    label: "未特指的重度蛋白质－能量营养不良"
  },
  {
    value: "E44",
    label: "中度和轻度蛋白质－能量营养不良"
  },
  {
    value: "E45",
    label: "继后于蛋白质－能量营养不良的发育迟缓"
  },
  {
    value: "E46",
    label: "未特指的蛋白质－能量营养不良"
  },
  {
    value: "E50",
    label: "维生素Ａ缺乏"
  },
  {
    value: "E51",
    label: "硫胺素缺乏"
  },
  {
    value: "E52",
    label: "烟酸缺乏［糙皮病］"
  },
  {
    value: "E53",
    label: "其他Ｂ族维生素缺乏"
  },
  {
    value: "E54",
    label: "抗坏血酸缺乏"
  },
  {
    value: "E55",
    label: "维生素Ｄ缺乏"
  },
  {
    value: "E56",
    label: "其他维生素缺乏"
  },
  {
    value: "E58",
    label: "饮食性钙缺乏"
  },
  {
    value: "E59",
    label: "饮食性硒缺乏"
  },
  {
    value: "E60",
    label: "饮食性锌缺乏"
  },
  {
    value: "E61",
    label: "其他营养元素缺乏"
  },
  {
    value: "E63",
    label: "其他营养缺乏"
  },
  {
    value: "E64",
    label: "营养不良和其他营养缺乏的后遗症"
  },
  {
    value: "E65",
    label: "局部性肥胖症"
  },
  {
    value: "E66",
    label: "肥胖"
  },
  {
    value: "E67",
    label: "其他营养过度"
  },
  {
    value: "E68",
    label: "营养过度后遗症"
  },
  {
    value: "E70",
    label: "芳香氨基酸代谢紊乱"
  },
  {
    value: "E71",
    label: "支链氨基酸代谢和脂肪酸代谢紊乱"
  },
  {
    value: "E72",
    label: "其他氨基酸代谢紊乱"
  },
  {
    value: "E73",
    label: "乳糖耐受不良"
  },
  {
    value: "E74",
    label: "其他碳水化合物代谢紊乱"
  },
  {
    value: "E75",
    label: "（神经）鞘脂类代谢疾患和其他脂贮积紊乱"
  },
  {
    value: "E76",
    label: "氨基葡聚糖代谢紊乱"
  },
  {
    value: "E77",
    label: "糖蛋白代谢紊乱"
  },
  {
    value: "E78",
    label: "脂蛋白代谢疾患和其他脂血症"
  },
  {
    value: "E79",
    label: "嘌呤和嘧啶代谢紊乱"
  },
  {
    value: "E80",
    label: "卟啉和胆红素代谢紊乱"
  },
  {
    value: "E83",
    label: "矿物质代谢紊乱"
  },
  {
    value: "E84",
    label: "囊性纤维病"
  },
  {
    value: "E85",
    label: "淀粉样变性"
  },
  {
    value: "E86",
    label: "血容量缺失"
  },
  {
    value: "E87",
    label: "液体－电解质及酸碱平衡的其他紊乱"
  },
  {
    value: "E88",
    label: "其他代谢紊乱"
  },
  {
    value: "E89",
    label: "操作后内分泌和代谢紊乱，不可归类在他处者"
  },
  {
    value: "E90",
    label: "分类于他处的疾病引起的营养和代谢疾患"
  },
  {
    value: "F00",
    label: "阿尔茨海默病性痴呆（Ｇ30.－）"
  },
  {
    value: "F01",
    label: "血管性痴呆"
  },
  {
    value: "F02",
    label: "分类于他处的其他疾病引起的痴呆"
  },
  {
    value: "F03",
    label: "未特指的痴呆"
  },
  {
    value: "F04",
    label: "器质性遗忘综合征，非由酒精和其他精神活性物质所致"
  },
  {
    value: "F05",
    label: "谵妄，非由酒精和其他精神活性物质所致"
  },
  {
    value: "F06",
    label: "由于脑损害和机能障碍及躯体疾病引起的其他精神障碍"
  },
  {
    value: "F07",
    label: "由于脑部疾病、损害和功能障碍引起的人格和行为障碍"
  },
  {
    value: "F09",
    label: "未特指的器质性或症状性精神障碍"
  },
  {
    value: "F10",
    label: "由于使用酒精引起的精神和行为障碍"
  },
  {
    value: "F11",
    label: "由于使用类鸦片药引起的精神和行为障碍"
  },
  {
    value: "F12",
    label: "由于使用大麻类物质引起的精神和行为障碍"
  },
  {
    value: "F13",
    label: "由于使用镇静剂或催眠剂引起的精神和行为障碍"
  },
  {
    value: "F14",
    label: "由于使用可卡因引起的精神和行为障碍"
  },
  {
    value: "F15",
    label: "由于使用其他兴奋剂，包括咖啡因，引起的精神和行为障碍"
  },
  {
    value: "F16",
    label: "由于使用致幻剂引起的精神和行为障碍"
  },
  {
    value: "F17",
    label: "由于使用烟草引起的精神和行为障碍"
  },
  {
    value: "F18",
    label: "由于使用挥发性溶剂引起的精神和行为障碍"
  },
  {
    value: "F19",
    label: "由于使用多种药物和其他精神活性物质引起的精神和行为障碍"
  },
  {
    value: "F20",
    label: "精神分裂症"
  },
  {
    value: "F21",
    label: "分裂型障碍"
  },
  {
    value: "F22",
    label: "持久的妄想性障碍"
  },
  {
    value: "F23",
    label: "急性而短暂的精神病性障碍"
  },
  {
    value: "F24",
    label: "感应性妄想性障碍"
  },
  {
    value: "F25",
    label: "分裂情感性障碍"
  },
  {
    value: "F28",
    label: "其他非器质性精神病性障碍"
  },
  {
    value: "F29",
    label: "未特指的非器质性精神病"
  },
  {
    value: "F30",
    label: "躁狂发作"
  },
  {
    value: "F31",
    label: "双相情感障碍"
  },
  {
    value: "F32",
    label: "抑郁发作"
  },
  {
    value: "F33",
    label: "复发性抑郁障碍"
  },
  {
    value: "F34",
    label: "持续性心境［情感］障碍"
  },
  {
    value: "F38",
    label: "其他心境［情感］障碍"
  },
  {
    value: "F39",
    label: "未特指的心境［情感］障碍"
  },
  {
    value: "F40",
    label: "恐怖性焦虑障碍"
  },
  {
    value: "F41",
    label: "其他焦虑障碍"
  },
  {
    value: "F42",
    label: "强迫性障碍"
  },
  {
    value: "F43",
    label: "严重应激反应及适应障碍"
  },
  {
    value: "F44",
    label: "分离［转换］性障碍"
  },
  {
    value: "F45",
    label: "躯体形式障碍"
  },
  {
    value: "F48",
    label: "其他神经症性障碍"
  },
  {
    value: "F50",
    label: "进食障碍"
  },
  {
    value: "F51",
    label: "非器质性睡眠障碍"
  },
  {
    value: "F52",
    label: "非器质性障碍或疾病引起的性功能障碍"
  },
  {
    value: "F53",
    label: "与产褥期有关的精神和行为障碍，不可归类在他处者"
  },
  {
    value: "F54",
    label: "与归类在他处的障碍或疾病有关的心理和行为因素"
  },
  {
    value: "F55",
    label: "非依赖性物质滥用"
  },
  {
    value: "F59",
    label: "与生理紊乱和躯体因素有关的未特指的行为综合征"
  },
  {
    value: "F60",
    label: "特异性人格障碍"
  },
  {
    value: "F61",
    label: "混合型和其他人格障碍"
  },
  {
    value: "F62",
    label: "持久性人格改变，非由脑损害和疾病所致"
  },
  {
    value: "F63",
    label: "习惯和冲动障碍"
  },
  {
    value: "F64",
    label: "性身份障碍"
  },
  {
    value: "F65",
    label: "性偏好障碍"
  },
  {
    value: "F66",
    label: "与性发育和性取向有关的心理和行为障碍"
  },
  {
    value: "F68",
    label: "成人人格和行为的其他障碍"
  },
  {
    value: "F69",
    label: "未特指的成人人格和行为障碍"
  },
  {
    value: "F70",
    label: "轻度精神发育迟滞"
  },
  {
    value: "F71",
    label: "中度精神发育迟滞"
  },
  {
    value: "F72",
    label: "重度精神发育迟滞"
  },
  {
    value: "F73",
    label: "极重度精神发育迟滞"
  },
  {
    value: "F78",
    label: "其他精神发育迟滞"
  },
  {
    value: "F79",
    label: "未特指的精神发育迟滞"
  },
  {
    value: "F80",
    label: "特定性言语和语言发育障碍"
  },
  {
    value: "F81",
    label: "特定性学习技能发育障碍"
  },
  {
    value: "F82",
    label: "特定性运动功能发育障碍"
  },
  {
    value: "F83",
    label: "混合性特定性发育障碍"
  },
  {
    value: "F84",
    label: "弥漫性发育障碍"
  },
  {
    value: "F88",
    label: "其他心理发育障碍"
  },
  {
    value: "F89",
    label: "未特指的心理发育障碍"
  },
  {
    value: "F90",
    label: "多动性障碍"
  },
  {
    value: "F91",
    label: "品行障碍"
  },
  {
    value: "F92",
    label: "品行和情绪混合性障碍"
  },
  {
    value: "F93",
    label: "特发于童年的情绪障碍"
  },
  {
    value: "F94",
    label: "特发于童年和青少年的社会功能障碍"
  },
  {
    value: "F95",
    label: "抽动障碍"
  },
  {
    value: "F98",
    label: "通常起病于童年和青少年期的其他行为和情绪障碍"
  },
  {
    value: "F99",
    label: "精神障碍，其他方面未特指"
  },
  {
    value: "G00",
    label: "细菌性脑膜炎，不可归类在他处者"
  },
  {
    value: "G01",
    label: "分类于他处的细菌性疾病引起的脑膜炎"
  },
  {
    value: "G02",
    label: "分类于他处的其他传染病和寄生虫病引起的脑膜炎"
  },
  {
    value: "G03",
    label: "由于其他和未特指原因引起的脑膜炎"
  },
  {
    value: "G04",
    label: "脑炎、脊髓炎和脑脊髓炎"
  },
  {
    value: "G05",
    label: "分类于他处的疾病引起的脑炎、脊髓炎和脑脊髓炎"
  },
  {
    value: "G06",
    label: "颅内和脊柱内脓肿及肉芽肿"
  },
  {
    value: "G07",
    label: "分类于他处的疾病引起的颅内、脊柱内脓肿和肉芽肿"
  },
  {
    value: "G08",
    label: "颅内和脊柱内的静脉炎和血栓性静脉炎"
  },
  {
    value: "G09",
    label: "中枢神经系统炎性疾病的后遗症"
  },
  {
    value: "G10",
    label: "亨廷顿病"
  },
  {
    value: "G11",
    label: "遗传性共济失调"
  },
  {
    value: "G12",
    label: "脊髓性肌肉萎缩和有关的综合征"
  },
  {
    value: "G13",
    label: "分类于他处的疾病引起的主要影响中枢神经系统的全身性萎缩"
  },
  {
    value: "G20",
    label: "帕金森病"
  },
  {
    value: "G21",
    label: "继发性帕金森综合征"
  },
  {
    value: "G22",
    label: "分类于他处的疾病引起的帕金森综合征"
  },
  {
    value: "G23",
    label: "基底节的其他变性性病症"
  },
  {
    value: "G24",
    label: "肌张力障碍"
  },
  {
    value: "G25",
    label: "其他锥体外束和运动疾患"
  },
  {
    value: "G26",
    label: "分类于他处的疾病引起的锥体外束和运动疾患"
  },
  {
    value: "G30",
    label: "阿尔茨海默病"
  },
  {
    value: "G31",
    label: "神经系统的其他变性性疾病，不可归类在他处者"
  },
  {
    value: "G32",
    label: "分类于他处的疾病引起的神经系统其他变性性疾患"
  },
  {
    value: "G35",
    label: "多发性硬化"
  },
  {
    value: "G36",
    label: "其他急性播散性脱髓鞘"
  },
  {
    value: "G37",
    label: "中枢神经系统的其他脱髓鞘疾病"
  },
  {
    value: "G40",
    label: "癫痫"
  },
  {
    value: "G41",
    label: "癫痫状态"
  },
  {
    value: "G43",
    label: "偏头痛"
  },
  {
    value: "G44",
    label: "其他头痛综合征"
  },
  {
    value: "G45",
    label: "短暂性大脑缺血性发作和相关的综合征"
  },
  {
    value: "G46",
    label: "脑血管疾病引起的脑血管综合征（Ｉ60－Ｉ67）"
  },
  {
    value: "G47",
    label: "睡眠障碍"
  },
  {
    value: "G50",
    label: "三叉神经疾患"
  },
  {
    value: "G51",
    label: "面神经疾患"
  },
  {
    value: "G52",
    label: "其他颅神经疾患"
  },
  {
    value: "G53",
    label: "分类于他处的疾病引起的颅神经疾患"
  },
  {
    value: "G54",
    label: "神经根和神经丛疾患"
  },
  {
    value: "G55",
    label: "分类于他处的疾病引起的神经根和神经丛压迫"
  },
  {
    value: "G56",
    label: "上肢单神经病"
  },
  {
    value: "G57",
    label: "下肢单神经病"
  },
  {
    value: "G58",
    label: "其他单神经病"
  },
  {
    value: "G59",
    label: "分类于他处的疾病引起的单神经病"
  },
  {
    value: "G60",
    label: "遗传性和特发神经病"
  },
  {
    value: "G61",
    label: "炎性多神经病"
  },
  {
    value: "G62",
    label: "其他多神经病"
  },
  {
    value: "G63",
    label: "分类于他处的疾病引起的多神经病"
  },
  {
    value: "G64",
    label: "周围神经系统的其他疾患"
  },
  {
    value: "G70",
    label: "重症肌无力和其他肌神经疾患"
  },
  {
    value: "G71",
    label: "肌肉的原发性疾患"
  },
  {
    value: "G72",
    label: "其他肌病"
  },
  {
    value: "G73",
    label: "分类于他处的疾病引起的肌神经接点和肌肉的疾患"
  },
  {
    value: "G80",
    label: "婴儿脑性麻痹［瘫痪］"
  },
  {
    value: "G81",
    label: "偏瘫"
  },
  {
    value: "G82",
    label: "截瘫和四肢瘫"
  },
  {
    value: "G83",
    label: "其他麻痹［瘫痪］综合征"
  },
  {
    value: "G90",
    label: "自主神经系统的疾患"
  },
  {
    value: "G91",
    label: "脑积水"
  },
  {
    value: "G92",
    label: "中毒性脑病"
  },
  {
    value: "G93",
    label: "脑的其他疾患"
  },
  {
    value: "G94",
    label: "分类于他处的疾病引起的脑的其他疾患"
  },
  {
    value: "G95",
    label: "脊髓的其他疾病"
  },
  {
    value: "G96",
    label: "中枢神经系统的其他疾患"
  },
  {
    value: "G97",
    label: "神经系统的操作后疾患，不可归类在他处者"
  },
  {
    value: "G98",
    label: "神经系统的其他疾患，不可归类在他处者"
  },
  {
    value: "G99",
    label: "分类于他处的疾病引起的神经系统的其他疾患"
  },
  {
    value: "H00",
    label: "睑腺炎和睑板腺囊肿"
  },
  {
    value: "H01",
    label: "眼睑的其他炎症"
  },
  {
    value: "H02",
    label: "眼睑的其他疾患"
  },
  {
    value: "H03",
    label: "分类于他处的疾病引起的眼睑疾患"
  },
  {
    value: "H04",
    label: "泪器系疾患"
  },
  {
    value: "H05",
    label: "眼眶疾患"
  },
  {
    value: "H06",
    label: "分类于他处的疾病引起的泪器系和眼眶疾患"
  },
  {
    value: "H10",
    label: "结膜炎"
  },
  {
    value: "H11",
    label: "结膜的其他疾患"
  },
  {
    value: "H13",
    label: "分类于他处的疾病引起的结膜疾患"
  },
  {
    value: "H15",
    label: "巩膜疾患"
  },
  {
    value: "H16",
    label: "角膜炎"
  },
  {
    value: "H17",
    label: "角膜瘢痕和混浊"
  },
  {
    value: "H18",
    label: "角膜的其他疾患"
  },
  {
    value: "H19",
    label: "分类于他处的疾病引起的巩膜和角膜疾患"
  },
  {
    value: "H20",
    label: "虹膜睫状体炎"
  },
  {
    value: "H21",
    label: "虹膜和睫状体的其他疾患"
  },
  {
    value: "H22",
    label: "分类于他处的疾病引起的虹膜和睫状体疾患"
  },
  {
    value: "H25",
    label: "老年性白内障"
  },
  {
    value: "H26",
    label: "其他白内障"
  },
  {
    value: "H27",
    label: "晶状体的其他疾患"
  },
  {
    value: "H28",
    label: "分类于他处的疾病引起的白内障和晶状体的其他疾患"
  },
  {
    value: "H30",
    label: "脉络膜视网膜炎"
  },
  {
    value: "H31",
    label: "脉络膜的其他疾患"
  },
  {
    value: "H32",
    label: "分类于他处的疾病引起的脉络膜视网膜疾患"
  },
  {
    value: "H33",
    label: "视网膜脱离和断裂"
  },
  {
    value: "H34",
    label: "视网膜血管闭塞"
  },
  {
    value: "H35",
    label: "其他视网膜疾患"
  },
  {
    value: "H36",
    label: "分类于他处的疾病引起的视网膜疾患"
  },
  {
    value: "H40",
    label: "青光眼"
  },
  {
    value: "H42",
    label: "分类于他处的疾病引起的青光眼"
  },
  {
    value: "H43",
    label: "玻璃体疾患"
  },
  {
    value: "H44",
    label: "眼球疾患"
  },
  {
    value: "H45",
    label: "分类于他处的疾病引起的玻璃体和眼球疾患"
  },
  {
    value: "H46",
    label: "视神经炎"
  },
  {
    value: "H47",
    label: "视［第二］神经和视路的其他疾患"
  },
  {
    value: "H48",
    label: "分类于他处的疾病引起的视［第二］神经和视路疾患"
  },
  {
    value: "H49",
    label: "麻痹性斜视"
  },
  {
    value: "H50",
    label: "其他斜视"
  },
  {
    value: "H51",
    label: "双眼运动的其他疾患"
  },
  {
    value: "H52",
    label: "屈光和调节疾患"
  },
  {
    value: "H53",
    label: "视觉障碍"
  },
  {
    value: "H54",
    label: "盲和视力低下"
  },
  {
    value: "H55",
    label: "眼球震颤和其他不规则眼球运动"
  },
  {
    value: "H57",
    label: "眼和附器的其他疾患"
  },
  {
    value: "H58",
    label: "分类于他处的疾病引起的眼和附器的其他疾患"
  },
  {
    value: "H59",
    label: "眼和附器的操作后疾患，不可归类在他处者"
  },
  {
    value: "H60",
    label: "外耳炎"
  },
  {
    value: "H61",
    label: "外耳的其他疾患"
  },
  {
    value: "H62",
    label: "分类于他处的疾病引起的外耳疾患"
  },
  {
    value: "H65",
    label: "非化脓性中耳炎"
  },
  {
    value: "H66",
    label: "化脓性和未特指的中耳炎"
  },
  {
    value: "H67",
    label: "分类于他处的疾病引起的中耳炎"
  },
  {
    value: "H68",
    label: "咽鼓管炎和阻塞"
  },
  {
    value: "H69",
    label: "咽鼓管的其他疾患"
  },
  {
    value: "H70",
    label: "乳突炎和有关情况"
  },
  {
    value: "H71",
    label: "中耳胆脂瘤"
  },
  {
    value: "H72",
    label: "鼓膜穿孔"
  },
  {
    value: "H73",
    label: "鼓膜的其他疾患"
  },
  {
    value: "H74",
    label: "中耳和乳突的其他疾患"
  },
  {
    value: "H75",
    label: "分类于他处的疾病引起的中耳和乳突的其他疾患"
  },
  {
    value: "H80",
    label: "耳硬化症"
  },
  {
    value: "H81",
    label: "前庭功能疾患"
  },
  {
    value: "H82",
    label: "分类于他处的疾病引起的眩晕综合征"
  },
  {
    value: "H83",
    label: "内耳的其他疾病"
  },
  {
    value: "H90",
    label: "传导性和感音神经性听觉丧失"
  },
  {
    value: "H91",
    label: "其他听觉丧失"
  },
  {
    value: "H92",
    label: "耳痛和耳的渗出"
  },
  {
    value: "H93",
    label: "耳的其他疾患，不可归类在他处者"
  },
  {
    value: "H94",
    label: "分类于他处的疾病引起的耳的其他疾患"
  },
  {
    value: "H95",
    label: "耳和乳突的操作后疾患，不可归类在他处者"
  },
  {
    value: "I00",
    label: "风湿热，未提及心脏受累"
  },
  {
    value: "I01",
    label: "风湿热，伴有心脏受累"
  },
  {
    value: "I02",
    label: "风湿性舞蹈病"
  },
  {
    value: "I05",
    label: "风湿性二尖瓣疾病"
  },
  {
    value: "I06",
    label: "风湿性主动脉瓣疾病"
  },
  {
    value: "I07",
    label: "风湿性三尖瓣疾病"
  },
  {
    value: "I08",
    label: "多个心瓣膜疾病"
  },
  {
    value: "I09",
    label: "其他风湿性心脏病"
  },
  {
    value: "I10",
    label: "特发性（原发性）高血压"
  },
  {
    value: "I11",
    label: "高血压性心脏病"
  },
  {
    value: "I12",
    label: "高血压性肾脏病"
  },
  {
    value: "I13",
    label: "高血压性心脏和肾脏病"
  },
  {
    value: "I15",
    label: "继发性高血压"
  },
  {
    value: "I20",
    label: "心绞痛"
  },
  {
    value: "I21",
    label: "急性心肌梗死"
  },
  {
    value: "I22",
    label: "随后性心肌梗死"
  },
  {
    value: "I23",
    label: "急性心肌梗死后的某些近期并发症"
  },
  {
    value: "I24",
    label: "其他急性缺血性心脏病"
  },
  {
    value: "I25",
    label: "慢性缺血性心脏病(冠心病)"
  },
  {
    value: "I26",
    label: "肺栓塞"
  },
  {
    value: "I27",
    label: "其他肺原性心脏病"
  },
  {
    value: "I28",
    label: "肺血管的其他疾病"
  },
  {
    value: "I30",
    label: "急性心包炎"
  },
  {
    value: "I31",
    label: "心包的其他疾病"
  },
  {
    value: "I32",
    label: "分类于他处的疾病引起的心包炎"
  },
  {
    value: "I33",
    label: "急性和亚急性心内膜炎"
  },
  {
    value: "I34",
    label: "非风湿性二尖瓣疾患"
  },
  {
    value: "I35",
    label: "非风湿性主动脉瓣疾患"
  },
  {
    value: "I36",
    label: "非风湿性三尖瓣疾患"
  },
  {
    value: "I37",
    label: "肺动脉瓣疾患"
  },
  {
    value: "I38",
    label: "心内膜炎，瓣膜未特指"
  },
  {
    value: "I39",
    label: "分类于他处的疾病引起的心内膜炎和心瓣膜疾患"
  },
  {
    value: "I40",
    label: "急性心肌炎"
  },
  {
    value: "I41",
    label: "分类于他处的疾病引起的心肌炎"
  },
  {
    value: "I42",
    label: "心肌病"
  },
  {
    value: "I43",
    label: "分类于他处的疾病引起的心肌病"
  },
  {
    value: "I44",
    label: "房室传导阻滞和左束支传导阻滞"
  },
  {
    value: "I45",
    label: "其他传导疾患"
  },
  {
    value: "I46",
    label: "心脏停搏"
  },
  {
    value: "I47",
    label: "阵发性心动过速"
  },
  {
    value: "I48",
    label: "心房纤颤和扑动"
  },
  {
    value: "I49",
    label: "其他的心脏心律失常"
  },
  {
    value: "I50",
    label: "心力衰竭"
  },
  {
    value: "I51",
    label: "心脏病的并发症和不明确表述"
  },
  {
    value: "I52",
    label: "分类于他处的疾病引起的其他心脏疾患"
  },
  {
    value: "I60",
    label: "蛛网膜下出血"
  },
  {
    value: "I61",
    label: "脑内出血"
  },
  {
    value: "I62",
    label: "其他非创伤性颅内出血"
  },
  {
    value: "I63",
    label: "脑梗死"
  },
  {
    value: "I64",
    label: "中风，未特指为出血或梗死(脑中风、脑卒中)"
  },
  {
    value: "I65",
    label: "入脑前动脉的闭塞和狭窄，未造成脑梗死"
  },
  {
    value: "I66",
    label: "大脑动脉的闭塞和狭窄，未造成脑梗死"
  },
  {
    value: "I67",
    label: "其他脑血管病"
  },
  {
    value: "I68",
    label: "分类于他处的疾病引起的脑血管疾患"
  },
  {
    value: "I69",
    label: "脑血管病后遗症"
  },
  {
    value: "I70",
    label: "动脉粥样硬化症"
  },
  {
    value: "I71",
    label: "主动脉动脉瘤和动脉壁夹层形成"
  },
  {
    value: "I72",
    label: "其他动脉瘤"
  },
  {
    value: "I73",
    label: "其他周围血管疾病"
  },
  {
    value: "I74",
    label: "动脉栓塞和血栓形成"
  },
  {
    value: "I77",
    label: "动脉和小动脉和其他疾患"
  },
  {
    value: "I78",
    label: "毛细血管疾病"
  },
  {
    value: "I79",
    label: "分类于他处的疾病引起的动脉、小动脉和毛细血管疾患"
  },
  {
    value: "I80",
    label: "静脉炎和血栓静脉炎"
  },
  {
    value: "I81",
    label: "门静脉血栓形成"
  },
  {
    value: "I82",
    label: "其他静脉栓塞和血栓形成"
  },
  {
    value: "I83",
    label: "下肢静脉曲张"
  },
  {
    value: "I84",
    label: "痔"
  },
  {
    value: "I85",
    label: "食管静脉曲张"
  },
  {
    value: "I86",
    label: "其他部位的静脉曲张"
  },
  {
    value: "I87",
    label: "静脉的其他疾患"
  },
  {
    value: "I88",
    label: "非特异性淋巴结炎"
  },
  {
    value: "I89",
    label: "淋巴管和淋巴结的其他非感染性疾患"
  },
  {
    value: "I95",
    label: "低血压"
  },
  {
    value: "I97",
    label: "循环系统的操作后疾患，不可归类在他处者"
  },
  {
    value: "I98",
    label: "分类于他处的疾病引起的循环系统的其他疾患"
  },
  {
    value: "I99",
    label: "循环系统其他和未特指的疾患"
  },
  {
    value: "J00",
    label: "急性鼻咽炎［普通感冒］"
  },
  {
    value: "J01",
    label: "急性鼻窦炎"
  },
  {
    value: "J02",
    label: "急性咽炎"
  },
  {
    value: "J03",
    label: "急性扁桃体炎"
  },
  {
    value: "J04",
    label: "急性喉炎和气管炎"
  },
  {
    value: "J05",
    label: "急性梗阻性喉炎［哮吼］和会厌炎"
  },
  {
    value: "J06",
    label: "多发性和未特指部位的急性上呼吸道感染"
  },
  {
    value: "J09",
    label: "被标明的禽流感病毒引起的流行性感冒"
  },
  {
    value: "J10",
    label: "由于被标明的流行性感冒病毒引起的流行性感冒"
  },
  {
    value: "J11",
    label: "流行性感冒，病毒未标明"
  },
  {
    value: "J12",
    label: "病毒性肺炎，不可归类在他处者"
  },
  {
    value: "J13",
    label: "由于肺炎链球菌引起的肺炎"
  },
  {
    value: "J14",
    label: "由于流行性感冒嗜血杆菌引起的肺炎"
  },
  {
    value: "J15",
    label: "细菌性肺炎，不可归类在他处者"
  },
  {
    value: "J16",
    label: "由于其他传染性病原体引起的肺炎，不可归类在他处者"
  },
  {
    value: "J17",
    label: "分类于他处的疾病引起的肺炎"
  },
  {
    value: "J18",
    label: "肺炎，病原体未特指"
  },
  {
    value: "J20",
    label: "急性支气管炎"
  },
  {
    value: "J21",
    label: "急性细支气管炎"
  },
  {
    value: "J22",
    label: "未特指的急性下呼吸道感染"
  },
  {
    value: "J30",
    label: "血管舒缩性和变应性鼻炎"
  },
  {
    value: "J31",
    label: "慢性鼻炎，鼻咽炎和咽炎"
  },
  {
    value: "J32",
    label: "慢性鼻窦炎"
  },
  {
    value: "J33",
    label: "鼻息肉"
  },
  {
    value: "J34",
    label: "鼻和鼻窦的其他疾患"
  },
  {
    value: "J35",
    label: "扁桃体和腺样体慢性疾病"
  },
  {
    value: "J36",
    label: "扁桃体周围脓肿"
  },
  {
    value: "J37",
    label: "慢性喉炎和喉气管炎"
  },
  {
    value: "J38",
    label: "声带和喉疾病，不可归类在他处者"
  },
  {
    value: "J39",
    label: "上呼吸道的其他疾病"
  },
  {
    value: "J40",
    label: "支气管炎，未特指为急性或慢性"
  },
  {
    value: "J41",
    label: "单纯性和粘液化脓性慢性支气管炎"
  },
  {
    value: "J42",
    label: "未特指的慢性支气管炎"
  },
  {
    value: "J43",
    label: "肺气肿"
  },
  {
    value: "J44",
    label: "其他慢性阻塞性肺病"
  },
  {
    value: "J45",
    label: "哮喘"
  },
  {
    value: "J46",
    label: "哮喘持续状态"
  },
  {
    value: "J47",
    label: "支气管扩张症"
  },
  {
    value: "J60",
    label: "煤炭工尘肺"
  },
  {
    value: "J61",
    label: "由于石棉和其他矿物纤维引起的尘肺"
  },
  {
    value: "J62",
    label: "由于含硅［矽］粉尘引起的尘肺"
  },
  {
    value: "J63",
    label: "由于其他无机粉尘引起的尘肺"
  },
  {
    value: "J64",
    label: "未特指的尘肺"
  },
  {
    value: "J65",
    label: "与结核病有关的尘肺"
  },
  {
    value: "J66",
    label: "由于特指的有机粉尘引起的气道疾病"
  },
  {
    value: "J67",
    label: "由于有机粉尘引起的过敏性肺炎"
  },
  {
    value: "J68",
    label: "由于吸入化学制剂、气体、烟雾和蒸气引起的呼吸性情况"
  },
  {
    value: "J69",
    label: "由于固体和液体引起的肺炎"
  },
  {
    value: "J70",
    label: "由于其他外部物质引起的呼吸性情况"
  },
  {
    value: "J80",
    label: "成人型呼吸窘迫综合征"
  },
  {
    value: "J81",
    label: "肺水肿"
  },
  {
    value: "J82",
    label: "肺嗜酸性粒细胞增多，不可归类在他处者"
  },
  {
    value: "J84",
    label: "其他间质性肺疾病"
  },
  {
    value: "J85",
    label: "肺和纵隔脓肿"
  },
  {
    value: "J86",
    label: "脓胸"
  },
  {
    value: "J90",
    label: "胸膜渗出，不可归类在他处者"
  },
  {
    value: "J91",
    label: "分类于他处的情况引起的胸膜渗漏"
  },
  {
    value: "J92",
    label: "胸膜斑"
  },
  {
    value: "J93",
    label: "气胸"
  },
  {
    value: "J94",
    label: "其他胸膜情况"
  },
  {
    value: "J95",
    label: "操作后的呼吸性疾患，不可归类在他处者"
  },
  {
    value: "J96",
    label: "呼吸衰竭，不可归类在他处者"
  },
  {
    value: "J98",
    label: "其他呼吸性疾患"
  },
  {
    value: "J99",
    label: "分类于他处的疾病引起的呼吸性疾患"
  },
  {
    value: "K00",
    label: "牙齿发育和萌出疾患"
  },
  {
    value: "K01",
    label: "埋伏牙和阻生牙"
  },
  {
    value: "K02",
    label: "龋（齿）"
  },
  {
    value: "K03",
    label: "牙齿硬组织的其他疾病"
  },
  {
    value: "K04",
    label: "牙髓和根尖周组织疾病"
  },
  {
    value: "K05",
    label: "龈炎和牙周疾病"
  },
  {
    value: "K06",
    label: "牙龈和无牙牙槽嵴的其他疾患"
  },
  {
    value: "K07",
    label: "牙面畸形［包括错牙合］"
  },
  {
    value: "K08",
    label: "牙齿及支持结构的其他疾患"
  },
  {
    value: "K09",
    label: "口腔囊肿，不可归类在他处者"
  },
  {
    value: "K10",
    label: "颌的其他疾病"
  },
  {
    value: "K11",
    label: "涎腺疾病"
  },
  {
    value: "K12",
    label: "口炎和有关损害"
  },
  {
    value: "K13",
    label: "唇及口腔粘膜的其他疾病"
  },
  {
    value: "K14",
    label: "舌疾病"
  },
  {
    value: "K20",
    label: "食管炎"
  },
  {
    value: "K21",
    label: "胃－食管反流性疾病"
  },
  {
    value: "K22",
    label: "食管的其他疾病"
  },
  {
    value: "K23",
    label: "分类于他处的疾病引起的食管疾患"
  },
  {
    value: "K25",
    label: "胃溃疡"
  },
  {
    value: "K26",
    label: "十二指肠溃疡"
  },
  {
    value: "K27",
    label: "消化性溃疡，部位未特指"
  },
  {
    value: "K28",
    label: "胃空肠溃疡"
  },
  {
    value: "K29",
    label: "胃炎和十二指肠炎"
  },
  {
    value: "K30",
    label: "消化不良"
  },
  {
    value: "K31",
    label: "胃和十二指肠的其他疾病"
  },
  {
    value: "K35",
    label: "急性阑尾炎"
  },
  {
    value: "K36",
    label: "其他阑尾炎"
  },
  {
    value: "K37",
    label: "未特指的阑尾炎"
  },
  {
    value: "K38",
    label: "阑尾的其他疾病"
  },
  {
    value: "K40",
    label: "腹股沟疝"
  },
  {
    value: "K41",
    label: "股疝"
  },
  {
    value: "K42",
    label: "脐疝"
  },
  {
    value: "K43",
    label: "腹疝"
  },
  {
    value: "K44",
    label: "隔疝"
  },
  {
    value: "K45",
    label: "其他腹部疝"
  },
  {
    value: "K46",
    label: "未特指的腹部疝"
  },
  {
    value: "K50",
    label: "克罗恩［克隆］病［节段性肠炎］"
  },
  {
    value: "K51",
    label: "溃疡性结肠炎"
  },
  {
    value: "K52",
    label: "其他非感染性胃肠炎和结肠炎"
  },
  {
    value: "K55",
    label: "肠血管疾患"
  },
  {
    value: "K56",
    label: "无力性肠梗阻和肠梗阻不伴有疝"
  },
  {
    value: "K57",
    label: "肠憩室性疾病"
  },
  {
    value: "K58",
    label: "过敏性大肠综合征"
  },
  {
    value: "K59",
    label: "其他功能性肠疾患"
  },
  {
    value: "K60",
    label: "肛门及直肠区的裂和瘘"
  },
  {
    value: "K61",
    label: "肛门和直肠区脓肿"
  },
  {
    value: "K62",
    label: "肛门和直肠的其他疾病"
  },
  {
    value: "K63",
    label: "肠的其他疾病"
  },
  {
    value: "K65",
    label: "腹膜炎"
  },
  {
    value: "K66",
    label: "腹膜的其他疾患"
  },
  {
    value: "K67",
    label: "分类于他处的传染病引起的腹膜疾患"
  },
  {
    value: "K70",
    label: "酒精性肝病"
  },
  {
    value: "K71",
    label: "中毒性肝病"
  },
  {
    value: "K72",
    label: "肝衰竭，不可归类在他处者"
  },
  {
    value: "K73",
    label: "慢性肝炎，不可归类在他处者"
  },
  {
    value: "K74",
    label: "肝纤维化和硬变"
  },
  {
    value: "K75",
    label: "其他炎性肝脏疾病"
  },
  {
    value: "K76",
    label: "肝的其他疾病"
  },
  {
    value: "K77",
    label: "分类于他处的疾病引起的肝疾患"
  },
  {
    value: "K80",
    label: "胆石病"
  },
  {
    value: "K81",
    label: "胆囊炎"
  },
  {
    value: "K82",
    label: "胆囊的其他疾病"
  },
  {
    value: "K83",
    label: "胆道的其他疾病"
  },
  {
    value: "K85",
    label: "急性胰腺炎"
  },
  {
    value: "K86",
    label: "胰腺的其他疾病"
  },
  {
    value: "K87",
    label: "分类于他处的疾病引起的胆囊、胆道和胰腺疾患"
  },
  {
    value: "K90",
    label: "肠吸收障碍"
  },
  {
    value: "K91",
    label: "消化系统的操作后疾患，不可归类在他处者"
  },
  {
    value: "K92",
    label: "消化系统的其他疾病"
  },
  {
    value: "K93",
    label: "分类于他处的疾病引起的其他消化器官疾患"
  },
  {
    value: "L00",
    label: "葡萄球菌性烫伤皮肤综合征"
  },
  {
    value: "L01",
    label: "脓疱病"
  },
  {
    value: "L02",
    label: "皮肤脓肿、疖和痈"
  },
  {
    value: "L03",
    label: "蜂窝织炎"
  },
  {
    value: "L04",
    label: "急性淋巴结炎"
  },
  {
    value: "L05",
    label: "藏毛囊肿"
  },
  {
    value: "L08",
    label: "皮肤和皮下组织其他局部感染"
  },
  {
    value: "L10",
    label: "天疱疮"
  },
  {
    value: "L11",
    label: "其他皮肤棘层松解性疾患"
  },
  {
    value: "L12",
    label: "类天疱疮"
  },
  {
    value: "L13",
    label: "其他大疱性疾患"
  },
  {
    value: "L14",
    label: "分类于他处的疾病引起的大疱性疾患"
  },
  {
    value: "L20",
    label: "特应性皮炎"
  },
  {
    value: "L21",
    label: "脂溢性皮炎"
  },
  {
    value: "L22",
    label: "尿布皮炎"
  },
  {
    value: "L23",
    label: "变应性接触性皮炎"
  },
  {
    value: "L24",
    label: "刺激性接触性皮炎"
  },
  {
    value: "L25",
    label: "未特指的接触性皮炎"
  },
  {
    value: "L26",
    label: "剥脱性皮炎"
  },
  {
    value: "L27",
    label: "由于内服物质引起的皮炎"
  },
  {
    value: "L28",
    label: "慢性单纯性苔癣和痒疹"
  },
  {
    value: "L29",
    label: "瘙痒症"
  },
  {
    value: "L30",
    label: "其他皮炎"
  },
  {
    value: "L40",
    label: "牛皮癣"
  },
  {
    value: "L41",
    label: "类牛皮癣"
  },
  {
    value: "L42",
    label: "玫瑰糠疹"
  },
  {
    value: "L43",
    label: "扁平苔癣"
  },
  {
    value: "L44",
    label: "其他丘疹鳞屑性疾患"
  },
  {
    value: "L45",
    label: "分类于他处的疾病引起的丘疹鳞屑性疾患"
  },
  {
    value: "L50",
    label: "荨麻疹"
  },
  {
    value: "L51",
    label: "多形红斑"
  },
  {
    value: "L52",
    label: "结节性红斑"
  },
  {
    value: "L53",
    label: "其他红斑性情况"
  },
  {
    value: "L54",
    label: "分类于他处的疾病引起的红斑"
  },
  {
    value: "L55",
    label: "晒伤"
  },
  {
    value: "L56",
    label: "由于紫外线辐射引起的其他急性皮肤改变"
  },
  {
    value: "L57",
    label: "由于慢性暴露于非电离辐射下引起的皮肤改变"
  },
  {
    value: "L58",
    label: "放射性皮炎"
  },
  {
    value: "L59",
    label: "与辐射有关的皮肤和皮下组织其他疾患"
  },
  {
    value: "L60",
    label: "甲疾患"
  },
  {
    value: "L62",
    label: "分类于他处的疾病引起的甲疾患"
  },
  {
    value: "L63",
    label: "局限性脱发"
  },
  {
    value: "L64",
    label: "雄激素性脱发"
  },
  {
    value: "L65",
    label: "其他非瘢痕性毛发缺失"
  },
  {
    value: "L66",
    label: "瘢痕性脱发［瘢痕性毛发缺失］"
  },
  {
    value: "L67",
    label: "毛色和毛干异常"
  },
  {
    value: "L68",
    label: "多毛症"
  },
  {
    value: "L70",
    label: "痤疮"
  },
  {
    value: "L71",
    label: "酒渣鼻"
  },
  {
    value: "L72",
    label: "皮肤和皮下组织滤泡囊肿"
  },
  {
    value: "L73",
    label: "其他滤泡性疾患"
  },
  {
    value: "L74",
    label: "外分泌汗腺疾患"
  },
  {
    value: "L75",
    label: "顶（浆分）泌汗腺疾患"
  },
  {
    value: "L80",
    label: "白斑"
  },
  {
    value: "L81",
    label: "色素沉着的其他疾患"
  },
  {
    value: "L82",
    label: "皮脂溢性角化病"
  },
  {
    value: "L83",
    label: "黑色棘皮症"
  },
  {
    value: "L84",
    label: "鸡眼和胼胝"
  },
  {
    value: "L85",
    label: "其他表皮肥厚"
  },
  {
    value: "L86",
    label: "分类于他处的疾病引起的皮肤角化病"
  },
  {
    value: "L87",
    label: "经表皮排除疾患"
  },
  {
    value: "L88",
    label: "坏疽性脓皮症"
  },
  {
    value: "L89",
    label: "褥疮性溃疡"
  },
  {
    value: "L90",
    label: "皮肤萎缩性疾患"
  },
  {
    value: "L91",
    label: "皮肤肥大性疾患"
  },
  {
    value: "L92",
    label: "皮肤和皮下组织肉芽肿性疾患"
  },
  {
    value: "L93",
    label: "红斑狼疮"
  },
  {
    value: "L94",
    label: "其他局限性结缔组织疾患"
  },
  {
    value: "L95",
    label: "局限于皮肤的脉管炎，不可归类在他处者"
  },
  {
    value: "L97",
    label: "下肢溃疡，不可归类在他处者"
  },
  {
    value: "L98",
    label: "皮肤和皮下组织的其他疾患，不可归类在他处者"
  },
  {
    value: "L99",
    label: "分类于他处的疾病引起的皮肤和皮下组织的其他疾患"
  },
  {
    value: "M00",
    label: "化脓性关节炎"
  },
  {
    value: "M01",
    label: "分类于他处的传染病和寄生虫病引起的关节的直接感染"
  },
  {
    value: "M02",
    label: "反应性关节病"
  },
  {
    value: "M03",
    label: "分类于他处的疾病引起的感染后和反应性关节病"
  },
  {
    value: "M05",
    label: "血清反应阳性的类风湿性关节炎"
  },
  {
    value: "M06",
    label: "其他类风湿性关节炎"
  },
  {
    value: "M07",
    label: "牛皮癣性和肠病性关节病"
  },
  {
    value: "M08",
    label: "幼年型关节炎"
  },
  {
    value: "M09",
    label: "分类于他处的疾病引起的幼年型关节炎"
  },
  {
    value: "M10",
    label: "痛风"
  },
  {
    value: "M11",
    label: "其他晶体性关节病"
  },
  {
    value: "M12",
    label: "其他特指的关节病"
  },
  {
    value: "M13",
    label: "其他关节炎"
  },
  {
    value: "M14",
    label: "分类于他处的其他疾病引起的关节病"
  },
  {
    value: "M15",
    label: "多关节病"
  },
  {
    value: "M16",
    label: "髋关节病"
  },
  {
    value: "M17",
    label: "膝关节病"
  },
  {
    value: "M18",
    label: "第一腕掌关节的关节病"
  },
  {
    value: "M19",
    label: "其他关节病"
  },
  {
    value: "M20",
    label: "手指和脚趾的后天性变形"
  },
  {
    value: "M21",
    label: "四肢其他后天性变形"
  },
  {
    value: "M22",
    label: "髌骨疾患"
  },
  {
    value: "M23",
    label: "膝内部紊乱"
  },
  {
    value: "M24",
    label: "其他特指的关节紊乱"
  },
  {
    value: "M25",
    label: "其他关节疾患，不可归类在他处者"
  },
  {
    value: "M30",
    label: "结节性多动脉炎和有关情况"
  },
  {
    value: "M31",
    label: "其他坏死性血管病"
  },
  {
    value: "M32",
    label: "系统性红斑狼疮"
  },
  {
    value: "M33",
    label: "皮多肌炎"
  },
  {
    value: "M34",
    label: "全身性硬皮病"
  },
  {
    value: "M35",
    label: "结缔组织的其他系统性受累"
  },
  {
    value: "M36",
    label: "分类于他处的疾病引起的系统性结缔组织疾患"
  },
  {
    value: "M40",
    label: "脊柱后凸和脊柱前凸"
  },
  {
    value: "M41",
    label: "脊柱侧凸"
  },
  {
    value: "M42",
    label: "脊柱骨软骨病"
  },
  {
    value: "M43",
    label: "其他变形性背部病"
  },
  {
    value: "M45",
    label: "关节强硬性脊椎炎"
  },
  {
    value: "M46",
    label: "其他炎性脊椎病"
  },
  {
    value: "M47",
    label: "脊椎关节强硬"
  },
  {
    value: "M48",
    label: "其他脊椎病"
  },
  {
    value: "M49",
    label: "分类于他处的疾病引起的脊椎病"
  },
  {
    value: "M50",
    label: "颈椎间盘疾患"
  },
  {
    value: "M51",
    label: "其他椎间盘疾患"
  },
  {
    value: "M53",
    label: "其他背部病，不可归类在他处者"
  },
  {
    value: "M54",
    label: "背痛"
  },
  {
    value: "M60",
    label: "肌炎"
  },
  {
    value: "M61",
    label: "肌肉钙化和骨化"
  },
  {
    value: "M62",
    label: "肌肉其他疾患"
  },
  {
    value: "M63",
    label: "分类于他处的疾病引起的肌肉疾患"
  },
  {
    value: "M65",
    label: "滑膜炎和腱鞘炎"
  },
  {
    value: "M66",
    label: "滑膜和肌腱的自发性破裂"
  },
  {
    value: "M67",
    label: "滑膜和肌腱的其他疾患"
  },
  {
    value: "M68",
    label: "分类于他处的疾病引起的滑膜和肌腱疾患"
  },
  {
    value: "M70",
    label: "与使用、过度使用和压迫有关的软组织疾患"
  },
  {
    value: "M71",
    label: "其他粘液囊病"
  },
  {
    value: "M72",
    label: "成纤维细胞疾患"
  },
  {
    value: "M73",
    label: "分类于他处的疾病引起的软组织疾患"
  },
  {
    value: "M75",
    label: "肩损害"
  },
  {
    value: "M76",
    label: "下肢肌腱端病，不包括足"
  },
  {
    value: "M77",
    label: "其他肌腱端病"
  },
  {
    value: "M79",
    label: "其他软组织疾患，不可归类在他处者"
  },
  {
    value: "M80",
    label: "骨质疏松，伴有病理性骨折"
  },
  {
    value: "M81",
    label: "骨质疏松，不伴有病理性骨折"
  },
  {
    value: "M82",
    label: "分类于他处的疾病引起的骨质疏松"
  },
  {
    value: "M83",
    label: "成人骨软化"
  },
  {
    value: "M84",
    label: "骨连续性疾患"
  },
  {
    value: "M85",
    label: "骨密度和结构的其他疾患"
  },
  {
    value: "M86",
    label: "骨髓炎"
  },
  {
    value: "M87",
    label: "骨坏死"
  },
  {
    value: "M88",
    label: "骨佩吉特病[变形性骨炎]"
  },
  {
    value: "M89",
    label: "骨的其他疾患"
  },
  {
    value: "M90",
    label: "分类于他处的疾患引起的骨病"
  },
  {
    value: "M91",
    label: "髋和骨盆的幼年型软骨病"
  },
  {
    value: "M92",
    label: "其他幼年型骨软骨病"
  },
  {
    value: "M93",
    label: "其他骨软骨病"
  },
  {
    value: "M94",
    label: "软骨的其他疾患"
  },
  {
    value: "M95",
    label: "肌肉骨骼系统和结缔组织的其他后天性变形"
  },
  {
    value: "M96",
    label: "操作后肌肉骨骼疾患，不可归类在他处者"
  },
  {
    value: "M99",
    label: "生物力学损害，不可归类在他处者"
  },
  {
    value: "N00",
    label: "急性肾炎综合征"
  },
  {
    value: "N01",
    label: "急进型肾炎综合征"
  },
  {
    value: "N02",
    label: "复发性和持续性血尿"
  },
  {
    value: "N03",
    label: "慢性肾炎综合征"
  },
  {
    value: "N04",
    label: "肾变病综合征"
  },
  {
    value: "N05",
    label: "未特指的肾炎综合征"
  },
  {
    value: "N06",
    label: "孤立性蛋白尿，伴有特指的形态学损害"
  },
  {
    value: "N07",
    label: "遗传性肾病变，不可归类在他处者"
  },
  {
    value: "N08",
    label: "分类于他处的疾病引起的肾小球疾患"
  },
  {
    value: "N10",
    label: "急性肾小管-间质肾炎"
  },
  {
    value: "N11",
    label: "慢性肾小管-间质肾炎"
  },
  {
    value: "N12",
    label: "肾小管-间质肾炎，未特指急性或慢性"
  },
  {
    value: "N13",
    label: "阻塞性和反流性尿路病"
  },
  {
    value: "N14",
    label: "药物和重金属诱发的肾小管-间质和肾小管情况"
  },
  {
    value: "N15",
    label: "其他肾小管-间质疾病"
  },
  {
    value: "N16",
    label: "分类于他处的疾病引起的肾小管-间质疾患"
  },
  {
    value: "N17",
    label: "急性肾衰竭"
  },
  {
    value: "N18",
    label: "慢性肾衰竭"
  },
  {
    value: "N19",
    label: "未特指的肾衰竭(尿毒症)"
  },
  {
    value: "N20",
    label: "肾和输尿管结石"
  },
  {
    value: "N21",
    label: "下泌尿道结石"
  },
  {
    value: "N22",
    label: "分类于他处的疾病引起的泌尿道结石"
  },
  {
    value: "N23",
    label: "未特指的肾绞痛"
  },
  {
    value: "N25",
    label: "肾小管功能损害所致的疾患"
  },
  {
    value: "N26",
    label: "未特指的肾挛缩"
  },
  {
    value: "N27",
    label: "原因不明的小肾"
  },
  {
    value: "N28",
    label: "肾和输尿管的其他疾患，不可归类在他处者"
  },
  {
    value: "N29",
    label: "分类于他处的疾病引起的肾和输尿管的其他疾患"
  },
  {
    value: "N30",
    label: "膀胱炎"
  },
  {
    value: "N31",
    label: "膀胱神经肌肉功能不良，不可归类在他处者"
  },
  {
    value: "N32",
    label: "膀胱的其他疾患"
  },
  {
    value: "N33",
    label: "分类于他处的疾病引起的膀胱疾患"
  },
  {
    value: "N34",
    label: "尿道炎和尿道综合征"
  },
  {
    value: "N35",
    label: "尿道狭窄"
  },
  {
    value: "N36",
    label: "尿道的其他疾患"
  },
  {
    value: "N37",
    label: "分类于他处的疾病引起的尿道疾患"
  },
  {
    value: "N39",
    label: "泌尿系统的其他疾患"
  },
  {
    value: "N40",
    label: "前列腺增生"
  },
  {
    value: "N41",
    label: "前列腺炎性疾病"
  },
  {
    value: "N42",
    label: "前列腺的其他疾患"
  },
  {
    value: "N43",
    label: "水囊肿[睾丸鞘膜积液]和精子囊肿"
  },
  {
    value: "N44",
    label: "睾丸扭转"
  },
  {
    value: "N45",
    label: "睾丸炎和附睾炎"
  },
  {
    value: "N46",
    label: "男性不育症"
  },
  {
    value: "N47",
    label: "包皮过长、包茎和包茎嵌顿"
  },
  {
    value: "N48",
    label: "阴茎的其他疾患"
  },
  {
    value: "N49",
    label: "男性生殖器官炎性疾患，不可归类在他处者"
  },
  {
    value: "N50",
    label: "男性生殖器官的其他疾患"
  },
  {
    value: "N51",
    label: "分类于他处的疾病引起的男性生殖器官疾患"
  },
  {
    value: "N60",
    label: "良性乳房发育不良"
  },
  {
    value: "N61",
    label: "乳房炎性疾患"
  },
  {
    value: "N62",
    label: "乳房肥大"
  },
  {
    value: "N63",
    label: "未特指的乳房肿块"
  },
  {
    value: "N64",
    label: "乳房的其他疾患"
  },
  {
    value: "N70",
    label: "输卵管炎和卵巢炎"
  },
  {
    value: "N71",
    label: "子宫炎性疾病，除外宫颈"
  },
  {
    value: "N72",
    label: "宫颈炎性疾病"
  },
  {
    value: "N73",
    label: "其他女性盆腔炎性疾病"
  },
  {
    value: "N74",
    label: "分类于他处的疾病引起的女性盆腔炎性疾患"
  },
  {
    value: "N75",
    label: "前庭大腺[巴多林腺]疾病"
  },
  {
    value: "N76",
    label: "阴道和外阴的其他炎症"
  },
  {
    value: "N77",
    label: "分类于他处的疾病引起的外阴阴道溃疡和炎症"
  },
  {
    value: "N80",
    label: "子宫内膜异位"
  },
  {
    value: "N81",
    label: "女性生殖器脱垂"
  },
  {
    value: "N82",
    label: "累及女性生殖道的瘘"
  },
  {
    value: "N83",
    label: "卵巢、输卵管和阔韧带的非炎性疾患"
  },
  {
    value: "N84",
    label: "女性生殖道息肉"
  },
  {
    value: "N85",
    label: "子宫其他非炎性疾患，除外宫颈"
  },
  {
    value: "N86",
    label: "宫颈糜烂和外翻"
  },
  {
    value: "N87",
    label: "宫颈发育不良"
  },
  {
    value: "N88",
    label: "宫颈其他非炎性疾患"
  },
  {
    value: "N89",
    label: "阴道的其他非炎性疾患"
  },
  {
    value: "N90",
    label: "外阴和会阴的其他非炎性疾患"
  },
  {
    value: "N91",
    label: "无月经、月经过少和月经稀少"
  },
  {
    value: "N92",
    label: "月经过多、频繁而且不规则"
  },
  {
    value: "N93",
    label: "其他异常的子宫和阴道出血"
  },
  {
    value: "N94",
    label: "与女性生殖器官和月经周期有关的疼痛和其他情况"
  },
  {
    value: "N95",
    label: "绝经期和其他围绝经期的疾患"
  },
  {
    value: "N96",
    label: "习惯性流产"
  },
  {
    value: "N97",
    label: "女性不育症"
  },
  {
    value: "N98",
    label: "与人工授精有关的并发症"
  },
  {
    value: "N99",
    label: "泌尿生殖系统的操作后疾患，不可归类在他处者"
  },
  {
    value: "O00",
    label: "异位妊娠"
  },
  {
    value: "O02",
    label: "受孕的其他异常产物"
  },
  {
    value: "O03",
    label: "自然流产"
  },
  {
    value: "O04",
    label: "医疗性流产"
  },
  {
    value: "O05",
    label: "其他流产"
  },
  {
    value: "O06",
    label: "未特指的流产"
  },
  {
    value: "O07",
    label: "企图流产失败"
  },
  {
    value: "O08",
    label: "流产、异位妊娠和葡萄胎妊娠后的并发症"
  },
  {
    value: "O10",
    label: "原有的高血压并发于妊娠、分娩和产褥期"
  },
  {
    value: "O11",
    label: "原有高血压性疾患，并发蛋白尿"
  },
  {
    value: "O12",
    label: "妊娠[妊娠引起的]水肿和蛋白尿，不伴有高血压"
  },
  {
    value: "O13",
    label: "妊娠[妊娠引起的]高血压，不伴有有意义的蛋白尿"
  },
  {
    value: "O14",
    label: "妊娠[妊娠引起的]高血压，伴有有意义的蛋白尿"
  },
  {
    value: "O15",
    label: "子痫"
  },
  {
    value: "O16",
    label: "未特指的孕产妇高血压(妊高症)"
  },
  {
    value: "O20",
    label: "早期妊娠出血"
  },
  {
    value: "O21",
    label: "妊娠剧吐"
  },
  {
    value: "O22",
    label: "妊娠期静脉并发症"
  },
  {
    value: "O23",
    label: "妊娠期泌尿生殖道的感染"
  },
  {
    value: "O24",
    label: "妊娠期糖尿病"
  },
  {
    value: "O25",
    label: "妊娠期营养不良"
  },
  {
    value: "O26",
    label: "主要与妊娠有关的其他情况的孕产妇医疗"
  },
  {
    value: "O28",
    label: "孕产妇产前筛选检查的异常所见"
  },
  {
    value: "O29",
    label: "妊娠期间麻醉的并发症"
  },
  {
    value: "O30",
    label: "多胎妊娠"
  },
  {
    value: "O31",
    label: "特发于多胎妊娠的并发症"
  },
  {
    value: "O32",
    label: "为已知或可疑胎儿先露异常给予的孕产妇医疗"
  },
  {
    value: "O33",
    label: "为已知或可疑胎盆不称给予的孕产妇医疗"
  },
  {
    value: "O34",
    label: "为已知或可疑盆腔器官异常给予的孕产妇医疗"
  },
  {
    value: "O35",
    label: "为已知或可疑胎儿异常和损害给予的孕产妇医疗"
  },
  {
    value: "O36",
    label: "为其他已知或可疑的胎儿问题给予的孕产妇医疗"
  },
  {
    value: "O40",
    label: "羊水过多"
  },
  {
    value: "O41",
    label: "羊水和胎膜的其他疾患"
  },
  {
    value: "O42",
    label: "胎膜早破"
  },
  {
    value: "O43",
    label: "胎盘疾患"
  },
  {
    value: "O44",
    label: "前置胎盘"
  },
  {
    value: "O45",
    label: "胎盘的过早分离[胎盘早期脱离]"
  },
  {
    value: "O46",
    label: "产前出血，不可归类在他处者"
  },
  {
    value: "O47",
    label: "假临产"
  },
  {
    value: "O48",
    label: "过期妊娠"
  },
  {
    value: "O60",
    label: "早产"
  },
  {
    value: "O61",
    label: "引产失败"
  },
  {
    value: "O62",
    label: "产力异常"
  },
  {
    value: "O63",
    label: "滞产"
  },
  {
    value: "O64",
    label: "由于胎儿的胎位不正和先露异常引起的梗阻性分娩"
  },
  {
    value: "O65",
    label: "由于母体骨盆异常引起的梗阻性分娩"
  },
  {
    value: "O66",
    label: "其他梗阻性分娩"
  },
  {
    value: "O67",
    label: "产程和分娩并发分娩期内出血，不可归类在他处者"
  },
  {
    value: "O68",
    label: "产程和分娩并发胎儿应激反应[窘迫]"
  },
  {
    value: "O69",
    label: "产程和分娩并发脐带并发症"
  },
  {
    value: "O70",
    label: "分娩时会阴裂伤"
  },
  {
    value: "O71",
    label: "其他产科创伤"
  },
  {
    value: "O72",
    label: "产后出血"
  },
  {
    value: "O73",
    label: "胎盘和胎膜滞留，不伴有出血"
  },
  {
    value: "O74",
    label: "产程和分娩期间麻醉的并发症"
  },
  {
    value: "O75",
    label: "产程和分娩的其他并发症，不可归类在他处者"
  },
  {
    value: "O80",
    label: "单胎顺产"
  },
  {
    value: "O81",
    label: "借助产钳和真空吸引器的单胎分娩"
  },
  {
    value: "O82",
    label: "经剖宫产术的单胎分娩"
  },
  {
    value: "O83",
    label: "其他助产的单胎分娩"
  },
  {
    value: "O84",
    label: "多胎分娩"
  },
  {
    value: "O85",
    label: "产褥期脓毒病"
  },
  {
    value: "O86",
    label: "其他产褥感染"
  },
  {
    value: "O87",
    label: "产褥期的静脉并发症"
  },
  {
    value: "O88",
    label: "产科栓塞"
  },
  {
    value: "O89",
    label: "产褥期中麻醉的并发症"
  },
  {
    value: "O90",
    label: "产褥期的并发症，不可归类在他处者"
  },
  {
    value: "O91",
    label: "与分娩有关的乳房感染"
  },
  {
    value: "O92",
    label: "与分娩有关的乳房和哺乳的其他疾患"
  },
  {
    value: "O94",
    label: "妊娠、分娩和产褥期并发症的后遗症 *"
  },
  {
    value: "O95",
    label: "未特指原因的产科死亡"
  },
  {
    value: "O96",
    label: "任何产科原因的死亡，发生于分娩后42天以上至一年以内"
  },
  {
    value: "O97",
    label: "由于直接产科原因后遗症的死亡"
  },
  {
    value: "O98",
    label: "可归类在他处的孕产妇的传染病和寄生虫病并发于妊娠、分娩和产褥期"
  },
  {
    value: "O99",
    label: "可归类在他处的孕产妇的其他疾病并发于妊娠、分娩和产褥期"
  },
  {
    value: "P00",
    label: "胎儿和新生儿受母体情况影响，这些情况可能与本次妊娠无关"
  },
  {
    value: "P01",
    label: "胎儿和新生儿受母体妊娠并发症的影响"
  },
  {
    value: "P02",
    label: "胎儿和新生儿受胎盘、脐带和胎膜的并发症的影响"
  },
  {
    value: "P03",
    label: "胎儿和新生儿受产程和分娩的其他并发症的影响"
  },
  {
    value: "P04",
    label: "胎儿和新生儿受经胎盘或母乳传播的有害物质的影响"
  },
  {
    value: "P05",
    label: "胎儿生长缓慢和胎儿营养不良"
  },
  {
    value: "P07",
    label: "与孕期短和低出生体重有关的疾患，不可归类在他处者"
  },
  {
    value: "P08",
    label: "与孕期长和高出生体重有关的疾患"
  },
  {
    value: "P10",
    label: "由于产伤引起的颅内出血"
  },
  {
    value: "P11",
    label: "中枢神经系统的其他产伤"
  },
  {
    value: "P12",
    label: "头皮产伤"
  },
  {
    value: "P13",
    label: "骨骼的产伤"
  },
  {
    value: "P14",
    label: "周围神经系统的产伤"
  },
  {
    value: "P15",
    label: "其他产伤"
  },
  {
    value: "P20",
    label: "子宫内低氧症"
  },
  {
    value: "P21",
    label: "出生窒息"
  },
  {
    value: "P22",
    label: "新生儿呼吸窘迫"
  },
  {
    value: "P23",
    label: "先天性肺炎"
  },
  {
    value: "P24",
    label: "新生儿吸入综合征"
  },
  {
    value: "P25",
    label: "起源于围生期的间质肺气肿及有关情况"
  },
  {
    value: "P26",
    label: "起源于围生期的肺出血"
  },
  {
    value: "P27",
    label: "起源于围生期的慢性呼吸性疾病"
  },
  {
    value: "P28",
    label: "起源于围生期的其他呼吸性情况"
  },
  {
    value: "P29",
    label: "起源于围生期的心血管疾患"
  },
  {
    value: "P35",
    label: "先天性病毒性疾病"
  },
  {
    value: "P36",
    label: "新生儿细菌性脓毒症"
  },
  {
    value: "P37",
    label: "其他先天性传染病和寄生虫病"
  },
  {
    value: "P38",
    label: "新生儿脐炎，伴有或不伴有轻度"
  },
  {
    value: "P39",
    label: "特发于围生期的其他感染"
  },
  {
    value: "P50",
    label: "胎儿失血"
  },
  {
    value: "P51",
    label: "新生儿脐带出血"
  },
  {
    value: "P52",
    label: "胎儿和新生儿颅内非创伤性出血"
  },
  {
    value: "P53",
    label: "胎儿和新生儿出血性疾病"
  },
  {
    value: "P54",
    label: "新生儿其他出血"
  },
  {
    value: "P55",
    label: "胎儿和新生儿的溶血性疾病"
  },
  {
    value: "P56",
    label: "由于溶血性疾病引起的胎儿水肿"
  },
  {
    value: "P57",
    label: "核黄疸"
  },
  {
    value: "P58",
    label: "由于其他过度溶血引起的新生儿黄疸"
  },
  {
    value: "P59",
    label: "其他和未特指原因所致的新生儿黄疸"
  },
  {
    value: "P60",
    label: "胎儿和新生儿播散性血管内凝血"
  },
  {
    value: "P61",
    label: "其他围生期血液疾患"
  },
  {
    value: "P70",
    label: "特发于胎儿和新生儿的短暂性碳水化合物代谢紊乱"
  },
  {
    value: "P71",
    label: "短暂性新生儿钙和镁代谢疾患"
  },
  {
    value: "P72",
    label: "新生儿其他短暂性内分泌疾患"
  },
  {
    value: "P74",
    label: "新生儿其他短暂性电解质和代谢紊乱"
  },
  {
    value: "P75",
    label: "胎粪性肠梗阻"
  },
  {
    value: "P76",
    label: "新生儿其他肠梗阻"
  },
  {
    value: "P77",
    label: "胎儿和新生儿的坏死性小肠结肠炎"
  },
  {
    value: "P78",
    label: "其他围生期的消化系统疾患"
  },
  {
    value: "P80",
    label: "新生儿低温症"
  },
  {
    value: "P81",
    label: "新生儿其他体温调节障碍"
  },
  {
    value: "P83",
    label: "特发于胎儿和新生儿体被的其他情况"
  },
  {
    value: "P90",
    label: "新生儿抽搐"
  },
  {
    value: "P91",
    label: "新生儿的其他大脑障碍"
  },
  {
    value: "P92",
    label: "新生儿喂养问题"
  },
  {
    value: "P93",
    label: "胎儿和新生儿用药引起的反应和中毒"
  },
  {
    value: "P94",
    label: "新生儿肌张力疾患"
  },
  {
    value: "P95",
    label: "未特指原因的胎儿死亡"
  },
  {
    value: "P96",
    label: "起源于围生期的其他情况"
  },
  {
    value: "Q00",
    label: "无脑畸形和类似畸形"
  },
  {
    value: "Q01",
    label: "脑膨出"
  },
  {
    value: "Q02",
    label: "小头"
  },
  {
    value: "Q03",
    label: "先天性脑积水"
  },
  {
    value: "Q04",
    label: "脑的其他先天性畸形"
  },
  {
    value: "Q05",
    label: "脊柱裂"
  },
  {
    value: "Q06",
    label: "脊髓的其他先天性畸形"
  },
  {
    value: "Q07",
    label: "神经系统其他先天性畸形"
  },
  {
    value: "Q10",
    label: "眼睑、泪器和眼眶先天性畸形"
  },
  {
    value: "Q11",
    label: "无眼、小眼和巨眼"
  },
  {
    value: "Q12",
    label: "先天性晶状体畸形"
  },
  {
    value: "Q13",
    label: "眼前段先天性畸形"
  },
  {
    value: "Q14",
    label: "眼后段先天性畸形"
  },
  {
    value: "Q15",
    label: "眼的其他先天性畸形"
  },
  {
    value: "Q16",
    label: "引起听力缺陷的耳先天性畸形"
  },
  {
    value: "Q17",
    label: "耳的其他先天性畸形"
  },
  {
    value: "Q18",
    label: "面和颈部的其他先天性畸形"
  },
  {
    value: "Q20",
    label: "心腔和心连接的先天性畸形"
  },
  {
    value: "Q21",
    label: "心间隔先天性畸形"
  },
  {
    value: "Q22",
    label: "肺动脉瓣和三尖瓣先天性畸形"
  },
  {
    value: "Q23",
    label: "主动脉瓣和二尖瓣先天性畸形"
  },
  {
    value: "Q24",
    label: "心脏的其他先天性畸形"
  },
  {
    value: "Q25",
    label: "大动脉先天性畸形"
  },
  {
    value: "Q26",
    label: "大静脉先天性畸形"
  },
  {
    value: "Q27",
    label: "周围循环系统的其他先天性畸形"
  },
  {
    value: "Q28",
    label: "循环系统的其他先天性畸形"
  },
  {
    value: "Q30",
    label: "鼻先天性畸形"
  },
  {
    value: "Q31",
    label: "喉先天性畸形"
  },
  {
    value: "Q32",
    label: "气管和支气管先天性畸形"
  },
  {
    value: "Q33",
    label: "肺先天性畸形"
  },
  {
    value: "Q34",
    label: "呼吸系统的其他先天性畸形"
  },
  {
    value: "Q35",
    label: "腭裂"
  },
  {
    value: "Q36",
    label: "唇裂"
  },
  {
    value: "Q37",
    label: "腭裂，伴有唇裂"
  },
  {
    value: "Q38",
    label: "舌、口和咽的其他先天性畸形"
  },
  {
    value: "Q39",
    label: "食管先天性畸形"
  },
  {
    value: "Q40",
    label: "上消化道的其他先天性畸形"
  },
  {
    value: "Q41",
    label: "小肠先天性缺如、闭锁和狭窄"
  },
  {
    value: "Q42",
    label: "大肠先天性缺如、闭锁和狭窄"
  },
  {
    value: "Q43",
    label: "肠的其他先天性畸形"
  },
  {
    value: "Q44",
    label: "胆囊、胆管和肝先天性畸形"
  },
  {
    value: "Q45",
    label: "消化系统的其他先天性畸形"
  },
  {
    value: "Q50",
    label: "卵巢、输卵管和阔韧带先天性畸形"
  },
  {
    value: "Q51",
    label: "子宫和宫颈先天性畸形"
  },
  {
    value: "Q52",
    label: "女性生殖器的其他先天性畸形"
  },
  {
    value: "Q53",
    label: "睾丸未降"
  },
  {
    value: "Q54",
    label: "尿道下裂"
  },
  {
    value: "Q55",
    label: "男性生殖器官的其他先天性畸形"
  },
  {
    value: "Q56",
    label: "性别不清和假两性同体"
  },
  {
    value: "Q60",
    label: "肾缺如和肾的其他萎缩性缺陷"
  },
  {
    value: "Q61",
    label: "囊性肾病"
  },
  {
    value: "Q62",
    label: "肾盂的先天性梗阻性缺如和输尿管先天性畸形"
  },
  {
    value: "Q63",
    label: "肾的其他先天性畸形"
  },
  {
    value: "Q64",
    label: "泌尿系统的其他先天性畸形"
  },
  {
    value: "Q65",
    label: "髋先天性变形"
  },
  {
    value: "Q66",
    label: "足先天性变形"
  },
  {
    value: "Q67",
    label: "头、面、脊柱和胸的先天性肌肉骨骼变形"
  },
  {
    value: "Q68",
    label: "肌肉骨骼的其他先天性变形"
  },
  {
    value: "Q69",
    label: "多指[趾]"
  },
  {
    value: "Q70",
    label: "并指[趾]"
  },
  {
    value: "Q71",
    label: "上肢短缺缺陷"
  },
  {
    value: "Q72",
    label: "下肢短缺缺陷"
  },
  {
    value: "Q73",
    label: "未特指四肢的短缺缺陷"
  },
  {
    value: "Q74",
    label: "四肢的其他先天性畸形"
  },
  {
    value: "Q75",
    label: "颅和面骨的其他先天性畸形"
  },
  {
    value: "Q76",
    label: "脊柱及骨性胸廓先天性畸形"
  },
  {
    value: "Q77",
    label: "骨软骨发育不良，伴有管状骨和脊柱的发育缺陷"
  },
  {
    value: "Q78",
    label: "其他骨软骨发育不良"
  },
  {
    value: "Q79",
    label: "肌肉骨骼系统先天性畸形，不可归类在他处者"
  },
  {
    value: "Q80",
    label: "先天性鱼鳞病"
  },
  {
    value: "Q81",
    label: "大疱性表皮松懈"
  },
  {
    value: "Q82",
    label: "皮肤的其他先天性畸形"
  },
  {
    value: "Q83",
    label: "乳房先天性畸形"
  },
  {
    value: "Q84",
    label: "体被的其他先天性畸形"
  },
  {
    value: "Q85",
    label: "斑痣性错构瘤病，不可归类在他处者"
  },
  {
    value: "Q86",
    label: "由于已知的外源性原因引起的先天性畸形综合征，不可归类在他处者"
  },
  {
    value: "Q87",
    label: "影响多系统的其他特指先天性畸形综合征"
  },
  {
    value: "Q89",
    label: "其他先天性畸形，不可归类在他处者"
  },
  {
    value: "Q90",
    label: "唐氏综合征"
  },
  {
    value: "Q91",
    label: "爱德华兹综合征和帕套综合征"
  },
  {
    value: "Q92",
    label: "常染色体的其他三体和部分三体性，不可归类在他处者"
  },
  {
    value: "Q93",
    label: "常染色体的单体性和缺失，不可归类在他处者"
  },
  {
    value: "Q95",
    label: "平衡重排和结构标记，不可归类在他处者"
  },
  {
    value: "Q96",
    label: "先天性卵巢发育不全[特纳综合征]"
  },
  {
    value: "Q97",
    label: "其他的性染色体异常，女性表型，不可归类在他处者"
  },
  {
    value: "Q98",
    label: "其他的性染色体异常，男性表型，不可归类在他处者"
  },
  {
    value: "Q99",
    label: "其他染色体异常，不可归类在他处者"
  },
  {
    value: "R00",
    label: "心脏搏动异常"
  },
  {
    value: "R01",
    label: "心脏杂音和其他心音"
  },
  {
    value: "R02",
    label: "坏疽，不可归类在他处者"
  },
  {
    value: "R03",
    label: "血压读数异常，无诊断者"
  },
  {
    value: "R04",
    label: "呼吸道出血"
  },
  {
    value: "R05",
    label: "咳嗽"
  },
  {
    value: "R06",
    label: "呼吸异常"
  },
  {
    value: "R07",
    label: "咽痛和胸痛"
  },
  {
    value: "R09",
    label: "累及循环和呼吸系统的其他症状和体征"
  },
  {
    value: "R10",
    label: "腹部和盆腔痛"
  },
  {
    value: "R11",
    label: "恶心和呕吐"
  },
  {
    value: "R12",
    label: "胃灼热"
  },
  {
    value: "R13",
    label: "咽下困难"
  },
  {
    value: "R14",
    label: "胃肠气胀及有关情况"
  },
  {
    value: "R15",
    label: "大便失禁"
  },
  {
    value: "R16",
    label: "肝大和脾大，不可归类在他处者"
  },
  {
    value: "R17",
    label: "未特指的黄疸"
  },
  {
    value: "R18",
    label: "腹水"
  },
  {
    value: "R19",
    label: "累及消化系统和腹部的其他症状和体征"
  },
  {
    value: "R20",
    label: "皮肤感觉障碍"
  },
  {
    value: "R21",
    label: "皮疹和其他非特异性皮肤疹"
  },
  {
    value: "R22",
    label: "皮肤和皮下组织的局部肿胀、肿物和肿块"
  },
  {
    value: "R23",
    label: "其他皮肤改变"
  },
  {
    value: "R25",
    label: "异常的不随意运动"
  },
  {
    value: "R26",
    label: "步态和运动异常"
  },
  {
    value: "R27",
    label: "其他协调缺乏"
  },
  {
    value: "R29",
    label: "累及神经和肌肉骨骼系统的其他症状和体征"
  },
  {
    value: "R30",
    label: "与排尿有关的疼痛"
  },
  {
    value: "R31",
    label: "未特指的血尿"
  },
  {
    value: "R32",
    label: "未特指的尿失禁"
  },
  {
    value: "R33",
    label: "尿潴留"
  },
  {
    value: "R34",
    label: "无尿和少尿"
  },
  {
    value: "R35",
    label: "多尿"
  },
  {
    value: "R36",
    label: "尿道排出物"
  },
  {
    value: "R39",
    label: "累及泌尿系统的其他症状和体征"
  },
  {
    value: "R40",
    label: "木僵、嗜眠和昏迷"
  },
  {
    value: "R41",
    label: "累及认知功能和意识的其他症状和体征"
  },
  {
    value: "R42",
    label: "头晕和眩晕"
  },
  {
    value: "R43",
    label: "嗅觉和味觉障碍"
  },
  {
    value: "R44",
    label: "累及一般感觉和知觉的其他症状和体征"
  },
  {
    value: "R45",
    label: "累及情绪状态的症状和体征"
  },
  {
    value: "R46",
    label: "累及外貌与行为的症状和体征"
  },
  {
    value: "R47",
    label: "言语障碍，不可归类在他处者"
  },
  {
    value: "R48",
    label: "诵读困难和其他象征性机能障碍，不可归类在他处者"
  },
  {
    value: "R49",
    label: "语音障碍"
  },
  {
    value: "R50",
    label: "原因不明的发热"
  },
  {
    value: "R51",
    label: "头痛"
  },
  {
    value: "R52",
    label: "疼痛，不可归类在他处者"
  },
  {
    value: "R53",
    label: "不适和疲劳"
  },
  {
    value: "R54",
    label: "衰老"
  },
  {
    value: "R55",
    label: "晕厥和虚脱"
  },
  {
    value: "R56",
    label: "惊厥，不可归类在他处者"
  },
  {
    value: "R57",
    label: "休克，不可归类在他处者"
  },
  {
    value: "R58",
    label: "出血，不可归类在他处者"
  },
  {
    value: "R59",
    label: "淋巴结增大"
  },
  {
    value: "R60",
    label: "水肿，不可归类在他处者"
  },
  {
    value: "R61",
    label: "多汗症"
  },
  {
    value: "R62",
    label: "未达到预期正常生理发育水平"
  },
  {
    value: "R63",
    label: "有关食物和液体摄取的症状和体征"
  },
  {
    value: "R64",
    label: "恶病质"
  },
  {
    value: "R65",
    label: "全身炎症反应综合征 *"
  },
  {
    value: "R68",
    label: "其他的一般症状和体征"
  },
  {
    value: "R69",
    label: "原因不知和原因未特指的发病"
  },
  {
    value: "R70",
    label: "红细胞沉降率升高和血浆粘滞度异常"
  },
  {
    value: "R71",
    label: "红细胞异常"
  },
  {
    value: "R72",
    label: "白细胞异常，不可归类在他处者"
  },
  {
    value: "R73",
    label: "血糖水平升高"
  },
  {
    value: "R74",
    label: "血清酶水平异常"
  },
  {
    value: "R75",
    label: "人类免疫缺陷病毒[HIV]的实验室证据"
  },
  {
    value: "R76",
    label: "其他血清免疫学异常所见"
  },
  {
    value: "R77",
    label: "血浆蛋白的其他异常"
  },
  {
    value: "R78",
    label: "血中发现通常不出现的药物和其他物质"
  },
  {
    value: "R79",
    label: "血液化学的其他异常所见"
  },
  {
    value: "R80",
    label: "孤立性蛋白尿"
  },
  {
    value: "R81",
    label: "葡萄糖尿"
  },
  {
    value: "R82",
    label: "尿的其他异常所见"
  },
  {
    value: "R83",
    label: "脑脊液的异常所见"
  },
  {
    value: "R84",
    label: "呼吸器官和胸腔标本的异常所见"
  },
  {
    value: "R85",
    label: "消化器官和腹腔标本的异常所见"
  },
  {
    value: "R86",
    label: "男性生殖器官标本的异常所见"
  },
  {
    value: "R87",
    label: "女性生殖器官标本的异常所见"
  },
  {
    value: "R89",
    label: "其他器官、系统和组织标本的异常所见"
  },
  {
    value: "R90",
    label: "中枢神经系统诊断性影象检查的异常所见"
  },
  {
    value: "R91",
    label: "肺诊断性影像检查的异常所见"
  },
  {
    value: "R92",
    label: "乳房诊断性影像检查的异常所见"
  },
  {
    value: "R93",
    label: "R93.4其他身体结构诊断性影像检查的异常所见"
  },
  {
    value: "R94",
    label: "功能检查的异常结果"
  },
  {
    value: "R95",
    label: "婴儿猝死综合征"
  },
  {
    value: "R96",
    label: "其他猝死，原因不知"
  },
  {
    value: "R98",
    label: "无人在场的死亡"
  },
  {
    value: "R99",
    label: "其他原因不明确和未特指原因的死亡"
  },
  {
    value: "S00",
    label: "头部浅表损伤"
  },
  {
    value: "S01",
    label: "头部开放性伤口"
  },
  {
    value: "S02",
    label: "颅骨和面骨骨折"
  },
  {
    value: "S03",
    label: "头部的关节和韧带脱位、扭伤和劳损"
  },
  {
    value: "S04",
    label: "颅神经损伤"
  },
  {
    value: "S05",
    label: "眼和眶损伤"
  },
  {
    value: "S06",
    label: "颅内损伤"
  },
  {
    value: "S07",
    label: "头部挤压伤"
  },
  {
    value: "S08",
    label: "头的部分创伤性切断"
  },
  {
    value: "S09",
    label: "头部其他和未特指的损伤"
  },
  {
    value: "S10",
    label: "颈部浅表损伤"
  },
  {
    value: "S11",
    label: "颈部开放性伤口"
  },
  {
    value: "S12",
    label: "颈部骨折"
  },
  {
    value: "S13",
    label: "颈部水平的关节和韧带脱位、扭伤和劳损"
  },
  {
    value: "S14",
    label: "颈部水平的神经和脊髓损伤"
  },
  {
    value: "S15",
    label: "颈部水平的血管损伤"
  },
  {
    value: "S16",
    label: "颈部水平的肌肉和肌腱损伤"
  },
  {
    value: "S17",
    label: "颈部挤压伤"
  },
  {
    value: "S18",
    label: "颈部水平的创伤性切断"
  },
  {
    value: "S19",
    label: "颈部其他和未特指的损伤"
  },
  {
    value: "S20",
    label: "胸部浅表损伤"
  },
  {
    value: "S21",
    label: "胸部开放性伤口"
  },
  {
    value: "S22",
    label: "肋骨、胸骨和胸部脊柱骨折"
  },
  {
    value: "S23",
    label: "胸部的关节和韧带脱位、扭伤和劳损"
  },
  {
    value: "S24",
    label: "胸部水平的神经和脊髓损伤"
  },
  {
    value: "S25",
    label: "胸部血管损伤"
  },
  {
    value: "S26",
    label: "心脏损伤"
  },
  {
    value: "S27",
    label: "其他和未特指的胸内器官损伤"
  },
  {
    value: "S28",
    label: "胸部挤压伤和胸的部分创伤性切断"
  },
  {
    value: "S29",
    label: "胸部其他和未特指的损伤"
  },
  {
    value: "S30",
    label: "腹部、下背和骨盆浅表损伤"
  },
  {
    value: "S31",
    label: "腹部、下背和骨盆开放性伤口"
  },
  {
    value: "S32",
    label: "腰部脊柱和骨盆骨折"
  },
  {
    value: "S33",
    label: "腰部脊柱和骨盆的关节和韧带脱位、扭伤和劳损"
  },
  {
    value: "S34",
    label: "腰部、下背和骨盆水平的神经和腰部脊髓损伤"
  },
  {
    value: "S35",
    label: "腰部、下背和骨盆水平血管损伤"
  },
  {
    value: "S36",
    label: "腹内器官损伤"
  },
  {
    value: "S37",
    label: "盆腔器官损伤"
  },
  {
    value: "S38",
    label: "腹部、下背和骨盆的部分挤压伤和创伤性切断"
  },
  {
    value: "S39",
    label: "腹部、下背和骨盆其他和未特指的损伤"
  },
  {
    value: "S40",
    label: "肩和上臂浅表损伤"
  },
  {
    value: "S41",
    label: "肩和上臂开放性伤口"
  },
  {
    value: "S42",
    label: "肩和上臂骨折"
  },
  {
    value: "S43",
    label: "肩胛带的关节和韧带脱位、扭伤和劳损"
  },
  {
    value: "S44",
    label: "肩和上臂水平的神经损伤"
  },
  {
    value: "S45",
    label: "肩和上臂水平的血管损伤"
  },
  {
    value: "S46",
    label: "肩和上臂水平的肌肉和肌腱损伤"
  },
  {
    value: "S47",
    label: "肩和上臂水平挤压伤"
  },
  {
    value: "S48",
    label: "肩和上臂创伤性切断"
  },
  {
    value: "S49",
    label: "肩和上臂其他和未特指的损伤"
  },
  {
    value: "S50",
    label: "前臂浅表损伤"
  },
  {
    value: "S51",
    label: "前臂开放性伤口"
  },
  {
    value: "S52",
    label: "前臂骨折"
  },
  {
    value: "S53",
    label: "肘关节和韧带脱位、扭伤和劳损"
  },
  {
    value: "S54",
    label: "前臂水平的神经损伤"
  },
  {
    value: "S55",
    label: "前臂水平的血管损伤"
  },
  {
    value: "S56",
    label: "前臂水平的肌肉和肌腱损伤"
  },
  {
    value: "S57",
    label: "前臂挤压伤"
  },
  {
    value: "S58",
    label: "前臂创伤性切断"
  },
  {
    value: "S59",
    label: "前臂其他和未特指的损伤"
  },
  {
    value: "S60",
    label: "腕和手浅表损伤"
  },
  {
    value: "S61",
    label: "腕和手开放性伤口"
  },
  {
    value: "S62",
    label: "腕和手水平的骨折"
  },
  {
    value: "S63",
    label: "腕和手水平的关节和韧带脱位、扭伤和劳损"
  },
  {
    value: "S64",
    label: "腕和手水平的神经损伤"
  },
  {
    value: "S65",
    label: "腕和手水平的血管损伤"
  },
  {
    value: "S66",
    label: "腕和手水平的肌肉和肌腱损伤"
  },
  {
    value: "S67",
    label: "腕和手挤压伤"
  },
  {
    value: "S68",
    label: "腕和手创伤性切断"
  },
  {
    value: "S69",
    label: "腕和手其他和未特指的损伤"
  },
  {
    value: "S70",
    label: "髋和大腿浅表损伤"
  },
  {
    value: "S71",
    label: "髋和大腿开放性伤口"
  },
  {
    value: "S72",
    label: "股骨骨折"
  },
  {
    value: "S73",
    label: "髋的关节和韧带脱位、扭伤和劳损"
  },
  {
    value: "S74",
    label: "髋和大腿水平的神经损伤"
  },
  {
    value: "S75",
    label: "髋和大腿水平的血管损伤"
  },
  {
    value: "S76",
    label: "髋和大腿水平的肌肉和肌腱损伤"
  },
  {
    value: "S77",
    label: "髋和大腿挤压伤"
  },
  {
    value: "S78",
    label: "髋和大腿创伤性切断"
  },
  {
    value: "S79",
    label: "髋和大腿其他和未特指的损伤"
  },
  {
    value: "S80",
    label: "小腿浅表损伤"
  },
  {
    value: "S81",
    label: "小腿开放性伤口"
  },
  {
    value: "S82",
    label: "小腿骨折，包括踝"
  },
  {
    value: "S83",
    label: "膝的关节和韧带脱位、扭伤和劳损"
  },
  {
    value: "S84",
    label: "小腿水平的神经损伤"
  },
  {
    value: "S85",
    label: "小腿水平的血管损伤"
  },
  {
    value: "S86",
    label: "小腿水平的肌肉和肌腱损伤"
  },
  {
    value: "S87",
    label: "小腿挤压伤"
  },
  {
    value: "S88",
    label: "小腿创伤性切断"
  },
  {
    value: "S89",
    label: "小腿其他和未特指的损伤"
  },
  {
    value: "S90",
    label: "踝和足浅表损伤"
  },
  {
    value: "S91",
    label: "踝和足开放性伤口"
  },
  {
    value: "S92",
    label: "足骨折，除外踝"
  },
  {
    value: "S93",
    label: "踝和足水平的关节和韧带脱位、扭伤和劳损"
  },
  {
    value: "S94",
    label: "踝和足水平的神经损伤"
  },
  {
    value: "S95",
    label: "踝和足水平的血管损伤"
  },
  {
    value: "S96",
    label: "踝和足水平的肌肉和肌腱损伤"
  },
  {
    value: "S97",
    label: "踝和足挤压伤"
  },
  {
    value: "S98",
    label: "踝和足创伤性切断"
  },
  {
    value: "S99",
    label: "踝和足其他和未特指的损伤"
  },
  {
    value: "T00",
    label: "累及身体多个部位的浅表损伤"
  },
  {
    value: "T01",
    label: "累及身体多个部位的开放性伤口"
  },
  {
    value: "T02",
    label: "累及身体多个部位的骨折"
  },
  {
    value: "T03",
    label: "累及身体多个部位的脱位、扭伤和劳损"
  },
  {
    value: "T04",
    label: "累及身体多个部位的挤压伤"
  },
  {
    value: "T05",
    label: "累及身体多个部位的创伤性切断"
  },
  {
    value: "T06",
    label: "累及身体多个部位的其他损伤，不可归类在他处者"
  },
  {
    value: "T07",
    label: "未特指的多处损伤"
  },
  {
    value: "T08",
    label: "脊柱骨折，水平未特指"
  },
  {
    value: "T09",
    label: "脊柱和躯干的其他损伤，水平未特指"
  },
  {
    value: "T10",
    label: "上肢骨折，水平未特指"
  },
  {
    value: "T11",
    label: "上肢的其他损伤，水平未特指"
  },
  {
    value: "T12",
    label: "下肢骨折，水平未特指"
  },
  {
    value: "T13",
    label: "下肢的其他损伤，水平未特指"
  },
  {
    value: "T14",
    label: "身体未特指部位的损伤"
  },
  {
    value: "T15",
    label: "外眼异物"
  },
  {
    value: "T16",
    label: "耳内异物"
  },
  {
    value: "T17",
    label: "呼吸道内异物"
  },
  {
    value: "T18",
    label: "消化道内异物"
  },
  {
    value: "T19",
    label: "泌尿生殖道内异物"
  },
  {
    value: "T20",
    label: "头和颈烧伤和腐蚀伤"
  },
  {
    value: "T21",
    label: "躯干烧伤和腐蚀伤"
  },
  {
    value: "T22",
    label: "肩和上肢烧伤和腐蚀伤，除外腕和手"
  },
  {
    value: "T23",
    label: "腕和手烧伤和腐蚀伤"
  },
  {
    value: "T24",
    label: "髋和下肢烧伤和腐蚀伤 ，除外踝和足"
  },
  {
    value: "T25",
    label: "踝和足烧伤和腐蚀伤"
  },
  {
    value: "T26",
    label: "限于眼和附器的烧伤和腐蚀伤"
  },
  {
    value: "T27",
    label: "呼吸道烧伤和腐蚀伤"
  },
  {
    value: "T28",
    label: "其他内部器官的烧伤和腐蚀伤"
  },
  {
    value: "T29",
    label: "身体多个部位的烧伤和腐蚀伤"
  },
  {
    value: "T30",
    label: "烧伤和腐蚀伤，身体部位未特指"
  },
  {
    value: "T31",
    label: "根据体表累及范围分类的烧伤"
  },
  {
    value: "T32",
    label: "根据体表累及范围分类的腐蚀伤"
  },
  {
    value: "T33",
    label: "浅表冻伤"
  },
  {
    value: "T34",
    label: "冻伤，伴有组织坏死"
  },
  {
    value: "T35",
    label: "累及身体多个部位的冻伤和未特指的冻伤"
  },
  {
    value: "T36",
    label: "全身性抗生素中毒"
  },
  {
    value: "T37",
    label: "其他全身性抗感染药和抗寄生虫药中毒"
  },
  {
    value: "T38",
    label: "激素类及其合成代用品和拮抗剂中毒，不可归类在他处者"
  },
  {
    value: "T39",
    label: "非类鸦片镇痛药、退热药和抗风湿药中毒"
  },
  {
    value: "T40",
    label: "麻醉剂和致幻药[致幻剂]中毒"
  },
  {
    value: "T41",
    label: "麻醉药和治疗性气体中毒"
  },
  {
    value: "T42",
    label: "镇癫痫药、镇静-催眠剂和抗震颤麻痹药中毒"
  },
  {
    value: "T43",
    label: "对精神有影响的药物中毒，不可归类在他处者"
  },
  {
    value: "T44",
    label: "主要影响自主神经系统的药物中毒"
  },
  {
    value: "T45",
    label: "主要为全身性和血液学的制剂中毒，不可归类在他处者"
  },
  {
    value: "T46",
    label: "主要影响心血管系统的制剂中毒"
  },
  {
    value: "T47",
    label: "主要影响胃肠系统的制剂中毒"
  },
  {
    value: "T48",
    label: "主要作用于平滑肌和骨骼肌及呼吸系统的制剂中毒"
  },
  {
    value: "T49",
    label: "主要影响皮肤和粘膜的局部制剂及眼科、耳鼻喉科和牙科的药物中毒"
  },
  {
    value: "T50",
    label: "利尿剂和其他及未特指的药物、药剂和生物制品中毒"
  },
  {
    value: "T51",
    label: "酒精的毒性效应"
  },
  {
    value: "T52",
    label: "有机溶剂的毒性效应"
  },
  {
    value: "T53",
    label: "脂环烃和芳香族烃的卤素衍生物的毒性效应"
  },
  {
    value: "T54",
    label: "腐蚀性物质的毒性效应"
  },
  {
    value: "T55",
    label: "皂类和洗涤剂的毒性效应"
  },
  {
    value: "T56",
    label: "金属的毒性效应"
  },
  {
    value: "T57",
    label: "其他无机物质的毒性效应"
  },
  {
    value: "T58",
    label: "一氧化碳的毒性效应"
  },
  {
    value: "T59",
    label: "其他气体、烟雾和蒸气的毒性效应"
  },
  {
    value: "T60",
    label: "杀虫剂的毒性效应"
  },
  {
    value: "T61",
    label: "作为海味食入有害物质的毒性效应"
  },
  {
    value: "T62",
    label: "作为食物食入的其他有害物质的毒性效应"
  },
  {
    value: "T63",
    label: "与有毒动物接触的毒性效应"
  },
  {
    value: "T64",
    label: "黄曲霉素和其他霉菌毒素污染食物的毒性效应"
  },
  {
    value: "T65",
    label: "其他和未特指物质的毒性效应"
  },
  {
    value: "T66",
    label: "辐射的未特指效应"
  },
  {
    value: "T67",
    label: "热和光的效应"
  },
  {
    value: "T68",
    label: "低体温"
  },
  {
    value: "T69",
    label: "降温的其他效应"
  },
  {
    value: "T70",
    label: "气压和水压的效应"
  },
  {
    value: "T71",
    label: "窒息"
  },
  {
    value: "T73",
    label: "其他缺乏的效应"
  },
  {
    value: "T74",
    label: "虐待综合征"
  },
  {
    value: "T75",
    label: "其他外因的效应"
  },
  {
    value: "T78",
    label: "有害效应，不可归类在他处者"
  },
  {
    value: "T79",
    label: "创伤的某些早期并发症，不可归类在他处者"
  },
  {
    value: "T80",
    label: "输注、输血和治疗性注射后的并发症"
  },
  {
    value: "T81",
    label: "操作并发症，不可归类在他处者"
  },
  {
    value: "T82",
    label: "心脏和血管假体装置、植入物和移植物的并发症"
  },
  {
    value: "T83",
    label: "泌尿生殖系假体装置、植入物和移植物的并发症"
  },
  {
    value: "T84",
    label: "内部矫形外科假体装置、植入物和移植物的并发症"
  },
  {
    value: "T85",
    label: "其他内部假体装置、植入物和移植物的并发症"
  },
  {
    value: "T86",
    label: "移植器官和组织的失败和排斥"
  },
  {
    value: "T87",
    label: "再附着和截断术所特有的并发症"
  },
  {
    value: "T88",
    label: "手术和医疗的其他并发症，不可归类在他处者"
  },
  {
    value: "T90",
    label: "头部损伤后遗症"
  },
  {
    value: "T91",
    label: "颈和躯干损伤后遗症"
  },
  {
    value: "T92",
    label: "上肢损伤后遗症"
  },
  {
    value: "T93",
    label: "下肢损伤后遗症"
  },
  {
    value: "T94",
    label: "涉及多个和未特指身体部位损伤的后遗症"
  },
  {
    value: "T95",
    label: "烧伤、腐蚀伤和冻伤后遗症"
  },
  {
    value: "T96",
    label: "药物、药剂和生物制品中毒后遗症"
  },
  {
    value: "T97",
    label: "主要为非药用物质毒性效应的后遗症"
  },
  {
    value: "T98",
    label: "外因的其他和未特指效应的后遗症"
  },
  {
    value: "U04",
    label: "严重急性呼吸道综合症(SARS)"
  },
  {
    value: "V01",
    label: "行人在与脚踏车碰撞中的损伤"
  },
  {
    value: "V02",
    label: "行人在与两轮或三轮摩托车碰撞中的损伤"
  },
  {
    value: "V03",
    label: "行人在与小汽车、轻型货车或篷车碰撞中的损伤"
  },
  {
    value: "V04",
    label: "行人在与重型运输车或公共汽车碰撞中的损伤"
  },
  {
    value: "V05",
    label: "行人在与火车或铁路车辆碰撞中的损伤"
  },
  {
    value: "V06",
    label: "行人在与其他非机动车辆碰撞中的损伤"
  },
  {
    value: "V09",
    label: "行人在其他和未特指运输事故中的损伤"
  },
  {
    value: "V10",
    label: "骑脚踏车人员在脚踏车与行人或牲畜碰撞中的损伤"
  },
  {
    value: "V11",
    label: "骑脚踏车人员在脚踏车育其他脚踏车碰撞中的损伤"
  },
  {
    value: "V12",
    label: "骑脚踏车人员在脚踏车与两轮或三轮机动车碰撞中的损伤"
  },
  {
    value: "V13",
    label: "骑脚踏车人员在脚踏车与小汽车、轻型货车或篷车碰撞中的损伤"
  },
  {
    value: "V14",
    label: "骑脚踏车人员在脚踏车与重型运输车或公共汽车碰撞中的损伤"
  },
  {
    value: "V15",
    label: "骑脚踏车人员在脚踏车与火车或铁路车辆碰撞中的损伤"
  },
  {
    value: "V16",
    label: "骑脚踏车人员在脚踏车与其他非机动车辆碰撞中的损伤"
  },
  {
    value: "V17",
    label: "骑脚踏车人员在脚踏车与固定或静止物体碰撞中的损伤"
  },
  {
    value: "V18",
    label: "骑脚踏车人员在非碰撞性运输事故中的损伤"
  },
  {
    value: "V19",
    label: "骑脚踏车人员在其他和未特指的运输事故中的损伤"
  },
  {
    value: "V20",
    label: "骑摩托车人员在摩托车与行人或牲畜碰撞中的损伤"
  },
  {
    value: "V21",
    label: "骑摩托车人员在摩托车与脚踏车碰撞中的损伤"
  },
  {
    value: "V22",
    label: "骑摩托车人员在摩托车与两轮或三轮机动车碰撞中的损伤"
  },
  {
    value: "V23",
    label: "骑摩托车人员在摩托车与小汽车、轻型货车或篷车碰撞中的损伤"
  },
  {
    value: "V24",
    label: "骑摩托车人员在摩托车与重型运输车或公共汽车碰撞中的损伤"
  },
  {
    value: "V25",
    label: "骑摩托车人员在摩托车与火车或铁路车辆碰撞中的损伤"
  },
  {
    value: "V26",
    label: "骑摩托车人员在摩托车与其他非机动车辆碰撞中的损伤"
  },
  {
    value: "V27",
    label: "骑摩托车人员在摩托车与固定或静止物体碰撞中的损伤"
  },
  {
    value: "V28",
    label: "骑摩托车人员在非碰撞性运输事故中的损伤"
  },
  {
    value: "V29",
    label: "骑摩托车人员在其他和未特指的运输事故中的损伤"
  },
  {
    value: "V30",
    label: "三轮机动车乘员在三轮机动车与行人或牲畜碰撞中的损伤"
  },
  {
    value: "V31",
    label: "三轮机动车乘员在三轮机动车与脚踏车碰撞中的损伤"
  },
  {
    value: "V32",
    label: "三轮机动车乘员在三轮机动车与两轮或三轮机动车碰撞中的损伤"
  },
  {
    value: "V33",
    label: "三轮机动车乘员在三轮机动车与小汽车、轻型货车或篷车碰撞中的损伤"
  },
  {
    value: "V34",
    label: "三轮机动车乘员在三轮机动车与重型运输车或公共汽车碰撞中的损伤"
  },
  {
    value: "V35",
    label: "三轮机动车乘员在三轮机动车与火车或铁路车辆碰撞中的损伤"
  },
  {
    value: "V36",
    label: "三轮机动车乘员在三轮机动车与其他非机动车辆碰撞中的损伤"
  },
  {
    value: "V37",
    label: "三轮机动车乘员在三轮机动车与固定或静止物体碰撞中的损伤"
  },
  {
    value: "V38",
    label: "三轮机动车乘员在非碰撞性运输事故中的损伤"
  },
  {
    value: "V39",
    label: "三轮机动车乘员在其他和未特指的运输事故中的损伤"
  },
  {
    value: "V40",
    label: "小汽车乘员在小汽车与行人或牲畜碰撞中的损伤"
  },
  {
    value: "V41",
    label: "小汽车乘员在小汽车与脚踏车碰撞中的损伤"
  },
  {
    value: "V42",
    label: "小汽车乘员在小汽车与两轮或三轮机动车碰撞中的损伤"
  },
  {
    value: "V43",
    label: "小汽车乘员在小汽车与小汽车、轻型货车或篷车碰撞中的损伤"
  },
  {
    value: "V44",
    label: "小汽车乘员在小汽车与重型运输车或公共汽车碰撞中的损伤"
  },
  {
    value: "V45",
    label: "小汽车乘员在小汽车与火车或铁路车辆碰撞中的损伤"
  },
  {
    value: "V46",
    label: "小汽车乘员在小汽车与其他非机动车辆碰撞中的损伤"
  },
  {
    value: "V47",
    label: "小汽车乘员在小汽车与固定或静止物体碰撞中的损伤"
  },
  {
    value: "V48",
    label: "小汽车乘员在非碰撞性运输事故中的损伤"
  },
  {
    value: "V49",
    label: "小汽车乘员在其他和未特指的运输事故中的损伤"
  },
  {
    value: "V50",
    label: "轻型货车或蓬车乘员在轻型货车或篷车与行人或牲畜碰撞中的损伤"
  },
  {
    value: "V51",
    label: "轻型货车或篷车乘员在轻型货车或蓬车与脚踏车碰撞中的损伤"
  },
  {
    value: "V52",
    label: "轻型货车或篷车乘员在轻型货车或篷车与两轮或三轮机动车碰撞中的损伤"
  },
  {
    value: "V53",
    label: "轻型货车或篷车乘员在轻型货车或篷车与小汽车、轻型货车或蓬车碰撞中的损伤"
  },
  {
    value: "V54",
    label: "轻型货车或篷车乘员在轻型货车或篷车与重型运输车或公共汽车碰撞中的损伤"
  },
  {
    value: "V55",
    label: "轻型货车或篷车乘员在轻型货车或篷车与火车或铁路车辆碰撞中的损伤"
  },
  {
    value: "V56",
    label: "轻型货车或篷车乘员在轻型货车或篷车与其他非机动车辆碰撞中的损伤"
  },
  {
    value: "V57",
    label: "轻型货车或篷车乘员在轻型货车或篷车与固定或静止物体碰撞中的损伤"
  },
  {
    value: "V58",
    label: "轻型货车或篷车乘员在非碰撞性运输事故中的损伤"
  },
  {
    value: "V59",
    label: "轻型货车或篷车乘员在其他和未特指的运输事故中的损伤"
  },
  {
    value: "V60",
    label: "重型运输车乘员在重型运输车与行人或牲畜碰撞中的损伤"
  },
  {
    value: "V61",
    label: "重型运输车乘员在重型运输车与脚踏车碰撞中的损伤"
  },
  {
    value: "V62",
    label: "重型运输车乘员在重型运输车与两轮或三轮机动车碰撞中的损伤"
  },
  {
    value: "V63",
    label: "重型运输车乘员在重型运输车与小汽车、轻型货车或篷车碰撞中的损伤"
  },
  {
    value: "V64",
    label: "重型运输车乘员在重型运输车与重型运输车或公共汽车碰撞中的损伤"
  },
  {
    value: "V65",
    label: "重型运输车乘员在重型运输车与火车或铁路车辆碰撞中的损伤"
  },
  {
    value: "V66",
    label: "重型运输车乘员在重型运输车与其他非机动车辆碰撞中的损伤"
  },
  {
    value: "V67",
    label: "重型运输车乘员在重型运输车与固定或静止物体碰撞中的损伤"
  },
  {
    value: "V68",
    label: "重型运输车乘员在非碰撞性运输事故中的损伤"
  },
  {
    value: "V69",
    label: "重型运输车乘员在其他和未特指的运输事故中的损伤"
  },
  {
    value: "V70",
    label: "公共汽车乘员在公共汽车与行人或牲畜碰撞中的损伤"
  },
  {
    value: "V71",
    label: "公共汽车乘员在公共汽车与脚踏车碰撞中的损伤"
  },
  {
    value: "V72",
    label: "公共汽车乘员在公共汽车与两轮或三轮机动车碰撞中的损伤"
  },
  {
    value: "V73",
    label: "公共汽车乘员在公共汽车与小汽车、轻型货车或篷车碰撞中的损伤"
  },
  {
    value: "V74",
    label: "公共汽车乘员在公共汽车与重型运输车或公共汽车碰撞中的损伤"
  },
  {
    value: "V75",
    label: "公共汽车乘员在公共汽车与火车或铁路车辆碰撞中的损伤"
  },
  {
    value: "V76",
    label: "公共汽车乘员在公共汽车与其他非机动车辆碰撞中的损伤"
  },
  {
    value: "V77",
    label: "公共汽车乘员在公共汽车与固定或静止物体碰撞中的损伤"
  },
  {
    value: "V78",
    label: "公共汽车乘员在非碰撞性运输事故中的损伤"
  },
  {
    value: "V79",
    label: "公共汽车乘员在其他和未特指的运输事故中的损伤"
  },
  {
    value: "V80",
    label: "牲畜骑手或畜挽车辆乘员在运输事故中的损伤"
  },
  {
    value: "V81",
    label: "火车或铁路车辆乘员在运输事故中的损伤"
  },
  {
    value: "V82",
    label: "(市内有轨)电车乘员在运输事故中的损伤"
  },
  {
    value: "V83",
    label: "主要用于工业厂区的专用车辆上乘员在运输事故中的损伤"
  },
  {
    value: "V84",
    label: "主要用于农业的专用车辆上的乘员在运输事故中的损伤"
  },
  {
    value: "V85",
    label: "专用建筑车辆上乘员在运输事故中的损伤"
  },
  {
    value: "V86",
    label: "专用的全地带车辆或其他主要设计用于越野的机动车辆上的乘员在运输事故中的损伤"
  },
  {
    value: "V87",
    label: "特指类型的交通事故，但受害者的运输方式不明"
  },
  {
    value: "V88",
    label: "特指类型的非交通事故，但受害者的运输方式不明"
  },
  {
    value: "V89",
    label: "机动或非机动车辆事故，车辆类型未特指"
  },
  {
    value: "V90",
    label: "船舶事故引起的淹溺和沉没"
  },
  {
    value: "V91",
    label: "船舶事故引起的其他损伤"
  },
  {
    value: "V92",
    label: "与水上运输有关的非船舶事故的淹溺和沉没"
  },
  {
    value: "V93",
    label: "非船舶事故的船上事故，未引起淹溺和沉没"
  },
  {
    value: "V94",
    label: "其他和未特指的水上运输事故"
  },
  {
    value: "V95",
    label: "动力飞行器事故引起乘员损伤"
  },
  {
    value: "V96",
    label: "无动力飞行器事故引起乘员损伤"
  },
  {
    value: "V97",
    label: "其他特指的空中运输事故"
  },
  {
    value: "V98",
    label: "其他特指的运输事故"
  },
  {
    value: "V99",
    label: "未特指的运输事故"
  },
  {
    value: "W00",
    label: "在涉及冰和雪的同一平面上跌倒"
  },
  {
    value: "W01",
    label: "在同一平面上滑倒、绊倒和摔倒"
  },
  {
    value: "W02",
    label: "涉及溜冰、滑雪、溜旱冰或滑板时的跌倒"
  },
  {
    value: "W03",
    label: "由于被别人碰撞或推动引起的在同一平面上的其他跌倒"
  },
  {
    value: "W04",
    label: "在被他人运送或搀扶时的跌倒"
  },
  {
    value: "W05",
    label: "涉及轮椅上的跌落"
  },
  {
    value: "W06",
    label: "涉及床上的跌落"
  },
  {
    value: "W07",
    label: "涉及椅子上的跌落"
  },
  {
    value: "W08",
    label: "涉及其他家具上的跌落"
  },
  {
    value: "W09",
    label: "涉及运动场设施上的跌落"
  },
  {
    value: "W10",
    label: "在楼梯或台阶上跌倒和跌落"
  },
  {
    value: "W11",
    label: "在梯子上跌倒和跌落"
  },
  {
    value: "W12",
    label: "在脚手架上跌倒和跌落"
  },
  {
    value: "W13",
    label: "从房屋或建筑结构上跌落或跌出"
  },
  {
    value: "W14",
    label: "从树上跌落"
  },
  {
    value: "W15",
    label: "从悬崖上跌落"
  },
  {
    value: "W16",
    label: "潜水或跳水引起的损伤，除外淹溺和沉没"
  },
  {
    value: "W17",
    label: "从一个平面至另一平面的其他跌落"
  },
  {
    value: "W18",
    label: "在同一平面的其他跌倒"
  },
  {
    value: "W19",
    label: "未特指的跌倒"
  },
  {
    value: "W20",
    label: "被投掷、抛出或坠落物体击中"
  },
  {
    value: "W21",
    label: "撞击体育设施上或被体育设施击中"
  },
  {
    value: "W22",
    label: "撞击其他物体上或被其他物体击中"
  },
  {
    value: "W23",
    label: "被物体钩住、挤压、轧住或夹住"
  },
  {
    value: "W24",
    label: "接触升降和传送装置，不可归类在他处者"
  },
  {
    value: "W25",
    label: "接触锋利的玻璃"
  },
  {
    value: "W26",
    label: "接触刀、剑或匕首"
  },
  {
    value: "W27",
    label: "接触无动力手工工具"
  },
  {
    value: "W28",
    label: "接触动力割草机"
  },
  {
    value: "W29",
    label: "接触其他动力手工工具和家用机械"
  },
  {
    value: "W30",
    label: "接触农业机械"
  },
  {
    value: "W31",
    label: "接触其他和未特指的机械"
  },
  {
    value: "W32",
    label: "手枪发射"
  },
  {
    value: "W33",
    label: "步枪、猎枪和较大火器发射"
  },
  {
    value: "W34",
    label: "其他和未特指的火器发射"
  },
  {
    value: "W35",
    label: "锅炉爆炸和破裂"
  },
  {
    value: "W36",
    label: "高压气罐爆炸和破裂"
  },
  {
    value: "W37",
    label: "压缩轮胎、管子和软管爆炸和破裂"
  },
  {
    value: "W38",
    label: "其他特指压缩装置的爆炸和破裂"
  },
  {
    value: "W39",
    label: "烟火发射"
  },
  {
    value: "W40",
    label: "其他材料爆炸"
  },
  {
    value: "W41",
    label: "暴露于高压喷射下"
  },
  {
    value: "W42",
    label: "暴露于噪音下"
  },
  {
    value: "W43",
    label: "暴露于振动下"
  },
  {
    value: "W44",
    label: "异物进入或穿入眼或自然腔口"
  },
  {
    value: "W45",
    label: "异物或物体经皮肤进入"
  },
  {
    value: "W46",
    label: "接触皮下注射器针头  *"
  },
  {
    value: "W49",
    label: "暴露于其他和未特指的无生命机械性力量下"
  },
  {
    value: "W50",
    label: "被别人殴打、踢、拧、咬或抓伤"
  },
  {
    value: "W51",
    label: "撞到别人或意外被别人碰撞"
  },
  {
    value: "W52",
    label: "被蜂拥人群挤压、推挤或踏踩"
  },
  {
    value: "W53",
    label: "被鼠咬伤"
  },
  {
    value: "W54",
    label: "被狗咬伤或抓伤"
  },
  {
    value: "W55",
    label: "被其他哺乳动物咬伤或抓伤"
  },
  {
    value: "W56",
    label: "接触海生动物的损伤"
  },
  {
    value: "W57",
    label: "被无毒昆虫和其他无毒节肢动物咬伤或螫伤"
  },
  {
    value: "W58",
    label: "被鳄鱼或短吻鳄咬伤或抓伤"
  },
  {
    value: "W59",
    label: "被其他爬行动物咬伤或压伤"
  },
  {
    value: "W60",
    label: "接触植物荆棘和刺以及锐利叶片的损伤"
  },
  {
    value: "W64",
    label: "暴露于其他和未特指的有生命机械性力量下"
  },
  {
    value: "W65",
    label: "在浴盆内淹溺和沉没"
  },
  {
    value: "W66",
    label: "落入浴盆后淹溺和沉没"
  },
  {
    value: "W67",
    label: "在游泳池中淹溺和沉没"
  },
  {
    value: "W68",
    label: "落入游泳池后淹溺和沉没"
  },
  {
    value: "W69",
    label: "在自然水域中淹溺和沉没"
  },
  {
    value: "W70",
    label: "落入自然水域后淹溺和沉没"
  },
  {
    value: "W73",
    label: "其他特指的淹溺和沉没"
  },
  {
    value: "W74",
    label: "未特指的淹溺和沉没"
  },
  {
    value: "W75",
    label: "在床上意外窒息和绞窄"
  },
  {
    value: "W76",
    label: "其他意外悬吊和绞窄"
  },
  {
    value: "W77",
    label: "由于塌方、坠落土块和其他物质引起对呼吸的威胁"
  },
  {
    value: "W78",
    label: "吸入胃内容物"
  },
  {
    value: "W79",
    label: "吸入或咽下食物引起的呼吸道梗阻"
  },
  {
    value: "W80",
    label: "吸入和咽下其他物体引起的呼吸道梗阻"
  },
  {
    value: "W81",
    label: "被封闭于或陷入低氧环境"
  },
  {
    value: "W83",
    label: "其他特指的对呼吸的威胁"
  },
  {
    value: "W84",
    label: "未特指的对呼吸的威胁"
  },
  {
    value: "W85",
    label: "暴露于输电线路下"
  },
  {
    value: "W86",
    label: "暴露于其他特指的电流下"
  },
  {
    value: "W87",
    label: "暴露于未特指的电流下"
  },
  {
    value: "W88",
    label: "暴露于电离辐射下"
  },
  {
    value: "W89",
    label: "暴露于人造可见光和紫外线下"
  },
  {
    value: "W90",
    label: "暴露于其他非电离辐射下"
  },
  {
    value: "W91",
    label: "暴露于未特指类型的辐射下"
  },
  {
    value: "W92",
    label: "暴露于人为原因的过热环境下"
  },
  {
    value: "W93",
    label: "暴露于人为原因的过冷环境下"
  },
  {
    value: "W94",
    label: "暴露于高气压、低气压和气压改变环境下"
  },
  {
    value: "W99",
    label: "暴露于其他和未特指的人为环境因素下"
  },
  {
    value: "X00",
    label: "暴露于房屋或建筑结构内的无控制性火焰下"
  },
  {
    value: "X01",
    label: "暴露于房屋或建筑结构外的无控制性火焰下"
  },
  {
    value: "X02",
    label: "暴露于房屋或建筑结构内的控制性火焰下"
  },
  {
    value: "X03",
    label: "暴露于房屋或建筑结构外的控制性火焰下"
  },
  {
    value: "X04",
    label: "暴露于高度易燃材料的起火下"
  },
  {
    value: "X05",
    label: "暴露于睡衣的起火或焚毁下"
  },
  {
    value: "X06",
    label: "暴露于其他衣着用品和装饰品的起火或焚毁下"
  },
  {
    value: "X08",
    label: "暴露于其他特指的烟、火和火焰下"
  },
  {
    value: "X09",
    label: "暴露于未特指的烟、火和火焰下"
  },
  {
    value: "X10",
    label: "接触热饮料、食物和动植物油"
  },
  {
    value: "X11",
    label: "接触热自来水"
  },
  {
    value: "X12",
    label: "接触其他热液体"
  },
  {
    value: "X13",
    label: "接触蒸气和热蒸气"
  },
  {
    value: "X14",
    label: "接触热空气和气体"
  },
  {
    value: "X15",
    label: "接触热的家用器具"
  },
  {
    value: "X16",
    label: "接触热的取暖器具、散热器和管"
  },
  {
    value: "X17",
    label: "接触热的发动机、机械和工具"
  },
  {
    value: "X18",
    label: "接触其他热的金属"
  },
  {
    value: "X19",
    label: "接触其他和未特指的热和烫的物质"
  },
  {
    value: "X20",
    label: "接触毒蛇和晰蜴"
  },
  {
    value: "X21",
    label: "接触毒蜘蛛"
  },
  {
    value: "X22",
    label: "接触蝎子"
  },
  {
    value: "X23",
    label: "接触大黄蜂、黄蜂和蜜蜂"
  },
  {
    value: "X24",
    label: "接触蜈蚣和（热带）有毒的千足虫"
  },
  {
    value: "X25",
    label: "接触其他特指的有毒的节肢动物"
  },
  {
    value: "X26",
    label: "接触有毒的海生动物和植物"
  },
  {
    value: "X27",
    label: "接触其他特指的有毒动物"
  },
  {
    value: "X28",
    label: "接触其他特指的有毒植物"
  },
  {
    value: "X29",
    label: "接触未特指的有毒动物或植物"
  },
  {
    value: "X30",
    label: "暴露于过度自然热下"
  },
  {
    value: "X31",
    label: "暴露于过度自然冷下"
  },
  {
    value: "X32",
    label: "暴露于阳光下"
  },
  {
    value: "X33",
    label: "闪电的受害者"
  },
  {
    value: "X34",
    label: "地震受害者"
  },
  {
    value: "X35",
    label: "火山爆发受害者"
  },
  {
    value: "X36",
    label: "雪崩、山崩和其他地面运动受害者"
  },
  {
    value: "X37",
    label: "灾难性暴风雨受害者"
  },
  {
    value: "X38",
    label: "洪水受害者"
  },
  {
    value: "X39",
    label: "暴露于其他和未特指的自然力量下"
  },
  {
    value: "X40",
    label: "非类鸦片镇痛药、退热药和抗风湿药的意外中毒及暴露于该类药物"
  },
  {
    value: "X41",
    label: "镇癫痫药、镇静－催眠剂、抗震颤麻痹药和对精神有影响的药物的意外中毒及暴露于该类药物，不可归类在他处者"
  },
  {
    value: "X42",
    label: "麻醉剂和致幻药［致幻剂］意外中毒及暴露于该类药物，不可归类在他处者"
  },
  {
    value: "X43",
    label: "作用于自主神经系统的其他药物的意外中毒及暴露于该类药物"
  },
  {
    value: "X44",
    label: "其他和未特指的药物、药剂和生物制品的意外中毒及暴露于该类物质"
  },
  {
    value: "X45",
    label: "酒精的意外中毒及暴露于酒精"
  },
  {
    value: "X46",
    label: "有机溶剂和卤素烃及其蒸气的意外中毒及暴露于该类物质"
  },
  {
    value: "X47",
    label: "其他气体和蒸气的意外中毒及暴露于该类物质"
  },
  {
    value: "X48",
    label: "杀虫剂的意外中毒及暴露于杀虫剂"
  },
  {
    value: "X49",
    label: "其他和未特指的化学制品和有害物质的意外中毒及暴露于该类物质"
  },
  {
    value: "X50",
    label: "操劳过度和剧烈或重复运动"
  },
  {
    value: "X51",
    label: "旅行和运动"
  },
  {
    value: "X52",
    label: "长期滞留在失重环境下"
  },
  {
    value: "X53",
    label: "食物缺乏"
  },
  {
    value: "X54",
    label: "水缺乏"
  },
  {
    value: "X57",
    label: "未特指的贫困"
  },
  {
    value: "X58",
    label: "暴露于其他特指的因素下"
  },
  {
    value: "X59",
    label: "暴露于未特指的因素下"
  },
  {
    value: "X60",
    label: "非类鸦片镇痛药、退热药和抗风湿药的故意自毒及暴露于该类药物"
  },
  {
    value: "X61",
    label: "镇癫痫药、镇静－催眠剂、抗震颤麻痹药和对精神有影响的药物的故意自毒及暴露于该类药"
  },
  {
    value: "X62",
    label: "麻醉剂和致幻药［致幻剂］故意自毒及暴露于该类药物，不可归类在他处者"
  },
  {
    value: "X63",
    label: "作用于自主神经系统的其他药物的故意自毒及暴露于该类药物"
  },
  {
    value: "X64",
    label: "其他和未特指的药物、药剂和生物制品的故意自毒及暴露于该类药物"
  },
  {
    value: "X65",
    label: "酒精的故意自毒及暴露于酒精"
  },
  {
    value: "X66",
    label: "有机溶剂和卤素烃及其蒸气的故意自毒及暴露于该类物质"
  },
  {
    value: "X67",
    label: "其他气体和蒸气的故意自毒及暴露于该类物质"
  },
  {
    value: "X68",
    label: "杀虫剂的故意自毒及暴露于杀虫剂"
  },
  {
    value: "X69",
    label: "其他和未特指的化学制品和有害物质的故意自毒及暴露于该类物质"
  },
  {
    value: "X70",
    label: "用悬吊、绞勒和窒息方式故意自害"
  },
  {
    value: "X71",
    label: "用淹溺和沉没方式故意自害"
  },
  {
    value: "X72",
    label: "用手枪发射方式故意自害"
  },
  {
    value: "X73",
    label: "用步枪、猎枪和大型火器发射方式故意自害"
  },
  {
    value: "X74",
    label: "用其他和未特指的火器发射方式故意自害"
  },
  {
    value: "X75",
    label: "用爆炸物方式故意自害"
  },
  {
    value: "X76",
    label: "用烟、火和火焰方式故意自害"
  },
  {
    value: "X77",
    label: "用蒸气、热气和热物体方式故意自害"
  },
  {
    value: "X78",
    label: "用尖锐物体方式故意自害"
  },
  {
    value: "X79",
    label: "用钝器方式故意自害"
  },
  {
    value: "X80",
    label: "用从高处跳下方式故意自害"
  },
  {
    value: "X81",
    label: "用跳下或躺倒在移动物体前的方式故意自害"
  },
  {
    value: "X82",
    label: "用机动车辆碰撞方式故意自害"
  },
  {
    value: "X83",
    label: "用其他特指的方式故意自害"
  },
  {
    value: "X84",
    label: "用未特指的方式故意自害"
  },
  {
    value: "X85",
    label: "用药物、药剂和生物制品进行加害"
  },
  {
    value: "X86",
    label: "用腐蚀性物质进行加害"
  },
  {
    value: "X87",
    label: "用杀虫剂进行加害"
  },
  {
    value: "X88",
    label: "用气体和蒸气进行加害"
  },
  {
    value: "X89",
    label: "用其他特指的化学制品和有害物质进行加害"
  },
  {
    value: "X90",
    label: "用未特指的化学制品或有害物质进行加害"
  },
  {
    value: "X91",
    label: "用悬吊、绞勒和窒息进行加害"
  },
  {
    value: "X92",
    label: "用淹溺和沉没进行加害"
  },
  {
    value: "X93",
    label: "用手枪发射进行加害"
  },
  {
    value: "X94",
    label: "用步枪、猎枪和大型火器发射进行加害"
  },
  {
    value: "X95",
    label: "用其他和未特指的火器发射进行加害"
  },
  {
    value: "X96",
    label: "用爆炸物进行加害"
  },
  {
    value: "X97",
    label: "用烟、火和火焰进行加害"
  },
  {
    value: "X98",
    label: "用蒸气、热气和热物体进行加害"
  },
  {
    value: "X99",
    label: "用尖锐物体进行加害"
  },
  {
    value: "Y00",
    label: "用钝器进行加害"
  },
  {
    value: "Y01",
    label: "用从高处推下进行加害"
  },
  {
    value: "Y02",
    label: "用将受害者推向或放置在移动物体前进行加害"
  },
  {
    value: "Y03",
    label: "用机动车辆碰撞进行加害"
  },
  {
    value: "Y04",
    label: "用暴力进行加害"
  },
  {
    value: "Y05",
    label: "暴力的性加害"
  },
  {
    value: "Y06",
    label: "被忽视照料和遗弃"
  },
  {
    value: "Y07",
    label: "其他虐待综合征"
  },
  {
    value: "Y08",
    label: "用其他特指的手段进行加害"
  },
  {
    value: "Y09",
    label: "用未特指的手段进行加害"
  },
  {
    value: "Y10",
    label: "非类鸦片镇痛药、退热药和抗风湿药的中毒及暴露于该类药物，意图不确定的"
  },
  {
    value: "Y11",
    label: "镇癫痫药、镇静－催眠剂、抗震颤麻痹药和对精神有影响的药物的中毒及暴露于该类药物，意图不确定的"
  },
  {
    value: "Y12",
    label: "麻醉剂和致幻药［致幻剂］的中毒及暴露于该类药物，不可归类在他处，意图不确定的"
  },
  {
    value: "Y13",
    label: "作用于自主神经系统的其他药物的中毒及暴露于该类药物，意图不确定的"
  },
  {
    value: "Y14",
    label: "其他和未特指的药物、药剂和生物制品的中毒及暴露于该类药物，意图不确定的"
  },
  {
    value: "Y15",
    label: "酒精中毒及暴露于酒精，意图不确定的"
  },
  {
    value: "Y16",
    label: "有机溶剂和卤素烃及其蒸气的中毒及暴露于该类物质，意图不确定的"
  },
  {
    value: "Y17",
    label: "其他气体和蒸气的中毒及暴露于该类物质，意图不确定的"
  },
  {
    value: "Y18",
    label: "杀虫剂的中毒及暴露于杀虫剂，意图不确定的"
  },
  {
    value: "Y19",
    label: "其他和未特指的化学制品和有害物质的中毒及暴露于该类物质，意图不确定的"
  },
  {
    value: "Y20",
    label: "悬吊、绞勒和窒息，意图不确定的"
  },
  {
    value: "Y21",
    label: "淹溺和沉没，意图不确定的"
  },
  {
    value: "Y22",
    label: "手枪发射，意图不确定的"
  },
  {
    value: "Y23",
    label: "步枪、猎枪和大型火器发射，意图不确定的"
  },
  {
    value: "Y24",
    label: "其他和未特指的火器发射，意图不确定的"
  },
  {
    value: "Y25",
    label: "接触爆炸物，意图不确定的"
  },
  {
    value: "Y26",
    label: "暴露于烟、火和火焰下，意图不确定的"
  },
  {
    value: "Y27",
    label: "接触蒸气、热气和热物体，意图不确定的"
  },
  {
    value: "Y28",
    label: "接触尖锐物体，意图不确定的"
  },
  {
    value: "Y29",
    label: "接触钝器，意图不确定的"
  },
  {
    value: "Y30",
    label: "从高处跌落、跳下或被推下，意图不确定的"
  },
  {
    value: "Y31",
    label: "在移动物体前跌倒、躺卧或跑动以及进入移动物体，意图不确定的"
  },
  {
    value: "Y32",
    label: "机动车辆的碰撞，意图不确定的"
  },
  {
    value: "Y33",
    label: "其他特指的事件，意图不确定的"
  },
  {
    value: "Y34",
    label: "未特指的事件，意图不确定的"
  },
  {
    value: "Y35",
    label: "依法处置"
  },
  {
    value: "Y36",
    label: "作战行动"
  },
  {
    value: "Y40",
    label: "全身性抗生素"
  },
  {
    value: "Y41",
    label: "其他全身性抗感染药和抗寄生虫药"
  },
  {
    value: "Y42",
    label: "激素类及其合成代用品和拮抗药，不可归类在他处者"
  },
  {
    value: "Y43",
    label: "主要为全身性制剂"
  },
  {
    value: "Y44",
    label: "主要影响血液组成成分的制剂"
  },
  {
    value: "Y45",
    label: "镇痛药、退热药和消炎药"
  },
  {
    value: "Y46",
    label: "镇癫痫药和抗震颤麻痹药"
  },
  {
    value: "Y47",
    label: "镇静剂、催眠药和抗焦虑药"
  },
  {
    value: "Y48",
    label: "麻醉药和治疗性气体"
  },
  {
    value: "Y49",
    label: "对精神有影响的药物，不可归类在他处者"
  },
  {
    value: "Y50",
    label: "中枢神经系统兴奋剂，不可归类在他处者"
  },
  {
    value: "Y51",
    label: "主要影响自主神经系统的药物"
  },
  {
    value: "Y52",
    label: "主要影响心血管系统的制剂"
  },
  {
    value: "Y53",
    label: "主要影响胃肠系统的制剂"
  },
  {
    value: "Y54",
    label: "主要影响水平衡和矿物质及尿酸代谢的制剂"
  },
  {
    value: "Y55",
    label: "主要作用于平滑肌和骨骼肌及呼吸系统的制剂"
  },
  {
    value: "Y56",
    label: "主要影响皮肤和粘膜的局部制剂及眼科、耳鼻喉科和牙科的药物"
  },
  {
    value: "Y57",
    label: "其他和未特指的药物和药剂"
  },
  {
    value: "Y58",
    label: "细菌疫苗类"
  },
  {
    value: "Y59",
    label: "其他和未特指的疫苗类和生物制品"
  },
  {
    value: "Y60",
    label: "在手术和医疗中非故意的切割、针刺、穿孔或出血"
  },
  {
    value: "Y61",
    label: "在手术和医疗中异物意外地遗留在体内"
  },
  {
    value: "Y62",
    label: "在手术和医疗中无菌预防措施的失败"
  },
  {
    value: "Y63",
    label: "在手术和医疗中使用剂量不当"
  },
  {
    value: "Y64",
    label: "医疗或生物材料被污染"
  },
  {
    value: "Y65",
    label: "在手术和医疗中的其他意外事故"
  },
  {
    value: "Y66",
    label: "未给予手术和医疗"
  },
  {
    value: "Y69",
    label: "在手术和医疗中未特指的意外事故"
  },
  {
    value: "Y70",
    label: "与有害事件有关的麻醉装置"
  },
  {
    value: "Y71",
    label: "与有害事件有关的心血管装置"
  },
  {
    value: "Y72",
    label: "与有害事件有关的耳鼻喉科装置"
  },
  {
    value: "Y73",
    label: "与有害事件有关的胃肠病学和泌尿科装置"
  },
  {
    value: "Y74",
    label: "与有害事件有关的综合医院和个人使用的装置"
  },
  {
    value: "Y75",
    label: "与有害事件有关的神经科装置"
  },
  {
    value: "Y76",
    label: "与有害事件有关的妇产科装置"
  },
  {
    value: "Y77",
    label: "与有害事件有关的眼科装置"
  },
  {
    value: "Y78",
    label: "与有害事件有关的放射学装置"
  },
  {
    value: "Y79",
    label: "与有害事件有关的矫形外科装置"
  },
  {
    value: "Y80",
    label: "与有害事件有关的理疗装置"
  },
  {
    value: "Y81",
    label: "与有害事件有关的普通外科和整形外科装置"
  },
  {
    value: "Y82",
    label: "与有害事件有关的其他和未特指的医疗装置"
  },
  {
    value: "Y83",
    label: "外科手术和其他外科操作作为病人异常反应或以后并发症的原因，而在操作当时并未提及意外事故"
  },
  {
    value: "Y84",
    label: "其他医疗操作作为病人异常反应或以后并发症的原因，而在操作当时并未提及意外事故"
  },
  {
    value: "Y85",
    label: "运输事故的后遗症"
  },
  {
    value: "Y86",
    label: "其他事故的后遗症"
  },
  {
    value: "Y87",
    label: "故意自害、加害和意图不确定事件的后遗症"
  },
  {
    value: "Y88",
    label: "手术和医疗作为外因的后遗症"
  },
  {
    value: "Y89",
    label: "其他外因的后遗症"
  },
  {
    value: "Y90",
    label: "测定血中酒精水平以获得酒精影响的证据"
  },
  {
    value: "Y91",
    label: "测定中毒水平以获得酒精影响的证据"
  },
  {
    value: "Y95",
    label: "医源性情况"
  },
  {
    value: "Y96",
    label: "与工作有关的情况"
  },
  {
    value: "Y97",
    label: "与环境污染有关的情况"
  },
  {
    value: "Y98",
    label: "与生活方式有关的情况"
  }
]
