package cn.luyou.service.impl;

import cn.luyou.model.MedicationManagement;
import cn.luyou.mapper.MedicationManagementMapper;
import cn.luyou.service.MedicationManagementService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class MedicationManagementServiceImpl extends ServiceImpl<MedicationManagementMapper, MedicationManagement>
        implements MedicationManagementService {
}
