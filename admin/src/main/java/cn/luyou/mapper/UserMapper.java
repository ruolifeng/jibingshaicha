package cn.luyou.mapper;

import cn.luyou.model.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper（模板示例）
 * 继承 BaseMapper 即可获得完整的 CRUD 方法
 *
 * @author ruolifeng
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
