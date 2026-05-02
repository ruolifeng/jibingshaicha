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
    private Long departmentId;
    private List<String> roles;
    /** 权限编码列表 */
    private List<String> permissions;
}
