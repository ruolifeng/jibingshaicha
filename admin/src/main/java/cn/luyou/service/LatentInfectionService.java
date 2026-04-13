package cn.luyou.service;

import cn.luyou.model.LatentInfection;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface LatentInfectionService extends IService<LatentInfection> {

    IPage<LatentInfection> queryPage(int page, int size, String populationType,
                                      String name, String idNumber, Integer trackingStatus, Integer archived);

    /** 追踪操作 */
    void track(Long id, Integer status, String remark);

    /** 转诊操作 */
    void referral(Long id, String result, String remark);
}
