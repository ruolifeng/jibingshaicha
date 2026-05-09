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
@TableName("department")
public class Department extends BaseEntity {

    /** 部门名称 */
    private String name;

    /** 描述 */
    private String description;

    /** 上级部门ID，NULL 表示市级顶级部门 */
    private Long parentId;

    /**
     * 部门层级：1=市级，2=区县，3=社区（社区卫生服务站等）。
     * 数据可见范围：当前节点仅能看谁的下级部门产生的数据；同级区县互不可见。
     */
    private Integer level;
}
