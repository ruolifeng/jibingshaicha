package cn.luyou.service;

import cn.luyou.model.ReferralTracking;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReferralTrackingService extends IService<ReferralTracking> {

    /** 新增推介或追踪记录 */
    ReferralTracking create(Map<String, Object> params);

    /** 查询单条记录详情 */
    ReferralTracking getDetail(Long id);

    /** 分页查询（creatorName/entryUnit 拆分筛选；creatorOrEntryUnit 保留兼容） */
    IPage<ReferralTracking> queryPage(int page, int size, String bizMode,
                                      String name, String idNumber,
                                      Integer trackingStatus, Integer archived,
                                      String phone, String township,
                                      String dateFrom, String dateTo, String sourceType,
                                      String creatorOrEntryUnit, String columnFilters,
                                      String createTimeFrom, String createTimeTo,
                                      String creatorName, String entryUnit);

    default IPage<ReferralTracking> queryPage(int page, int size, String bizMode,
                                              String name, String idNumber,
                                              Integer trackingStatus, Integer archived,
                                              String phone, String township,
                                              String dateFrom, String dateTo, String sourceType,
                                              String creatorOrEntryUnit, String columnFilters,
                                              String createTimeFrom, String createTimeTo) {
        return queryPage(page, size, bizMode, name, idNumber, trackingStatus, archived,
                phone, township, dateFrom, dateTo, sourceType, creatorOrEntryUnit, columnFilters,
                createTimeFrom, createTimeTo, null, null);
    }

    default IPage<ReferralTracking> queryPage(int page, int size, String bizMode,
                                              String name, String idNumber,
                                              Integer trackingStatus, Integer archived,
                                              String phone, String township,
                                              String dateFrom, String dateTo, String sourceType,
                                              String creatorOrEntryUnit, String columnFilters) {
        return queryPage(page, size, bizMode, name, idNumber, trackingStatus, archived,
                phone, township, dateFrom, dateTo, sourceType, creatorOrEntryUnit, columnFilters, null, null);
    }

    default IPage<ReferralTracking> queryPage(int page, int size, String bizMode,
                                              String name, String idNumber,
                                              Integer trackingStatus, Integer archived,
                                              String phone, String township,
                                              String dateFrom, String dateTo, String sourceType,
                                              String creatorOrEntryUnit) {
        return queryPage(page, size, bizMode, name, idNumber, trackingStatus, archived,
                phone, township, dateFrom, dateTo, sourceType, creatorOrEntryUnit, null, null, null);
    }

    /** 表头/搜索栏 Excel 式筛选：权限范围内某列去重值 */
    List<String> listDistinctColumnValues(String field, String bizMode);

    /** 大疫情表导入（创建 bizMode=track, sourceType=epidemic 记录） */
    Map<String, Object> previewEpidemicImport(MultipartFile file);

    /**
     * @param townshipReceiversJson 五级跨镇导入时「乡镇→三级用户ID」JSON，如 {"邓井关街道":"123"}
     */
    Map<String, Object> importEpidemic(MultipartFile file, boolean addDuplicateRecords, String townshipReceiversJson);

    default Map<String, Object> importEpidemic(MultipartFile file, boolean addDuplicateRecords) {
        return importEpidemic(file, addDuplicateRecords, null);
    }

    /** 区县三级确认：大疫情跨镇导入 */
    void confirmCrossTown(Long id);

    /** 区县三级拒绝：大疫情跨镇导入（归档） */
    void rejectCrossTown(Long id, String reason);

    /** 按业务类型 + 证件号 + 姓名判断是否已有记录 */
    boolean existsByIdNumberAndName(String bizMode, String idNumber, String name);

    /** 导出追踪记录 Excel（ids 非空时仅导出勾选行） */
    void exportTrack(HttpServletResponse response, String bizMode,
                     String name, String idNumber, String phone, String township,
                     String dateFrom, String dateTo, String sourceType,
                     String creatorOrEntryUnit, List<Long> ids,
                     String createTimeFrom, String createTimeTo, Integer trackingStatus,
                     String creatorName, String entryUnit);

    default void exportTrack(HttpServletResponse response, String bizMode,
                             String name, String idNumber, String phone, String township,
                             String dateFrom, String dateTo, String sourceType,
                             String creatorOrEntryUnit, List<Long> ids,
                             String createTimeFrom, String createTimeTo, Integer trackingStatus) {
        exportTrack(response, bizMode, name, idNumber, phone, township,
                dateFrom, dateTo, sourceType, creatorOrEntryUnit, ids, createTimeFrom, createTimeTo,
                trackingStatus, null, null);
    }

    default void exportTrack(HttpServletResponse response, String bizMode,
                             String name, String idNumber, String phone, String township,
                             String dateFrom, String dateTo, String sourceType,
                             String creatorOrEntryUnit, List<Long> ids,
                             String createTimeFrom, String createTimeTo) {
        exportTrack(response, bizMode, name, idNumber, phone, township,
                dateFrom, dateTo, sourceType, creatorOrEntryUnit, ids, createTimeFrom, createTimeTo, null);
    }

    default void exportTrack(HttpServletResponse response, String bizMode,
                             String name, String idNumber, String phone, String township,
                             String dateFrom, String dateTo, String sourceType,
                             String creatorOrEntryUnit, List<Long> ids) {
        exportTrack(response, bizMode, name, idNumber, phone, township,
                dateFrom, dateTo, sourceType, creatorOrEntryUnit, ids, null, null);
    }

    default void exportTrack(HttpServletResponse response, String bizMode,
                             String name, String idNumber, String phone, String township,
                             String dateFrom, String dateTo, String sourceType,
                             String creatorOrEntryUnit) {
        exportTrack(response, bizMode, name, idNumber, phone, township,
                dateFrom, dateTo, sourceType, creatorOrEntryUnit, null, null, null);
    }

    /** 更新基本信息 */
    void update(Long id, Map<String, Object> params);

    /** 发送推介通知（biz_mode=recommend，recommendStatus: 0→1） */
    void sendRecommend(Long id);

    /** 接收方确认推介（recommendStatus: 1→2） */
    void confirmRecommend(Long id);

    /** 接收方拒绝推介（recommendStatus: 1→3，归档） */
    void rejectRecommend(Long id, String reason);

    /** 接收方开启共同追踪（发起方与接收方均可追踪，次数合并计算） */
    void enableJointTracking(Long id);

    /** 追踪操作（更新 trackingStatus，处理未到位次数上限） */
    void track(Long id, Integer status, String remark, LocalDate actualArrivalDate);

    /** 保存到位后的感染筛查+胸片信息 */
    void saveScreening(Long id, Map<String, Object> params);

    /** 保存诊断结果并分流（确诊患者仅标红结案；潜伏感染者进入潜伏感染管理） */
    void saveDiagnosis(Long id, String diagnosisResult, String diagnosisRemark);

    /** 删除记录（软删） */
    void deleteRecord(Long id);

    /** 批量删除 */
    int batchDelete(List<Long> ids);

    /** 按筛选条件删除（与 list/export 同参，含 bizMode），返回删除条数 */
    int deleteByFilter(String bizMode, String name, String idNumber, Integer trackingStatus, Integer archived,
                       String phone, String township, String dateFrom, String dateTo, String sourceType,
                       String creatorOrEntryUnit, String columnFilters,
                       String createTimeFrom, String createTimeTo,
                       String creatorName, String entryUnit);

    default int deleteByFilter(String bizMode, String name, String idNumber, Integer trackingStatus, Integer archived,
                               String phone, String township, String dateFrom, String dateTo, String sourceType,
                               String creatorOrEntryUnit, String columnFilters,
                               String createTimeFrom, String createTimeTo) {
        return deleteByFilter(bizMode, name, idNumber, trackingStatus, archived, phone, township,
                dateFrom, dateTo, sourceType, creatorOrEntryUnit, columnFilters,
                createTimeFrom, createTimeTo, null, null);
    }

    default int deleteByFilter(String bizMode, String name, String idNumber, Integer trackingStatus, Integer archived,
                               String phone, String township, String dateFrom, String dateTo, String sourceType,
                               String creatorOrEntryUnit, String columnFilters) {
        return deleteByFilter(bizMode, name, idNumber, trackingStatus, archived, phone, township,
                dateFrom, dateTo, sourceType, creatorOrEntryUnit, columnFilters, null, null);
    }

    /** 删除权限范围内指定 bizMode 的全部记录，返回删除条数 */
    int deleteAll(String bizMode);

    /** 首页统计：推介人数（已发送推介，与推介管理列表一致的数据权限） */
    long countRecommendSentForDashboard(Integer statYear, List<Long> filterDeptIds);

    /** 首页统计：推介到位人数（statYear 内已发送推介中，任意时间到位者） */
    long countRecommendArrivedForDashboard(Integer statYear, List<Long> filterDeptIds);

    /**
     * 首页追踪统计（追踪模块，统计周期：自然年 1/1—12/31）。
     */
    Map<String, Object> getTrackDashboardStats(Integer statYear, List<Long> filterDeptIds);

    /** 首页待追踪人数：与追踪管理列表一致，仅统计 biz_mode=track 且 trackingStatus=待追踪 的记录 */
    long countPendingTrackingForDashboard(List<Long> filterDeptIds);
}
