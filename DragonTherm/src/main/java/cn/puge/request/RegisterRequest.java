package cn.puge.request;

import cn.puge.util.ValidationUtil;

/**
 * 注册请求类
 * 用于用户注册时的参数封装与校验
 *
 * @author GuoKaiLe
 * @since 1.0
 */
public class RegisterRequest {

    /**
     * 手机号
     */
    private String phone;

    /**
     * 密码
     */
    private String password;

    /**
     * 确认密码
     */
    private String confirmPassword;

    /**
     * 昵称（可选）
     */
    private String nickname;

    /**
     * 客户端IP
     */
    private String clientIp;

    // ==================== Getter & Setter ====================

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

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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
        // 手机号校验
        //运用工具类ValidationUtil的isValidPhone方法，校验手机号格式是否正确
        if (!ValidationUtil.isValidPhone(phone)) {
            throw new IllegalArgumentException("手机号格式不正确");
        }

        // 密码校验
        if (!ValidationUtil.isValidPassword(password)) {
            throw new IllegalArgumentException("密码必须是6-20位字母或数字");
        }

        // 确认密码校验
        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("请确认密码");
        }
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("两次密码输入不一致");
        }

        // 昵称校验（可选，但有值时不能为空）
        if (nickname != null && nickname.trim().isEmpty()) {
            nickname = null;
        }
        if (nickname != null && nickname.length() > 20) {
            throw new IllegalArgumentException("昵称不能超过20个字符");
        }
    }

    @Override
    public String toString() {
        return "RegisterRequest{" +
                "phone='" + phone + '\'' +
                ", nickname='" + nickname + '\'' +
                ", clientIp='" + clientIp + '\'' +
                '}';
    }
}
