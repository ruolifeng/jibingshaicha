package cn.luyou.common.customError;


import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.result.ResultRes;
import cn.luyou.common.result.ResultResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.List;
import java.util.stream.Collectors;


/**
 * 自定义异常
 *
 * @author ruolifeng
 */
@Slf4j
@ControllerAdvice
public class ExceptionAdvice {

    /**
     * 处理ServiceException
     *
     * @param serviceException ServiceException
     * @param request          请求参数
     * @return 接口响应
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    public ResultResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex,
                                                                     HttpServletRequest request) {
        log.warn("request {} upload size exceeded \n", request, ex);
        return ResultRes.error(StatusEnum.PARAM_INVALID, "文件大小超过限制，单个文件不能超过 25MB");
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseBody
    public ResultResponse<Void> handleMultipartException(MultipartException ex, HttpServletRequest request) {
        log.warn("request {} multipart parse failed \n", request, ex);
        return ResultRes.error(StatusEnum.PARAM_INVALID, "文件上传格式错误，请重新选择图片后重试");
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    public ResultResponse<Void> handleServiceException(ServiceException serviceException, HttpServletRequest request) {
        log.warn("用户自定义异常 {} \n", request, serviceException);
        return ResultRes.error(serviceException.getStatus(), serviceException.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseBody
    public ResultResponse<Void> handleDataIntegrityViolationException(DataIntegrityViolationException ex,
                                                                      HttpServletRequest request) {
        log.error("request {} data integrity violation \n", request, ex);
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        if (detail != null && detail.contains("Incorrect date value")) {
            return ResultRes.error(StatusEnum.PARAM_INVALID,
                    "导入失败：存在无法识别的日期（如出生日期写成 19390624 这类数字）。请检查 Excel 日期列后重试");
        }
        return ResultRes.error(StatusEnum.PARAM_INVALID, "导入失败：数据格式不符合数据库要求，请检查后重试");
    }

    /**
     * 其他异常拦截
     *
     * @param ex      异常
     * @param request 请求参数
     * @return 接口响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResultResponse<Void> handleException(Exception ex, HttpServletRequest request) {
        log.error("request {} throw unExpectException \n", request, ex);
        return ResultRes.error(StatusEnum.SERVICE_ERROR);
    }

    /**
     * 参数非法校验
     *
     * @param ex 错误实体
     * @return 错误的信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public ResultResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        try {
            List<ObjectError> errors = ex.getBindingResult().getAllErrors();
            String message = errors.stream().map(ObjectError::getDefaultMessage).collect(Collectors.joining(","));
            log.error("param illegal: {}", message);
            return ResultRes.error(StatusEnum.PARAM_INVALID, message);
        } catch (Exception e) {
            return ResultRes.error(StatusEnum.SERVICE_ERROR);
        }
    }

    @ExceptionHandler(UnexpectedTypeException.class)
    @ResponseBody
    public ResultResponse<Void> handleUnexpectedTypeException(UnexpectedTypeException ex,
                                                          HttpServletRequest request) {
        log.error("catch UnexpectedTypeException, errorMessage: \n", ex);
        return ResultRes.error(StatusEnum.PARAM_INVALID, ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public ResultResponse<Void> handlerConstraintViolationException(ConstraintViolationException ex, HttpServletRequest request) {
        log.error("request {} throw ConstraintViolationException \n", request, ex);
        return ResultRes.error(StatusEnum.PARAM_INVALID, ex.getMessage());
    }

    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResultResponse<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
                                                                  HttpServletRequest request) {
        log.error("request {} throw ucManagerException \n", request, ex);
        return ResultRes.error(StatusEnum.SERVICE_ERROR);
    }

    @ExceptionHandler({HttpRequestMethodNotSupportedException.class, HttpMediaTypeException.class})
    @ResponseBody
    public ResultResponse<Void> handleMethodNotSupportedException(Exception ex) {
        log.error("HttpRequestMethodNotSupportedException \n", ex);
        return ResultRes.error(StatusEnum.HTTP_METHOD_NOT_SUPPORT);
    }

}
