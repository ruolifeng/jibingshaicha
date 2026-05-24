package cn.luyou.service;

import cn.luyou.model.ImportResult;
import cn.luyou.model.ScreeningCloseContact;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

public interface ScreeningCloseContactService extends IService<ScreeningCloseContact> {

    ImportResult uploadAndParse(MultipartFile file);

    IPage<ScreeningCloseContact> queryPage(int page, int size, String name, String idNumber,
                                            String district, Integer ccStatus, String finalScreeningResult,
                                            String phone, String dateFrom, String dateTo);

    /** 新增单条筛查记录 */
    void createScreening(ScreeningCloseContact data);

    /** 级联删除筛查记录（同步删除后续所有关联数据） */
    void deleteScreeningCascade(Long id);

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
}
