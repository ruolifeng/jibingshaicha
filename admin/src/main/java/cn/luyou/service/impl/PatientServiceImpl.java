package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.EpidemicReport;
import cn.luyou.model.Patient;
import cn.luyou.mapper.PatientMapper;
import cn.luyou.service.EpidemicReportService;
import cn.luyou.service.PatientService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatientServiceImpl extends ServiceImpl<PatientMapper, Patient>
        implements PatientService {

    private final EpidemicReportService epidemicReportService;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<Patient> queryPage(int page, int size, String populationType,
                                     String name, String idNumber, Integer archived) {
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(populationType), Patient::getPopulationType, populationType)
                .like(StrUtil.isNotBlank(name), Patient::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), Patient::getIdNumber, idNumber)
                .eq(archived != null, Patient::getArchived, archived)
                .orderByDesc(Patient::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importEpidemic(MultipartFile file, String populationType) {
        String batchId = IdUtil.fastSimpleUUID();
        // headRowNumber(0) 使首行（表头）也作为数据行读入，以便构建列索引映射
        List<Map<Integer, String>> allRows = new ArrayList<>();
        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    allRows.add(new LinkedHashMap<>(data));
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("大疫情表解析完成，共 {} 行（含表头）", allRows.size());
                }
            }).sheet().headRowNumber(0).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel读取失败");
        }

        if (allRows.size() < 2) {
            log.warn("大疫情表无数据行，跳过导入");
            return 0;
        }

        // 解析第一行表头，构建 字段名 -> 列索引 映射
        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> headerIndex = new LinkedHashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            if (StrUtil.isNotBlank(entry.getValue())) {
                headerIndex.put(entry.getValue().trim(), entry.getKey());
            }
        }
        log.info("大疫情表表头解析：{}", headerIndex.keySet());

        List<Map<Integer, String>> dataRows = allRows.subList(1, allRows.size());

        int matchCount = 0;
        for (Map<Integer, String> row : dataRows) {
            String nameVal = getFieldByHeader(row, headerIndex, "姓名");
            String idNumberVal = getFieldByHeader(row, headerIndex, "证件号", "身份证号", "身份证");

            // 跳过姓名和证件号均为空的空行
            if (StrUtil.isBlank(nameVal) && StrUtil.isBlank(idNumberVal)) {
                continue;
            }

            String rawJson;
            try {
                rawJson = objectMapper.writeValueAsString(row);
            } catch (Exception e) {
                rawJson = row.toString();
            }

            // 优先按证件号精确匹配，再按姓名模糊匹配
            Patient matched = null;
            if (StrUtil.isNotBlank(idNumberVal)) {
                matched = lambdaQuery()
                        .eq(Patient::getPopulationType, populationType)
                        .eq(Patient::getIdNumber, idNumberVal)
                        .last("LIMIT 1")
                        .one();
            }
            if (matched == null && StrUtil.isNotBlank(nameVal)) {
                matched = lambdaQuery()
                        .eq(Patient::getPopulationType, populationType)
                        .like(Patient::getName, nameVal)
                        .last("LIMIT 1")
                        .one();
            }

            EpidemicReport report = EpidemicReport.builder()
                    .populationType(populationType)
                    .rawData(rawJson)
                    .uploadBatch(batchId)
                    .build();

            if (matched != null) {
                matched.setEpidemicData(rawJson);
                updateById(matched);
                report.setPatientId(matched.getId());
                report.setMatched(1);
                matchCount++;
            } else {
                Patient newPatient = Patient.builder()
                        .populationType(populationType)
                        .name(nameVal)
                        .idNumber(idNumberVal)
                        .source("epidemic")
                        .archived(0)
                        .epidemicData(rawJson)
                        .build();
                save(newPatient);
                report.setPatientId(newPatient.getId());
                report.setMatched(0);
            }

            epidemicReportService.save(report);
        }

        log.info("大疫情导入完成：共 {} 条数据，匹配 {} 条", dataRows.size(), matchCount);
        return dataRows.size();
    }

    @Override
    public void archivePatient(Long id) {
        Patient patient = getById(id);
        if (patient == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "患者不存在");
        }
        patient.setArchived(1);
        patient.setArchivedTime(LocalDateTime.now());
        updateById(patient);
    }

    @Override
    public IPage<Patient> queryHistoryPage(int page, int size, String populationType,
                                            String name, String idNumber,
                                            String startTime, String endTime) {
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(populationType), Patient::getPopulationType, populationType)
                .eq(Patient::getArchived, 1)
                .like(StrUtil.isNotBlank(name), Patient::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), Patient::getIdNumber, idNumber)
                .ge(StrUtil.isNotBlank(startTime), Patient::getArchivedTime, startTime)
                .le(StrUtil.isNotBlank(endTime), Patient::getArchivedTime, endTime + " 23:59:59")
                .orderByDesc(Patient::getArchivedTime);
        return page(new Page<>(page, size), wrapper);
    }

    /**
     * 根据表头名称从数据行中提取字段值。
     * 支持多个候选字段名，先精确匹配，再按"表头包含关键字"模糊匹配。
     */
    private String getFieldByHeader(Map<Integer, String> row, Map<String, Integer> headerIndex,
                                    String... fieldNames) {
        for (String fieldName : fieldNames) {
            // 精确匹配
            Integer idx = headerIndex.get(fieldName);
            if (idx != null) {
                String val = row.get(idx);
                if (StrUtil.isNotBlank(val)) {
                    return val.trim();
                }
            }
            // 模糊匹配（表头中包含目标字段名）
            for (Map.Entry<String, Integer> entry : headerIndex.entrySet()) {
                if (entry.getKey().contains(fieldName)) {
                    String val = row.get(entry.getValue());
                    if (StrUtil.isNotBlank(val)) {
                        return val.trim();
                    }
                }
            }
        }
        return null;
    }
}
