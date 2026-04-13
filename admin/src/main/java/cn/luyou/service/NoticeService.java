package cn.luyou.service;

import cn.luyou.model.Notice;
import com.baomidou.mybatisplus.extension.service.IService;

public interface NoticeService extends IService<Notice> {

    /** 发送通知单 */
    void send(Notice notice);

    /** 确认接收 */
    void confirm(Long id);
}
