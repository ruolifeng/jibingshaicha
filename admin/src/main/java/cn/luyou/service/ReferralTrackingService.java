package cn.luyou.service;

import cn.luyou.model.ReferralTracking;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ReferralTrackingService extends IService<ReferralTracking> {

    /** 新增推介或追踪记录 */
    ReferralTracking create(Map<String, Object> params);

    /** 查询单条记录详情 */
    ReferralTracking getDetail(Long id);

    /** 分页查询 */
    IPage<ReferralTracking> queryPage(int page, int size, String bizMode,
                                      String name, String idNumber,
                                      Integer trackingStatus, Integer archived,
                                      String phone, String township,
                                      String dateFrom, String dateTo, String sourceType);

    /** 大疫情表导入（创建 bizMode=track, sourceType=epidemic 记录） */
    Map<String, Object> importEpidemic(MultipartFile file);

    /** 导出追踪记录 Excel */
    void exportTrack(HttpServletResponse response, String bizMode,
                     String name, String idNumber, String phone, String township,
                     String dateFrom, String dateTo, String sourceType);

    /** 更新基本信息 */
    void update(Long id, Map<String, Object> params);

    /** 发送推介通知（biz_mode=recommend，recommendStatus: 0→1） */
    void sendRecommend(Long id);

    /** 接收方确认推介（recommendStatus: 1→2） */
    void confirmRecommend(Long id);

    /** 接收方拒绝推介（recommendStatus: 1→3，归档） */
    void rejectRecommend(Long id, String reason);

    /** 追踪操作（更新 trackingStatus，处理未到位次数上限） */
    void track(Long id, Integer status, String remark);

    /** 保存到位后的感染筛查+胸片信息 */
    void saveScreening(Long id, Map<String, Object> params);

    /** 保存诊断结果并分流（创建患者/潜伏感染者记录） */
    void saveDiagnosis(Long id, String diagnosisResult);

    /** 删除记录（软删） */
    void deleteRecord(Long id);
}
