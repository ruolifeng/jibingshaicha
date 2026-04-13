package cn.luyou.service.impl;

import cn.luyou.model.FollowUpVisit;
import cn.luyou.mapper.FollowUpVisitMapper;
import cn.luyou.service.FollowUpVisitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FollowUpVisitServiceImpl extends ServiceImpl<FollowUpVisitMapper, FollowUpVisit>
        implements FollowUpVisitService {
}
