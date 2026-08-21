package cn.luyou.service;

import cn.luyou.model.MedicationPickup;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MedicationPickupService extends IService<MedicationPickup> {

    void savePickup(MedicationPickup pickup);

    /** 潜伏感染者领药保存 */
    void saveLatentPickup(MedicationPickup pickup);

    List<MedicationPickup> listByPatientId(Long patientId);

    /** 按潜伏感染者查询领药记录 */
    List<MedicationPickup> listByLatentInfectionId(Long latentInfectionId);

    /** 删除潜伏感染者领药记录（逻辑删除） */
    void deleteLatentPickup(Long id);
}
