package cn.luyou.service;

import cn.luyou.model.MedicationPickup;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MedicationPickupService extends IService<MedicationPickup> {

    void savePickup(MedicationPickup pickup);

    List<MedicationPickup> listByPatientId(Long patientId);
}
