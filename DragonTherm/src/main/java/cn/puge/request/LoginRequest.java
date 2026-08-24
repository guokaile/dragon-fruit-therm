package cn.puge.request;

import cn.puge.util.ValidationUtil;

/**
 * 登录请求基类
 * 定义三种登录方式的公共参数
 *
 * @author guokaile
 * @since 1.0
 */
public class LoginRequest {

    /**
     * 登录类型枚举
     */
    //enum是Java的关键字，用于定义枚举类型，即固定的，有限的一组常量
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

        // 每个枚举值附带的属性（用来对应前端传的登录类型code、以及业务描述）
        private final int code;
        private final String description;

        //枚举类的构造方法（只能用private/默认修饰符，不能用public）
        LoginType(int code, String description) {
            this.code = code;
            this.description = description;
        }

        // 提供getter方法，用于获取枚举值的属性
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
        //枚举类的「反向查找工具方法」——它是连接「前端传的数字标识（code）」和「后端枚举实例」的核心桥梁
        // 给业务用的方法：通过前端传的code，匹配对应的枚举值
        // 这是一个静态方法，直接用类名调用，不需要实例化对象，LoginType是返回值的类型，getByCode是静态查询工具方法
        public static LoginType getByCode(int code) {
            //遍历所有枚举值（values()是枚举类内置的静态方法，返回所有枚举值的数组）
            for (LoginType type : values()) {
                //比较当前枚举值的code和前端传的code是否一致
                if (type.code == code) {
                    //找到匹配的枚举值，直接返回
                    return type;
                }
            }
            //遍历完还没找到，返回null（对应前端传了非法code）
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
    //validate()内部的分层校验逻辑，先通用校验，再根据登录类型分层校验
    public void validate() {
        //通用校验：登录类型不能为空（所有的登录类型都必须指定）
        if (loginType == null) {
            throw new IllegalArgumentException("登录类型不能为空");
        }
        //参数翻译：根据登录类型code，获取对应的登录类型枚举值，把code转换为枚举实例
        LoginType type = LoginType.getByCode(loginType);
        if (type == null) {
            throw new IllegalArgumentException("无效的登录类型");
        }
        //专属校验：根据登录类型分层校验参数，确保各个登录类型对应的参数都符合要求（按照登录类型分发校验方法）
        switch (type) {
            case SMS_CODE:
                validateSmsCodeLogin();//只校验短信登录参数
                break;
            case WECHAT:
                validateWechatLogin();//只校验微信登录参数
                break;
            case PASSWORD:
                validatePasswordLogin();//只校验账号密码登录参数
                break;
            default:
                // 兜底校验：防止以后枚举加了新值但这里没更新（大厂强制要求）
                throw new IllegalArgumentException("不支持的登录类型");//参数校验失败异常
        }
    }

    /**
     * 校验手机号验证码登录参数
     */
    //必须定义为private类型，因为校验方法是内部方法，不希望被外部调用，否则可能发生校验遗漏，且维护成本高
    private void validateSmsCodeLogin() {
        //1.校验手机号合法性（基础参数校验）。调用ValidationUtil.Java里的isValidPhone()方法校验手机号格式
        if (!ValidationUtil.isValidPhone(phone)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }
        //2.校验验证码非空（核心参数校验，含短路逻辑）
        //trim()是Java string类的内置方法，用于移除字符串首尾的空格字符（只移除首尾空格，不移除中间空格）
        //isEmpty()是Java string类的内置方法，用于判断字符串长度是否为0（包含空格）
        //trim().isEmpty()-->这是大厂后端校验用户输入的标准「空白清洗校验」写法，专门解决「用户误输入空白字符」的场景。
        if (smsCode == null || smsCode.trim().isEmpty()) {
            throw new IllegalArgumentException("验证码不能为空");
        }
        //3.校验验证码长度（格式校验）
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
