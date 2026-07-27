package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningCloseContact;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ScreeningCloseContactService extends IService<ScreeningCloseContact> {

    ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid, boolean confirmSkipDuplicateInFile);

    default ImportResult uploadAndParse(MultipartFile file, boolean confirmSkipInvalid) {
        return uploadAndParse(file, confirmSkipInvalid, false);
    }

    default ImportResult uploadAndParse(MultipartFile file) {
        return uploadAndParse(file, false);
    }

    IPage<ScreeningCloseContact> queryPage(int page, int size, String name, String idNumber,
                                            String district, Integer ccStatus, String finalScreeningResult,
                                            String phone, String dateFrom, String dateTo,
                                            String createTimeFrom, String createTimeTo,
                                            String creatorUsername, String columnFilters, String formatIssue);

    default IPage<ScreeningCloseContact> queryPage(int page, int size, String name, String idNumber,
                                                   String district, Integer ccStatus, String finalScreeningResult,
                                                   String phone, String dateFrom, String dateTo,
                                                   String createTimeFrom, String createTimeTo) {
        return queryPage(page, size, name, idNumber, district, ccStatus, finalScreeningResult,
                phone, dateFrom, dateTo, createTimeFrom, createTimeTo, null, null, null);
    }

    /** 导出：ids 非空时仅导出勾选行，否则按当前筛选条件导出全部匹配数据 */
    List<ScreeningCloseContact> listForExport(String name, String idNumber, String district,
                                               Integer ccStatus, String finalScreeningResult, String phone,
                                               String dateFrom, String dateTo, String createTimeFrom,
                                               String createTimeTo, String creatorUsername,
                                               String columnFilters, String formatIssue, List<Long> ids);

    /** 新增单条筛查记录 */
    void createScreening(ScreeningCloseContact data);

    /** 级联删除筛查记录（同步删除后续所有关联数据） */
    void deleteScreeningCascade(Long id);

    /** 批量级联删除 */
    void batchDeleteCascade(List<Long> ids);

    /** 按筛选条件删除（与 list/export 同参），返回删除条数 */
    int deleteByFilter(String name, String idNumber, String district, Integer ccStatus,
                       String finalScreeningResult, String phone, String dateFrom, String dateTo,
                       String createTimeFrom, String createTimeTo, String creatorUsername,
                       String columnFilters, String formatIssue);

    /** 删除权限范围内全部密接筛查记录，返回删除条数 */
    int deleteAll();

    /** 更新筛查记录 */
    void updateScreening(ScreeningCloseContact data);

    /** 设置预计完成治疗时间（潜伏感染者-开展预防治疗后系统设置） */
    void setExpectedTreatmentEndDate(Long id, LocalDate expectedDate);

    /** 确认治疗是否完成（到预计完成时间时操作） */
    void confirmTreatmentDone(Long id, boolean done);

    /** 提交3月复查结果（未发现异常流程） */
    void submitThreeMonthCheck(Long id, LocalDate checkDate, String checkResult, String finalResult);

    /** 分类统计（各 finalScreeningResult 数量） */
    Map<String, Long> countByFinalResult();

    /** 查询详情并补全随访到期日等衍生字段 */
    ScreeningCloseContact getEnrichedById(Long id);

    /** 表头 Excel 式筛选：某列实际去重值（叠加部门权限） */
    List<String> listDistinctColumnValues(String field);
}
