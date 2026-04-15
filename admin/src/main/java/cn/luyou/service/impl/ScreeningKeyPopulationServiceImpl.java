package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningKeyPopulation;
import cn.luyou.model.LatentInfection;
import cn.luyou.mapper.ScreeningKeyPopulationMapper;
import cn.luyou.service.ScreeningKeyPopulationService;
import cn.luyou.service.LatentInfectionService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScreeningKeyPopulationServiceImpl extends ServiceImpl<ScreeningKeyPopulationMapper, ScreeningKeyPopulation>
        implements ScreeningKeyPopulationService {

    private final LatentInfectionService latentInfectionService;

    private static final List<String> POSITIVE_KEYWORDS = Arrays.asList(
            "PPD+", "PPD++", "PPD+++", "EC阳性", "IGRA阳性"
    );

    @Override
    public ImportResult uploadAndParse(MultipartFile file) {
        String batchId = IdUtil.fastSimpleUUID();
        List<ScreeningKeyPopulation> dataList = new ArrayList<>();
        ImportResult result = new ImportResult();
        AtomicInteger rowNum = new AtomicInteger(6); // 数据从第6行开始

        try {
            // V4 重点人群模板：第1行大分组，第2行字段名，第3行子字段细项，第4行空行，第5行填写说明，数据从第6行开始
            EasyExcel.read(file.getInputStream(), ScreeningKeyPopulation.class, new ReadListener<ScreeningKeyPopulation>() {
                @Override
                public void invoke(ScreeningKeyPopulation data, AnalysisContext context) {
                    int row = rowNum.getAndIncrement();
                    if (StrUtil.isNotBlank(data.getIdNumber()) && !isValidIdCard(data.getIdNumber())) {
                        result.addError(row, data.getName(), "身份证号格式不正确");
                    }
                    if (StrUtil.isNotBlank(data.getPhone()) && !isValidPhone(data.getPhone())) {
                        result.addError(row, data.getName(), "手机号格式不正确");
                    }
                    data.setUploadBatch(batchId);
                    data.setIsLatent(isPositive(data.getInfectionResult()) ? 1 : 0);
                    dataList.add(data);
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("重点人群筛查数据解析完成，共 {} 条", dataList.size());
                }
            }).sheet().headRowNumber(5).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
        }

        if (dataList.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        saveBatch(dataList, 500);
        result.setSuccessCount(dataList.size());

        List<LatentInfection> latentList = dataList.stream()
                .filter(d -> d.getIsLatent() == 1)
                .map(d -> LatentInfection.builder()
                        .screeningId(d.getId())
                        .populationType("keyPopulation")
                        .name(d.getName())
                        .idNumber(d.getIdNumber())
                        .gender(d.getGender())
                        .age(d.getAge())
                        .phone(d.getPhone())
                        .infectionResult(d.getInfectionResult())
                        .trackingStatus(0)
                        .notInPlaceCount(0)
                        .archived(0)
                        .build())
                .toList();
        if (!latentList.isEmpty()) {
            latentInfectionService.saveBatch(latentList, 500);
            log.info("自动创建重点人群潜伏感染记录 {} 条", latentList.size());
        }

        return result;
    }

    @Override
    public IPage<ScreeningKeyPopulation> queryPage(int page, int size, String name, String idNumber,
                                                    String district, Integer isLatent) {
        LambdaQueryWrapper<ScreeningKeyPopulation> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(name), ScreeningKeyPopulation::getName, name)
                .eq(StrUtil.isNotBlank(idNumber), ScreeningKeyPopulation::getIdNumber, idNumber)
                .eq(StrUtil.isNotBlank(district), ScreeningKeyPopulation::getDistrict, district)
                .eq(isLatent != null, ScreeningKeyPopulation::getIsLatent, isLatent)
                .orderByDesc(ScreeningKeyPopulation::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    private boolean isPositive(String infectionResult) {
        if (StrUtil.isBlank(infectionResult)) return false;
        return POSITIVE_KEYWORDS.stream().anyMatch(infectionResult::contains);
    }

    private boolean isValidIdCard(String id) {
        if (id == null || id.length() != 18) return false;
        if (!id.matches("\\d{17}[\\dXx]")) return false;
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        String[] checkCodes = {"1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2"};
        int sum = 0;
        for (int i = 0; i < 17; i++) sum += Character.getNumericValue(id.charAt(i)) * weights[i];
        return checkCodes[sum % 11].equalsIgnoreCase(String.valueOf(id.charAt(17)));
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("^1[3-9]\\d{9}$");
    }
}
