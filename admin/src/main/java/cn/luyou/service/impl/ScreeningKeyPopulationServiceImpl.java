package cn.luyou.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
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
    public int uploadAndParse(MultipartFile file) {
        String batchId = IdUtil.fastSimpleUUID();
        List<ScreeningKeyPopulation> dataList = new ArrayList<>();

        try {
            EasyExcel.read(file.getInputStream(), ScreeningKeyPopulation.class, new ReadListener<ScreeningKeyPopulation>() {
                @Override
                public void invoke(ScreeningKeyPopulation data, AnalysisContext context) {
                    data.setUploadBatch(batchId);
                    data.setIsLatent(isPositive(data.getInfectionResult()) ? 1 : 0);
                    dataList.add(data);
                }
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("重点人群筛查数据解析完成，共 {} 条", dataList.size());
                }
            }).sheet().headRowNumber(1).doRead();
        } catch (IOException e) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件读取失败: " + e.getMessage());
        }

        if (dataList.isEmpty()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "Excel文件中无有效数据");
        }

        saveBatch(dataList, 500);

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

        return dataList.size();
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
}
