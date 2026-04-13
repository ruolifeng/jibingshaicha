package cn.luyou.service.impl;

import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.Notice;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.service.NoticeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice>
        implements NoticeService {

    @Override
    public void send(Notice notice) {
        notice.setStatus(1);
        notice.setSentTime(LocalDateTime.now());
        notice.setTimeoutNotified(0);
        save(notice);
    }

    @Override
    public void confirm(Long id) {
        Notice notice = getById(id);
        if (notice == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单不存在");
        }
        if (notice.getStatus() == 2) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "通知单已确认");
        }
        notice.setStatus(2);
        notice.setConfirmedTime(LocalDateTime.now());
        updateById(notice);
    }
}
