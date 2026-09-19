package cn.luyou.service;

import cn.luyou.model.vo.UpcomingVisitSupervisionVO;
import cn.luyou.model.vo.VisitSupervisionDispatchResultVO;

import java.util.List;

public interface VisitSupervisionReminderService {

    /** 首页：辖区内未来 7 天内到期的随访/督导（含当天） */
    List<UpcomingVisitSupervisionVO> listUpcoming(List<Long> filterDeptIds);

    /**
     * 按系统当天日期扫描并发送站内提醒（短信配置开启时随消息联动）。
     * 节点：提前 7/3/1 天及当天；同一计划日、同一提前天数只发送一次。
     * 当天提醒为独立消息，不受提前节点「已读」影响。
     */
    VisitSupervisionDispatchResultVO dispatchMessages();

    /**
     * 短信接口：对当前距下次随访/督导 7/3/1 天及当天的对象，按填写人或通知单接收人电话发送短信。
     */
    VisitSupervisionDispatchResultVO dispatchSms();
}
