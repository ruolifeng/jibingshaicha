package cn.luyou.utils;

import cn.luyou.mapper.LatentInfectionMapper;
import cn.luyou.mapper.NoticeMapper;
import cn.luyou.mapper.PatientMapper;
import cn.luyou.mapper.SysMessageMapper;
import cn.luyou.mapper.UserMapper;
import cn.luyou.model.LatentInfection;
import cn.luyou.model.Notice;
import cn.luyou.model.Patient;
import cn.luyou.model.SysMessage;
import cn.luyou.model.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通知单发送人/接收人展示名填充。
 * <p>
 * 兼容：
 * 1. 转出副本历史数据清空了接收人或改写了发送人 → 回退源通知单；
 * 2. 通知单本身缺失 receiverOrgId/senderId，但系统消息里有记录 → 从 sys_message 回退。
 */
@Component
@RequiredArgsConstructor
public class NoticePartyFillSupport {

    private final UserMapper userMapper;
    private final SysMessageMapper sysMessageMapper;
    private final NoticeMapper noticeMapper;
    private final LatentInfectionMapper latentInfectionMapper;
    private final PatientMapper patientMapper;

    public void fillPartyNames(List<Notice> notices) {
        if (notices == null || notices.isEmpty()) {
            return;
        }
        TransferSourceContext ctx = resolveTransferContext(notices);
        Map<Long, Notice> sourceNoticeByNoticeId = ctx.sourceNoticeByNoticeId;
        Map<Long, Long> copyCreatorIdByBizId = ctx.copyCreatorIdByBizId;
        Map<Long, long[]> partyIdsByNoticeId = new HashMap<>();
        Set<Long> messageLookupIds = new HashSet<>();

        for (Notice n : notices) {
            if (n.getId() == null) {
                continue;
            }
            Long senderId = n.getSenderId();
            Long receiverId = n.getReceiverOrgId();
            Notice source = sourceNoticeByNoticeId.get(n.getId());
            if (source != null) {
                if (receiverId == null && source.getReceiverOrgId() != null) {
                    receiverId = source.getReceiverOrgId();
                }
                // 历史转出副本：发送人曾被改成转出接收方（= 副本业务 creatorId）→ 回退源单
                // 转出后重发（senderId 已是其他人）→ 保留
                Long copyCreatorId = n.getBizId() == null ? null : copyCreatorIdByBizId.get(n.getBizId());
                boolean senderRewritten = copyCreatorId != null && copyCreatorId.equals(senderId);
                if ((senderId == null || senderRewritten) && source.getSenderId() != null) {
                    senderId = source.getSenderId();
                }
            }
            partyIdsByNoticeId.put(n.getId(), new long[]{
                    senderId != null ? senderId : -1L,
                    receiverId != null ? receiverId : -1L
            });
            if (senderId == null || receiverId == null) {
                messageLookupIds.add(n.getId());
                if (source != null && source.getId() != null) {
                    messageLookupIds.add(source.getId());
                }
            }
        }

        Map<Long, Long> senderFromMsg = new HashMap<>();
        Map<Long, Long> receiverFromMsg = new HashMap<>();
        if (!messageLookupIds.isEmpty()) {
            List<SysMessage> messages = sysMessageMapper.selectList(new LambdaQueryWrapper<SysMessage>()
                    .in(SysMessage::getBizId, messageLookupIds)
                    .in(SysMessage::getType, "notice_receive", "notice_confirmed")
                    .orderByDesc(SysMessage::getId));
            for (SysMessage msg : messages) {
                Long noticeId = msg.getBizId();
                if (noticeId == null) continue;
                if ("notice_receive".equals(msg.getType())) {
                    if (msg.getSenderId() != null) {
                        senderFromMsg.putIfAbsent(noticeId, msg.getSenderId());
                    }
                    if (msg.getReceiverId() != null) {
                        receiverFromMsg.putIfAbsent(noticeId, msg.getReceiverId());
                    }
                } else if ("notice_confirmed".equals(msg.getType())) {
                    if (msg.getReceiverId() != null) {
                        senderFromMsg.putIfAbsent(noticeId, msg.getReceiverId());
                    }
                    if (msg.getSenderId() != null) {
                        receiverFromMsg.putIfAbsent(noticeId, msg.getSenderId());
                    }
                }
            }
        }

        Set<Long> userIds = new HashSet<>();
        for (Notice n : notices) {
            if (n.getId() == null) {
                continue;
            }
            long[] ids = partyIdsByNoticeId.get(n.getId());
            if (ids == null) {
                continue;
            }
            Long senderId = ids[0] > 0 ? ids[0] : null;
            Long receiverId = ids[1] > 0 ? ids[1] : null;
            Notice source = sourceNoticeByNoticeId.get(n.getId());
            if (senderId == null) {
                senderId = firstNonNull(senderFromMsg.get(n.getId()),
                        source != null ? senderFromMsg.get(source.getId()) : null);
            }
            if (receiverId == null) {
                receiverId = firstNonNull(receiverFromMsg.get(n.getId()),
                        source != null ? receiverFromMsg.get(source.getId()) : null);
            }
            partyIdsByNoticeId.put(n.getId(), new long[]{
                    senderId != null ? senderId : -1L,
                    receiverId != null ? receiverId : -1L
            });
            if (senderId != null) userIds.add(senderId);
            if (receiverId != null) userIds.add(receiverId);
        }

        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        for (Notice n : notices) {
            if (n.getId() == null) {
                continue;
            }
            long[] ids = partyIdsByNoticeId.get(n.getId());
            if (ids == null) {
                continue;
            }
            Long senderId = ids[0] > 0 ? ids[0] : null;
            Long receiverId = ids[1] > 0 ? ids[1] : null;
            User sender = userMap.get(senderId);
            if (sender != null) {
                n.setSenderName(blankToDefault(sender.getRealName(), sender.getUsername()));
                n.setSenderOrgName(sender.getOrgName());
            }
            User receiver = userMap.get(receiverId);
            if (receiver != null) {
                n.setReceiverName(blankToDefault(receiver.getRealName(), receiver.getUsername()));
                n.setReceiverOrgName(receiver.getOrgName());
            }
        }
    }

