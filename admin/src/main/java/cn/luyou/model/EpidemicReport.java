package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("epidemic_report")
public class EpidemicReport extends BaseEntity {

    private String populationType;
    private Long patientId;
    /** 原始导入数据（JSON） */
    private String rawData;
    private Integer matched;
    private String uploadBatch;
}
