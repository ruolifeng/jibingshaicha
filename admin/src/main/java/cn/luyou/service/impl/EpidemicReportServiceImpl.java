package cn.luyou.service.impl;

import cn.luyou.model.EpidemicReport;
import cn.luyou.mapper.EpidemicReportMapper;
import cn.luyou.service.EpidemicReportService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class EpidemicReportServiceImpl extends ServiceImpl<EpidemicReportMapper, EpidemicReport>
        implements EpidemicReportService {
}
