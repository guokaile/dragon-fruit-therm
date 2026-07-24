package cn.puge.response;

import cn.puge.entity.PugeUser;

/**
 * 登录响应类
 * 封装登录操作的返回结果
 *
 * @author Puge
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

        public UserInfo() {
        }

        public UserInfo(PugeUser user) {
            if (user != null) {
                this.userId = user.getUserId();
                this.phone = user.getPhone();
                this.nickname = user.getNickname();
                this.avatarUrl = user.getAvatarUrl();
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
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 创建成功响应
     *
     * @param user         用户信息
     * @param accessToken  访问令牌
     * @param refreshToken 刷新令牌
     * @param expiresIn    过期时间
     * @return 成功响应
     */
    public static LoginResponse success(PugeUser user, String accessToken,
                                        String refreshToken, Long expiresIn) {
        LoginResponse response = new LoginResponse();
        response.setCode(200);
        response.setMessage("登录成功");
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
