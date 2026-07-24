package cn.puge.request;

import cn.puge.util.ValidationUtil;

/**
 * 登录请求基类
 * 定义三种登录方式的公共参数
 *
 * @author Puge
 * @since 1.0
 */
public class LoginRequest {

    /**
     * 登录类型枚举
     */
    public enum LoginType {
        /**
         * 手机号验证码登录
         */
        SMS_CODE(1, "手机号验证码登录"),

        /**
         * 微信登录
         */
        WECHAT(2, "微信登录"),

        /**
         * 账号密码登录
         */
        PASSWORD(3, "账号密码登录");

        private final int code;
        private final String description;

        LoginType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        /**
         * 根据code获取枚举值
         *
         * @param code 登录类型code
         * @return 登录类型枚举
         */
        public static LoginType getByCode(int code) {
            for (LoginType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            return null;
        }
    }

    /**
     * 登录类型（1-手机验证码 2-微信 3-账号密码）
     */
    private Integer loginType;

    /**
     * 手机号（用于短信登录和账号密码登录）
     */
    private String phone;

    /**
     * 验证码（用于短信登录）
     */
    private String smsCode;

    /**
     * 密码（用于账号密码登录）
     */
    private String password;

    /**
     * 微信授权码（用于微信登录）
     */
    private String wechatCode;

    /**
     * 微信OpenId（用于微信登录，已授权情况下）
     */
    private String wechatOpenId;

    /**
     * 设备唯一标识
     */
    private String deviceId;

    /**
     * 客户端IP
     */
    private String clientIp;

    // ==================== Getter & Setter ====================

    public Integer getLoginType() {
        return loginType;
    }

    public void setLoginType(Integer loginType) {
        this.loginType = loginType;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getSmsCode() {
        return smsCode;
    }

    public void setSmsCode(String smsCode) {
        this.smsCode = smsCode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWechatCode() {
        return wechatCode;
    }

    public void setWechatCode(String wechatCode) {
        this.wechatCode = wechatCode;
    }

    public String getWechatOpenId() {
        return wechatOpenId;
    }

    public void setWechatOpenId(String wechatOpenId) {
        this.wechatOpenId = wechatOpenId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    /**
     * 参数校验
     *
     * @throws IllegalArgumentException 参数校验失败时抛出
     */
    public void validate() {
        if (loginType == null) {
            throw new IllegalArgumentException("登录类型不能为空");
        }

        LoginType type = LoginType.getByCode(loginType);
        if (type == null) {
            throw new IllegalArgumentException("无效的登录类型");
        }

        switch (type) {
            case SMS_CODE:
                validateSmsCodeLogin();
                break;
            case WECHAT:
                validateWechatLogin();
                break;
            case PASSWORD:
                validatePasswordLogin();
                break;
            default:
                throw new IllegalArgumentException("不支持的登录类型");
        }
    }

    /**
     * 校验手机号验证码登录参数
     */
    private void validateSmsCodeLogin() {
        if (!ValidationUtil.isValidPhone(phone)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (smsCode == null || smsCode.trim().isEmpty()) {
            throw new IllegalArgumentException("验证码不能为空");
        }
        if (smsCode.length() != 6) {
            throw new IllegalArgumentException("验证码必须为6位");
        }
    }

    /**
     * 校验微信登录参数
     */
    private void validateWechatLogin() {
        if ((wechatCode == null || wechatCode.trim().isEmpty())
                && (wechatOpenId == null || wechatOpenId.trim().isEmpty())) {
            throw new IllegalArgumentException("微信授权码或OpenId不能都为空");
        }
    }

    /**
     * 校验账号密码登录参数
     */
    private void validatePasswordLogin() {
        if (!ValidationUtil.isValidPhone(phone)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "loginType=" + loginType +
                ", phone='" + phone + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", clientIp='" + clientIp + '\'' +
                '}';
    }
}
