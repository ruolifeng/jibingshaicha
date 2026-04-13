package cn.luyou.service.impl;

import cn.luyou.model.LatentInfection;
import cn.luyou.model.SupervisionForm;
import cn.luyou.mapper.SupervisionFormMapper;
import cn.luyou.service.LatentInfectionService;
import cn.luyou.service.SupervisionFormService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SupervisionFormServiceImpl extends ServiceImpl<SupervisionFormMapper, SupervisionForm>
        implements SupervisionFormService {

    private final LatentInfectionService latentInfectionService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveAndArchive(SupervisionForm form) {
        form.setStatus(2);
        form.setArchivedTime(LocalDateTime.now());
        saveOrUpdate(form);

        // 督导表归档后同步归档对应的潜伏感染记录
        if (form.getLatentInfectionId() != null) {
            LatentInfection latent = latentInfectionService.getById(form.getLatentInfectionId());
            if (latent != null && latent.getArchived() != 1) {
                latent.setArchived(1);
                latentInfectionService.updateById(latent);
            }
        }
    }
}
