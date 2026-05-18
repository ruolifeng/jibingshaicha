package cn.luyou.service;

import cn.luyou.model.Patient;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

public interface PatientService extends IService<Patient> {

    IPage<Patient> queryPage(int page, int size, String populationType,
                              String name, String idNumber, Integer archived);

    /** 导入大疫情表并模糊匹配合并 */
    int importEpidemic(MultipartFile file, String populationType);

    /** 归档患者（停止完成时间） */
    void archivePatient(Long id);

    /** 历史患者列表 */
    IPage<Patient> queryHistoryPage(int page, int size, String populationType,
                                     String name, String idNumber,
                                     String startTime, String endTime);

    /** 删除患者（级联软删首次随访、后续随访、服药记录、通知单） */
    void deletePatient(Long id);

    /**
     * 导入专病网/病案信息表（populationType='specialDisease'）。
     * 提取字段：患者姓名、身份证号、性别、出生日期、年龄、联系电话、人群分类、
     *          现详细住址、户籍地址、现管单位、诊断结果（病原学阴/阳性）
     */
    int importSpecialDisease(MultipartFile file);
}
