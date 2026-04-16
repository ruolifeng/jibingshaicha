package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningSchool;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface ScreeningSchoolService extends IService<ScreeningSchool> {

    /**
     * 上传并解析学校人群筛查 Excel
     * @return 导入结果（成功条数 + 错误行列表）
     */
    ImportResult uploadAndParse(MultipartFile file);

    /**
     * 分页查询筛查数据
     */
    IPage<ScreeningSchool> queryPage(int page, int size, String name, String idNumber,
                                     String schoolName, String district, Integer isLatent);

    /**
     * 级联删除筛查记录（同步删除后续所有关联数据）
     */
    void deleteScreeningCascade(Long id);

    /**
     * 更新筛查记录（同步重新计算潜伏判定结果）
     */
    void updateScreening(ScreeningSchool data);
}
