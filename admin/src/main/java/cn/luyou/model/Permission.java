package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("permission")
public class Permission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 权限编码 */
    private String code;

    /** 权限名称 */
    private String name;

    /** 类型：1=菜单 2=按钮/操作 */
    private Integer type;

    /** 父权限ID，0为顶级 */
    private Long parentId;

    /** 排序号 */
    private Integer sort;

    /** 子权限列表（非数据库字段） */
    @TableField(exist = false)
    private List<Permission> children;
}
