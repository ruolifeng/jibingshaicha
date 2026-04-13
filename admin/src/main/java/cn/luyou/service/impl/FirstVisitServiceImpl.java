package cn.luyou.service.impl;

import cn.luyou.model.FirstVisit;
import cn.luyou.mapper.FirstVisitMapper;
import cn.luyou.service.FirstVisitService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class FirstVisitServiceImpl extends ServiceImpl<FirstVisitMapper, FirstVisit>
        implements FirstVisitService {
}
