package cn.luyou.service;

import cn.luyou.model.MedicationManagement;
import com.baomidou.mybatisplus.extension.service.IService;

public interface MedicationManagementService extends IService<MedicationManagement> {

    /**
     * 历史患者服药卡：将「停止完成时间」与最近一次后续随访的「停止治疗时间」对齐并回写。
     */
    void syncStopDateFromFollowUp(Long patientId);

    /**
     * 查询展示用：若已归档且随访有停止治疗时间，则覆盖服药卡停止完成时间。
     */
    void fillStopDateFromFollowUp(MedicationManagement medication);
}
