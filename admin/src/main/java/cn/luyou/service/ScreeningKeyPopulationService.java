package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningKeyPopulation;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface ScreeningKeyPopulationService extends IService<ScreeningKeyPopulation> {

    ImportResult uploadAndParse(MultipartFile file);

    IPage<ScreeningKeyPopulation> queryPage(int page, int size, String name, String idNumber,
                                             String district, Integer isLatent);

    /** 级联删除筛查记录（同步删除后续所有关联数据） */
    void deleteScreeningCascade(Long id);

    /** 更新筛查记录（同步重新计算潜伏判定结果） */
    void updateScreening(ScreeningKeyPopulation data);
}
