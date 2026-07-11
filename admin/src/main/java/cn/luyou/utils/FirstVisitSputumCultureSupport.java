package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.model.FirstVisit;
import cn.luyou.model.Patient;
import cn.luyou.model.SysMessage;
import cn.luyou.service.PatientService;
import cn.luyou.service.SysMessageService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 首次入户随访 — 痰培养「未做」提醒与补充状态同步。
 */
@Component
@RequiredArgsConstructor
public class FirstVisitSputumCultureSupport {

    /** 与病原学/通知单选项一致 */
    public static final String NOT_DONE = "未做";

    public static final String MSG_TYPE_PENDING = "sputum_culture_pending";
    public static final String MSG_TYPE_SUPPLEMENTED = "sputum_culture_supplemented";

    public static final int SUPPLEMENT_STATUS_PENDING = 0;
    public static final int SUPPLEMENT_STATUS_DONE = 1;

    private final SysMessageService sysMessageService;
    private final PatientService patientService;

    public static boolean isNotDone(String value) {
        return NOT_DONE.equals(StrUtil.trim(value));
    }

    /**
     * 保存前计算补充状态字段（仅已完成记录 status=1 时生效）。
     */
    public void prepareSupplementStatus(FirstVisit target, FirstVisit existing) {
        if (target == null || !Integer.valueOf(1).equals(target.getStatus())) {
            return;
        }
        String culture = StrUtil.trim(target.getSputumCulture());
        Integer prevStatus = existing != null ? existing.getSputumCultureSupplementStatus() : null;

        if (isNotDone(culture)) {
            target.setSputumCultureSupplementStatus(SUPPLEMENT_STATUS_PENDING);
            return;
        }
        if (Integer.valueOf(SUPPLEMENT_STATUS_PENDING).equals(prevStatus)
                || (existing != null && isNotDone(existing.getSputumCulture()))) {
            target.setSputumCultureSupplementStatus(SUPPLEMENT_STATUS_DONE);
            return;
        }
        if (Integer.valueOf(SUPPLEMENT_STATUS_DONE).equals(prevStatus)) {
            target.setSputumCultureSupplementStatus(SUPPLEMENT_STATUS_DONE);
            return;
        }
        target.setSputumCultureSupplementStatus(null);
    }

    /** 保存后同步系统消息（仅已完成记录）。 */
    public void syncMessages(FirstVisit saved, FirstVisit existing) {
        if (saved == null || saved.getPatientId() == null || !Integer.valueOf(1).equals(saved.getStatus())) {
            return;
        }
        String culture = StrUtil.trim(saved.getSputumCulture());
        Integer supplementStatus = saved.getSputumCultureSupplementStatus();
        String patientName = resolvePatientName(saved.getPatientId());

        if (Integer.valueOf(SUPPLEMENT_STATUS_PENDING).equals(supplementStatus) && isNotDone(culture)) {
            if (!hasPendingMessage(saved.getPatientId())) {
                sendPendingMessage(saved, patientName);
            }
            return;
        }
        if (Integer.valueOf(SUPPLEMENT_STATUS_DONE).equals(supplementStatus)) {
            sysMessageService.updatePendingMessageByBizId(
                    saved.getPatientId(),
                    MSG_TYPE_PENDING,
                    MSG_TYPE_SUPPLEMENTED,
                    "痰培养已补充",
                    buildSupplementedContent(patientName, culture));
        }
    }

    private boolean hasPendingMessage(Long patientId) {
        return sysMessageService.count(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getBizId, patientId)
                .eq(SysMessage::getType, MSG_TYPE_PENDING)) > 0;
    }

    private void sendPendingMessage(FirstVisit saved, String patientName) {
        Long receiverId = saved.getFilledBy() != null ? saved.getFilledBy() : BaseContext.getCurrentId();
        if (receiverId == null) {
            return;
        }
        sysMessageService.sendMessage(
                receiverId,
                "痰培养未补充",
                buildPendingContent(patientName),
                MSG_TYPE_PENDING,
                saved.getPatientId());
    }

    private String buildPendingContent(String patientName) {
        return String.format("患者【%s】首次入户随访痰培养为「未做」，请及时补充痰培养结果。", patientName);
    }

    private String buildSupplementedContent(String patientName, String culture) {
        return String.format("患者【%s】痰培养已补充为「%s」。", patientName, StrUtil.blankToDefault(culture, "—"));
    }

    private String resolvePatientName(Long patientId) {
        Patient patient = patientService.getById(patientId);
        return patient != null && StrUtil.isNotBlank(patient.getName()) ? patient.getName() : "患者";
    }
}
