package cn.luyou.model.vo;

import lombok.Data;

/** 通知单：更新联系电话、现居住地址、户籍地址，并同步人员主表 */
@Data
public class UpdateNoticeContactDTO {
    private String phone;
    private String currentAddress;
    private String householdAddress;
}
