package cn.luyou.service;

import cn.luyou.model.LatentCheck;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface LatentCheckService extends IService<LatentCheck> {

    List<LatentCheck> listByLatentId(Long latentInfectionId);
}
