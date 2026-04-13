package cn.luyou.service.impl;

import cn.luyou.model.SupervisionForm;
import cn.luyou.mapper.SupervisionFormMapper;
import cn.luyou.service.SupervisionFormService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SupervisionFormServiceImpl extends ServiceImpl<SupervisionFormMapper, SupervisionForm>
        implements SupervisionFormService {

    @Override
    public void saveAndArchive(SupervisionForm form) {
        form.setStatus(2);
        form.setArchivedTime(LocalDateTime.now());
        saveOrUpdate(form);
    }
}
