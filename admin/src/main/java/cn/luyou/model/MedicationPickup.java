package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("medication_pickup")
public class MedicationPickup extends BaseEntity {

    private Long patientId;
    private String populationType;
    /** 第几次领药（后端自动累加） */
    private Integer pickupSeq;
    /** 药品及用量 JSON：[{name,dosage}] */
    private String drugs;
    /** 领取数量 */
    private BigDecimal quantity;
    /** 领取数量单位（盒、瓶等） */
    private String quantityUnit;
    /** 领取时间 */
    private LocalDate pickupTime;
    /** 发药单位 */
    private String dispensingUnit;
    /** 备注 */
    private String remarks;
    private Long filledBy;

    /** 非数据库字段：当前用户是否可修改 */
    @TableField(exist = false)
    private Boolean editable;
}
