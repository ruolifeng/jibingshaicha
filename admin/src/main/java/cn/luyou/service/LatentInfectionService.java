package cn.luyou.service;

import cn.luyou.model.LatentInfection;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface LatentInfectionService extends IService<LatentInfection> {

    /**
     * 分页查询
     * @param referralResult 转诊结果过滤：null 不过滤；"pending" 查询尚未转诊（referralResult IS NULL）；其他值精确匹配
     */
    IPage<LatentInfection> queryPage(int page, int size, String populationType,
                                      String name, String idNumber, Integer trackingStatus, Integer archived,
                                      String referralResult);

    /** 追踪操作 */
    void track(Long id, Integer status, String remark);

    /**
     * 录入胸片检查与首次诊断结果（V4 追踪到位后新增步骤）
     * diagnosisFirst 取值：排除/疑似肺结核/潜伏感染者/确诊患者/其他
     */
    void saveXrayAndDiagnosis(Long id, Map<String, Object> data);

    /**
     * 批量导入胸片+诊断 Excel（含 Z-AE 列，按证件号匹配）
     */
    int importXrayBatch(MultipartFile file, String populationType);

    /** 转诊操作（V4 新增 suspected 疑似肺结核） */
    void referral(Long id, String result, String remark);

    /** 设置服药状态（进入预防治疗管理） */
    void setMedicationStatus(Long id, Integer medicationStatus);

    /** 结案归档（按期检查通过后） */
    void closeCase(Long id);
}
