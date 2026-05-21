package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.mapper.QuestionnaireConfigMapper;
import cn.luyou.model.QuestionnaireConfig;
import cn.luyou.model.ScreeningSchool;
import cn.luyou.model.dto.QuestionnaireFieldDTO;
import cn.luyou.model.dto.QuestionnaireFieldGroupDTO;
import cn.luyou.model.dto.ShowWhenDTO;
import cn.luyou.model.vo.QuestionnaireConfigVO;
import cn.luyou.service.QuestionnaireService;
import cn.luyou.service.ScreeningSchoolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionnaireServiceImpl extends ServiceImpl<QuestionnaireConfigMapper, QuestionnaireConfig>
        implements QuestionnaireService {

    private static final String UPLOAD_BATCH_PREFIX = "questionnaire";

    private static final java.util.Set<String> ALLOWED_FIELD_KEYS = java.util.Set.of(
            "year", "city", "district", "name", "gender", "birthDate", "age", "idType", "idNumber",
            "ethnicity", "phone", "householdAddress", "currentAddress", "schoolType", "schoolName",
            "className", "tbHistory", "closeContactHistory", "suspiciousSymptoms", "hasInfectionScreen",
            "screenDate", "screenMethod", "screenResult", "infectionResult", "hasChestXray",
            "chestXrayDate", "chestXrayResult", "remark"
    );

    private final ScreeningSchoolService screeningSchoolService;

    @Override
    public QuestionnaireConfigVO getConfig(String code) {
        QuestionnaireConfig config = getOrInitConfig(code);
        return toVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(String code, QuestionnaireConfigVO vo) {
        if (vo.getGroups() == null || vo.getGroups().isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "问卷字段不能为空");
        }
        validateFieldGroups(vo.getGroups());
        QuestionnaireConfig config = getOrInitConfig(code);
        if (StrUtil.isNotBlank(vo.getTitle())) {
            config.setTitle(vo.getTitle());
        }
        if (vo.getSubtitle() != null) {
            config.setSubtitle(vo.getSubtitle());
        }
        if (vo.getEnabled() != null) {
            config.setEnabled(vo.getEnabled() ? 1 : 0);
        }
        config.setFieldsJson(JSONUtil.toJsonStr(vo.getGroups()));
        updateById(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateEnabled(String code, boolean enabled) {
        QuestionnaireConfig config = getOrInitConfig(code);
        config.setEnabled(enabled ? 1 : 0);
        updateById(config);
    }

    @Override
    public QuestionnaireConfigVO getPublicConfig(String code) {
        QuestionnaireConfig config = getOrInitConfig(code);
        QuestionnaireConfigVO vo = toVO(config);
        if (!Boolean.TRUE.equals(vo.getEnabled())) {
            vo.setGroups(List.of());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submit(String code, Map<String, Object> formData) {
        QuestionnaireConfig config = getOrInitConfig(code);
        if (config.getEnabled() == null || config.getEnabled() != 1) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "问卷填写已关闭");
        }
        List<QuestionnaireFieldGroupDTO> groups = parseGroups(config.getFieldsJson());
        validateFormData(formData, groups);
        ScreeningSchool data = mapToScreeningSchool(formData);
        data.setUploadBatch(UPLOAD_BATCH_PREFIX + "_" + IdUtil.fastSimpleUUID());
        if (StrUtil.isBlank(data.getYear())) {
            data.setYear(String.valueOf(LocalDate.now().getYear()));
        }
        screeningSchoolService.createFromQuestionnaire(data);
    }

    @Override
    public IPage<ScreeningSchool> listSubmissions(String code, int page, int size, String name, String idNumber) {
        getOrInitConfig(code);
        return screeningSchoolService.page(new Page<>(page, size), buildSubmissionWrapper(name, idNumber));
    }

    @Override
    public List<ScreeningSchool> listSubmissionsForExport(String code, String name, String idNumber) {
        getOrInitConfig(code);
        return screeningSchoolService.list(buildSubmissionWrapper(name, idNumber));
    }

    private LambdaQueryWrapper<ScreeningSchool> buildSubmissionWrapper(String name, String idNumber) {
        LambdaQueryWrapper<ScreeningSchool> wrapper = Wrappers.<ScreeningSchool>lambdaQuery()
                .likeRight(ScreeningSchool::getUploadBatch, UPLOAD_BATCH_PREFIX)
                .orderByDesc(ScreeningSchool::getCreateTime);
        if (StrUtil.isNotBlank(name)) {
            wrapper.like(ScreeningSchool::getName, name);
        }
        if (StrUtil.isNotBlank(idNumber)) {
            wrapper.like(ScreeningSchool::getIdNumber, idNumber);
        }
        return wrapper;
    }

    private QuestionnaireConfig getOrInitConfig(String code) {
        QuestionnaireConfig config = getOne(Wrappers.<QuestionnaireConfig>lambdaQuery()
                .eq(QuestionnaireConfig::getCode, code)
                .last("LIMIT 1"));
        if (config != null) {
            if (StrUtil.isBlank(config.getFieldsJson()) || "[]".equals(config.getFieldsJson().trim())) {
                config.setFieldsJson(JSONUtil.toJsonStr(defaultSchoolGroups()));
                updateById(config);
            }
            return config;
        }
        QuestionnaireConfig init = QuestionnaireConfig.builder()
                .code(code)
                .title("学校人群结核病筛查调查问卷")
                .subtitle("请如实填写以下信息，所有数据仅用于结核病防控统计分析，信息将严格保密。")
                .enabled(1)
                .populationType("school")
                .fieldsJson(JSONUtil.toJsonStr(defaultSchoolGroups()))
                .build();
        save(init);
        return init;
    }

    private QuestionnaireConfigVO toVO(QuestionnaireConfig config) {
        QuestionnaireConfigVO vo = new QuestionnaireConfigVO();
        vo.setCode(config.getCode());
        vo.setTitle(config.getTitle());
        vo.setSubtitle(config.getSubtitle());
        vo.setEnabled(config.getEnabled() != null && config.getEnabled() == 1);
        vo.setPopulationType(config.getPopulationType());
        vo.setGroups(parseGroups(config.getFieldsJson()));
        return vo;
    }

    private List<QuestionnaireFieldGroupDTO> parseGroups(String json) {
        if (StrUtil.isBlank(json) || "[]".equals(json.trim())) {
            return defaultSchoolGroups();
        }
        List<QuestionnaireFieldGroupDTO> groups = JSONUtil.toList(json, QuestionnaireFieldGroupDTO.class);
        if (groups == null || groups.isEmpty()) {
            return defaultSchoolGroups();
        }
        return groups;
    }

    private void validateFieldGroups(List<QuestionnaireFieldGroupDTO> groups) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (QuestionnaireFieldGroupDTO group : groups) {
            if (group.getFields() == null || group.getFields().isEmpty()) {
                throw new ServiceException(StatusEnum.PARAM_INVALID, "分组「" + group.getGroup() + "」至少包含一个题目");
            }
            for (QuestionnaireFieldDTO field : group.getFields()) {
                if (StrUtil.isBlank(field.getKey()) || StrUtil.isBlank(field.getLabel())) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "题目字段标识和名称不能为空");
                }
                if (!keys.add(field.getKey())) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "字段标识重复：" + field.getKey());
                }
                if (!ALLOWED_FIELD_KEYS.contains(field.getKey())) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, "字段标识无效：" + field.getKey());
                }
            }
        }
    }

    private void validateFormData(Map<String, Object> formData, List<QuestionnaireFieldGroupDTO> groups) {
        for (QuestionnaireFieldGroupDTO group : groups) {
            if (group.getFields() == null) {
                continue;
            }
            for (QuestionnaireFieldDTO field : group.getFields()) {
                if (!Boolean.TRUE.equals(field.getRequired())) {
                    continue;
                }
                if (!isFieldVisible(field, formData)) {
                    continue;
                }
                Object value = formData.get(field.getKey());
                if (value == null || (value instanceof String s && StrUtil.isBlank(s))) {
                    throw new ServiceException(StatusEnum.PARAM_INVALID, field.getLabel() + "不能为空");
                }
            }
        }
    }

    private boolean isFieldVisible(QuestionnaireFieldDTO field, Map<String, Object> formData) {
        ShowWhenDTO showWhen = field.getShowWhen();
        if (showWhen == null || StrUtil.isBlank(showWhen.getField())) {
            return true;
        }
        Object current = formData.get(showWhen.getField());
        return showWhen.getValue() != null && showWhen.getValue().equals(String.valueOf(current));
    }

    private ScreeningSchool mapToScreeningSchool(Map<String, Object> formData) {
        ScreeningSchool data = new ScreeningSchool();
        data.setYear(asString(formData.get("year")));
        data.setCity(asString(formData.get("city")));
        data.setDistrict(asString(formData.get("district")));
        data.setName(asString(formData.get("name")));
        data.setGender(asString(formData.get("gender")));
        data.setBirthDate(asDate(formData.get("birthDate")));
        data.setAge(asInteger(formData.get("age")));
        data.setIdType(asString(formData.get("idType")));
        data.setIdNumber(asString(formData.get("idNumber")));
        data.setEthnicity(asString(formData.get("ethnicity")));
        data.setPhone(asString(formData.get("phone")));
        data.setHouseholdAddress(asString(formData.get("householdAddress")));
        data.setCurrentAddress(asString(formData.get("currentAddress")));
        data.setSchoolType(asString(formData.get("schoolType")));
        data.setSchoolName(asString(formData.get("schoolName")));
        data.setClassName(asString(formData.get("className")));
        data.setTbHistory(asString(formData.get("tbHistory")));
        data.setCloseContactHistory(asString(formData.get("closeContactHistory")));
        data.setSuspiciousSymptoms(asString(formData.get("suspiciousSymptoms")));
        data.setHasInfectionScreen(asString(formData.get("hasInfectionScreen")));
        data.setScreenDate(asDate(formData.get("screenDate")));
        data.setScreenMethod(asString(formData.get("screenMethod")));
        data.setScreenResult(asString(formData.get("screenResult")));
        data.setInfectionResult(asString(formData.get("infectionResult")));
        data.setHasChestXray(asString(formData.get("hasChestXray")));
        data.setChestXrayDate(asDate(formData.get("chestXrayDate")));
        data.setChestXrayResult(asString(formData.get("chestXrayResult")));
        data.setRemark(asString(formData.get("remark")));
        return data;
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        return String.valueOf(value).trim();
    }

    private Integer asInteger(Object value) {
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return null;
        }
        try {
            if (value instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "年龄格式不正确");
        }
    }

    private LocalDate asDate(Object value) {
        if (value == null || StrUtil.isBlank(String.valueOf(value))) {
            return null;
        }
        try {
            return LocalDate.parse(String.valueOf(value).trim());
        } catch (Exception e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "日期格式不正确");
        }
    }

    /** 默认学校人群问卷字段（与 feature-wenjuan 分支一致） */
    private List<QuestionnaireFieldGroupDTO> defaultSchoolGroups() {
        List<QuestionnaireFieldGroupDTO> groups = new ArrayList<>();

        groups.add(buildGroup("基本信息", List.of(
                field("year", "年份", "input", true, null, null),
                field("city", "市（州）", "input", true, null, null),
                field("district", "县（市、区）", "input", true, null, null),
                field("name", "姓名", "input", true, null, null),
                field("gender", "性别", "select", true, List.of("男", "女"), null),
                field("birthDate", "出生日期", "date", true, null, null),
                field("age", "年龄", "number", true, null, null),
                field("idType", "证件类型", "select", true, List.of("居民身份证", "护照", "其他"), null),
                field("idNumber", "证件号", "input", true, null, null),
                field("ethnicity", "民族", "input", false, null, null),
                field("phone", "联系电话", "input", true, null, null)
        )));

        groups.add(buildGroup("地址信息", List.of(
                field("householdAddress", "户籍所在地", "input", false, null, null),
                field("currentAddress", "现地址", "input", false, null, null)
        )));

        groups.add(buildGroup("学校信息", List.of(
                field("schoolType", "学校类型", "input", false, null, null),
                field("schoolName", "学校名称", "input", true, null, null),
                field("className", "班级（院系）", "input", false, null, null)
        )));

        groups.add(buildGroup("病史信息", List.of(
                field("tbHistory", "既往结核病史", "select", true, List.of("有", "无"), null),
                field("closeContactHistory", "密切接触史", "select", true, List.of("有", "无"), null),
                field("suspiciousSymptoms", "结核病可疑症状", "select", true, List.of("有", "无"), null)
        )));

        ShowWhenDTO infectionWhen = showWhen("hasInfectionScreen", "是");
        groups.add(buildGroup("感染筛查", List.of(
                field("hasInfectionScreen", "是否进行感染筛查", "select", true, List.of("是", "否"), null),
                field("screenDate", "感染筛查日期", "date", false, null, infectionWhen),
                field("screenMethod", "筛查方法（PPD/EC/IGRA）", "select", false, List.of("PPD", "EC", "IGRA"), infectionWhen),
                field("screenResult", "筛查结果", "input", false, null, infectionWhen),
                field("infectionResult", "感染筛查结果", "select", false,
                        List.of("PPD阴性", "PPD+", "PPD++", "PPD+++", "EC阴性", "EC阳性", "IGRA阴性", "IGRA阳性"),
                        infectionWhen)
        )));

        ShowWhenDTO xrayWhen = showWhen("hasChestXray", "是");
        groups.add(buildGroup("胸片检查", List.of(
                field("hasChestXray", "是否进行胸片检查", "select", true, List.of("是", "否"), null),
                field("chestXrayDate", "胸片检查日期", "date", false, null, xrayWhen),
                field("chestXrayResult", "胸片结果", "select", false, List.of("正常", "异常", "未查"), xrayWhen)
        )));

        groups.add(buildGroup("其他", List.of(
                field("remark", "备注", "textarea", false, null, null)
        )));

        return groups;
    }

    private QuestionnaireFieldGroupDTO buildGroup(String name, List<QuestionnaireFieldDTO> fields) {
        QuestionnaireFieldGroupDTO group = new QuestionnaireFieldGroupDTO();
        group.setGroup(name);
        group.setFields(fields);
        return group;
    }

    private QuestionnaireFieldDTO field(String key, String label, String type, boolean required,
                                        List<String> options, ShowWhenDTO showWhen) {
        QuestionnaireFieldDTO dto = new QuestionnaireFieldDTO();
        dto.setKey(key);
        dto.setLabel(label);
        dto.setType(type);
        dto.setRequired(required);
        dto.setOptions(options);
        dto.setShowWhen(showWhen);
        return dto;
    }

    private ShowWhenDTO showWhen(String field, String value) {
        ShowWhenDTO dto = new ShowWhenDTO();
        dto.setField(field);
        dto.setValue(value);
        return dto;
    }
}
