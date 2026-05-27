package cn.luyou.service.impl;

import cn.luyou.common.customError.ServiceException;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.model.SysMessage;
import cn.luyou.mapper.SysMessageMapper;
import cn.luyou.service.SysMessageService;
import cn.luyou.utils.BaseContext;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class SysMessageServiceImpl extends ServiceImpl<SysMessageMapper, SysMessage>
        implements SysMessageService {

    @Override
    public void sendMessage(Long receiverId, String title, String content, String type, Long bizId) {
        SysMessage msg = SysMessage.builder()
                .senderId(BaseContext.getCurrentId())
                .receiverId(receiverId)
                .title(title)
                .content(content)
                .type(type)
                .bizId(bizId)
                .isRead(0)
                .build();
        save(msg);
    }

    @Override
    public IPage<SysMessage> queryPage(Long receiverId, int page, int size, Integer isRead) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getReceiverId, receiverId)
                .eq(isRead != null, SysMessage::getIsRead, isRead)
                .orderByDesc(SysMessage::getCreateTime);
        return page(new Page<>(page, size), wrapper);
    }

    @Override
    public void markRead(Long id) {
        SysMessage msg = getById(id);
        if (msg != null) {
            msg.setIsRead(1);
            updateById(msg);
        }
    }

    @Override
    public long getUnreadCount(Long receiverId) {
        LambdaQueryWrapper<SysMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMessage::getReceiverId, receiverId)
                .eq(SysMessage::getIsRead, 0);
        return count(wrapper);
    }

    @Override
    public void deleteMessage(Long id, Long currentUserId) {
        SysMessage msg = getById(id);
        if (msg == null) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "消息不存在");
        }
        if (!msg.getReceiverId().equals(currentUserId)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "无权删除他人消息");
        }
        removeById(id);
    }

}
