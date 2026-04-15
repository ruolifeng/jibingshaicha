package cn.luyou.service;

import cn.luyou.model.LatentFollowUp;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface LatentFollowUpService extends IService<LatentFollowUp> {

    List<LatentFollowUp> listByLatentId(Long latentInfectionId);
}
