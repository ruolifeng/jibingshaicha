package cn.luyou.utils;

import cn.hutool.core.util.StrUtil;
import cn.luyou.common.cuenum.StatusEnum;
import cn.luyou.common.customError.ServiceException;

import java.util.regex.Pattern;

/**
 * 用户密码强度校验：至少 8 位，须包含字母、数字及特殊符号。
 */
public final class PasswordValidator {

    private static final Pattern STRONG_PASSWORD = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{}|;:'\",.<>?/~\\\\]).{8,}$"
    );

    private static final String MESSAGE = "密码至少8位，须包含字母、数字及特殊符号";

    private PasswordValidator() {
    }

    public static void assertStrongPassword(String password) {
        if (StrUtil.isBlank(password)) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, "密码不能为空");
        }
        if (!STRONG_PASSWORD.matcher(password).matches()) {
            throw new ServiceException(StatusEnum.PARAM_INVALID, MESSAGE);
        }
    }
}
