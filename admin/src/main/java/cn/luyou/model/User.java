package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码（BCrypt加密存储，响应时不输出） */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 角色：1=超级管理员 2=一级 3=二级 4=三级 5=四级 6=五级 */
    private Integer role;

    /** 所属机构名称 */
    private String orgName;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
