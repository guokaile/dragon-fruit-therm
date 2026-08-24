package cn.puge.response;

import cn.puge.entity.PugeUser;

/**
 * 登录响应类
 * 封装登录操作的返回结果
 *
 * @author guokaile
 * @since 1.0
 */
public class LoginResponse {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应信息
     */
    private String message;

    /**
     * 登录成功标识
     */
    private Boolean success;

    /**
     * 访问令牌（登录成功后返回）
     */
    private String accessToken;

    /**
     * 刷新令牌（登录成功后返回）
     */
    private String refreshToken;

    /**
     * 令牌过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户信息（登录成功后返回）
     */
    private UserInfo userInfo;

    /**
     * 用户信息内部类
     */
    public static class UserInfo {
        /**
         * 用户ID
         */
        private String userId;

        /**
         * 手机号
         */
        private String phone;

        /**
         * 昵称
         */
        private String nickname;

        /**
         * 头像URL
         */
        private String avatarUrl;

        /**
         * 用户角色（admin=管理员, user=普通用户）
         */
        private String role;

        public UserInfo() {
        }

        public UserInfo(PugeUser user) {
            if (user != null) {
                this.userId = user.getUserId();
                this.phone = user.getPhone();
                this.nickname = user.getNickname();
                this.avatarUrl = user.getAvatarUrl();
                this.role = user.getRole();
            }
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getAvatarUrl() {
            return avatarUrl;
        }

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }

    // ==================== 静态工厂方法 ====================

    //采用了四个静态工厂方法，两个用于创建成功响应，两个用于创建失败响应。方法重载实现默认参数。

    /**
     * 创建成功响应
     *
     * @param user         用户信息
     * @param accessToken  访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn    过期时间
     * @return 成功响应
     */
    //为了简化将RT放在响应实体中，但是实践的业务场景中会引发安全问题RT泄露。出产环境会通过【HttpOnly cookie】隔离存储RT。
    public static LoginResponse success(PugeUser user, String accessToken,
                                        String refreshToken, Long expiresIn) {
        return success(user, accessToken, refreshToken, expiresIn, "登录成功");
    }

    /**
     * 创建成功响应（支持自定义消息，如注册成功）
     *
     * @param user         用户信息
     * @param accessToken  访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn    过期时间
     * @param message      自定义消息
     * @return 成功响应
     */
    public static LoginResponse success(PugeUser user, String accessToken,
                                        String refreshToken, Long expiresIn, String message) {
        LoginResponse response = new LoginResponse();
        //默认成功码为200
        response.setCode(200);
        //把Service层传来的自定义message赋值给DTO的message字段
        response.setMessage(message);
        //默认成功标识为true
        response.setSuccess(true);
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(expiresIn);
        response.setUserInfo(new UserInfo(user));
        return response;
    }

    /**
     * 创建失败响应
     *
     * @param code    错误码
     * @param message 错误信息
     * @return 失败响应
     */
    public static LoginResponse fail(Integer code, String message) {
        LoginResponse response = new LoginResponse();
        response.setCode(code);
        response.setMessage(message);
        response.setSuccess(false);
        return response;
    }

    /**
     * 创建失败响应（默认错误码）
     *
     * @param message 错误信息
     * @return 失败响应
     */
    public static LoginResponse fail(String message) {
        return fail(500, message);
    }

    // ==================== Getter & Setter ====================

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    @Override
    public String toString() {
        return "LoginResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", success=" + success +
                ", accessToken='" + accessToken + '\'' +
                ", expiresIn=" + expiresIn +
                ", userInfo=" + userInfo +
                '}';
    }
}
