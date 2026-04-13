package cn.luyou.common.result;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

/**
 * @param <T>
 * @author ruolifeng
 */
@Setter
@Getter
public class ResultResponse<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = -1133637474601003587L;

    /**
     * 接口响应状态码
     */
    private Integer code;

    /**
     * 接口响应信息
     */
    private String msg;

    /**
     * 接口响应的数据
     */
    private T data;
}    
