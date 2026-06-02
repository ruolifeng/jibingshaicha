package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningKeyPopulation;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface ScreeningKeyPopulationService extends IService<ScreeningKeyPopulation> {

    /**
     * 上传并解析 Excel（sourceType 默认 'keyPopulation'，疫情筛查传 'regular'）
     */
    ImportResult uploadAndParse(MultipartFile file, String sourceType);

    /** @deprecated 兼容旧调用，sourceType 默认 keyPopulation */
    @Deprecated
    default ImportResult uploadAndParse(MultipartFile file) {
        return uploadAndParse(file, "keyPopulation");
    }

    IPage<ScreeningKeyPopulation> queryPage(int page, int size, String name, String idNumber,
                                             String phone, String district, String townshipCommunity,
                                             String crowdCategory, String screenMethod, Integer isLatent,
                                             String sourceType, String diagnosisFirst,
                                             String dateFrom, String dateTo, String entryUnit);

    /** @deprecated 兼容旧调用，sourceType 默认 keyPopulation */
    @Deprecated
    default IPage<ScreeningKeyPopulation> queryPage(int page, int size, String name, String idNumber,
                                                    String phone, String district, String townshipCommunity,
                                                    String crowdCategory, String screenMethod, Integer isLatent) {
        return queryPage(page, size, name, idNumber, phone, district, townshipCommunity,
                crowdCategory, screenMethod, isLatent, "keyPopulation", null, null, null, null);
    }

    /** 新增单条筛查记录（同步判定潜伏并自动创建潜伏感染记录） */
    void createScreening(ScreeningKeyPopulation data);

    /** 级联删除筛查记录（同步删除后续所有关联数据） */
    void deleteScreeningCascade(Long id);

    /** 更新筛查记录（同步重新计算潜伏判定结果） */
    void updateScreening(ScreeningKeyPopulation data);
}
