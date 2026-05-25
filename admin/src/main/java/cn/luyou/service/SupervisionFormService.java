package cn.luyou.service;

import cn.luyou.model.SupervisionForm;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface SupervisionFormService extends IService<SupervisionForm> {

    /** 保存草稿（status=0） */
    void saveDraft(SupervisionForm form);

    /** 提交督导表（status=1，每次提交新增一条记录） */
    void saveSubmit(SupervisionForm form);

    /** 保存并归档督导表（status=2） */
    void saveAndArchive(SupervisionForm form);

    /** 查询草稿 */
    SupervisionForm getDraft(Long latentInfectionId);

    /** 已提交/已归档记录列表 */
    List<SupervisionForm> listCompleted(Long latentInfectionId, Integer role);
}
