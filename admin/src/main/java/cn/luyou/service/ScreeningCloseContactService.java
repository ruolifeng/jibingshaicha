package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningCloseContact;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface ScreeningCloseContactService extends IService<ScreeningCloseContact> {

    ImportResult uploadAndParse(MultipartFile file);

    IPage<ScreeningCloseContact> queryPage(int page, int size, String name, String idNumber,
                                            String district, Integer isLatent);

    /** 新增单条筛查记录（按三轮规则判定并在阳性时创建潜伏感染记录） */
    void createScreening(ScreeningCloseContact data);

    /** 级联删除筛查记录（同步删除后续所有关联数据） */
    void deleteScreeningCascade(Long id);

    /** 更新筛查记录（重新执行三轮判定逻辑） */
    void updateScreening(ScreeningCloseContact data);
}
