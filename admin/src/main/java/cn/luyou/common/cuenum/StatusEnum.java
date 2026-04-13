package cn.luyou.common.cuenum;

/**7xxx，需要给出提示的错误toast, 8xx需要给出提示的错题【model】，9xxx不需要给出提示的错误
 * @author ruolifeng
 * 状态码枚举
 */
public enum StatusEnum {
    SUCCESS(200 ,"请求处理成功"),
    UNAUTHORIZED(401 ,"用户认证失败"),
    FORBIDDEN(403 ,"权限不足"),
    SERVICE_ERROR(500, "服务器去旅行了，请稍后重试"),
    HTTP_METHOD_NOT_SUPPORT(502, "不支持的请求方法"),
    PARAM_INVALID(503, "无效的参数"),
    DELETE_ERROR(504,"删除数据失败"),
    ;
    public final Integer code;  
  
    public final String message;  
  
    StatusEnum(Integer code, String message) {  
        this.code = code;  
        this.message = message;  
    }  
  
}
