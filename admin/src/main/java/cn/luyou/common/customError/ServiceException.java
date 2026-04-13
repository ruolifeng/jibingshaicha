package cn.luyou.common.customError;


import cn.luyou.common.cuenum.StatusEnum;
import lombok.Getter;

import java.io.Serial;

/**
 * @author ruolifeng
 * 自定义异常
 */
@Getter
public class ServiceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -3303518302920463234L;

    private final StatusEnum status;

    public ServiceException(StatusEnum status, String message) {
        super(message);
        this.status = status;
    }

    public ServiceException(StatusEnum status) {
        this(status, status.message);
    }
}
