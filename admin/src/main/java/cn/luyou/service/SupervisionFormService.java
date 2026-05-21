package cn.luyou.service;

import cn.luyou.model.SupervisionForm;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SupervisionFormService extends IService<SupervisionForm> {

    /** 提交督导表（status=1，不归档） */
    void saveSubmit(SupervisionForm form);

    /** 保存并归档督导表（status=2） */
    void saveAndArchive(SupervisionForm form);
}
