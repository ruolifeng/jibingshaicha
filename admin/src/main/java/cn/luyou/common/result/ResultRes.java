package cn.luyou.common.result;


import cn.luyou.common.cuenum.StatusEnum;

/**
 * 统一结果返回
 * @author ruolifeng
 *
 */
public class ResultRes {
    /**
     * 封装成功响应的方法
     * @param data 响应数据
     * @return reponse
     * @param <T> 响应数据类型
     */
    public static <T> ResultResponse<T> success(T data) {

        ResultResponse<T> response = new ResultResponse<>();
        response.setData(data);
        response.setCode(StatusEnum.SUCCESS.code);
        response.setMsg(StatusEnum.SUCCESS.message);
        return response;
    }

    /**
     * 封装error的响应
     * @param statusEnum error响应的状态值
     * @return 响应信息
     * @param <T> 泛型类
     */
    public static <T> ResultResponse<T> error(StatusEnum statusEnum) {
        return error(statusEnum, statusEnum.message);
    }

    /**
     * 封装error的响应  可自定义错误信息
     * @param statusEnum error响应的状态值
     * @return 响应信息
     * @param <T> 泛型类
     */
    public static <T> ResultResponse<T> error(StatusEnum statusEnum, String errorMsg) {
        ResultResponse<T> response = new ResultResponse<>();
        response.setCode(statusEnum.code);
        response.setMsg(errorMsg);
        return response;
    }

//   自定义 code 和 message

    public static <T> ResultResponse<T> error(Integer code, String errorMsg) {
        ResultResponse<T> response = new ResultResponse<>();
        response.setCode(code);
        response.setMsg(errorMsg);
        return response;
    }
}