    /**
     * 解析转出副本的源通知单，以及副本业务 creatorId（用于识别历史「发送人被改写」）。
     */
    private TransferSourceContext resolveTransferContext(List<Notice> notices) {
        TransferSourceContext ctx = new TransferSourceContext();
        Set<Long> latentBizIds = new HashSet<>();
        Set<Long> patientBizIds = new HashSet<>();
        for (Notice n : notices) {
            if (n.getId() == null || n.getBizId() == null || n.getNoticeType() == null) continue;
            if ("latent".equals(n.getNoticeType())) {
                latentBizIds.add(n.getBizId());
            } else if ("patient".equals(n.getNoticeType())) {
                patientBizIds.add(n.getBizId());
            }
        }

        Map<Long, Long> sourceLatentByBiz = Map.of();
        if (!latentBizIds.isEmpty()) {
            List<LatentInfection> latents = latentInfectionMapper.selectBatchIds(latentBizIds);
            sourceLatentByBiz = latents.stream()
                    .filter(l -> l.getSourceLatentId() != null)
                    .collect(Collectors.toMap(LatentInfection::getId, LatentInfection::getSourceLatentId, (a, b) -> a));
            for (LatentInfection l : latents) {
                if (l.getSourceLatentId() != null && l.getCreatorId() != null) {
                    ctx.copyCreatorIdByBizId.put(l.getId(), l.getCreatorId());
                }
            }
        }
        Map<Long, Long> sourcePatientByBiz = Map.of();
        if (!patientBizIds.isEmpty()) {
            List<Patient> patients = patientMapper.selectBatchIds(patientBizIds);
            sourcePatientByBiz = patients.stream()
                    .filter(p -> p.getSourcePatientId() != null)
                    .collect(Collectors.toMap(Patient::getId, Patient::getSourcePatientId, (a, b) -> a));
            for (Patient p : patients) {
                if (p.getSourcePatientId() != null && p.getCreatorId() != null) {
                    ctx.copyCreatorIdByBizId.put(p.getId(), p.getCreatorId());
                }
            }
        }

        Set<Long> sourceBizIds = new HashSet<>();
        sourceBizIds.addAll(sourceLatentByBiz.values());
        sourceBizIds.addAll(sourcePatientByBiz.values());
        if (sourceBizIds.isEmpty()) {
            return ctx;
        }

        Map<String, Notice> sourceByKey = noticeMapper.selectList(new LambdaQueryWrapper<Notice>()
                        .in(Notice::getBizId, sourceBizIds)
                        .in(Notice::getNoticeType, "latent", "patient")
                        .orderByDesc(Notice::getId))
                .stream()
                .collect(Collectors.toMap(
                        n -> partyKey(n.getBizId(), n.getNoticeType(), n.getPopulationType()),
                        n -> n,
                        (a, b) -> a));

        for (Notice n : notices) {
            if (n.getId() == null || n.getBizId() == null) continue;
            Long sourceBizId = "latent".equals(n.getNoticeType())
                    ? sourceLatentByBiz.get(n.getBizId())
                    : "patient".equals(n.getNoticeType()) ? sourcePatientByBiz.get(n.getBizId()) : null;
            if (sourceBizId == null) continue;
            Notice source = sourceByKey.get(partyKey(sourceBizId, n.getNoticeType(), n.getPopulationType()));
            if (source == null && n.getPopulationType() != null) {
                source = sourceByKey.get(partyKey(sourceBizId, n.getNoticeType(), null));
            }
            if (source != null) {
                ctx.sourceNoticeByNoticeId.put(n.getId(), source);
            }
        }
        return ctx;
    }

    private static String partyKey(Long bizId, String noticeType, String populationType) {
        return bizId + "|" + nullToEmpty(noticeType) + "|" + nullToEmpty(populationType);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static Long firstNonNull(Long a, Long b) {
        return a != null ? a : b;
    }

    private static String blankToDefault(String value, String defaultValue) {
        return value != null && !value.isBlank() ? value : defaultValue;
    }

    private static final class TransferSourceContext {
        private final Map<Long, Notice> sourceNoticeByNoticeId = new HashMap<>();
        /** 转出副本业务 id → creatorId（历史改写后的发送人） */
        private final Map<Long, Long> copyCreatorIdByBizId = new HashMap<>();
    }
}
