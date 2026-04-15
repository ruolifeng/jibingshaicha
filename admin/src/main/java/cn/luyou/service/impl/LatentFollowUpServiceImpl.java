package cn.luyou.service.impl;

import cn.luyou.model.LatentFollowUp;
import cn.luyou.mapper.LatentFollowUpMapper;
import cn.luyou.service.LatentFollowUpService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LatentFollowUpServiceImpl extends ServiceImpl<LatentFollowUpMapper, LatentFollowUp>
        implements LatentFollowUpService {

    @Override
    public List<LatentFollowUp> listByLatentId(Long latentInfectionId) {
        return list(new LambdaQueryWrapper<LatentFollowUp>()
                .eq(LatentFollowUp::getLatentInfectionId, latentInfectionId)
                .orderByDesc(LatentFollowUp::getFollowUpDate));
    }
}
