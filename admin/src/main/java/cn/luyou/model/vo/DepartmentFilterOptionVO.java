package cn.luyou.model.vo;

import lombok.Data;

import java.util.List;

/** 统计分析部门筛选下拉树节点 */
@Data
public class DepartmentFilterOptionVO {

    private Long id;
    private String name;
    /** 1 市级 2 区县 3 社区/街道/乡镇 */
    private Integer level;
    private Long parentId;
    private List<DepartmentFilterOptionVO> children;
}
