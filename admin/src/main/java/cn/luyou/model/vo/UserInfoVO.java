package cn.luyou.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoVO {
    private Long id;
    private String username;
    private String realName;
    private Integer role;
    private String roleName;
    private String orgName;
    /** 联系电话 */
    private String phone;
    private String avatar;
    private Long departmentId;
    /** 所属部门名称（系统部门树） */
    private String departmentName;
    private List<String> roles;
    /** 权限编码列表 */
    private List<String> permissions;
}
