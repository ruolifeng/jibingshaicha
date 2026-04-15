package cn.luyou.service.impl;

import cn.luyou.model.LatentCheck;
import cn.luyou.mapper.LatentCheckMapper;
import cn.luyou.service.LatentCheckService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LatentCheckServiceImpl extends ServiceImpl<LatentCheckMapper, LatentCheck>
        implements LatentCheckService {

    @Override
    public List<LatentCheck> listByLatentId(Long latentInfectionId) {
        return list(new LambdaQueryWrapper<LatentCheck>()
                .eq(LatentCheck::getLatentInfectionId, latentInfectionId)
                .orderByDesc(LatentCheck::getCheckDate));
    }
}
