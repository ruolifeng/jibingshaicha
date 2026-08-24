package cn.luyou.model.vo;

import lombok.Data;

import java.util.List;

/** 患者通知单：更新痰培养、耐药情况、治疗方案，并通知本区县三级用户 */
@Data
public class UpdateNoticeCultureResistanceDTO {
    private String sputumCulture;
    private String drugResistance;
    /** 治疗方案（含其它敏感方案详情文本） */
    private String treatmentPlan;
    /** 本区县三级用户 ID，可为空（仅保存不同步消息） */
    private List<Long> receiverUserIds;
}
