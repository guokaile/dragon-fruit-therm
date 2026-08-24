package cn.puge.util;

import java.util.regex.Pattern;

/**
 * 校验工具类
 * 提供各种参数校验方法
 *
 * @author Puge
 * @since 1.0
 */
// 通用校验工具类ValidationUtil
// 提供各种参数校验方法
// 1. 校验手机号格式
// 2. 校验验证码格式
// 3. 校验密码格式
// 4. 校验字符串是否为空
// 5. 校验字符串是否不为空
public class ValidationUtil {

    /**
     * 中国手机号正则表达式（宽松匹配）
     * 匹配以1开头的11位数字
     */
    //pattern是Java.util.regex.Pattern类，是Java正则表达式的编译后的对象
    //它不是字符串，而是预编译的正则匹配引擎，性能远高于每次使用字符串正则匹配
    //Pattern.compile() 预编译正则表达式，返回Pattern对象
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
        //trim()是Java string类的内置方法，用于移除字符串首尾的空格字符（只移除首尾空格，不移除中间空格）
        //isEmpty()是Java string类的内置方法，用于判断字符串长度是否为0（包含空格）
        //trim().isEmpty()-->这是大厂后端校验用户输入的标准「空白清洗校验」写法，专门解决「用户误输入空白字符」的场景。
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        //Java 正则匹配 API 的标准链式调用，核心是用「预编译的正则引擎」对待校验的字符串做全匹配校验
        //matcher()方法返回一个Matcher对象，用于对字符串进行正则匹配
        //matches()方法判断字符串是否与正则表达式匹配
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
