package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("questionnaire")
public class Questionnaire implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long departmentId;
    private String title;
    private String description;
    private String category;
    /** 模板类型：null=普通问卷, public=公用模板, private=专属模板 */
    private String templateType;
    /** 0-草稿 1-已发布 2-已暂停 3-已关闭 */
    private Integer status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalVisits;
    private Integer totalResponses;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer isDeleted;
}
