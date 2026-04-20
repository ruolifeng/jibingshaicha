package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningKeyPopulation;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface ScreeningKeyPopulationService extends IService<ScreeningKeyPopulation> {

    ImportResult uploadAndParse(MultipartFile file);

    IPage<ScreeningKeyPopulation> queryPage(int page, int size, String name, String idNumber,
                                             String phone, String district, String townshipCommunity,
                                             String crowdCategory, String screenMethod, Integer isLatent);

    /** 新增单条筛查记录（同步判定潜伏并自动创建潜伏感染记录） */
    void createScreening(ScreeningKeyPopulation data);

    /** 级联删除筛查记录（同步删除后续所有关联数据） */
    void deleteScreeningCascade(Long id);

    /** 更新筛查记录（同步重新计算潜伏判定结果） */
    void updateScreening(ScreeningKeyPopulation data);
}
