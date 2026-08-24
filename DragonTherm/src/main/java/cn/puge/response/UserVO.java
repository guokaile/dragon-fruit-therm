package cn.puge.response;

import cn.puge.entity.PugeUser;
import cn.puge.util.DesensitizationUtil;

/**
 * 用户视图对象（View Object）
 * 用于向前端返回用户信息，仅包含安全字段
 * 敏感字段（如密码、微信OpenId）不会暴露给前端
 *
 * @author Puge
 * @since 1.0
 */
public class UserVO {

    /**
     * 用户唯一标识
     */
    private String userId;

    /**
     * 手机号（脱敏后）
     */
    private String phone;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 用户头像URL
     */
    private String avatarUrl;

    /**
     * 用户角色（admin=管理员, user=普通用户）
     */
    private String role;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createTime;

    /**
     * 无参构造方法
     */
    public UserVO() {
    }

    /**
     * 从用户实体转换为视图对象
     * 自动对手机号进行脱敏处理，过滤敏感字段
     *
     * @param user 用户实体
     * @return 前端展示用的用户VO
     */
    public static UserVO fromEntity(PugeUser user) {
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        vo.userId = user.getUserId();
        vo.phone = DesensitizationUtil.desensitizePhone(user.getPhone());
        vo.nickname = user.getNickname();
        vo.avatarUrl = user.getAvatarUrl();
        vo.role = user.getRole();
        vo.createTime = user.getCreateTime();
        return vo;
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

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "UserVO{" +
                "userId='" + userId + '\'' +
                ", phone='" + phone + '\'' +
                ", nickname='" + nickname + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", role='" + role + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}