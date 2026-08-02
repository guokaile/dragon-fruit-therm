package cn.puge.entity;

/**
 * 用户实体类
 * 用于存储用户基本信息
 *
 * @author Puge
 * @since 1.0
 */
public class PugeUser {

    /**
     * 用户唯一标识
     */
    private String userId;

    /**
     * 用户手机号（作为登录账号）
     */
    private String phone;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 微信OpenId（用于微信登录）
     */
    private String wechatOpenId;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像URL
     */
    private String avatarUrl;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新时间
     */
    private Long updateTime;

    /**
     * 用户角色（admin=管理员, user=普通用户）
     */
    private String role;

    /**
     * 默认构造器
     */
    public PugeUser() {
    }

    /**
     * 全参构造器
     *
     * @param userId       用户ID
     * @param phone        手机号
     * @param password     密码
     * @param wechatOpenId 微信OpenId
     * @param nickname     昵称
     * @param avatarUrl    头像URL
     */
    public PugeUser(String userId, String phone, String password, String wechatOpenId,
                    String nickname, String avatarUrl, String role) {
        this.userId = userId;
        this.phone = phone;
        this.password = password;
        this.wechatOpenId = wechatOpenId;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.role = role;
        this.createTime = System.currentTimeMillis();
        this.updateTime = System.currentTimeMillis();
    }

    // ==================== Getter & Setter ====================

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWechatOpenId() {
        return wechatOpenId;
    }

    public void setWechatOpenId(String wechatOpenId) {
        this.wechatOpenId = wechatOpenId;
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

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public Long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Long updateTime) {
        this.updateTime = updateTime;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "PugeUser{" +
                "userId='" + userId + '\'' +
                ", phone='" + phone + '\'' +
                ", nickname='" + nickname + '\'' +
                ", role='" + role + '\'' +
                ", wechatOpenId='" + wechatOpenId + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
