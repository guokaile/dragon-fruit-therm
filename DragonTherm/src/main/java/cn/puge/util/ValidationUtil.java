package cn.puge.util;

import java.util.regex.Pattern;

/**
 * 校验工具类
 * 提供各种参数校验方法
 *
 * @author Puge
 * @since 1.0
 */
public class ValidationUtil {

    /**
     * 中国手机号正则表达式（宽松匹配）
     * 匹配以1开头的11位数字
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    /**
     * 验证码正则表达式（6位数字）
     */
    private static final Pattern SMS_CODE_PATTERN = Pattern.compile("^\\d{6}$");

    /**
     * 密码正则表达式（6-20位字母或数字）
     */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^[a-zA-Z0-9]{6,20}$");

    /**
     * 校验手机号格式
     *
     * @param phone 手机号
     * @return 是否合法
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * 校验验证码格式
     *
     * @param smsCode 验证码
     * @return 是否合法
     */
    public static boolean isValidSmsCode(String smsCode) {
        if (smsCode == null || smsCode.trim().isEmpty()) {
            return false;
        }
        return SMS_CODE_PATTERN.matcher(smsCode).matches();
    }

    /**
     * 校验密码格式
     *
     * @param password 密码
     * @return 是否合法
     */
    public static boolean isValidPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * 校验字符串是否为空
     *
     * @param str 待校验字符串
     * @return 是否为空
     */
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 校验字符串是否不为空
     *
     * @param str 待校验字符串
     * @return 是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
