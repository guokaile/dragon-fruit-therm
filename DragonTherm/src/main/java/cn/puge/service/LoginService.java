package cn.puge.service;

import cn.puge.entity.PugeUser;
import cn.puge.exception.BusinessException;
import cn.puge.request.LoginRequest;
import cn.puge.response.LoginResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 登录业务服务类
 * 核心业务逻辑，处理三种登录方式
 *
 * @author GuoKaiLe
 * @since 1.0
 */
public class LoginService {

    /**
     * 短信验证码服务
     */
    private final SmsCodeService smsCodeService;

    /**
     * 存放用户信息的Map集合
     * 运用静态的Map集合来模拟数据库中的数据
     */
    private static final Map<String, PugeUser> PUGE_USER_MAP = new ConcurrentHashMap<>();

    /**
     * 存放AccessToken的Map集合
     * key: token值
     * value: 用户ID
     */
    private static final Map<String, String> TOKEN_USER_MAP = new ConcurrentHashMap<>();

    /**
     * AccessToken有效时间（秒）
     * 默认2小时
     */
    private static final long ACCESS_TOKEN_EXPIRE_TIME = 2 * 60 * 60;

    /**
     * RefreshToken有效时间（秒）
     * 默认7天
     */
    private static final long REFRESH_TOKEN_EXPIRE_TIME = 7 * 24 * 60 * 60;

    /**
     * 静态代码块，初始化模拟用户数据
     */
    static {
        // 模拟用户一
        PugeUser pugeUser = new PugeUser();
        pugeUser.setUserId("U001");
        pugeUser.setPhone("13546069966");
        pugeUser.setPassword("123456");
        pugeUser.setNickname("火龙果用户一");
        pugeUser.setCreateTime(System.currentTimeMillis());
        PUGE_USER_MAP.put("13546069966", pugeUser);

        // 模拟用户二
        PugeUser pugeUser1 = new PugeUser();
        pugeUser1.setUserId("U002");
        pugeUser1.setPhone("13935193040");
        pugeUser1.setPassword("654321");
        pugeUser1.setNickname("火龙果用户二");
        pugeUser1.setCreateTime(System.currentTimeMillis());
        PUGE_USER_MAP.put("13935193040", pugeUser1);

        // 模拟微信用户（用于微信登录测试）
        PugeUser wechatUser = new PugeUser();
        wechatUser.setUserId("W001");
        wechatUser.setWechatOpenId("oABC123DEF456");
        wechatUser.setNickname("微信用户");
        wechatUser.setAvatarUrl("https://example.com/avatar.jpg");
        wechatUser.setCreateTime(System.currentTimeMillis());
        // 使用特殊前缀存储微信用户
        PUGE_USER_MAP.put("WECHAT_oABC123DEF456", wechatUser);
    }

    /**
     * 构造函数
     */
    public LoginService() {
        this.smsCodeService = new SmsCodeService();
    }

    /**
     * 用户登录入口
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    public LoginResponse login(LoginRequest request) {
        try {
            // 参数校验
            request.validate();

            // 根据登录类型分发处理
            LoginRequest.LoginType loginType = LoginRequest.LoginType.getByCode(request.getLoginType());
            if (loginType == null) {
                return LoginResponse.fail(400, "不支持的登录类型");
            }

            switch (loginType) {
                case SMS_CODE:
                    return handleSmsCodeLogin(request);
                case WECHAT:
                    return handleWechatLogin(request);
                case PASSWORD:
                    return handlePasswordLogin(request);
                default:
                    return LoginResponse.fail(400, "登录类型处理异常");
            }
        } catch (BusinessException e) {
            return LoginResponse.fail(400, e.getMessage());
        } catch (IllegalArgumentException e) {
            return LoginResponse.fail(400, e.getMessage());
        } catch (Exception e) {
            return LoginResponse.fail(500, "系统异常，请稍后重试");
        }
    }

    /**
     * 处理手机号验证码登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    private LoginResponse handleSmsCodeLogin(LoginRequest request) {
        String phone = request.getPhone();
        String smsCode = request.getSmsCode();

        // 校验验证码
        smsCodeService.verifyCode(phone, smsCode);

        // 查询用户（不存在则自动注册）
        PugeUser user = getUserByPhone(phone);
        if (user == null) {
            user = registerUser(phone, null, null);
        }

        // 生成令牌并返回
        return generateLoginResponse(user);
    }

    /**
     * 处理微信登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    private LoginResponse handleWechatLogin(LoginRequest request) {
        String wechatOpenId;

        // 如果请求中已有OpenId，直接使用
        if (request.getWechatOpenId() != null && !request.getWechatOpenId().trim().isEmpty()) {
            wechatOpenId = request.getWechatOpenId();
        } else {
            // 否则通过授权码换取OpenId（模拟实现）
            wechatOpenId = exchangeCodeForOpenId(request.getWechatCode());
        }

        // 查询微信用户
        PugeUser user = getUserByWechatOpenId(wechatOpenId);
        if (user == null) {
            // 模拟微信授权获取用户信息
            user = registerWechatUser(wechatOpenId);
        }

        // 生成令牌并返回
        return generateLoginResponse(user);
    }

    /**
     * 处理账号密码登录
     *
     * @param request 登录请求
     * @return 登录响应
     */
    private LoginResponse handlePasswordLogin(LoginRequest request) {
        String phone = request.getPhone();
        String password = request.getPassword();

        // 查询用户
        PugeUser user = getUserByPhone(phone);
        if (user == null) {
            return LoginResponse.fail(400, "用户不存在");
        }

        // 校验密码
        if (!password.equals(user.getPassword())) {
            return LoginResponse.fail(400, "密码错误");
        }

        // 生成令牌并返回
        return generateLoginResponse(user);
    }

