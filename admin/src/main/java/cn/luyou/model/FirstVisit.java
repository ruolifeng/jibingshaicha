package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("first_visit")
public class FirstVisit extends BaseEntity {

    private Long patientId;
    private String populationType;
    private LocalDate visitDate;
    /** 随访内容（JSON） */
    private String visitContent;
    private Long filledBy;
}
