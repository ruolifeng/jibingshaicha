package cn.luyou.model.vo;

import lombok.Data;

/** 潜伏感染者通知单：仅更新登记号，并同步潜伏感染主表 */
@Data
public class UpdateNoticeRegistrationNoDTO {
    private String registrationNo;
}
