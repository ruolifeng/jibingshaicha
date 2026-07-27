package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningKeyPopulation;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ScreeningKeyPopulationService extends IService<ScreeningKeyPopulation> {

    /**
     * 上传并解析 Excel（sourceType 默认 'keyPopulation'，疫情筛查传 'regular'）
     *
     * @param overwrite 是否与系统中同身份证号记录覆盖合并；false 时跳过重复行
     */
    ImportResult uploadAndParse(MultipartFile file, String sourceType, boolean overwrite,
                                boolean confirmSkipInvalid, boolean confirmSkipDuplicateInFile);

    /** @deprecated 兼容旧调用，默认覆盖重复、不确认跳过无效行 */
    default ImportResult uploadAndParse(MultipartFile file, String sourceType, boolean overwrite, boolean confirmSkipInvalid) {
        return uploadAndParse(file, sourceType, overwrite, confirmSkipInvalid, false);
    }

    /** 导入预览：检测与系统已有记录（身份证号）重复的数据 */
    Map<String, Object> previewUpload(MultipartFile file, String sourceType);

    /** @deprecated 兼容旧调用，sourceType 默认 keyPopulation，默认覆盖重复 */
    @Deprecated
    default ImportResult uploadAndParse(MultipartFile file) {
        return uploadAndParse(file, "keyPopulation", true, false, false);
    }

    /** @deprecated 兼容旧调用，默认覆盖重复 */
    @Deprecated
    default ImportResult uploadAndParse(MultipartFile file, String sourceType) {
        return uploadAndParse(file, sourceType, true, false, false);
    }

    IPage<ScreeningKeyPopulation> queryPage(int page, int size, String name, String idNumber,
                                             String phone, String district, String townshipCommunity,
                                             String crowdCategory, String screenMethod, Integer isLatent,
                                             String sourceType, String diagnosisFirst,
                                             String dateFrom, String dateTo, String entryUnit,
                                             String createTimeFrom, String createTimeTo,
                                             String creatorUsername, String hasChestXray,
                                             String chestXrayResult, String columnFilters,
                                             String formatIssue, String sortField, String sortOrder);

    /** 导出：ids 非空时仅导出勾选行，否则按当前筛选条件导出全部匹配数据 */
    List<ScreeningKeyPopulation> listForExport(String name, String idNumber,
                                                String phone, String district, String townshipCommunity,
                                                String crowdCategory, String screenMethod, Integer isLatent,
                                                String sourceType, String diagnosisFirst,
                                                String dateFrom, String dateTo, String entryUnit,
                                                String createTimeFrom, String createTimeTo,
                                                String creatorUsername, String hasChestXray,
                                                String chestXrayResult, String columnFilters,
                                                String formatIssue, String sortField, String sortOrder,
                                                List<Long> ids);

    /** @deprecated 兼容旧调用 */
    @Deprecated
    default IPage<ScreeningKeyPopulation> queryPage(int page, int size, String name, String idNumber,
                                                    String phone, String district, String townshipCommunity,
                                                    String crowdCategory, String screenMethod, Integer isLatent,
                                                    String sourceType, String diagnosisFirst,
                                                    String dateFrom, String dateTo, String entryUnit,
                                                    String createTimeFrom, String createTimeTo) {
        return queryPage(page, size, name, idNumber, phone, district, townshipCommunity,
                crowdCategory, screenMethod, isLatent, sourceType, diagnosisFirst, dateFrom, dateTo,
                entryUnit, createTimeFrom, createTimeTo, null, null, null, null, null, null, null);
    }

    /** @deprecated 兼容旧调用，sourceType 默认 keyPopulation */
    @Deprecated
    default IPage<ScreeningKeyPopulation> queryPage(int page, int size, String name, String idNumber,
                                                    String phone, String district, String townshipCommunity,
                                                    String crowdCategory, String screenMethod, Integer isLatent) {
        return queryPage(page, size, name, idNumber, phone, district, townshipCommunity,
                crowdCategory, screenMethod, isLatent, "keyPopulation", null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    /** 新增单条筛查记录（同步判定潜伏并自动创建潜伏感染记录） */
    void createScreening(ScreeningKeyPopulation data);

    /** 级联删除筛查记录（同步删除后续所有关联数据） */
    void deleteScreeningCascade(Long id);

    /** 批量级联删除筛查记录 */
    void batchDeleteCascade(List<Long> ids);

    /** 按筛选条件删除（与 list/export 同参，含 sourceType/部门权限），返回删除条数 */
    int deleteByFilter(String name, String idNumber, String phone, String district, String townshipCommunity,
                       String crowdCategory, String screenMethod, Integer isLatent, String sourceType,
                       String diagnosisFirst, String dateFrom, String dateTo, String entryUnit,
                       String createTimeFrom, String createTimeTo, String creatorUsername,
                       String hasChestXray, String chestXrayResult, String columnFilters, String formatIssue);

    /** 删除权限范围内指定 sourceType 的全部记录，返回删除条数 */
    int deleteAll(String sourceType);

    /** 更新筛查记录（同步重新计算潜伏判定结果） */
    void updateScreening(ScreeningKeyPopulation data);

    /** 表头 Excel 式筛选：某列实际去重值（叠加部门权限与 sourceType） */
    List<String> listDistinctColumnValues(String field, String sourceType);
}
