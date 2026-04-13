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
                                     String name, String idNumber);
}
