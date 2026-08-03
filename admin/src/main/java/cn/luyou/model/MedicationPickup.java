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

    /** 关联患者ID（潜伏感染领药记录为空） */
    private Long patientId;
    /** 关联潜伏感染者ID（患者领药记录为空） */
    private Long latentInfectionId;
    private String populationType;
    /** 第几次领药（后端自动累加） */
    private Integer pickupSeq;
    /** 药品及用量 JSON：[{name,dosage,quantity,quantityUnit}] */
    private String drugs;
    /** 领取数量 */
    private BigDecimal quantity;
    /** 领取数量单位（盒、瓶、片等） */
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
