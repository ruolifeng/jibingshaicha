package cn.luyou.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户额外分配的权限（与角色权限取并集后作为登录用户的有效权限）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_permission")
public class UserPermission implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long permissionId;
}
