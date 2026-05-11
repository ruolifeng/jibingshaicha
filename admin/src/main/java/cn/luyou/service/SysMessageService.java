package cn.luyou.service;

import cn.luyou.model.SysMessage;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface SysMessageService extends IService<SysMessage> {

    /** 发送系统消息 */
    void sendMessage(Long receiverId, String title, String content, String type, Long bizId);


    /** 查询用户消息列表 */
    IPage<SysMessage> queryPage(Long receiverId, int page, int size, Integer isRead);

    /** 标记已读 */
    void markRead(Long id);

    /** 获取未读消息数 */
    long getUnreadCount(Long receiverId);

    /** 删除消息（仅消息归属人可删） */
    void deleteMessage(Long id, Long currentUserId);
}
