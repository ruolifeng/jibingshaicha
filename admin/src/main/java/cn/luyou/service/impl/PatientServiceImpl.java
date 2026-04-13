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
        List<Map<Integer, String>> rawRows = new ArrayList<>();

        try {
            EasyExcel.read(file.getInputStream(), new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> data, AnalysisContext context) {
                    rawRows.add(data);
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("大疫情表解析完成，共 {} 条", rawRows.size());
                }
            }).sheet().doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel读取失败");
        }

        int matchCount = 0;
        for (Map<Integer, String> row : rawRows) {
            String rawJson;
            try {
                rawJson = objectMapper.writeValueAsString(row);
            } catch (Exception e) {
                rawJson = row.toString();
            }

            // 尝试模糊匹配（按姓名 + 证件号）
            String nameVal = extractField(row, "姓名");
            String idNumberVal = extractField(row, "证件号", "身份证");

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
                // 未匹配则新增为患者
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

        log.info("大疫情导入完成：共 {} 条，匹配 {} 条", rawRows.size(), matchCount);
        return rawRows.size();
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
                                            String name, String idNumber) {
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StrUtil.isNotBlank(populationType), Patient::getPopulationType, populationType)
                .eq(Patient::getArchived, 1)
                .like(StrUtil.isNotBlank(name), Patient::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), Patient::getIdNumber, idNumber)
                .orderByDesc(Patient::getArchivedTime);
        return page(new Page<>(page, size), wrapper);
    }

    /**
     * 从 Map<Integer, String> 中按列序号简单提取（大疫情表列序未知，采用值匹配）
     */
    private String extractField(Map<Integer, String> row, String... possibleNames) {
        for (String val : row.values()) {
            if (val != null && !val.isBlank()) {
                for (String name : possibleNames) {
                    if (val.contains(name)) {
                        return val;
                    }
                }
            }
        }
        // 退化：返回前几个非空值作为尝试（表头匹配）
        return row.values().stream().filter(v -> v != null && !v.isBlank()).findFirst().orElse(null);
    }
}
