package cn.puge.util;

/**
 * 数据脱敏工具类
 * 用于对敏感数据进行脱敏处理，防止敏感信息泄露
 *
 * @author Puge
 * @since 1.0
 */
public class DesensitizationUtil {

    /**
     * 对手机号进行脱敏处理
     * 脱敏规则：保留前3位和后4位，中间用4个星号替代
     * 示例：13800138000 → 138****8000
     *
     * @param phone 原始手机号
     * @return 脱敏后的手机号
     */
    public static String desensitizePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return phone;
        }
        int length = phone.length();
        if (length <= 7) {
            return phone.substring(0, 1) + "****";
        }
        return phone.substring(0, 3) + "****" + phone.substring(length - 4);
    }

    /**
     * 对邮箱进行脱敏处理
     * 脱敏规则：保留首字符和@及后面的域名，中间用星号替代
     * 示例：test@example.com → t***@example.com
     *
     * @param email 原始邮箱
     * @return 脱敏后的邮箱
     */
    public static String desensitizeEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        String prefix = email.substring(0, 1);
        String suffix = email.substring(atIndex);
        return prefix + "***" + suffix;
    }
}