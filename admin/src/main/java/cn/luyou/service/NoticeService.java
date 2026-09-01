package cn.luyou.service;

import cn.luyou.model.Notice;
import cn.luyou.model.vo.SentNoticeVO;
import cn.luyou.model.vo.UpdateNoticeContactDTO;
import cn.luyou.model.vo.UpdateNoticeCultureResistanceDTO;
import cn.luyou.model.vo.UpdateNoticeRegistrationNoDTO;
import cn.luyou.model.vo.UserInfoVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface NoticeService extends IService<Notice> {

    /** 保存通知单草稿（status=0，可重复保存/覆盖） */
    void saveAsDraft(Notice notice);

    /** 发送通知单（草稿→已发送；确认后可重发；已发送待确认时拒绝） */
    void send(Notice notice);

    /** 确认接收 */
    void confirm(Long id);

    /** 查询当前用户已发送的通知单（分页，含发送者/接收者名称） */
    IPage<SentNoticeVO> sentPage(Long senderId, int pageNum, int size);

    /** 手动催促接收方接收通知单 */
    void remind(Long id);

    /** 查询业务关联通知单列表（含下发人/接收人名称） */
    List<Notice> listByBizWithUsers(Long bizId, String noticeType);

    /** 查询通知单详情（含下发人/接收人名称） */
    Notice getDetailWithUsers(Long id);

    /** 患者所属区县的三级用户（role=4） */
    List<UserInfoVO> listDistrictLevel3Users(Long noticeId);

    /** 仅更新痰培养/耐药情况，同步首次随访并通知所选三级用户 */
    void updateCultureAndResistance(Long noticeId, UpdateNoticeCultureResistanceDTO dto);

    /** 仅更新潜伏感染者通知单登记号，并同步潜伏感染主表（总览/督导/服药/历史共用） */
    void updateRegistrationNo(Long noticeId, UpdateNoticeRegistrationNoDTO dto);

    /** 更新通知单联系电话、现居住地址、户籍地址，并同步患者/潜伏感染主表 */
    void updateContactInfo(Long noticeId, UpdateNoticeContactDTO dto);
}
