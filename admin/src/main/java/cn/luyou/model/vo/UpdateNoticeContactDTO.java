package cn.luyou.model.vo;

import lombok.Data;

/** 通知单：更新联系电话、地址、治疗机构、服药管理单位，并同步人员主表联系信息 */
@Data
public class UpdateNoticeContactDTO {
    private String phone;
    private String currentAddress;
    private String householdAddress;
    /** 治疗机构 */
    private String treatmentInstitution;
    /** 服药管理单位 */
    private String medicationManagementUnit;
}
