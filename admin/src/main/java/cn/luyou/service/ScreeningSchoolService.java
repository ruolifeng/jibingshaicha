package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningSchool;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ScreeningSchoolService extends IService<ScreeningSchool> {

    /**
     * 上传并解析学校人群筛查 Excel
     * @param confirmSkipInvalid 为 true 时跳过缺少姓名/证件号的行并继续导入有效数据
     * @return 导入结果（成功条数 + 错误行列表）
     */
    ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid, boolean confirmSkipDuplicateInFile);

    default ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid) {
        return uploadAndParse(file, confirmSkipInvalid, false);
    }

    default ImportResult uploadAndParse(MultipartFile file) {
        return uploadAndParse(file, false);
    }

    /**
     * 分页查询筛查数据
     */
    IPage<ScreeningSchool> queryPage(int page, int size, String name, String idNumber,
                                     String schoolName, String district, Integer isLatent, String diagnosisFirst,
                                     String phone, String year, String entryUnit,
                                     String createTimeFrom, String createTimeTo,
                                     String creatorUsername, String columnFilters,
                                     String sortField, String sortOrder);

    /** 导出：ids 非空时仅导出勾选行，否则按当前筛选条件导出全部匹配数据 */
    List<ScreeningSchool> listForExport(String name, String idNumber, String schoolName, String district,
                                        Integer isLatent, String diagnosisFirst, String phone, String year,
                                        String entryUnit, String createTimeFrom, String createTimeTo,
                                        String creatorUsername, String columnFilters,
                                        String sortField, String sortOrder, List<Long> ids);

    default IPage<ScreeningSchool> queryPage(int page, int size, String name, String idNumber,
                                             String schoolName, String district, Integer isLatent, String diagnosisFirst,
                                             String phone, String year, String entryUnit,
                                             String createTimeFrom, String createTimeTo) {
        return queryPage(page, size, name, idNumber, schoolName, district, isLatent, diagnosisFirst,
                phone, year, entryUnit, createTimeFrom, createTimeTo, null, null, null, null);
    }

    /** 新增单条筛查记录（同步判定潜伏并自动创建潜伏感染记录） */
    void createScreening(ScreeningSchool data);

    /**
     * 级联删除筛查记录（同步删除后续所有关联数据）
     */
    void deleteScreeningCascade(Long id);

    /**
     * 批量级联删除筛查记录（单事务，减少多次提交开销）
     */
    void batchDeleteCascade(List<Long> ids);

    /**
     * 更新筛查记录（同步重新计算潜伏判定结果）
     */
    void updateScreening(ScreeningSchool data);

    /** 问卷公开提交：无需登录，departmentId 为空 */
    void createFromQuestionnaire(ScreeningSchool data);
}
