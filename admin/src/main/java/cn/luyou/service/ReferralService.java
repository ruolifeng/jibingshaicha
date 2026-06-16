package cn.luyou.service;

import cn.luyou.model.Referral;
import cn.luyou.model.vo.ReferralDetailVO;
import cn.luyou.model.vo.SentReferralVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ReferralService extends IService<Referral> {

    /** 发起分级诊疗推送 */
    void send(Referral referral);

    /** 查询转诊详情（含发送方/接收方用户信息） */
    ReferralDetailVO detail(Long id);

    /** 接收方确认接收 */
    void confirm(Long id);

    /** 接收方拒绝 */
    void reject(Long id, String rejectReason);

    /** 发送方重新发起（拒绝后再次推送） */
    void resend(Long id);

    /** 查询某条业务记录关联的最新分级诊疗推送 */
    List<Referral> listByBiz(Long bizId, String bizType);

    /** 当前用户已发送的分级诊疗分页列表 */
    IPage<SentReferralVO> sentPage(Long senderId, int pageNum, int size);

    /** 删除业务关联的转出记录及消息（超级管理员强制删患者等场景） */
    void deleteReferralsAndMessagesByBizId(Long bizId);
}