    /**
     * 生成登录响应
     *
     * @param user 用户信息
     * @return 登录响应
     */
    private LoginResponse generateLoginResponse(PugeUser user) {
        // 生成AccessToken
        String accessToken = generateToken();

        // 生成RefreshToken
        String refreshToken = generateToken();

        // 存储Token映射
        TOKEN_USER_MAP.put(accessToken, user.getUserId());

        return LoginResponse.success(user, accessToken, refreshToken, ACCESS_TOKEN_EXPIRE_TIME);
    }

    /**
     * 生成随机令牌
     *
     * @return UUID格式的令牌
     */
    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息，不存在返回null
     */
    private PugeUser getUserByPhone(String phone) {
        return PUGE_USER_MAP.get(phone);
    }

    /**
     * 根据微信OpenId查询用户
     *
     * @param wechatOpenId 微信OpenId
     * @return 用户信息，不存在返回null
     */
    private PugeUser getUserByWechatOpenId(String wechatOpenId) {
        return PUGE_USER_MAP.get("WECHAT_" + wechatOpenId);
    }

    /**
     * 注册新用户（手机号注册）
     *
     * @param phone        手机号
     * @param password     密码（可为null）
     * @param wechatOpenId 微信OpenId（可为null）
     * @return 注册的用户信息
     */
    private PugeUser registerUser(String phone, String password, String wechatOpenId) {
        PugeUser user = new PugeUser();
        user.setUserId("U" + System.currentTimeMillis());
        user.setPhone(phone);
        user.setPassword(password);
        user.setWechatOpenId(wechatOpenId);
        user.setNickname("用户" + phone.substring(phone.length() - 4));
        user.setCreateTime(System.currentTimeMillis());

        PUGE_USER_MAP.put(phone, user);
        return user;
    }

    /**
     * 注册微信用户（模拟）
     *
     * @param wechatOpenId 微信OpenId
     * @return 注册的用户信息
     */
    private PugeUser registerWechatUser(String wechatOpenId) {
        PugeUser user = new PugeUser();
        user.setUserId("W" + System.currentTimeMillis());
        user.setWechatOpenId(wechatOpenId);
        user.setNickname("微信用户" + wechatOpenId.substring(wechatOpenId.length() - 4));
        user.setAvatarUrl("https://example.com/default-avatar.jpg");
        user.setCreateTime(System.currentTimeMillis());

        PUGE_USER_MAP.put("WECHAT_" + wechatOpenId, user);
        return user;
    }

    /**
     * 模拟微信授权码换取OpenId
     * 生产环境中应调用微信API
     *
     * @param code 微信授权码
     * @return OpenId
     */
    private String exchangeCodeForOpenId(String code) {
        // 模拟实现：实际应调用微信接口
        // 这里简单模拟返回一个OpenId
        System.out.println("【微信授权模拟】通过授权码 " + code + " 换取OpenId");
        return "oSIMPLE" + code.hashCode() % 1000000;
    }

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @return 发送的验证码
     */
    public String sendSmsCode(String phone) {
        return smsCodeService.sendCode(phone);
    }

    /**
     * 验证Token是否有效
     *
     * @param token 访问令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        return TOKEN_USER_MAP.containsKey(token);
    }

    /**
     * 根据Token获取用户ID
     *
     * @param token 访问令牌
     * @return 用户ID，不存在返回null
     */
    public String getUserIdByToken(String token) {
        return TOKEN_USER_MAP.get(token);
    }

    /**
     * 获取用户总数（用于测试）
     *
     * @return 用户数量
     */
    public int getUserCount() {
        return PUGE_USER_MAP.size();
    }
}
