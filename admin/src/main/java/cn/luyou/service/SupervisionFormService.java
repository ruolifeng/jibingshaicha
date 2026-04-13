package cn.luyou.service;

import cn.luyou.model.SupervisionForm;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SupervisionFormService extends IService<SupervisionForm> {

    /** 保存并归档督导表 */
    void saveAndArchive(SupervisionForm form);
}
